package com.huevos.client;

import com.huevos.VestItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Dibuja el chaleco (item en la ranura de pecho) sobre el torso del jugador. */
public class VestLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // Ajusta aquí si el chaleco queda desalineado en el cuerpo:
    public static final float SCALE = 0.9f;
    public static final float OFFSET_X = 0f, OFFSET_Y = 0f, OFFSET_Z = 0f; // en bloques (y hacia abajo)

    public VestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack st = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!(st.getItem() instanceof VestItem) || p.isInvisible()) return;
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        ps.translate(OFFSET_X, 0.375 + OFFSET_Y, OFFSET_Z);   // centro del torso
        ps.mulPose(Axis.XP.rotationDegrees(180f));            // y hacia arriba
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(0, 0.344, 0);                            // centra el modelo del chaleco
        Minecraft.getInstance().getItemRenderer().renderStatic(st, ItemDisplayContext.NONE, light,
                OverlayTexture.NO_OVERLAY, ps, buf, p.level(), p.getId());
        ps.popPose();
    }
}
