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

    /**
     * Every method below that reads/writes a Cinder Core's configuration has two forms: an
     * {@link ItemStack} overload (unchanged behavior - existing call sites in {@code CinderCoreItem},
     * {@code CinderForgeMachine}, the Configurator screen, etc. keep working exactly as before, reading
     * against the stack's own root tag via {@code getOrCreateTag()}/{@code getTag()}) and a
     * {@link CompoundTag} overload operating on whatever tag is handed to it. The Cinder Atlas
     * deployment tool (multiple loadouts, each holding several saved targets) is the reason the tag
     * overloads exist: a "slot" there is just a sub-{@code CompoundTag} nested under the tool's own
     * NBT rather than a whole stack's root tag, and every one of these methods works identically
     * either way since none of them actually need anything else from the stack itself.
     */
    public static void setTarget(ItemStack cinderCore, MultiblockMachineDefinition definition) {
        setTarget(cinderCore.getOrCreateTag(), definition);
    }

    public static void setTarget(CompoundTag tag, MultiblockMachineDefinition definition) {
        if (!definition.getId().equals(getTargetId(tag))) {
            // Switching targets invalidates any prior configuration - stale slice/preference data
            // from a different multiblock's pattern would be meaningless (or worse, silently
            // misapplied to the wrong predicate) against this one.
            tag.remove(SLICE_KEYS);
            tag.remove(SLICE_VALUES);
            tag.remove(BLOCK_PREFERENCES);
        }
        tag.putString(TARGET_KEY, definition.getId().toString());
    }

    public static @Nullable ResourceLocation getTargetId(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        return tag == null ? null : getTargetId(tag);
    }

    public static @Nullable ResourceLocation getTargetId(CompoundTag tag) {
        if (!tag.contains(TARGET_KEY)) return null;
        return ResourceLocation.tryParse(tag.getString(TARGET_KEY));
    }

    public static void clearTarget(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        if (tag == null) return;
        clearTarget(tag);
    }

    public static void clearTarget(CompoundTag tag) {
        tag.remove(TARGET_KEY);
        tag.remove(SLICE_KEYS);
        tag.remove(SLICE_VALUES);
        tag.remove(BLOCK_PREFERENCES);
    }

    public static @Nullable MultiblockMachineDefinition getTargetDefinition(ItemStack cinderCore) {
        return getTargetDefinition(getTargetId(cinderCore));
    }

    public static @Nullable MultiblockMachineDefinition getTargetDefinition(CompoundTag tag) {
        return getTargetDefinition(getTargetId(tag));
    }

    private static @Nullable MultiblockMachineDefinition getTargetDefinition(@Nullable ResourceLocation id) {
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
        return resolveSchema(cinderCore.getOrCreateTag());
    }

    public static @Nullable MultiblockSchemaInfo resolveSchema(CompoundTag tag) {
        MultiblockMachineDefinition definition = getTargetDefinition(tag);
        if (definition == null) return null;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return null;

        MultiblockSchemaInfo info = new MultiblockSchemaInfo();

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
        return scanFromWorld(cinderCore.getOrCreateTag(), level, controllerPos);
    }

    public static boolean scanFromWorld(CompoundTag tag, Level level, BlockPos controllerPos) {
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

        setTarget(tag, definition);

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
        writeUserPreferences(tag, definition, info);
        return true;
    }

    public static boolean applyConfiguration(ItemStack cinderCore, int[] sliceKeys, int[] sliceValues,
                                             char[] prefChars, int[] prefBaseIndices, int[] prefCandidateIndices) {
        return applyConfiguration(cinderCore.getOrCreateTag(), sliceKeys, sliceValues, prefChars, prefBaseIndices,
                prefCandidateIndices);
    }

    public static boolean applyConfiguration(CompoundTag tag, int[] sliceKeys, int[] sliceValues,
                                             char[] prefChars, int[] prefBaseIndices, int[] prefCandidateIndices) {
        MultiblockMachineDefinition definition = getTargetDefinition(tag);
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

        writeUserPreferences(tag, definition, info);
        return true;
    }

    public static void writeUserPreferences(ItemStack cinderCore, MultiblockMachineDefinition definition,
                                            MultiblockSchemaInfo info) {
        writeUserPreferences(cinderCore.getOrCreateTag(), definition, info);
    }

    public static void writeUserPreferences(CompoundTag tag, MultiblockMachineDefinition definition,
                                            MultiblockSchemaInfo info) {
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return;

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
        adjustSliceRepeat(cinderCore.getOrCreateTag(), sliceIndex, delta);
    }

    public static void adjustSliceRepeat(CompoundTag tag, int sliceIndex, int delta) {
        MultiblockMachineDefinition definition = getTargetDefinition(tag);
        if (definition == null) return;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null || sliceIndex < 0 || sliceIndex >= pattern.getSlices().length) return;

        MultiblockSchemaInfo info = resolveSchema(tag);
        if (info == null) return;

        var slice = pattern.getSlices()[sliceIndex];
        int current = info.getUserSliceRepeats().getOrDefault(sliceIndex, slice.getMinRepeats());
        int next = Math.max(slice.getMinRepeats(), Math.min(slice.getMaxRepeats(), current + delta));
        if (next == current) return;

        info.getUserSliceRepeats().put(sliceIndex, next);
        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        writeUserPreferences(tag, definition, info);
    }

    public static void setPredicateCandidate(ItemStack cinderCore, char predicateChar, int baseIndex,
                                             int candidateIndex) {
        setPredicateCandidate(cinderCore.getOrCreateTag(), predicateChar, baseIndex, candidateIndex);
    }

    public static void setPredicateCandidate(CompoundTag tag, char predicateChar, int baseIndex,
                                             int candidateIndex) {
        MultiblockMachineDefinition definition = getTargetDefinition(tag);
        if (definition == null) return;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return;

        MultiPredicate predicate = pattern.getPredicates().get(predicateChar);
        if (predicate == null || baseIndex < 0 || baseIndex >= predicate.predicates().size()) return;
        BasePredicate base = predicate.predicates().get(baseIndex);
        var candidates = base.getCandidates();
        if (candidateIndex < 0 || candidateIndex >= candidates.size()) return;

        MultiblockSchemaInfo info = resolveSchema(tag);
        if (info == null) return;

        info.putPredicatePreference(predicate, base, candidates.get(candidateIndex));
        info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        writeUserPreferences(tag, definition, info);
    }

    public static int getPredicateCandidateIndex(ItemStack cinderCore, MultiPredicate predicate, BasePredicate base) {
        return getPredicateCandidateIndex(cinderCore.getOrCreateTag(), predicate, base);
    }

    public static int getPredicateCandidateIndex(CompoundTag tag, MultiPredicate predicate, BasePredicate base) {
        MultiblockSchemaInfo info = resolveSchema(tag);
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
        return resolvePlacement(cinderCore.getOrCreateTag(), anchor, frontFacing, upFacing, isFlipped);
    }

    public static @Nullable ResolvedPlacement resolvePlacement(CompoundTag tag, BlockPos anchor,
                                                               Direction frontFacing, Direction upFacing,
                                                               boolean isFlipped) {
        MultiblockMachineDefinition definition = getTargetDefinition(tag);
        if (definition == null) return null;
        BlockPattern pattern = getPattern(definition);
        if (pattern == null) return null;

        MultiblockSchemaInfo info = resolveSchema(tag);
        if (info == null) return null;
        info.refreshSchema(definition, frontFacing, upFacing, isFlipped, null);

        BlockPos.MutableBlockPos origin = anchor.mutable();
        pattern.getOffset().apply(origin, frontFacing, upFacing, isFlipped);
        BlockPos originImmutable = origin.immutable();

        Map<BlockPos, BlockInfo> placements = new HashMap<>();
        for (var entry : info.getStructureBlocks().entrySet()) {
            placements.put(originImmutable.offset(entry.getKey()), entry.getValue());
        }

        // pattern.getOffset() anchors the structure on where its *controller* sits within the
        // pattern (verified by decompiling GTCEu's OriginOffset/BlockPattern#legacyStartOffset) -
        // correct for GTCEu's own use case of validating an already-placed controller block, but
        // wrong for us: we hand it the ghost-preview aim point as if it were that controller. A
        // controller that isn't at the structure's own floor (e.g. Cinder Forge's is vertically
        // centered, 2 rows above its bottom casing layer) makes the whole footprint sink that many
        // rows below the clicked surface, silently overlapping terrain instead of resting on it -
        // isSiteClear() would then correctly refuse to build there, but the fix belongs here: the
        // footprint should never be computed to require ground below where the player aimed in the
        // first place. Shifting the whole footprint (and the anchor) up by however far its lowest
        // row dips below the click point preserves the pattern's horizontal facing/centering while
        // guaranteeing the structure always rests on or above the surface instead of into it.
        int minY = placements.keySet().stream().mapToInt(BlockPos::getY).min().orElse(anchor.getY());
        if (minY < anchor.getY()) {
            BlockPos shift = new BlockPos(0, anchor.getY() - minY, 0);
            Map<BlockPos, BlockInfo> shifted = new HashMap<>();
            for (var entry : placements.entrySet()) {
                shifted.put(entry.getKey().offset(shift), entry.getValue());
            }
            placements = shifted;
            originImmutable = originImmutable.offset(shift);
        }

        return new ResolvedPlacement(placements, new Reference2IntOpenHashMap<>(info.getBlockCounts()),
                originImmutable);
    }

    /**
     * Whether every target position is currently empty enough to build into - {@link BlockState#canBeReplaced()}
     * (true for air, tall grass, snow layers, liquids, etc.), false for real terrain/existing builds.
     * {@link #resolvePlacement} itself is purely geometric and has no idea what's actually at those world
     * positions, so without this check a placement whose footprint dips into a hillside or floor would
     * silently overwrite that terrain instead of being refused - "clipping into the ground."
     */
    public static boolean isSiteClear(Level level, Map<BlockPos, BlockInfo> placements) {
        for (BlockPos pos : placements.keySet()) {
            if (!level.getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
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

    /** Deliberately uncached, unlike {@link #getCachedRequiredBlocks} - that single-slot cache is the
     *  right shape for "one held stack's tooltip re-rendering every frame," but wrong for a UI showing
     *  many loadout slots' rows at once (every row would evict the last, thrashing on every repaint).
     *  Callers displaying several slots simultaneously (the Cinder Atlas loadout list) should keep
     *  their own cache keyed per slot instead. */
    public static Reference2IntMap<Block> getRequiredBlocks(CompoundTag tag) {
        MultiblockSchemaInfo info = resolveSchema(tag);
        return info != null ? new Reference2IntOpenHashMap<>(info.getBlockCounts()) : new Reference2IntOpenHashMap<>();
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
