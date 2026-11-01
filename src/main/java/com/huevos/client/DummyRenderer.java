package com.huevos.client;

import com.huevos.DummyEntity;
import com.huevos.Huevos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Maniquí gris sobre una base roja, con el chaleco de su color puesto. */
public class DummyRenderer extends EntityRenderer<DummyEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(Huevos.ID, "textures/entity/dummy.png");
    public static final float VEST_SCALE = 0.9f; // ajusta si el chaleco no calza en el torso

    private final String color;
    private final PlayerModel<LivingEntity> model;

    public DummyRenderer(EntityRendererProvider.Context ctx, String color) {
        super(ctx);
        this.color = color;
        this.model = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(DummyEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();

        // base roja
        ps.pushPose();
        ps.translate(-0.4, 0, -0.4);
        ps.scale(0.8f, 0.06f, 0.8f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.RED_TERRACOTTA.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();

        // maniquí
        ps.translate(0, 0.06, 0);
        ps.mulPose(Axis.YP.rotationDegrees(180f - e.getYRot()));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0, -1.501, 0);
        model.young = false;
        model.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        // chaleco sobre el torso
        ps.pushPose();
        model.body.translateAndRotate(ps);
        ps.translate(0, 0.375, 0);
        ps.mulPose(Axis.XP.rotationDegrees(180f));
        ps.scale(VEST_SCALE, VEST_SCALE, VEST_SCALE);
        ps.translate(0, 0.344, 0);
        ItemStack st = new ItemStack(Huevos.VESTS.get(color).get());
        Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.NONE, light,
                OverlayTexture.NO_OVERLAY, ps, buf, e.level(), e.getId());
        ps.popPose();

        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DummyEntity e) { return TEX; }
}
