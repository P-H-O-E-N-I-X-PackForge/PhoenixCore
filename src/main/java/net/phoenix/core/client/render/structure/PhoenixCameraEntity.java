package net.phoenix.core.client.render.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class PhoenixCameraEntity extends Entity {

    public PhoenixCameraEntity(Level world) {
        super(EntityType.PIG, world);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return net.minecraft.world.entity.EntityDimensions.fixed(0.1f, 0.1f).withEyeHeight(0f);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag tag) {}
}
