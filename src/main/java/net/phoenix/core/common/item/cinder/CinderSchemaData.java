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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads/writes the "which multiblock, which optional parts" configuration baked into a Cinder
 * Core's NBT, and resolves it into a live {@link MultiblockSchemaInfo} using GTCEu's own
 * structure-pattern resolution engine ({@code AbstractStructureHelper}/{@code BlockPattern}) - none
 * of that resolution logic is ours, this class is purely the NBT<->data bridge for it.
 * <p>
 * V1 scope, both deliberate simplifications:
 * <ul>
 * <li>Only {@link BlockPattern}-based multiblocks are supported (the vast majority of GT
 * multiblocks). {@code ExpandablePattern}-based ones (variable-bounds structures) use a
 * non-char-keyed predicate identity that doesn't fit this NBT encoding yet.</li>
 * <li>Per-exact-position block overrides aren't supported - those are baked into a specific
 * facing/upFacing/flip, and orientation isn't decided until placement time, not configure time.
 * Only predicate-class-level choices (which apply regardless of orientation) and slice-repeat
 * counts are configurable.</li>
 * </ul>
 */
public final class CinderSchemaData {

    private CinderSchemaData() {}

    private static final String TARGET_KEY = "TargetMultiblock";
    private static final String SLICE_KEYS = "SliceRepeatKeys";
    private static final String SLICE_VALUES = "SliceRepeatValues";
    private static final String BLOCK_PREFERENCES = "BlockPreferences";

    public static void setTarget(ItemStack cinderCore, MultiblockMachineDefinition definition) {
        CompoundTag tag = cinderCore.getOrCreateTag();
        if (!definition.getId().equals(getTargetId(cinderCore))) {
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
        if (tag == null || !tag.contains(TARGET_KEY)) return null;
        return ResourceLocation.tryParse(tag.getString(TARGET_KEY));
    }

    /** Wipes the target and all configuration - lets a player back out to the target picker without
     *  re-crafting a fresh Core. */
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

    /** {@code null} both when the target isn't {@link BlockPattern}-based (see class doc) and when it
     *  has no {@code DEFAULT_STRUCTURE}-keyed pattern at all - a multiblock built entirely out of
     *  named substructures (Tier 1/2/3 Cinder Forges among them now) has nothing under that key, and
     *  {@code Map#get} returning {@code null} there used to NPE on the very next {@code .get()} call
     *  instead of being treated as "not a supported target", same as any other unsupported shape. */
    private static @Nullable BlockPattern getPattern(MultiblockMachineDefinition definition) {
        var patternSupplier = definition.getStructurePatterns().get(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        if (patternSupplier == null) return null;
        IBlockPattern pattern = patternSupplier.get();
        return pattern instanceof BlockPattern blockPattern ? blockPattern : null;
    }

    /**
     * Resolves the Core's saved configuration into a live {@link MultiblockSchemaInfo}. Returns
     * {@code null} if no target is set, the target no longer exists, or the target isn't a
     * {@link BlockPattern}-based multiblock (see class doc).
     * <p>
     * Runs {@code refreshSchema} twice: {@code MultiblockSchemaInfo.putPredicatePreference} writes
     * into an internal structure-helper table that only gets created BY the first
     * {@code refreshSchema} call (no public setter, no lazy-init on the preference side) - so saved
     * block-preference choices can't be applied until after a bootstrap pass, and need a second pass
     * afterward to actually take effect in the resolved result.
     */
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
            // No saved choices yet - default every repeatable slice to its minimum, matching how
            // GTCEu's own MultiblockPreviewWidget seeds a fresh configuration.
            for (int i = 0; i < pattern.getSlices().length; i++) {
                info.getUserSliceRepeats().put(i, pattern.getSlices()[i].getMinRepeats());
            }
        }

        // Bootstrap pass - creates the internal structure helper so predicate preferences below have
        // somewhere to go. Orientation here is an arbitrary fixed reference: predicate-class choices
        // and slice counts are both orientation-agnostic (see class doc), so this never needs to
        // match the orientation the Core is eventually placed with.
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
            // Re-resolve now that the saved preferences are actually registered on the helper.
            info.refreshSchema(definition, Direction.NORTH, Direction.UP, false, null);
        }

