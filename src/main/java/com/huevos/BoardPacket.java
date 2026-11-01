package com.huevos;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BoardPacket {
    public final boolean show;
    public final List<String> colors;
    public final List<Integer> scores;
    public final int seconds;

    public BoardPacket(boolean show, List<String> colors, List<Integer> scores, int seconds) {
        this.show = show; this.colors = colors; this.scores = scores; this.seconds = seconds;
    }

    public static void encode(BoardPacket p, FriendlyByteBuf b) {
        b.writeBoolean(p.show);
        b.writeVarInt(p.seconds);
        b.writeVarInt(p.colors.size());
        for (int i = 0; i < p.colors.size(); i++) {
            b.writeUtf(p.colors.get(i));
            b.writeVarInt(p.scores.get(i));
        }
    }

    public static BoardPacket decode(FriendlyByteBuf b) {
        boolean show = b.readBoolean();
        int sec = b.readVarInt();
        int n = b.readVarInt();
        List<String> c = new ArrayList<>();
        List<Integer> s = new ArrayList<>();
        for (int i = 0; i < n; i++) { c.add(b.readUtf()); s.add(b.readVarInt()); }
        return new BoardPacket(show, c, s, sec);
    }

    public static void handle(BoardPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientData.set(p.show, p.colors, p.scores, p.seconds));
        ctx.get().setPacketHandled(true);
    }
}
