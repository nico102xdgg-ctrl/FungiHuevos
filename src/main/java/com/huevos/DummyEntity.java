package com.huevos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** dummy_(color): maniquí decorativo con chaleco. Ya no asigna equipos (usa /huevos team). */
public class DummyEntity extends Entity {
    private final String color;

    public DummyEntity(EntityType<?> type, Level level, String color) {
        super(type, level);
        this.color = color;
    }

    public String getColor() { return color; }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return true; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player p && p.isCreative()) { discard(); return true; }
        return false;
    }
}
