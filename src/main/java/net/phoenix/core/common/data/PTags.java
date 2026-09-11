package net.phoenix.core.common.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("removal")
public class PTags {

    public static final TagKey<Item> FLOWERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "flowers"));
    public static final TagKey<Item> CROPS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "crops"));
    public static final TagKey<Item> MUSHROOMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "mushrooms"));
    public static final TagKey<Item> LOGS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("minecraft", "logs"));
    public static final TagKey<Item> PLANKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "planks"));
    public static final TagKey<Block> SOUL_FLOWERS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "soul_flowers"));

    /** Which multiblock controller blocks a Tier 1 / Tier 2 Cinder Forge is allowed to target -
     *  empty by default, meant to be filled in with real multiblock controller block IDs later. Tier
     *  3 is deliberately unrestricted (no tag gating at all). */
    public static final TagKey<Block> CINDER_FORGE_TIER1_ALLOWED = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "cinder_forge/tier1_allowed"));
    public static final TagKey<Block> CINDER_FORGE_TIER2_ALLOWED = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "cinder_forge/tier2_allowed"));
}
