package net.phoenix.core.mixin.accessor;

import net.minecraft.world.entity.player.Abilities;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Abilities.class)
public interface AbilitiesAccessor {

    @Accessor("flyingSpeed")
    float getFlyingSpeed();

    @Accessor("flyingSpeed") // Must match the MCP/Yarn/Mojang field name
    void setFlyingSpeed(float speed);
}
