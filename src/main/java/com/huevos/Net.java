package com.huevos;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String V = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Huevos.ID, "main"), () -> V, V::equals, V::equals);

    public static void register() {
        CH.registerMessage(0, BoardPacket.class, BoardPacket::encode, BoardPacket::decode, BoardPacket::handle);
    }
}
