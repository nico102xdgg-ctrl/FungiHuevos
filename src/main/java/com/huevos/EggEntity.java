package com.huevos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class EggEntity extends Entity {
    private static final EntityDataAccessor<String> COLOR =
            SynchedEntityData.defineId(EggEntity.class, EntityDataSerializers.STRING);

    private int pickupDelay = 0;
    private boolean settled = false;

    public EggEntity(EntityType<?> type, Level level) { super(type, level); }

    public String getColor() { return this.entityData.get(COLOR); }

    public void setColor(String c) {
        this.entityData.set(COLOR, Huevos.EGG_ITEMS.containsKey(c) ? c : "blanco");
    }

    public void setPickupDelay(int t) { this.pickupDelay = t; }
    public boolean canPickup() { return pickupDelay <= 0; }

    @Override protected void defineSynchedData() { this.entityData.define(COLOR, "blanco"); }
    @Override protected void readAdditionalSaveData(CompoundTag t) { setColor(t.getString("Color")); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { t.putString("Color", getColor()); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return true; }

    /** Física simple (solo servidor): cae, rebota poco y se queda quieto. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (pickupDelay > 0) pickupDelay--;
        if (settled) return;

        Vec3 m = getDeltaMovement().add(0, -0.04, 0);
        setDeltaMovement(m);
        move(MoverType.SELF, m);

        Vec3 n = getDeltaMovement();
        double f = onGround() ? 0.5 : 0.98;
        n = new Vec3(n.x * f, n.y * 0.98, n.z * f);
        setDeltaMovement(n);

        if (onGround() && n.horizontalDistanceSqr() < 1.0E-3) {
            settled = true;
            setDeltaMovement(Vec3.ZERO);
        }
        hasImpulse = true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            GameManager.tryPickup(sp, this);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player p && p.isCreative()) { discard(); return true; }
        return false;
    }
}
