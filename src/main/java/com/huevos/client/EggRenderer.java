package com.huevos.client;

import com.huevos.EggEntity;
import com.huevos.Huevos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class EggRenderer extends EntityRenderer<EggEntity> {
    /** Escala del huevo en el mundo (el modelo mide ~1.3 bloques). */
    public static final float SCALE = 0.45f;

    public EggRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(EggEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(0, 0.5, 0);
        ItemStack st = new ItemStack(Huevos.EGG_ITEMS.get(e.getColor()).get());
        Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.NONE, light,
                OverlayTexture.NO_OVERLAY, ps, buf, e.level(), e.getId());
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EggEntity e) { return InventoryMenu.BLOCK_ATLAS; }
}
