package net.phoenix.core.common.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.jetbrains.annotations.Nullable;

public class TetoSlime extends Slime {

    public TetoSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        targetSelector.removeAllGoals(goal -> true);
        goalSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().equals("SlimeAttackGoal"));
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (reason != MobSpawnType.SPAWN_EGG && reason != MobSpawnType.COMMAND) {
            float roll = level.getRandom().nextFloat();
            setSize(roll < 0.55f ? 1 : roll < 0.90f ? 2 : 4, true);
        }
        return data;
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.NOTE;
    }
}
