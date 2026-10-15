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

    public static boolean isInRange(ItemStack stack, IWirelessAccessPoint accessPoint, Player player) {
        var location = accessPoint.getLocation();
        if (location.getLevel() != player.level()) return false;

        double range = CinderAtlasUpgrades.getWirelessRange(stack);
        if (Double.isInfinite(range)) return true;

        double distSq = player.position().distanceToSqr(Vec3.atCenterOf(location.getPos()));
        return distSq <= range * range;
    }

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

    public static boolean requestMissingMaterials(ServerPlayer player, ItemStack stack, ServerLevel level,
                                                  Reference2IntMap<Block> required) {
        IWirelessAccessPoint wap = getLinkedAccessPoint(stack, level);
        if (wap == null || !isInRange(stack, wap, player)) return false;
        IGrid grid = wap.getGrid();

        ICraftingService craftingService = grid.getCraftingService();
        KeyCounter stock = grid.getStorageService().getCachedInventory();
        IActionSource source = IActionSource.ofPlayer(player);

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

                var patterns = craftingService.getCraftingFor(key);
                PhoenixCore.LOGGER.info("[CinderAtlas] getCraftingFor({}) found {} pattern(s): {}", key,
                        patterns.size(), patterns);

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
