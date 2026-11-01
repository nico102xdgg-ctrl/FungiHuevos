package com.huevos;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Huevo en mano: mantén click derecho para cargar y suelta para lanzarlo. */
public class EggItem extends Item {
    public final String color;

    // Ajustes del lanzamiento
    public static final int FULL_CHARGE_TICKS = 20;   // 1 s para carga completa
    public static final double MIN_SPEED = 0.5;       // tiro flojo
    public static final double MAX_SPEED = 1.35;      // tiro con carga completa

    public EggItem(String color) {
        super(new Item.Properties().stacksTo(1));
        this.color = color;
    }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(p.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack st, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player p)) return;
        int charged = getUseDuration(st) - timeLeft;
        double power = Math.min(1.0, charged / (double) FULL_CHARGE_TICKS);
        double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * power;
        if (!level.isClientSide) {
            EggEntity egg = new EggEntity(Huevos.EGG.get(), level);
            egg.setColor(color);
            Vec3 look = p.getLookAngle();
            egg.setPos(p.getX() + look.x * 0.5, p.getEyeY() - 0.3, p.getZ() + look.z * 0.5);
            egg.setDeltaMovement(look.x * speed, look.y * speed + 0.12, look.z * speed);
            egg.setPickupDelay(15);
            level.addFreshEntity(egg);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.6f, 0.7f + (float) power * 0.3f);
        }
        if (!p.getAbilities().instabuild) st.shrink(1);
    }

    /** No se puede tirar con Q: el huevo solo se lanza cargando. */
    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) { return false; }
}
