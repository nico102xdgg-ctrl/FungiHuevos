package com.huevos.client;

import com.huevos.Huevos;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Huevos.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Huevos.EGG.get(), EggRenderer::new);
        for (var en : Huevos.DUMMIES.entrySet()) {
            String c = en.getKey();
            e.registerEntityRenderer(en.getValue().get(), ctx -> new DummyRenderer(ctx, c));
        }
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            PlayerRenderer pr = e.getSkin(skin);
            if (pr != null) pr.addLayer(new VestLayer(pr));
        }
    }

    @SubscribeEvent
    public static void gui(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("tabla", BoardOverlay::render);
    }
}
