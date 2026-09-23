package net.phoenix.core.integration.ae2;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.item.cinder.CinderAtlasItem;
import net.phoenix.core.common.item.cinder.CinderAtlasUpgrades;
import net.phoenix.core.common.item.cinder.CinderItems;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.util.Platform;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real AE2 wireless terminal binding for the Cinder Atlas - "normal wireless AE2 item" per the design
 * doc, not a custom shortcut. Registering this handler via {@link GridLinkables#register} is the
 * entire integration point: AE2's own Security Terminal already scans the held item against that
 * registry for its "link"/"unlink" UI (the same one a real Wireless Terminal uses), so no custom
 * linking screen is needed here at all - point the Atlas at a Security Terminal like any other
 * linkable item and AE2 handles the rest.
 * <p>
 * {@link #getLinkedGrid} mirrors AE2's own {@code WirelessTerminalItem#getLinkedGrid} algorithm
 * exactly (verified against the real 15.4.10-cosmolite.36 bytecode, no sources jar was available for
 * this build): resolve the bound {@link GlobalPos}'s dimension, find the currently-ticking block
 * entity there, require it be an {@link IWirelessAccessPoint}, read its grid - rather than guessing at
 * a simpler-but-wrong version of the same resolution.
 * <p>
 * Deliberately does NOT yet replicate the real wireless terminal's power cost (AE2's own draws from an
 * internal battery per use) - {@link CinderAtlasItem} isn't an {@code AEBasePoweredItem} yet. Free
 * queries for now; a real cost is a reasonable follow-up once the rest of the AE2-sourcing flow exists,
 * not a decision to make blind.
 * <p>
 * Grid access is server-only, same as AE2 itself - an {@code IGrid} has no meaning client-side. Nothing
 * here is called from client code; {@code CinderPreviewState}'s client-side "materials sufficient"
 * check still naturally reads as false for an Atlas (no local capability to check), same honest
 * "unknown until the server confirms" behavior as before this file existed. The real check happens
 * server-side in {@code C2SCinderCommitPacket}.
 */
public final class CinderAtlasWirelessLink implements IGridLinkableHandler {

    public static final CinderAtlasWirelessLink INSTANCE = new CinderAtlasWirelessLink();

    private static final String TAG_LINKED_POS = "LinkedAccessPoint";

    private CinderAtlasWirelessLink() {}

    public static void register() {
        GridLinkables.register(CinderItems.CINDER_ATLAS.get(), INSTANCE);
    }

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.getItem() instanceof CinderAtlasItem;
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .resultOrPartial(error -> PhoenixCore.LOGGER.error("[CinderAtlas] Failed to encode link: {}", error))
                .ifPresent(encoded -> stack.getOrCreateTag().put(TAG_LINKED_POS, encoded));
    }

    @Override
    public void unlink(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) tag.remove(TAG_LINKED_POS);
    }

    public static @Nullable GlobalPos getLinkedPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_LINKED_POS, Tag.TAG_COMPOUND)) return null;

        return GlobalPos.CODEC.decode(NbtOps.INSTANCE, tag.get(TAG_LINKED_POS))
                .resultOrPartial(error -> PhoenixCore.LOGGER.error("[CinderAtlas] Failed to decode link: {}", error))
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .orElse(null);
    }

    /** The access point itself, not just its grid - needed anywhere the caller must act as a real
     *  {@code IActionHost} (e.g. {@link CinderAtlasMenuHost#getActionableNode()}), the same way AE2's
     *  own {@code WirelessTerminalMenuHost} borrows its bound access point's grid node rather than being
     *  a grid node itself. */
    public static @Nullable IWirelessAccessPoint getLinkedAccessPoint(ItemStack stack, ServerLevel level) {
        GlobalPos pos = getLinkedPosition(stack);
        if (pos == null) return null;

        MinecraftServer server = level.getServer();
        ServerLevel targetLevel = server.getLevel(pos.dimension());
        if (targetLevel == null) return null;

        return Platform.getTickingBlockEntity(targetLevel, pos.pos()) instanceof IWirelessAccessPoint wap ?
                wap : null;
    }

    public static @Nullable IGrid getLinkedGrid(ItemStack stack, ServerLevel level) {
        IWirelessAccessPoint wap = getLinkedAccessPoint(stack, level);
        return wap != null ? wap.getGrid() : null;
    }

    /**
     * Design doc feature #7 - the real range check AE2's own wireless terminal has always done
     * (verified earlier via {@code WirelessTerminalMenuHost#getActionableNode}/{@code #rangeCheck}) that
     * the Atlas never had until upgrades gave it something to gate: distance from the linked access
     * point, compared against {@link CinderAtlasUpgrades#getWirelessRange}. Requires the player be in
     * the access point's own dimension at all - a cross-dimension "link" is never in range, same as a
     * real wireless terminal.
     */
    public static boolean isInRange(ItemStack stack, IWirelessAccessPoint accessPoint, Player player) {
        var location = accessPoint.getLocation();
        if (location.getLevel() != player.level()) return false;

        double range = CinderAtlasUpgrades.getWirelessRange(stack);
        if (Double.isInfinite(range)) return true;

        double distSq = player.position().distanceToSqr(Vec3.atCenterOf(location.getPos()));
        return distSq <= range * range;
    }

    /**
     * Every {@code AEItemKey.of(...)} call in this class (here and below) uses the {@code ItemLike}
     * overload directly on the {@link Block}, not {@code AEItemKey.of(new ItemStack(block.asItem()))} -
     * changed 2026-09-16 while chasing a report of crafting requests reporting success with real
     * patterns present but never actually crafting (`patternSteps=0`, `simulation=true`). Decompiling
     * {@code AEItemKey} confirmed the {@code ItemStack} overload additionally serializes the stack's
     * Forge capability data into the key's identity (`internedCaps`), which the plain {@code ItemLike}
     * overload skips entirely - a fresh {@code new ItemStack(item)} could carry different (or just
     * different-shaped-empty) capability data than however a real pattern's output key or the network's
     * stored key was derived, making two keys that represent "the same item" compare unequal. Not
     * confirmed as the fix for that exact bug (a plain block having non-trivial default capability data
     * is a real but unconfirmed possibility), but strictly safer and simpler either way - one fewer
     * allocation and one less axis two logically-identical keys could diverge on.
     */
    public static boolean hasSufficientMaterials(ItemStack stack, ServerLevel level, Player player,
                                                 Reference2IntMap<Block> required) {
        IWirelessAccessPoint wap = getLinkedAccessPoint(stack, level);
        if (wap == null || !isInRange(stack, wap, player)) return false;

        KeyCounter stock = wap.getGrid().getStorageService().getCachedInventory();
        for (var entry : required.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key == null || stock.get(key) < entry.getIntValue()) return false;
        }
        return true;
    }

    /** Caller must already have confirmed {@link #hasSufficientMaterials} - this doesn't re-check, same
     *  contract {@code CinderSchemaData#consumeMaterials} uses for a Cinder Core's own stocked
     *  inventory. */
    public static void extractMaterials(ItemStack stack, ServerLevel level, Reference2IntMap<Block> required,
                                        Player player) {
        IGrid grid = getLinkedGrid(stack, level);
        if (grid == null) return;

        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.ofPlayer(player);
        for (var entry : required.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key == null) continue;
            storage.extract(key, entry.getIntValue(), Actionable.MODULATE, source);
        }
    }

    /**
     * Real, headless auto-populate for the shortfall (design doc #3/#4) - for every required block
     * still short of the linked network's live stock, kicks off a genuine
     * {@link ICraftingService#beginCraftingCalculation} and, once it resolves, submits it with
     * {@link ICraftingService#submitJob}. No UI involved and no player interaction needed once
     * triggered.
     * <p>
     * Passes {@code null} for the {@code ICraftingRequester} parameter - verified against AE2's own
     * {@code CraftConfirmMenu#startJob} bytecode that this is exactly what a normal in-UI "start job"
     * button does too when there's no dedicated machine tracking the request (it's an optional callback
     * interface for a requester that wants to be notified as crafted items come back, e.g. a physical
     * autocrafting machine - a portable item with nothing to notify has no need for one). This was the
     * piece that looked like a hard blocker earlier (assuming a real grid node was required to submit
     * any job at all) - it isn't; only the *simulation* side (`beginCraftingCalculation`) needs an
     * {@link ICraftingSimulationRequester}, and that interface only requires {@code getActionSource()},
     * trivially satisfiable with a lambda.
     * <p>
     * Calculation runs off the server thread (each {@link Future} is awaited on the common pool, not
     * blocking the caller), then hops back via {@link MinecraftServer#execute} before touching the grid
     * again for CPU selection/job submission, matching this codebase's existing
     * enqueueWork-for-server-mutation convention.
     *
     * @return {@code false} only if the Atlas isn't linked to a network at all; {@code true} once
     *         requests have been kicked off (their eventual success/failure is reported to the player
     *         asynchronously as each calculation resolves).
     */
    public static boolean requestMissingMaterials(ServerPlayer player, ItemStack stack, ServerLevel level,
                                                  Reference2IntMap<Block> required) {
        IWirelessAccessPoint wap = getLinkedAccessPoint(stack, level);
        if (wap == null || !isInRange(stack, wap, player)) return false;
        IGrid grid = wap.getGrid();

        ICraftingService craftingService = grid.getCraftingService();
        KeyCounter stock = grid.getStorageService().getCachedInventory();
        IActionSource source = IActionSource.ofPlayer(player);
        // Root cause of "reports it's going to craft, then does nothing" (found by decompiling
        // CraftingTreeNode#buildChildPatterns): that method only expands a node into real crafting
        // steps when `simRequester.getGridNode()` is non-null - it's the actual, unconditional gate
        // on `craftingService.getCraftingFor(...)` ever getting called *inside* the calculation
        // engine (a bare `() -> source` lambda here only overrides getActionSource(); getGridNode()
        // silently falls back to its interface default, which resolves through
        // IActionSource#machine() - empty for a player-sourced action, since a player isn't a grid
        // machine - so it always returned null). With no grid node, every node in the tree builds
        // zero child patterns and immediately fails as "fully missing," regardless of whether a
        // pattern exists or the network has the input stocked - exactly matching every symptom seen
        // across three repros (pattern found via the direct getCraftingFor(key) diagnostic, ample
        // input stock, yet patternSteps always 0). Overriding getGridNode() to the linked Wireless
        // Access Point's own actionable node - a real node already on this exact grid - is what a
        // physical requester machine gets for free via its own getActionableNode(); a handheld,
        // wireless tool has no such node of its own, so it borrows the access point's.
        IGridNode requesterNode = wap.getActionableNode();
        ICraftingSimulationRequester requester = new ICraftingSimulationRequester() {

            @Override
            public IActionSource getActionSource() {
                return source;
            }

            @Override
            public @Nullable IGridNode getGridNode() {
                return requesterNode;
            }
        };
        MinecraftServer server = level.getServer();

        Map<AEItemKey, Long> missing = new LinkedHashMap<>();
        for (var entry : required.reference2IntEntrySet()) {
            AEItemKey key = AEItemKey.of(entry.getKey());
            if (key == null) continue;
            long need = entry.getIntValue() - stock.get(key);
            if (need > 0) {
                missing.put(key, need);
                // Diagnostic (2026-09-16) - a direct, security-unfiltered "does the network even see a
                // pattern for this exact key" check, independent of the full calculation engine. If this
                // is empty while a pattern visibly exists and works when crafted manually, the bug is a
                // key mismatch (this synthesized AEItemKey doesn't equal the pattern's real output key,
                // most likely an NBT/data-component difference `new ItemStack(block.asItem())` doesn't
                // carry); if it's non-empty, the problem is deeper in beginCraftingCalculation itself.
                var patterns = craftingService.getCraftingFor(key);
                PhoenixCore.LOGGER.info("[CinderAtlas] getCraftingFor({}) found {} pattern(s): {}", key,
                        patterns.size(), patterns);
                // Follow-up diagnostic (2026-09-16) - a repro with this pattern found showed the plan's
                // missingItems() as the *entire requested output*, patternSteps=0: AE2 located the
                // pattern but produced zero steps from it, which points at one of the pattern's own
                // inputs being unavailable (and not itself craftable) rather than a key-matching issue.
                // Dumps each pattern's input slots (every possible ingredient variant + current network
                // stock of it) so the actual missing ingredient is visible instead of inferred.
                for (var pattern : patterns) {
                    for (var input : pattern.getInputs()) {
                        StringBuilder slotDump = new StringBuilder();
                        for (var possible : input.getPossibleInputs()) {
                            if (slotDump.length() > 0) slotDump.append(" OR ");
                            slotDump.append(possible.amount()).append("x").append(possible.what())
                                    .append(" (have ").append(stock.get(possible.what())).append(")");
                        }
                        PhoenixCore.LOGGER.info("[CinderAtlas]   input slot: {}", slotDump);
                    }
                }
            }
        }

        if (missing.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("Nothing missing - the network already has everything needed."), true);
            return true;
        }

        int total = missing.size();
        AtomicInteger queued = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger pending = new AtomicInteger(total);
        // Only ever appended from server.execute callbacks, which always run one-at-a-time on the main
        // server thread - genuinely safe without synchronization, not just "probably fine."
        List<String> failureDetails = new java.util.ArrayList<>();

        for (var entry : missing.entrySet()) {
            AEItemKey key = entry.getKey();
            long amount = entry.getValue();
            Future<ICraftingPlan> future = craftingService.beginCraftingCalculation(level, requester, key, amount,
                    CalculationStrategy.CRAFT_LESS);

            CompletableFuture.runAsync(() -> {
                ICraftingPlan plan;
                try {
                    plan = future.get();
                } catch (Exception e) {
                    PhoenixCore.LOGGER.warn("[CinderAtlas] Crafting calculation failed for {}: {}", key,
                            e.toString());
                    plan = null;
                }

                ICraftingPlan finalPlan = plan;
                server.execute(() -> {
                    boolean success = false;
                    String failureReason = "calculation produced no plan (recipe not found, or a required " +
                            "ingredient can't be sourced at all)";

                    if (finalPlan != null) {
                        // Diagnostic logging (2026-09-16) - added after a report of "says it's going to
                        // make them all, then does nada" with a network that had spare CPU capacity and
                        // real patterns for the item. The whole submission pipeline (this method's cpu
                        // selection through AE2's own CraftingCpuLogic#trySubmitJob) was traced against
                        // the real AE2 bytecode and looks structurally correct, so the remaining
                        // candidates - a plan that's flagged simulation()-only, or still reports
                        // missingItems() despite CRAFT_LESS, or a submitJob error this code was
                        // discarding - need real data from an actual run to pin down further.
                        // KeyCounter has no toString() (logging it directly just prints its hashcode,
                        // useless) - enumerate it so a "missing" plan actually says what's missing.
                        StringBuilder missingDump = new StringBuilder();
                        for (var missingEntry : finalPlan.missingItems()) {
                            if (missingDump.length() > 0) missingDump.append(", ");
                            missingDump.append(missingEntry.getLongValue()).append("x")
                                    .append(missingEntry.getKey());
                        }
                        PhoenixCore.LOGGER.info(
                                "[CinderAtlas] Plan for {}x{}: simulation={} bytes={} missingItems=[{}] " +
                                        "patternSteps={}",
                                amount, key, finalPlan.simulation(), finalPlan.bytes(), missingDump,
                                finalPlan.patternTimes().size());

                        // Matches AE2's own CraftConfirmMenu#cpuMatches exactly (verified via bytecode) -
                        // the real UI never just grabs any idle CPU, it also requires the CPU's available
                        // storage cover the plan's byte size. Skipping that check (an earlier version of
                        // this code did) let submitJob get handed a CPU too small for the job.
                        ICraftingCPU cpu = craftingService.getCpus().stream()
                                .filter(c -> !c.isBusy() && c.getAvailableStorage() >= finalPlan.bytes())
                                .findFirst().orElse(null);
                        if (cpu == null) {
                            failureReason = "no crafting CPU is both idle and big enough for this job";
                        } else {
                            ICraftingSubmitResult result = craftingService.submitJob(finalPlan, null, cpu, true,
                                    source);
                            success = result.successful();
                            PhoenixCore.LOGGER.info(
                                    "[CinderAtlas] submitJob for {}x{}: successful={} errorCode={} " +
                                            "errorDetail={} link={}",
                                    amount, key, success, result.errorCode(), result.errorDetail(), result.link());
                            if (!success) {
                                failureReason = result.errorCode() +
                                        (result.errorDetail() != null ? " (" + result.errorDetail() + ")" : "");
                            }
                        }
                    }

                    (success ? queued : failed).incrementAndGet();
                    if (!success) failureDetails.add(key + ": " + failureReason);

                    if (pending.decrementAndGet() == 0) {
                        String summary = "Requested crafting for " + queued.get() + "/" + total +
                                " missing material" + (total == 1 ? "" : "s") +
                                (failureDetails.isEmpty() ? "." : " - " + String.join("; ", failureDetails) + ".");
                        player.displayClientMessage(Component.literal(summary), false);
                    }
                });
            });
        }

        return true;
    }
}
