package net.phoenix.core.integration.gregpacks.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.phoenix.core.integration.gregpacks.common.registry.GregPacksBlocks;

import java.util.List;
import java.util.Set;

public class GregPacksLootTableProvider {

    public static LootTableProvider create(PackOutput output,
                                           java.util.concurrent.CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(OmniPackBlockLoot::new,
                        LootContextParamSets.BLOCK)),
                registries);
    }

    static class OmniPackBlockLoot extends BlockLootSubProvider {

        protected OmniPackBlockLoot(net.minecraft.core.HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            add(GregPacksBlocks.BASIC_OMNIPACK_BLOCK.get(), omniPackDrop(GregPacksBlocks.BASIC_OMNIPACK_BLOCK.get()));
            add(GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.get(),
                    omniPackDrop(GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.get()));
            add(GregPacksBlocks.ELITE_OMNIPACK_BLOCK.get(), omniPackDrop(GregPacksBlocks.ELITE_OMNIPACK_BLOCK.get()));
        }

        private LootTable.Builder omniPackDrop(Block block) {
            return LootTable.lootTable().withPool(
                    LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(block)
                                    .apply(CopyCustomDataFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                            .copy("Items", "Items", CopyCustomDataFunction.MergeStrategy.REPLACE)
                                            .copy("Upgrades", "Upgrades", CopyCustomDataFunction.MergeStrategy.REPLACE)
                                            .copy("StoredEU", "StoredEU", CopyCustomDataFunction.MergeStrategy.REPLACE)
                                            .copy("Fluid", "Fluid", CopyCustomDataFunction.MergeStrategy.REPLACE))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                    GregPacksBlocks.BASIC_OMNIPACK_BLOCK.get(),
                    GregPacksBlocks.ADVANCED_OMNIPACK_BLOCK.get(),
                    GregPacksBlocks.ELITE_OMNIPACK_BLOCK.get());
        }
    }
}
