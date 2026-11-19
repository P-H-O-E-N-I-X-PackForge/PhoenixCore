package net.phoenix.core.api.pattern;

import com.gregtechceu.gtceu.api.multiblock.error.PatternErrorType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.phoenix.core.PhoenixCore;

/** The pattern error types Core adds to GT's registry. */
public final class PhoenixPatternErrors {

    private static final DeferredRegister<PatternErrorType> TYPES = DeferredRegister
            .create(GTRegistries.Keys.PATTERN_ERROR_TYPE, PhoenixCore.MOD_ID);

    public static final DeferredHolder<PatternErrorType, PatternErrorType> PHOENIX_ERROR = TYPES.register(
            "phoenix_error",
            () -> new PatternErrorType(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "phoenix_error"),
                    PhoenixPatternError.CODEC));

    private PhoenixPatternErrors() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
