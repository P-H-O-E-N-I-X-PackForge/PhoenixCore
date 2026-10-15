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
    public float getEyeHeight(net.minecraft.world.entity.Pose pose,
                              net.minecraft.world.entity.EntityDimensions dimensions) {
        return 0f;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag tag) {}
}
