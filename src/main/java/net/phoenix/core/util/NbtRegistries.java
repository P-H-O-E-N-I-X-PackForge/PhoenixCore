package net.phoenix.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

public final class NbtRegistries {

    public static final HolderLookup.Provider PROVIDER = RegistryAccess
            .fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private NbtRegistries() {}
}
