package net.phoenix.core.common.machine.multiblock.ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;

public enum WardMode {

    NONE,
    HOSTILE,
    PASSIVE,
    NEUTRAL,
    ALL;

    public boolean matches(Entity entity) {
        return switch (this) {
            case NONE -> false;
            case HOSTILE -> entity instanceof Monster;
            case PASSIVE -> entity instanceof Animal;
            case NEUTRAL -> entity instanceof NeutralMob;
            case ALL -> entity instanceof Mob;
        };
    }
}
