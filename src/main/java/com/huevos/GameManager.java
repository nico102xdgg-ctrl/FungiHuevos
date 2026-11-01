package com.huevos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = Huevos.ID)
public final class GameManager {
    public enum State { IDLE, RUNNING, ENDED }

    public static final int GAME_TICKS = 10 * 60 * 20;      // 10 minutos

    static State state = State.IDLE;
    static int ticks = 0;
    static Map<String, Integer> scores = new LinkedHashMap<>();

    private GameManager() {}

    // ------------------------------------------------------------ eventos
    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) { GameConfig.load(); state = State.IDLE; ticks = 0; scores = new LinkedHashMap<>(); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { state = State.IDLE; }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            boolean show = state != State.IDLE;
            Net.CH.send(PacketDistributor.PLAYER.with(() -> sp), packet(show));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || state != State.RUNNING) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ticks--;
        if (ticks % 10 == 0) {
            scores = countScores(server);
            if (ticks % 20 == 0) sync(true);
        }
        if (ticks <= 0) end(server, true);
    }

    /** Si un jugador con huevo muere, el huevo queda en el suelo en vez de perderse. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Iterator<ItemEntity> it = e.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            if (ie.getItem().getItem() instanceof EggItem ei) {
                spawnEgg(sp.serverLevel(), ei.color, sp.getX(), sp.getY(), sp.getZ());
                it.remove();
            }
        }
    }

    /** Click derecho a un jugador que lleva un huevo = se lo quitas (equipos distintos). */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (state != State.RUNNING) return;
        if (!(e.getEntity() instanceof ServerPlayer thief) || !(e.getTarget() instanceof ServerPlayer victim)) return;
        String a = teamOf(thief), b = teamOf(victim);
        if (a == null || b == null || a.equals(b) || !Teams.valid(a) || !Teams.valid(b)) return;
        if (carrying(thief)) {
            thief.displayClientMessage(Component.literal("Solo puedes llevar un huevo."), true);
            return;
        }
        String color = takeEgg(victim);
        if (color == null) return;
        giveEgg(thief, color);
        victim.displayClientMessage(Component.literal("¡Te quitaron el huevo!").withStyle(ChatFormatting.RED), true);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ------------------------------------------------------------ flujo
    static void start(MinecraftServer server) {
        state = State.RUNNING;
        ticks = GAME_TICKS;
        scores = countScores(server);
        sync(true);
    }

    static void end(MinecraftServer server, boolean natural) {
        if (state != State.RUNNING) return;
        state = State.ENDED;
        scores = countScores(server);
        sync(true);
        stopSound(server);
        if (natural) {
            String loser = null; int min = Integer.MAX_VALUE; boolean tie = false;
            for (String c : Huevos.COLORS) {
                int s = scores.getOrDefault(c, 0);
                if (s < min) { min = s; loser = c; tie = false; } else if (s == min) tie = true;
            }
            if (tie) server.getPlayerList().broadcastSystemMessage(
                    Component.literal("¡Terminó el tiempo! Empate en el último lugar.").withStyle(ChatFormatting.GOLD), false);
            else server.getPlayerList().broadcastSystemMessage(
                    Component.literal("¡Terminó el tiempo! Equipo eliminado: " + loser + "  (/huevos kill " + loser + ")")
                            .withStyle(Teams.fmt(loser)), false);
        }
    }

    static void stop(MinecraftServer server) { end(server, false); }

    static void reset(MinecraftServer server) {
        state = State.IDLE; ticks = 0; scores = new LinkedHashMap<>();
        stopSound(server);
        for (EggEntity e : server.overworld().getEntities(EntityTypeTest.forClass(EggEntity.class), x -> true)) e.discard();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) takeEgg(p);
        Net.CH.send(PacketDistributor.ALL.noArg(), new BoardPacket(false, new ArrayList<>(), new ArrayList<>(), 0));
    }

    // ------------------------------------------------------------ mecánicas
    public static boolean tryPickup(ServerPlayer p, EggEntity egg) {
        if (state != State.RUNNING || !egg.canPickup()) return false;
        String t = teamOf(p);
        if (t == null || !Teams.valid(t)) {
            p.displayClientMessage(Component.literal("No estás en ningún equipo."), true);
            return false;
        }
        if (carrying(p)) {
            p.displayClientMessage(Component.literal("Solo puedes llevar un huevo."), true);
            return false;
        }
        AABB own = GameConfig.zone(t);
        if (own != null && own.contains(egg.position())) {
            p.displayClientMessage(Component.literal("No puedes tomar huevos de tu propia base."), true);
            return false;
        }
        giveEgg(p, egg.getColor());
        egg.discard();
        return true;
    }

    public static boolean carrying(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
            if (inv.getItem(i).getItem() instanceof EggItem) return true;
        return false;
    }

    /** Quita el huevo del inventario y devuelve su color (o null si no tenía). */
    static String takeEgg(ServerPlayer p) {
        Inventory inv = p.getInventory();
        String color = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof EggItem ei) {
                color = ei.color;
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return color;
    }

    /** Pone el huevo en la mano principal (lo que tenía en la mano pasa a otro espacio). */
    static void giveEgg(ServerPlayer p, String color) {
        ItemStack egg = new ItemStack(Huevos.EGG_ITEMS.get(color).get());
        ItemStack hand = p.getMainHandItem();
        if (!hand.isEmpty()) {
            Inventory inv = p.getInventory();
            ItemStack copy = hand.copy();
            boolean placed = false;
            for (int i = 0; i < 36; i++) {
                if (i != inv.selected && inv.getItem(i).isEmpty()) { inv.setItem(i, copy); placed = true; break; }
            }
            if (!placed) p.drop(copy, false);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, egg);
    }

    static void spawnEgg(ServerLevel lv, String color, double x, double y, double z) {
        EggEntity e = new EggEntity(Huevos.EGG.get(), lv);
        e.setColor(color);
        e.setPos(x, y, z);
        lv.addFreshEntity(e);
    }

    static Map<String, Integer> countScores(MinecraftServer server) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String c : Huevos.COLORS) m.put(c, 0);
        for (EggEntity e : server.overworld().getEntities(EntityTypeTest.forClass(EggEntity.class), x -> true)) {
            for (String c : Huevos.COLORS) {
                AABB z = GameConfig.zone(c);
                if (z != null && z.contains(e.position())) { m.merge(c, 1, Integer::sum); break; }
            }
        }
        return m;
    }

    // ------------------------------------------------------------ utilidades
    public static String teamOf(ServerPlayer p) {
        Team t = p.getTeam();
        return t == null ? null : t.getName();
    }

    private static BoardPacket packet(boolean show) {
        List<String> c = new ArrayList<>(Arrays.asList(Huevos.COLORS));
        List<Integer> s = new ArrayList<>();
        for (String x : c) s.add(scores.getOrDefault(x, 0));
        return new BoardPacket(show, c, s, (Math.max(ticks, 0) + 19) / 20);
    }

    static void sync(boolean show) { Net.CH.send(PacketDistributor.ALL.noArg(), packet(show)); }

    private static void stopSound(MinecraftServer s) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), "stopsound @a");
    }

    private static PlayerTeam getOrCreateTeam(Scoreboard sb, String color) {
        PlayerTeam t = sb.getPlayerTeam(color);
        if (t == null) {
            t = sb.addPlayerTeam(color);
            t.setColor(Teams.fmt(color));
        }
        return t;
    }

    // ------------------------------------------------------------ comandos
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("huevos").requires(s -> s.hasPermission(2))
                .then(Commands.literal("start").executes(c -> {
                    if (state == State.RUNNING) return fail(c, "Ya hay una partida en curso.");
                    start(c.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("stop").executes(c -> {
                    if (state != State.RUNNING) return fail(c, "No hay partida en curso.");
                    stop(c.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    reset(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Juego reiniciado."), true);
                    return 1;
                }))
                .then(Commands.literal("team")
                        .then(Commands.literal("join").then(Commands.argument("color", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Huevos.COLORS, b))
                                .then(Commands.argument("player", EntityArgument.players()).executes(c -> {
                                    String col = StringArgumentType.getString(c, "color");
                                    if (!Teams.valid(col)) return fail(c, "Color inválido (rojo, azul, verde, amarillo).");
                                    Scoreboard sb = c.getSource().getServer().getScoreboard();
                                    PlayerTeam t = getOrCreateTeam(sb, col);
                                    int n = 0;
                                    for (ServerPlayer p : EntityArgument.getPlayers(c, "player")) {
                                        sb.addPlayerToTeam(p.getScoreboardName(), t);
                                        n++;
                                    }
                                    int fn = n;
                                    c.getSource().sendSuccess(() -> Component.literal(fn + " jugador(es) al equipo " + col), true);
                                    return n;
                                }))))
                        .then(Commands.literal("leave").then(Commands.argument("player", EntityArgument.players()).executes(c -> {
                            Scoreboard sb = c.getSource().getServer().getScoreboard();
                            int n = 0;
                            for (ServerPlayer p : EntityArgument.getPlayers(c, "player")) {
                                Team t = p.getTeam();
                                if (t instanceof PlayerTeam pt && Teams.valid(pt.getName())) {
                                    sb.removePlayerFromTeam(p.getScoreboardName(), pt);
                                    n++;
                                }
                            }
                            int fn = n;
                            c.getSource().sendSuccess(() -> Component.literal(fn + " jugador(es) sacados de su equipo"), true);
                            return n;
                        }))))
                .then(Commands.literal("kill").then(Commands.argument("color", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Huevos.COLORS, b))
                        .executes(c -> {
                            String col = StringArgumentType.getString(c, "color");
                            if (!Teams.valid(col)) return fail(c, "Color inválido (rojo, azul, verde, amarillo).");
                            int n = 0;
                            for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers())
                                if (col.equals(teamOf(p))) { p.kill(); n++; }
                            int fn = n;
                            c.getSource().sendSuccess(() -> Component.literal("Eliminados " + fn + " jugadores del equipo " + col), true);
                            return n;
                        })))
                .then(Commands.literal("spawn").executes(c -> {
                    ServerLevel lv = c.getSource().getServer().overworld();
                    int n = 0;
                    for (int[] p : GameConfig.INSTANCE.spawns) { spawnEgg(lv, "blanco", p[0] + 0.5, p[1], p[2] + 0.5); n++; }
                    int fn = n;
                    c.getSource().sendSuccess(() -> Component.literal("Huevos blancos spawneados: " + fn), true);
                    return n;
                }))
                .then(Commands.literal("base").then(Commands.argument("color", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Huevos.COLORS, b))
                        .then(Commands.argument("desde", BlockPosArgument.blockPos())
                                .then(Commands.argument("hasta", BlockPosArgument.blockPos()).executes(c -> {
                                    String col = StringArgumentType.getString(c, "color");
                                    if (!Teams.valid(col)) return fail(c, "Color inválido.");
                                    BlockPos a = BlockPosArgument.getBlockPos(c, "desde");
                                    BlockPos b = BlockPosArgument.getBlockPos(c, "hasta");
                                    GameConfig.INSTANCE.bases.put(col, new int[]{a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()});
                                    GameConfig.save();
                                    c.getSource().sendSuccess(() -> Component.literal("Base " + col + " guardada."), true);
                                    return 1;
                                })))))
                .then(Commands.literal("spawnpoint")
                        .then(Commands.literal("add")
                                .executes(c -> addSpawn(c, BlockPos.containing(c.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(c -> addSpawn(c, BlockPosArgument.getBlockPos(c, "pos")))))
                        .then(Commands.literal("clear").executes(c -> {
                            GameConfig.INSTANCE.spawns.clear();
                            GameConfig.save();
                            c.getSource().sendSuccess(() -> Component.literal("Puntos de huevos borrados."), true);
                            return 1;
                        }))));
    }

    private static int addSpawn(CommandContext<CommandSourceStack> c, BlockPos p) {
        GameConfig.INSTANCE.spawns.add(new int[]{p.getX(), p.getY(), p.getZ()});
        GameConfig.save();
        int n = GameConfig.INSTANCE.spawns.size();
        c.getSource().sendSuccess(() -> Component.literal("Punto #" + n + " agregado en " + p.toShortString()), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