        return info;
    }

    /**
     * Shift-right-clicking an already-built, formed multiblock scans it into a Core instead of
     * picking blind from the target list: sets the target to that structure's definition, then reads
     * the real placed {@code BlockState} at every ambiguous predicate position (via the controller's
     * own {@code getFrontFacing()}/{@code getUpwardsFacing()}/{@code isFlipped()} and
     * {@code AbstractStructureHelper#getPredicateFromPos}) and records whichever candidate is
     * actually standing there as this Core's preference - reproducing the exact build, not just the
     * abstract shape.
     * <p>
     * Known v1 limitation: variable-height slice repeat counts are NOT detected from the real
     * structure (they default to each slice's minimum, same as a fresh unconfigured Core) - working
     * out how many times a slice was actually repeated would mean re-resolving the whole flattened
     * pattern per candidate count and cross-checking against real blocks slice-by-slice (repeat
     * counts aren't independent - changing one shifts every later slice's position in the flattened
     * pattern), which is a lot of complexity for something the Configurator's own steppers already
     * fix in two clicks after scanning.
     *
     * @return {@code false} if the target position isn't a formed multiblock controller.
     */
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

    /**
     * Server-side entry point for the Configurator's "Confirm" action: rebuilds a
     * {@link MultiblockSchemaInfo} from raw client-sent choices (never trust the client's own NBT
     * directly), re-derives it against this Core's actual target definition, and persists it. Slice
     * preferences are {@code sliceKeys[i] -> sliceValues[i]}; block preferences are parallel arrays
     * of (predicate char, index into that predicate's expanded {@code BasePredicate} list, index
     * into that base predicate's candidate list) - the same encoding {@link #resolveSchema} reads
     * back out of NBT, just carried over the network first instead of already being in the stack's
     * tag. Returns {@code false} (no-op) if this Core has no valid target or isn't a
     * {@code BlockPattern}-based multiblock.
     */
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
        // Bootstrap pass - see resolveSchema's doc for why this has to happen before any
        // putPredicatePreference call.
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

    /**
     * Persists a resolved {@link MultiblockSchemaInfo}'s user choices back into the Core's NBT -
     * called when the Configurator screen closes.
     */
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

    /**
     * Server-authoritative mutation for the Cinder Forge multiblock's live UI (unlike the item
     * Configurator, that UI edits continuously rather than batching into one "Confirm" packet): bumps
     * one variable slice's repeat count by {@code delta}, clamped to its pattern-defined bounds, and
     * immediately re-persists. No-op if there's nothing to resolve.
     */
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

    /**
     * Server-authoritative mutation for the Cinder Forge's live UI: sets one predicate row's choice
     * to a specific candidate by index - backs a real "see every option, pick one directly" dropdown
     * selector, the same power the item Configurator's own dropdown has, rather than a click-to-cycle
     * button.
     */
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

    /** Which candidate index a predicate row is currently showing - for highlighting the active
     *  option in the dropdown's option list. Defaults to 0 (the first candidate) if nothing's been
     *  chosen yet, matching how an unconfigured predicate resolves. */
    public static int getPredicateCandidateIndex(ItemStack cinderCore, MultiPredicate predicate, BasePredicate base) {
        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        if (info == null || info.getStructureHelper() == null) return 0;
        BlockInfo current = info.getStructureHelper().getBlockPreferences().get(predicate, base);
        int index = current != null ? base.getCandidates().indexOf(current) : -1;
        return Math.max(0, index);
    }

    /** A schema resolved against an actual world placement site - what {@link #resolvePlacement} hands
     *  to both the preview renderer (bounding box + material check) and the commit handler (the same
     *  data, just trusted server-side instead of used for display). */
    public record ResolvedPlacement(Map<BlockPos, BlockInfo> placements, Reference2IntMap<Block> blockCounts,
                                    BlockPos origin) {}

    /** A {@link ResolvedPlacement} degraded down to only what's actually stocked - see
     *  {@link #resolvePartialPlacements}. */
    public record PartialPlacement(Map<BlockPos, BlockInfo> placements, Reference2IntMap<Block> consumed) {}

    /**
     * Force-build path: instead of hard-rejecting a commit when materials are insufficient, builds
     * whatever positions the currently stocked materials actually cover and leaves the rest as gaps
     * (pre-existing terrain, unplaced). Walks {@code resolved.placements()} once, handing out each
     * block type from a stocked-materials budget on a first-come basis; a position whose block type's
     * budget has already run dry is simply skipped rather than placed. Positions whose block type
     * isn't tracked as a required material at all (e.g. air) are always kept.
     */
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

    /**
     * Resolves a Core's saved configuration into absolute world positions for a specific placement
     * site and orientation. {@link #resolveSchema} always leaves its result resolved against a fixed
     * NORTH/UP reference frame (predicate/slice choices are orientation-agnostic - see class doc), so
     * this re-runs {@code refreshSchema} a third time against the real orientation; the structure
     * helper's block preferences persist across that call since they're keyed by predicate identity,
     * not position, so nothing chosen in the Configurator is lost.
     */
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

    // Single-slot cache backing getCachedRequiredBlocks - fine as a shared static since tooltip hover
    // is normally one stack at a time; a switch to a different stack just costs one extra resolution.
    private static @Nullable CompoundTag cachedRequiredTag = null;
    private static Reference2IntMap<Block> cachedRequiredBlocks = new Reference2IntOpenHashMap<>();

    /**
     * Cache-backed "what does this Core's current configuration require in total" lookup, for
     * UI/tooltip use where {@link #resolveSchema}'s full GTCEu pattern-resolution pass would otherwise
     * re-run on every render frame the tooltip/UI is visible. Only actually re-resolves when the
     * stack's NBT has changed since the last call.
     */
    public static Reference2IntMap<Block> getCachedRequiredBlocks(ItemStack cinderCore) {
        CompoundTag tag = cinderCore.getTag();
        if (tag != null && tag.equals(cachedRequiredTag)) return cachedRequiredBlocks;

        MultiblockSchemaInfo info = resolveSchema(cinderCore);
        cachedRequiredBlocks = info != null ? new Reference2IntOpenHashMap<>(info.getBlockCounts()) :
                new Reference2IntOpenHashMap<>();
        cachedRequiredTag = tag != null ? tag.copy() : null;
        return cachedRequiredBlocks;
    }

    /** Tallies the {@link net.minecraft.world.item.BlockItem}s stocked in a Core's material inventory. */
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

    /** Server-side only: pulls the required counts out of the Core's stocked-materials inventory.
     *  Caller must have already confirmed {@link #hasSufficientMaterials} - this doesn't re-check. */
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

    /**
     * Attempts to fill an unconfigured, empty Cinder Core in place from raw materials sitting in
     * adjacent slots of the same {@code inventory} - the one operation both the ME hatch's automatic
     * per-tick attempt ({@code CinderForgeHatchPartMachine}) and a Tier 1 Cinder Forge's manual
     * "Package" button run, so both go through the exact same logic instead of two copies drifting
     * apart. No-ops (returns {@code false}) if the core slot is empty/not a Core, has no target, is
     * already stocked, or materials are insufficient - safe to call speculatively on every tick or
     * every button press.
     *
     * @param inventory the handler backing both the core slot and the material slots
     * @param coreSlot the slot index holding the Cinder Core to fill
     * @param materialsStart the first material slot index (immediately after the core slot)
     * @param materialsCount how many material slots follow
     * @return {@code true} if the Core was actually filled by this call
     */
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
