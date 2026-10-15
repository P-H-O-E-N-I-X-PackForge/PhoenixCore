package net.phoenix.core.integration.continuum.pdim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.continuum.data.ContinuumBody;

import org.jetbrains.annotations.Nullable;

public final class PdimDimensions {

    private PdimDimensions() {}

    public static final String PREFIX = "pdim/";

    public static final int PLOT_SPACING = 1024;
    public static final int PLATFORM_Y = 64;

    public static final float MIN_GRAVITY = 0.1f;
    public static final float MAX_GRAVITY = 3.0f;

    public static ResourceKey<Level> keyFor(ResourceLocation body) {
        return ResourceKey.create(Registries.DIMENSION, PhoenixCore.id(PREFIX + body.getPath()));
    }

    public static @Nullable ResourceLocation bodyOf(ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        if (!PhoenixCore.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith(PREFIX)) return null;
        return PhoenixCore.id(id.getPath().substring(PREFIX.length()));
    }

    public static boolean isPersonal(ResourceKey<Level> dimension) {
        return bodyOf(dimension) != null;
    }

    public static BlockPos plotOrigin(int plot) {
        return new BlockPos((plot % 32) * PLOT_SPACING, PLATFORM_Y, (plot / 32) * PLOT_SPACING);
    }

    public static float clampGravity(float gravity) {
        return Math.max(MIN_GRAVITY, Math.min(MAX_GRAVITY, gravity));
    }

    public static float naturalGravity(ContinuumBody body) {
        return switch (body.type()) {
            case STAR -> 2.0f;
            case BLACK_HOLE -> 3.0f;
            default -> clampGravity((float) Math.pow(Math.max(0.05, body.radius()), 0.9));
        };
    }
}
