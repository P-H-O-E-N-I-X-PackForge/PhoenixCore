package net.phoenix.core.integration.continuum.common;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.block.LaunchPadBlock;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;
import net.phoenix.core.integration.continuum.item.ContinuumProbeItem;
import net.phoenix.core.integration.continuum.item.ContinuumRocketItem;
import net.phoenix.core.integration.continuum.machine.LaunchPadMachine;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Launching, landing and collecting missions. All of it runs on the server; clients only see snapshots. */
public final class ContinuumMissions {

    private ContinuumMissions() {}

    /**
     * @param probes how many extraction probes to carry; ignored for a survey
     * @return null if the mission launched, otherwise the reason it did not (shown to the player)
     */
    public static @Nullable String launch(ServerPlayer player, ResourceLocation destinationId, BlockPos pad,
                                          Mission.Type type, int probes) {
        var cfg = PhoenixConfigs.INSTANCE.continuum;
        MinecraftServer server = player.server;

        var level = player.level();
        if (!level.isLoaded(pad)) return "There is no launch pad there.";
        double range = cfg.launchRangeBlocks;
        if (player.distanceToSqr(Vec3.atCenterOf(pad)) > range * range) {
            return "You are too far from the launch pad.";
        }

        ContinuumBody body = ContinuumData.body(destinationId);
        if (body == null) return "That destination does not exist.";

        // the pad is either a Launch Complex (tier and power matter) or, if allowed, the plain testing block
        LaunchPadMachine complex = MetaMachine.getMachine(level, pad) instanceof LaunchPadMachine machine ? machine :
                null;
        long launchEnergy = 0;
        if (complex != null) {
            if (!complex.isFormed()) return "The launch complex is not fully built.";

            int padTier = complex.padTier();
            int needed = requiredPadTier(body);
            if (padTier < needed) {
                return "This pad is tier " + tierName(padTier) + " but " + body.name() + " needs tier " +
                        tierName(needed) + " or better. Fit a stronger energy hatch.";
            }

            launchEnergy = GTValues.V[Math.max(0, Math.min(padTier, GTValues.V.length - 1))] * cfg.launchEnergyAmpTicks;
            if (complex.storedEnergy() < launchEnergy) {
                return "The pad needs " + launchEnergy + " EU stored in its energy hatches to launch (it has " +
                        complex.storedEnergy() + ").";
            }
        } else if (level.getBlockState(pad).getBlock() instanceof LaunchPadBlock) {
            if (cfg.requireMultiblockPad) return "Missions launch from a Continuum Launch Complex. Build one.";
        } else {
            return "There is no launch pad there.";
        }

        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        ContinuumTeamData data = ContinuumTeamData.get(server);

        DiscoveryStage stage = data.effectiveStage(server, team, destinationId);
        if (stage == DiscoveryStage.UNKNOWN) return "That signal has not been resolved yet.";

        if (data.missions(team).size() >= cfg.maxMissionsPerTeam) {
            return "Your team has too many missions out. Collect some first.";
        }

        // everything that can refuse a launch is checked before anything is taken from the player
        long now = System.currentTimeMillis();
        int carried = 0;
        Outpost outpost = null;
        switch (type) {
            case EXTRACT -> {
                if (stage != DiscoveryStage.SURVEYED) {
                    return "Extraction needs a surveyed body. Fly a survey there first.";
                }
                if (body.yields().isEmpty()) return "There is nothing to extract there.";
                carried = Math.max(1, Math.min(probes, cfg.maxProbesPerMission));
                if (countProbes(player) < carried) return notEnoughProbes(carried);
            }
            case DEPLOY -> {
                if (stage != DiscoveryStage.SURVEYED) return "Outposts can only be built on a surveyed body.";
                if (body.yields().isEmpty() || body.isCentral() || body.type() == ContinuumBody.Type.BLACK_HOLE) {
                    return "Nothing can be built there.";
                }
                Outpost existing = data.outpost(team, destinationId);
                int room = cfg.maxProbesPerOutpost - (existing == null ? 0 : existing.probes());
                if (room <= 0) return "That outpost is already full.";
                carried = Math.max(1, Math.min(Math.min(probes, room), cfg.maxProbesPerMission));
                if (countProbes(player) < carried) return notEnoughProbes(carried);
            }
            case HAUL -> {
                outpost = data.outpost(team, destinationId);
                if (outpost == null) return "Your team has no outpost there.";
                outpost.settle(now, RocketStats.outpostCycleMillis(), RocketStats.outpostMaxReady(),
                        OutpostPower.payerFor(server, team));
                if (outpost.readyCycles() <= 0) return "Nothing is ready to haul yet.";
            }
            case SURVEY -> {}
        }

        ItemStack rocket = findRocket(player);
        if (rocket.isEmpty()) return "You need a rocket in your inventory.";
        if (RocketStats.interlockRefuses(rocket)) {
            return "Hull interlock engaged: the rocket is too worn to launch. Repair it first.";
        }

        // the last thing that can refuse: the power. Taken now, along with everything else the launch consumes.
        if (complex != null && !complex.drainEnergy(launchEnergy)) {
            return "The pad lost power before it could launch.";
        }

        ItemStack flown = rocket.split(1);
        if (carried > 0) removeProbes(player, carried);

        // a haul empties the outpost now: if the trip fails, what it carried is gone
        int haulProbes = 0;
        int haulCycles = 0;
        if (type == Mission.Type.HAUL && outpost != null) {
            haulProbes = outpost.probes();
            haulCycles = outpost.claim(now);
            data.setDirty();
        }

        float failureChance = RocketStats.failureChance(flown, stage);
        boolean succeeds = player.getRandom().nextFloat() >= failureChance;

        List<ItemStack> rewards = new ArrayList<>();
        if (succeeds && type == Mission.Type.EXTRACT) rewards = rollRewards(body, carried, player.getRandom());
        if (succeeds && type == Mission.Type.HAUL) {
            rewards = rollRewards(body, haulProbes * haulCycles, player.getRandom());
        }

        int shownProbes = type == Mission.Type.HAUL ? haulProbes : carried;
        data.addMission(new Mission(UUID.randomUUID(), team, player.getUUID(), player.getGameProfile().getName(),
                destinationId, type, shownProbes, rewards, now, RocketStats.tripMillis(body, flown), succeeds,
                RocketStats.wearCost(body, flown), flown, Mission.State.ACTIVE));
        return null;
    }

