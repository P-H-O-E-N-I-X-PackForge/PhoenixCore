package net.phoenix.core.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * A registry lookup that exists on both sides at any time, for reading and writing item stacks held in saved data.
 * It covers the static registries only, which is all the stored stacks need.
 */
public final class NbtRegistries {

    public static final HolderLookup.Provider PROVIDER = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private NbtRegistries() {}
}
