package net.phoenix.core.common.item.cinder;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class CinderSchemaData {

    private CinderSchemaData() {}

    private static final String TARGET_KEY = "TargetMultiblock";
    private static final String SLICE_KEYS = "SliceRepeatKeys";
    private static final String SLICE_VALUES = "SliceRepeatValues";
    private static final String BLOCK_PREFERENCES = "BlockPreferences";

    public static void setTarget(ItemStack cinderCore, MultiblockMachineDefinition definition) {
        CompoundTag tag = cinderCore.getOrCreateTag();
        if (!definition.getId().equals(getTargetId(cinderCore))) {

            tag.remove(SLICE_KEYS);
            tag.remove(SLICE_VALUES);
            tag.remove(BLOCK_PREFERENCES);
        }
        tag.putString(TARGET_KEY, definition.getId().toString());
    }

    public static @Nullable ResourceLocation getTargetId(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        if (tag == null || !tag.contains(TARGET_KEY)) return null;
        return ResourceLocation.tryParse(tag.getString(TARGET_KEY));
    }

    public static void clearTarget(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        if (tag == null) return;
        tag.remove(TARGET_KEY);
        tag.remove(SLICE_KEYS);
        tag.remove(SLICE_VALUES);
        tag.remove(BLOCK_PREFERENCES);
    }

    public static @Nullable MultiblockMachineDefinition getTargetDefinition(ItemStack cinderCore) {
        ResourceLocation id = getTargetId(cinderCore);
        if (id == null) return null;
        MachineDefinition definition = GTRegistries.MACHINES.get(id);
        return definition instanceof MultiblockMachineDefinition multi ? multi : null;
    }

    private static @Nullable BlockPattern getPattern(MultiblockMachineDefinition definition) {
        var patternSupplier = definition.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (patternSupplier == null) return null;
        IBlockPattern pattern = patternSupplier.get();
        return pattern instanceof BlockPattern blockPattern ? blockPattern : null;
    }

    public static @Nullable MultiblockSchemaInfo resolveSchema(ItemStack cinderCore) {
        MultiblockMachineDefinition definition = getTargetDefinition(cinderCore);
        if (definition == null) return null;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return null;

        MultiblockSchemaInfo info = new MultiblockSchemaInfo();
        CompoundTag tag = cinderCore.getOrCreateTag();

        if (tag.contains(SLICE_KEYS) && tag.contains(SLICE_VALUES)) {
            int[] keys = tag.getIntArray(SLICE_KEYS);
            int[] values = tag.getIntArray(SLICE_VALUES);
            int length = Math.min(keys.length, values.length);
            for (int i = 0; i < length; i++) {
                info.getUserSliceRepeats().put(keys[i], values[i]);
            }
        } else {

            for (int i = 0; i < pattern.getSlices().length; i++) {
                info.getUserSliceRepeats().put(i, pattern.getSlices()[i].getMinRepeats());
            }
        }

        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);

        boolean hasBlockPreferences = tag.contains(BLOCK_PREFERENCES, Tag.TAG_LIST);
        if (hasBlockPreferences) {
            ListTag preferences = tag.getList(BLOCK_PREFERENCES, CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < preferences.size(); i++) {
                CompoundTag entry = preferences.getCompound(i);
                char c = (char) entry.getInt("Char");
                int baseIndex = entry.getInt("Base");
                int candidateIndex = entry.getInt("Candidate");

                MultiPredicate predicate = pattern.getPredicates().get(c);
                if (predicate == null || baseIndex < 0 || baseIndex >= predicate.predicates().size()) continue;
                BasePredicate base = predicate.predicates().get(baseIndex);
                if (candidateIndex < 0 || candidateIndex >= base.getCandidates().size()) continue;
                BlockInfo blockInfo = base.getCandidates().get(candidateIndex);

                info.putPredicatePreference(predicate, base, blockInfo);
            }

            info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        }

        return info;
    }

    public static boolean scanFromWorld(ItemStack cinderCore, Level level, BlockPos controllerPos) {
        if (!(MetaMachine.getMachine(level, controllerPos) instanceof MultiblockControllerMachine controller) ||
                !controller.isFormed()) {
            return false;
        }

        MultiblockMachineDefinition definition = controller.getDefinition();
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return false;

        Direction frontFacing = controller.getFrontFacing();
        Direction upFacing = controller.getUpwardsFacing();
        boolean flipped = controller.isFlipped();

        setTarget(cinderCore, definition);

        MultiblockSchemaInfo info = new MultiblockSchemaInfo();
        for (int i = 0; i < pattern.getSlices().length; i++) {
            info.getUserSliceRepeats().put(i, pattern.getSlices()[i].getMinRepeats());
        }
        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);

        var structureHelper = info.getStructureHelper();
        if (structureHelper == null) return false;

        BlockPos.MutableBlockPos origin = controllerPos.mutable();
        pattern.getOffset().apply(origin, frontFacing, upFacing, flipped);
        BlockPos originImmutable = origin.immutable();

        for (BlockPos localPos : info.getStructureBlocks().keySet()) {
            MultiPredicate predicate = structureHelper.getPredicateFromPos(pattern, localPos, Direction.NORTH,
                    Direction.UP, false);
            if (predicate == null || predicate.isAny() || predicate.isAir()) continue;

            Block realBlock = level.getBlockState(originImmutable.offset(localPos)).getBlock();

            for (BasePredicate base : predicate.expand()) {
                if (base.getCandidates().size() <= 1) continue;
                for (BlockInfo candidate : base.getCandidates()) {
                    if (candidate.getBlockState().getBlock() == realBlock) {
                        info.putPredicatePreference(predicate, base, candidate);
                        break;
                    }
                }
            }
        }

        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        writeUserPreferences(cinderCore, definition, info);
        return true;
    }

    public static boolean applyConfiguration(ItemStack cinderCore, int[] sliceKeys, int[] sliceValues,
                                             char[] prefChars, int[] prefBaseIndices, int[] prefCandidateIndices) {
        MultiblockMachineDefinition definition = getTargetDefinition(cinderCore);
        if (definition == null) return false;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return false;

        MultiblockSchemaInfo info = new MultiblockSchemaInfo();
        int sliceLength = Math.min(sliceKeys.length, sliceValues.length);
        for (int i = 0; i < sliceLength; i++) {
            info.getUserSliceRepeats().put(sliceKeys[i], sliceValues[i]);
        }

        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);

        int prefLength = Math.min(prefChars.length, Math.min(prefBaseIndices.length, prefCandidateIndices.length));
        for (int i = 0; i < prefLength; i++) {
            MultiPredicate predicate = pattern.getPredicates().get(prefChars[i]);
            if (predicate == null) continue;
            int baseIndex = prefBaseIndices[i];
            if (baseIndex < 0 || baseIndex >= predicate.predicates().size()) continue;
            BasePredicate base = predicate.predicates().get(baseIndex);
            int candidateIndex = prefCandidateIndices[i];
            if (candidateIndex < 0 || candidateIndex >= base.getCandidates().size()) continue;
            info.putPredicatePreference(predicate, base, base.getCandidates().get(candidateIndex));
        }
        if (prefLength > 0) {
            info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        }

        writeUserPreferences(cinderCore, definition, info);
        return true;
    }

    public static void writeUserPreferences(ItemStack cinderCore, MultiblockMachineDefinition definition,
                                            MultiblockSchemaInfo info) {
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return;
        CompoundTag tag = cinderCore.getOrCreateTag();

        Int2IntMap sliceRepeats = info.getUserSliceRepeats();
        if (sliceRepeats.isEmpty()) {
            tag.remove(SLICE_KEYS);
            tag.remove(SLICE_VALUES);
        } else {
            int[] keys = new int[sliceRepeats.size()];
            int[] values = new int[sliceRepeats.size()];
            int i = 0;
            for (var entry : sliceRepeats.int2IntEntrySet()) {
                keys[i] = entry.getIntKey();
                values[i] = entry.getIntValue();
                i++;
            }
            tag.putIntArray(SLICE_KEYS, keys);
            tag.putIntArray(SLICE_VALUES, values);
        }

        var structureHelper = info.getStructureHelper();
        var blockPreferences = structureHelper != null ? structureHelper.getBlockPreferences() : null;
        if (blockPreferences == null || blockPreferences.isEmpty()) {
            tag.remove(BLOCK_PREFERENCES);
        } else {
            ListTag preferences = new ListTag();
            for (var cell : blockPreferences.cellSet()) {
                MultiPredicate predicate = cell.getRowKey();
                BasePredicate base = cell.getColumnKey();
                BlockInfo chosen = cell.getValue();
                if (predicate == null || base == null || chosen == null) continue;

                var charEntry = pattern.getPredicates().char2ObjectEntrySet().stream()
                        .filter(e -> e.getValue().equals(predicate))
                        .findFirst().orElse(null);
                if (charEntry == null) continue;

                CompoundTag entry = new CompoundTag();
                entry.putInt("Char", charEntry.getCharKey());
                entry.putInt("Base", predicate.predicates().indexOf(base));
                entry.putInt("Candidate", base.getCandidates().indexOf(chosen));
                preferences.add(entry);
            }
            tag.put(BLOCK_PREFERENCES, preferences);
        }
    }

    public static void adjustSliceRepeat(ItemStack cinderCore, int sliceIndex, int delta) {
        MultiblockMachineDefinition definition = getTargetDefinition(cinderCore);
        if (definition == null) return;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null || sliceIndex < 0 || sliceIndex >= pattern.getSlices().length) return;

        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        if (info == null) return;

        var slice = pattern.getSlices()[sliceIndex];
        int current = info.getUserSliceRepeats().getOrDefault(sliceIndex, slice.getMinRepeats());
        int next = Math.max(slice.getMinRepeats(), Math.min(slice.getMaxRepeats(), current + delta));
        if (next == current) return;

        info.getUserSliceRepeats().put(sliceIndex, next);
        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        writeUserPreferences(cinderCore, definition, info);
    }

    public static void setPredicateCandidate(ItemStack cinderCore, char predicateChar, int baseIndex,
                                             int candidateIndex) {
        MultiblockMachineDefinition definition = getTargetDefinition(cinderCore);
        if (definition == null) return;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return;

        MultiPredicate predicate = pattern.getPredicates().get(predicateChar);
        if (predicate == null || baseIndex < 0 || baseIndex >= predicate.predicates().size()) return;
        BasePredicate base = predicate.predicates().get(baseIndex);
        var candidates = base.getCandidates();
        if (candidateIndex < 0 || candidateIndex >= candidates.size()) return;

        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        if (info == null) return;

        info.putPredicatePreference(predicate, base, candidates.get(candidateIndex));
        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        writeUserPreferences(cinderCore, definition, info);
    }

    public static int getPredicateCandidateIndex(ItemStack cinderCore, MultiPredicate predicate, BasePredicate base) {
        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        if (info == null || info.getStructureHelper() == null) return 0;
        BlockInfo current = info.getStructureHelper().getBlockPreferences().get(predicate, base);
        int index = current != null ? base.getCandidates().indexOf(current) : -1;
        return Math.max(0, index);
    }

    public record ResolvedPlacement(Map<BlockPos, BlockInfo> placements, Reference2IntMap<Block> blockCounts,
                                    BlockPos origin) {}

    public record PartialPlacement(Map<BlockPos, BlockInfo> placements, Reference2IntMap<Block> consumed) {}

    public static PartialPlacement resolvePartialPlacements(ItemStack cinderCore, ResolvedPlacement resolved) {
        Reference2IntMap<Block> required = resolved.blockCounts();
        Reference2IntMap<Block> budget = new Reference2IntOpenHashMap<>(tallyStocked(cinderCore));
        Map<BlockPos, BlockInfo> placements = new HashMap<>();
        Reference2IntMap<Block> consumed = new Reference2IntOpenHashMap<>();

        for (var entry : resolved.placements().entrySet()) {
            Block block = entry.getValue().getBlockState().getBlock();
            if (!required.containsKey(block)) {
                placements.put(entry.getKey(), entry.getValue());
                continue;
            }
            int have = budget.getOrDefault(block, 0);
            if (have <= 0) continue;
            budget.put(block, have - 1);
            consumed.merge(block, 1, Integer::sum);
            placements.put(entry.getKey(), entry.getValue());
        }
        return new PartialPlacement(placements, consumed);
    }

    public static @Nullable ResolvedPlacement resolvePlacement(ItemStack cinderCore, BlockPos anchor,
                                                               Direction frontFacing, Direction upFacing,
                                                               boolean isFlipped) {
        MultiblockMachineDefinition definition = getTargetDefinition(cinderCore);
        if (definition == null) return null;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return null;

        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        if (info == null) return null;
        info.refreshSchema(definition, frontFacing, upFacing, isFlipped, null);

        BlockPos.MutableBlockPos origin = anchor.mutable();
        pattern.getOffset().apply(origin, frontFacing, upFacing, isFlipped);
        BlockPos originImmutable = origin.immutable();

        Map<BlockPos, BlockInfo> placements = new HashMap<>();
        for (var entry : info.getStructureBlocks().entrySet()) {
            placements.put(originImmutable.offset(entry.getKey()), entry.getValue());
        }
        return new ResolvedPlacement(placements, new Reference2IntOpenHashMap<>(info.getBlockCounts()),
                originImmutable);
    }

    private static @Nullable CompoundTag cachedRequiredTag = null;
    private static Reference2IntMap<Block> cachedRequiredBlocks = new Reference2IntOpenHashMap<>();

    public static Reference2IntMap<Block> getCachedRequiredBlocks(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        if (tag != null && tag.equals(cachedRequiredTag)) return cachedRequiredBlocks;

        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        cachedRequiredBlocks = info != null ? new Reference2IntOpenHashMap<>(info.getBlockCounts()) :
                new Reference2IntOpenHashMap<>();
        cachedRequiredTag = tag != null ? tag.copy() : null;
        return cachedRequiredBlocks;
    }

    public static Reference2IntMap<Block> tallyStocked(ItemStack cinderCore) {
        Reference2IntMap<Block> stocked = new Reference2IntOpenHashMap<>();
        cinderCore.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof BlockItem blockItem) {
                    stocked.merge(blockItem.getBlock(), stack.getCount(), Integer::sum);
                }
            }
        });
        return stocked;
    }

    public static boolean hasSufficientMaterials(ItemStack cinderCore, Reference2IntMap<Block> required) {
        Reference2IntMap<Block> stocked = tallyStocked(cinderCore);
        for (var entry : required.reference2IntEntrySet()) {
            if (stocked.getOrDefault(entry.getKey(), 0) < entry.getIntValue()) return false;
        }
        return true;
    }

    public static void consumeMaterials(ItemStack cinderCore, Reference2IntMap<Block> required) {
        cinderCore.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (var entry : required.reference2IntEntrySet()) {
                int remaining = entry.getIntValue();
                for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) ||
                            blockItem.getBlock() != entry.getKey()) {
                        continue;
                    }
                    int take = Math.min(remaining, stack.getCount());
                    handler.extractItem(i, take, false);
                    remaining -= take;
                }
            }
        });
    }

    public static boolean tryCraftCore(IItemHandlerModifiable inventory, int coreSlot, int materialsStart,
                                       int materialsCount) {
        ItemStack core = inventory.getStackInSlot(coreSlot);
        if (core.isEmpty() || !(core.getItem() instanceof CinderCoreItem)) return false;
        if (getTargetId(core) == null) return false;
        if (!tallyStocked(core).isEmpty()) return false;

        MultiblockSchemaInfo schemaInfo = resolveSchema(core);
        if (schemaInfo == null) return false;
        Reference2IntMap<Block> required = new Reference2IntOpenHashMap<>(schemaInfo.getBlockCounts());
        if (required.isEmpty()) return false;

        Reference2IntMap<Block> available = new Reference2IntOpenHashMap<>();
        for (int i = 0; i < materialsCount; i++) {
            ItemStack stack = inventory.getStackInSlot(materialsStart + i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            available.merge(blockItem.getBlock(), stack.getCount(), Integer::sum);
        }
        for (var entry : required.reference2IntEntrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getIntValue()) return false;
        }

        for (var entry : required.reference2IntEntrySet()) {
            int remaining = entry.getIntValue();
            for (int i = 0; i < materialsCount && remaining > 0; i++) {
                int slot = materialsStart + i;
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) ||
                        blockItem.getBlock() != entry.getKey()) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                inventory.setStackInSlot(slot, stack);
                remaining -= take;
            }
        }

        core.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            int slot = 0;
            for (var entry : required.reference2IntEntrySet()) {
                handler.insertItem(slot++, new ItemStack(entry.getKey().asItem(), entry.getIntValue()), false);
            }
        });
        inventory.setStackInSlot(coreSlot, core);
        return true;
    }
}