    /** The pad tier a body's {@code gate.min_tier} asks for, from the config. */
    public static int requiredPadTier(ContinuumBody body) {
        var cfg = PhoenixConfigs.INSTANCE.continuum;
        return switch (body.minTier().toLowerCase(java.util.Locale.ROOT)) {
            case "early" -> cfg.padTierEarly;
            case "mid" -> cfg.padTierMid;
            case "late" -> cfg.padTierLate;
            case "endgame" -> cfg.padTierEndgame;
            default -> cfg.padTierStart;
        };
    }

    public static String tierName(int tier) {
        return GTValues.VN[Math.max(0, Math.min(tier, GTValues.VN.length - 1))];
    }

    private static String notEnoughProbes(int needed) {
        return "You need " + needed + " extraction probe" + (needed == 1 ? "" : "s") + " in your inventory.";
    }

    public static ItemStack findRocket(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumRocketItem) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumRocketItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static int countProbes(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumProbeItem) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumProbeItem) total += stack.getCount();
        }
        return total;
    }

    private static void removeProbes(ServerPlayer player, int count) {
        int remaining = count;
        for (var list : List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : list) {
                if (remaining <= 0) return;
                if (!(stack.getItem() instanceof ContinuumProbeItem)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    /** Rolls each probe against the body's yield table and merges the results into stacks. */
    static List<ItemStack> rollRewards(ContinuumBody body, int probes, RandomSource random) {
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < probes; i++) {
            for (ContinuumBody.Yield yield : body.yields()) {
                if (random.nextFloat() >= yield.chance()) continue;
                Item item = BuiltInRegistries.ITEM.get(yield.item());
                if (item == Items.AIR) continue;
                int count = yield.min() + random.nextInt(yield.max() - yield.min() + 1);
                totals.merge(item, count, Integer::sum);
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        totals.forEach((item, total) -> {
            int left = total;
            while (left > 0) {
                int size = Math.min(left, item.getMaxStackSize());
                stacks.add(new ItemStack(item, size));
                left -= size;
            }
        });
        return stacks;
    }

    /** Lands every mission whose time is up. Called about once a second. */
    public static void resolveDue(MinecraftServer server) {
        long now = System.currentTimeMillis();
        ContinuumTeamData data = ContinuumTeamData.get(server);

        for (UUID team : data.teamsWithMissions()) {
            boolean changed = false;
            for (Mission mission : List.copyOf(data.missions(team))) {
                if (!mission.due(now)) continue;
                land(server, data, mission);
                changed = true;
            }
            if (changed) {
                data.setDirty();
                ContinuumServerEvents.sendStateToTeam(server, team);
            }
        }
    }

    private static void land(MinecraftServer server, ContinuumTeamData data, Mission mission) {
        ContinuumBody destination = ContinuumData.body(mission.destination);
        String name = destination != null ? destination.name() : mission.destination.getPath();
        String probesText = mission.probes + " probe" + (mission.probes == 1 ? "" : "s");

        float wear = RocketStats.wear(mission.rocket) + mission.wearCost;
        if (mission.willSucceed) {
            mission.state = Mission.State.SUCCESS;
            RocketStats.setWear(mission.rocket, wear);

            String message = switch (mission.type) {
                case SURVEY -> {
                    if (destination != null) reveal(data, mission.team, destination);
                    yield "Mission to " + name + " landed: survey complete.";
                }
                case EXTRACT -> "Extraction run to " + name + " landed: resources are waiting for collection.";
                case DEPLOY -> {
                    data.deployProbes(mission.team, mission.destination, mission.probes, System.currentTimeMillis());
                    yield "Outpost on " + name + " established with " + probesText + ". It is producing now.";
                }
                case HAUL -> "Haul from " + name + " landed: the stockpile is waiting for collection.";
            };
            notifyTeam(server, mission.team, Component.literal(message).withStyle(ChatFormatting.GREEN));
        } else {
            mission.state = Mission.State.FAILED;
            RocketStats.setWear(mission.rocket, wear + RocketStats.FAILURE_WEAR);

            String lost = switch (mission.type) {
                case SURVEY -> "";
                case EXTRACT, DEPLOY -> " The " + probesText + " were lost.";
                case HAUL -> " The haul was lost.";
            };
            notifyTeam(server, mission.team,
                    Component.literal(mission.type.label() + " to " + name + " failed. The rocket came back damaged." +
                            lost).withStyle(ChatFormatting.RED));
        }
    }

    /** A landed survey resolves its target and moons, and picks up one more signal in the same system. */
    private static void reveal(ContinuumTeamData data, UUID team, ContinuumBody destination) {
        data.raise(team, destination.id(), DiscoveryStage.SURVEYED);

        for (ContinuumBody moon : ContinuumData.moonsOf(destination.id())) {
            data.raise(team, moon.id(), DiscoveryStage.DETECTED);
        }

        // the nearest still-unknown body in the system shows up as a signal
        ContinuumBody nearest = null;
        for (ContinuumBody other : ContinuumData.bodiesOf(destination.system())) {
            if (other.isMoon() || other.id().equals(destination.id())) continue;
            DiscoveryStage stage = data.storedStage(team, other.id());
            if (stage != DiscoveryStage.UNKNOWN) continue;
            if (nearest == null || other.orbitAu() < nearest.orbitAu()) nearest = other;
        }
        if (nearest != null) data.raise(team, nearest.id(), DiscoveryStage.DETECTED);
    }

    /** @return null on success, otherwise the reason */
    public static @Nullable String collect(ServerPlayer player, UUID missionId) {
        MinecraftServer server = player.server;
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        ContinuumTeamData data = ContinuumTeamData.get(server);

        Mission mission = data.mission(team, missionId).orElse(null);
        if (mission == null) return "That mission is gone.";
        if (!mission.state.finished()) return "That mission is still under way.";

        ItemHandlerHelper.giveItemToPlayer(player, mission.rocket.copy());
        int items = 0;
        for (ItemStack reward : mission.rewards) {
            items += reward.getCount();
            ItemHandlerHelper.giveItemToPlayer(player, reward.copy());
        }
        data.removeMission(team, missionId);
        ContinuumServerEvents.sendStateToTeam(server, team);

        if (items > 0) {
            player.sendSystemMessage(Component.literal("Collected the rocket and " + items + " resources.")
                    .withStyle(ChatFormatting.GREEN));
        }
        return null;
    }

    static void notifyTeam(MinecraftServer server, UUID team, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (TeamUtils.isPlayerOnTeam(player, team)) player.sendSystemMessage(message);
        }
    }
}
