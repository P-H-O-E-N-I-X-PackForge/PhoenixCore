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
import net.phoenix.core.integration.continuum.item.ContinuumRepairKitItem;
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

    private static final java.util.function.Predicate<ItemStack> IS_ROCKET = s -> s.getItem() instanceof ContinuumRocketItem;
    private static final java.util.function.Predicate<ItemStack> IS_PROBE = s -> s.getItem() instanceof ContinuumProbeItem;
    private static final java.util.function.Predicate<ItemStack> IS_KIT = s -> s.getItem() instanceof ContinuumRepairKitItem;

    /** Where a launch takes its rocket, probes and repair kits from: the complex's item input buses, or the player. */
    private interface Stock {

        ItemStack peekRocket();

        int probes();

        int kits();

        ItemStack takeRocket();

        void takeProbes(int count);

        void takeKits(int count);

        String place();
    }

    private record BusStock(LaunchPadMachine complex) implements Stock {

        @Override
        public ItemStack peekRocket() {
            return complex.peek(IS_ROCKET);
        }

        @Override
        public int probes() {
            return complex.count(IS_PROBE);
        }

        @Override
        public int kits() {
            return complex.count(IS_KIT);
        }

        @Override
        public ItemStack takeRocket() {
            List<ItemStack> taken = complex.take(IS_ROCKET, 1);
            return taken.isEmpty() ? ItemStack.EMPTY : taken.get(0);
        }

        @Override
        public void takeProbes(int count) {
            complex.take(IS_PROBE, count);
        }

        @Override
        public void takeKits(int count) {
            complex.take(IS_KIT, count);
        }

        @Override
        public String place() {
            return "in the launch complex's item input buses";
        }
    }

    private record PlayerStock(ServerPlayer player) implements Stock {

        @Override
        public ItemStack peekRocket() {
            return findRocket(player);
        }

        @Override
        public int probes() {
            return countProbes(player);
        }

        @Override
        public int kits() {
            return countKits(player);
        }

        @Override
        public ItemStack takeRocket() {
            ItemStack rocket = findRocket(player);
            return rocket.isEmpty() ? ItemStack.EMPTY : rocket.split(1);
        }

        @Override
        public void takeProbes(int count) {
            removeProbes(player, count);
        }

        @Override
        public void takeKits(int count) {
            removeKits(player, count);
        }

        @Override
        public String place() {
            return "in your inventory";
        }
    }

    /** What a pad can launch with right now, for the planner. {@code buses} says where it was counted. */
    public record PadStockView(boolean buses, ItemStack rocket, int probes, int kits) {}

    public static PadStockView padStock(ServerPlayer player, BlockPos pad) {
        var level = player.level();
        if (level.isLoaded(pad) && MetaMachine.getMachine(level, pad) instanceof LaunchPadMachine complex &&
                complex.isFormed()) {
            BusStock stock = new BusStock(complex);
            return new PadStockView(true, stock.peekRocket(), stock.probes(), stock.kits());
        }
        PlayerStock stock = new PlayerStock(player);
        return new PadStockView(false, stock.peekRocket(), stock.probes(), stock.kits());
    }

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

        Stock stock = complex != null ? new BusStock(complex) : new PlayerStock(player);

        // everything that can refuse a launch is checked before anything is taken
        long now = System.currentTimeMillis();
        int carried = 0;
        int kitsCarried = 0;
        Outpost outpost = null;
        switch (type) {
            case EXTRACT -> {
                if (stage != DiscoveryStage.SURVEYED) {
                    return "Extraction needs a surveyed body. Fly a survey there first.";
                }
                if (body.yields().isEmpty()) return "There is nothing to extract there.";
                carried = Math.max(1, Math.min(probes, cfg.maxProbesPerMission));
                if (stock.probes() < carried) return notEnoughProbes(carried, stock);
            }
            case DEPLOY -> {
                if (stage != DiscoveryStage.SURVEYED) return "Outposts can only be built on a surveyed body.";
                if (body.yields().isEmpty()) return "Nothing can be built there.";
                Outpost existing = data.outpost(team, destinationId);
                int room = cfg.maxProbesPerOutpost - (existing == null ? 0 : existing.probes());
                if (room <= 0) return "That outpost is already full.";
                carried = Math.max(1, Math.min(Math.min(probes, room), cfg.maxProbesPerMission));
                if (stock.probes() < carried) return notEnoughProbes(carried, stock);
            }
            case HAUL -> {
                outpost = data.outpost(team, destinationId);
                if (outpost == null) return "Your team has no outpost there.";
                outpost.settle(now, RocketStats.outpostCycleMillis(), RocketStats.outpostMaxReady(),
                        OutpostPower.payerFor(server, team));
                if (outpost.readyCycles() <= 0) return "Nothing is ready to haul yet.";
            }
            case REPAIR -> {
                outpost = data.outpost(team, destinationId);
                if (outpost == null) return "Your team has no outpost there.";
                outpost.settle(now, RocketStats.outpostCycleMillis(), RocketStats.outpostMaxReady(),
                        OutpostPower.payerFor(server, team));
                if (!outpost.damaged()) return "That outpost is not damaged.";
                kitsCarried = Math.max(1, cfg.outpostRepairKits);
                if (stock.kits() < kitsCarried) {
                    return "You need " + kitsCarried + " repair kit" + (kitsCarried == 1 ? "" : "s") + " " +
                            stock.place() + " to repair an outpost.";
                }
            }
            case SURVEY -> {
                // a surveyed body can still be surveyed deeper, until every deposit on it is mapped
                if (stage == DiscoveryStage.SURVEYED &&
                        (body.yields().isEmpty() || data.depth(team, destinationId) >= body.yields().size())) {
                    return body.name() + " is fully surveyed: every deposit is mapped.";
                }
            }
        }

        ItemStack rocket = stock.peekRocket();
        if (rocket.isEmpty()) return "You need a rocket " + stock.place() + ".";
        // working close to a star or a black hole takes shielding (merely observing it does not)
        String shield = RocketStats.requiredShield(body);
        if (shield != null && type != Mission.Type.SURVEY && RocketStats.level(rocket, shield) <= 0) {
            return "The rocket cannot work that close to " + body.name() + ". Fit a " + RocketStats.shieldName(shield) +
                    " (use it with the rocket in your other hand).";
        }
        if (RocketStats.interlockRefuses(rocket)) {
            return "Hull interlock engaged: the rocket is too worn to launch. Repair it first.";
        }

        // the last thing that can refuse: the power. Taken now, along with everything else the launch consumes.
        if (complex != null && !complex.drainEnergy(launchEnergy)) {
            return "The pad lost power before it could launch.";
        }

        ItemStack flown = stock.takeRocket();
        if (flown.isEmpty()) return "The rocket was taken before the launch could start.";
        if (carried > 0) stock.takeProbes(carried);
        if (kitsCarried > 0) stock.takeKits(kitsCarried);

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

        // a random event, rolled now and revealed when the mission lands
        int payload = type == Mission.Type.HAUL ? haulProbes : type == Mission.Type.REPAIR ? kitsCarried : carried;
        MissionEvent event = MissionEvent.roll(type, payload, player.getRandom(), cfg.missionEventChance);
        List<String> events = new ArrayList<>();
        if (event != null) events.add(event.id);

        List<ItemStack> rewards = new ArrayList<>();
        if (succeeds && type == Mission.Type.EXTRACT) {
            int rolls = carried - (event == MissionEvent.MICROMETEOROIDS ? 1 : 0) +
                    (event == MissionEvent.DERELICT ? 1 : 0);
            rewards = rollRewards(team, body, rolls, player.getRandom());
        }
        if (succeeds && type == Mission.Type.HAUL) {
            int rolls = haulProbes * haulCycles + (event == MissionEvent.DERELICT ? haulProbes : 0);
            rewards = rollRewards(team, body, rolls, player.getRandom());
        }

        long trip = RocketStats.tripMillis(body, flown);
        if (event == MissionEvent.TAILWIND) trip = Math.max(1000L, Math.round(trip * MissionEvent.TAILWIND_FACTOR));

        int shownProbes = type == Mission.Type.HAUL ? haulProbes : type == Mission.Type.REPAIR ? kitsCarried : carried;
        data.addMission(new Mission(UUID.randomUUID(), team, player.getUUID(), player.getGameProfile().getName(),
                destinationId, type, shownProbes, rewards, now, trip, succeeds,
                RocketStats.wearCost(body, flown), flown, Mission.State.ACTIVE, events));
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

    private static String notEnoughProbes(int needed, Stock stock) {
        return "You need " + needed + " extraction probe" + (needed == 1 ? "" : "s") + " " + stock.place() + ".";
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

    private static int countKits(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumRepairKitItem) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumRepairKitItem) total += stack.getCount();
        }
        return total;
    }

    private static void removeKits(ServerPlayer player, int count) {
        int remaining = count;
        for (var list : List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : list) {
                if (remaining <= 0) return;
                if (!(stack.getItem() instanceof ContinuumRepairKitItem)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
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
    static List<ItemStack> rollRewards(UUID team, ContinuumBody body, int probes, RandomSource random) {
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < probes; i++) {
            for (ContinuumBody.Yield yield : body.yields()) {
                if (random.nextFloat() >= yield.chance()) continue;
                Item item = BuiltInRegistries.ITEM.get(yield.item());
                if (item == Items.AIR) continue;
                int base = yield.min() + random.nextInt(yield.max() - yield.min() + 1);
                // the deposit's richness (unknown to the team until it is mapped) scales what comes back
                int count = Math.max(1, Math.round(base * Deposits.richness(team, body.id(), yield.item())));
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

        // only events that still matter count: a failure keeps just the solar flare
        mission.events.removeIf(id -> {
            MissionEvent e = MissionEvent.byId(id);
            return e == null || (!mission.willSucceed && e != MissionEvent.SOLAR_FLARE);
        });
        boolean flare = mission.events.contains(MissionEvent.SOLAR_FLARE.id);
        boolean micro = mission.events.contains(MissionEvent.MICROMETEOROIDS.id);
        boolean echo = mission.events.contains(MissionEvent.SIGNAL_ECHO.id);
        // (rebuilt after the survey below, which can add anomalies)

        float wear = RocketStats.wear(mission.rocket) + mission.wearCost + (flare ? MissionEvent.SOLAR_FLARE_WEAR : 0.0f);
        if (mission.willSucceed) {
            mission.state = Mission.State.SUCCESS;
            RocketStats.setWear(mission.rocket, wear);

            String message = switch (mission.type) {
                case SURVEY -> {
                    if (destination != null) {
                        boolean first = !data.storedStage(mission.team, destination.id())
                                .atLeast(DiscoveryStage.SURVEYED);
                        if (first) reveal(data, mission.team, destination);
                        if (echo) revealExtra(data, mission.team, destination);
                        surveyFindings(data, mission, destination);
                    }
                    yield "Mission to " + name + " landed: survey complete.";
                }
                case EXTRACT -> "Extraction run to " + name + " landed: resources are waiting for collection.";
                case DEPLOY -> {
                    int deployed = Math.max(1, mission.probes - (micro ? 1 : 0));
                    data.deployProbes(mission.team, mission.destination, deployed, System.currentTimeMillis());
                    yield "Outpost on " + name + " established with " + deployed + " probe" +
                            (deployed == 1 ? "" : "s") + ". It is producing now.";
                }
                case HAUL -> "Haul from " + name + " landed: the stockpile is waiting for collection.";
                case REPAIR -> {
                    Outpost repaired = data.outpost(mission.team, mission.destination);
                    if (repaired != null) repaired.repair(System.currentTimeMillis());
                    yield "Repair run to " + name + " landed: the outpost is producing again.";
                }
            };
            notifyTeam(server, mission.team,
                    Component.literal(message + eventText(mission)).withStyle(ChatFormatting.GREEN));
        } else {
            mission.state = Mission.State.FAILED;
            RocketStats.setWear(mission.rocket, wear + RocketStats.FAILURE_WEAR);

            String lost = switch (mission.type) {
                case SURVEY -> "";
                case EXTRACT, DEPLOY -> " The " + probesText + " were lost.";
                case HAUL -> " The haul was lost.";
                case REPAIR -> " The repair kits were lost and the outpost is still broken.";
            };
            notifyTeam(server, mission.team,
                    Component.literal(mission.type.label() + " to " + name + " failed. The rocket came back damaged." +
                            lost + eventText(mission)).withStyle(ChatFormatting.RED));
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

    private static String eventText(Mission mission) {
        StringBuilder text = new StringBuilder();
        for (String id : mission.events) {
            MissionEvent event = MissionEvent.byId(id);
            if (event != null) text.append(' ').append(event.text);
        }
        return text.toString();
    }

    /**
     * What a landed survey adds: it maps the next unmapped deposit, and may turn up an anomaly (a cache, an extra
     * mapped deposit, or ruins), which is added to the mission as an event so the landing card can show it.
     */
    private static void surveyFindings(ContinuumTeamData data, Mission mission, ContinuumBody destination) {
        int total = destination.yields().size();
        data.revealDeposit(mission.team, destination.id(), total);

        RandomSource random = RandomSource.create();
        if (random.nextDouble() >= PhoenixConfigs.INSTANCE.continuum.surveyAnomalyChance) return;

        MissionEvent anomaly = MissionEvent.rollAnomaly(random);
        mission.events.add(anomaly.id);
        switch (anomaly) {
            case SURFACE_CACHE -> mission.rewards.addAll(rollRewards(mission.team, destination, 2, random));
            case ANCIENT_RUINS -> mission.rewards.addAll(rollRewards(mission.team, destination, 5, random));
            case VEIN_SIGNATURE -> data.revealDeposit(mission.team, destination.id(), total);
            default -> {}
        }
    }

    /** A signal echo reveals one more unknown body in the system as a signal. */
    private static void revealExtra(ContinuumTeamData data, UUID team, ContinuumBody destination) {
        ContinuumBody nearest = null;
        for (ContinuumBody other : ContinuumData.bodiesOf(destination.system())) {
            if (other.isMoon() || other.id().equals(destination.id())) continue;
            if (data.storedStage(team, other.id()) != DiscoveryStage.UNKNOWN) continue;
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
