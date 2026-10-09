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

public final class ContinuumMissions {

    private ContinuumMissions() {}

    private static final java.util.function.Predicate<ItemStack> IS_ROCKET = s -> s
            .getItem() instanceof ContinuumRocketItem;
    private static final java.util.function.Predicate<ItemStack> IS_PROBE = s -> s
            .getItem() instanceof ContinuumProbeItem;
    private static final java.util.function.Predicate<ItemStack> IS_KIT = s -> s
            .getItem() instanceof ContinuumRepairKitItem;

    private interface Stock {

        ItemStack peekRocket();

        int rockets();

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
        public int rockets() {
            return complex.count(IS_ROCKET);
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
        public int rockets() {
            return countRockets(player);
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

    public record PadStockView(boolean buses, ItemStack rocket, int probes, int kits, int rockets) {}

    public static PadStockView padStock(ServerPlayer player, BlockPos pad) {
        var level = player.level();
        if (level.isLoaded(pad) && MetaMachine.getMachine(level, pad) instanceof LaunchPadMachine complex &&
                complex.isFormed()) {
            BusStock stock = new BusStock(complex);
            return new PadStockView(true, stock.peekRocket(), stock.probes(), stock.kits(), stock.rockets());
        }
        PlayerStock stock = new PlayerStock(player);
        return new PadStockView(false, stock.peekRocket(), stock.probes(), stock.kits(), stock.rockets());
    }

    public static @Nullable String launchMany(ServerPlayer player, ResourceLocation destinationId, BlockPos pad,
                                              Mission.Type type, int probes, int count) {
        boolean convoy = type == Mission.Type.EXTRACT || type == Mission.Type.DEPLOY || type == Mission.Type.RESCUE;
        int wanted = convoy ? Math.max(1, Math.min(count, PhoenixConfigs.INSTANCE.continuum.maxRocketsPerLaunch)) : 1;

        int launched = 0;
        String error = null;
        for (int i = 0; i < wanted; i++) {
            error = launch(player, destinationId, pad, type, probes);
            if (error != null) break;
            launched++;
        }
        if (launched == 0) return error;
        if (wanted > 1) {
            ContinuumBody body = ContinuumData.body(destinationId);
            String where = body != null ? body.name() : destinationId.getPath();
            player.sendSystemMessage(
                    Component.literal("Launched " + launched + " of " + wanted + " rockets to " + where +
                            (error != null ? " (stopped: " + error + ")" : ".")).withStyle(
                                    error != null ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }
        return null;
    }

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

        if (data.missions(team).size() >= cfg.maxMissionsPerTeam + data.stationCount(team)) {
            return "Your team has too many missions out. Collect some first (each orbital station adds a slot).";
        }
        int stationLevel = data.stationLevel(team, destinationId);
        Affinities.Factors affinity = Affinities.of(server, team, body);

        Stock stock = complex != null ? new BusStock(complex) : new PlayerStock(player);

        long now = System.currentTimeMillis();
        int carried = 0;
        int kitsCarried = 0;
        Outpost outpost = null;
        Mission stranded = null;
        switch (type) {
            case STATION -> {
                if (stage != DiscoveryStage.SURVEYED) return "Stations can only be built at a surveyed body.";
                if (stationLevel >= Stations.MAX_LEVEL) return "The station at " + body.name() + " is fully built.";
                for (Mission other : data.missions(team)) {
                    if (other.type == Mission.Type.STATION && other.destination.equals(destinationId) &&
                            !other.state.finished()) {
                        return "A station crew is already on its way to " + body.name() + ".";
                    }
                }
                carried = Math.max(1, cfg.stationProbesPerLevel);
                if (stock.probes() < carried) return notEnoughProbes(carried, stock);
            }
            case RESCUE -> {
                stranded = strandedAt(data, team, destinationId);
                if (stranded == null) return "There is no distress signal at " + body.name() + ".";
                if (stranded.rescuing) return "A rescue is already on its way to " + body.name() + ".";
                kitsCarried = Math.max(1, cfg.rescueKits);
                if (stock.kits() < kitsCarried) {
                    return "A rescue run needs " + kitsCarried + " repair kit" + (kitsCarried == 1 ? "" : "s") + " " +
                            stock.place() + ".";
                }
            }
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

                for (Mission other : data.missions(team)) {
                    if (other.type == Mission.Type.DEPLOY && other.destination.equals(destinationId) &&
                            !other.state.finished()) {
                        room -= other.probes;
                    }
                }
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

                if (stage == DiscoveryStage.SURVEYED &&
                        (body.yields().isEmpty() || data.depth(team, destinationId) >= body.yields().size())) {
                    return body.name() + " is fully surveyed: every deposit is mapped.";
                }
            }
        }

        ItemStack rocket = stock.peekRocket();
        if (rocket.isEmpty()) return "You need a rocket " + stock.place() + ".";

        String shield = RocketStats.requiredShield(body);
        if (shield != null && type != Mission.Type.SURVEY && RocketStats.level(rocket, shield) <= 0) {
            return "The rocket cannot work that close to " + body.name() + ". Fit a " + RocketStats.shieldName(shield) +
                    " (use it with the rocket in your other hand).";
        }
        if (RocketStats.interlockRefuses(rocket)) {
            return "Hull interlock engaged: the rocket is too worn to launch. Repair it first.";
        }
        if (stranded != null) {
            long fades = stranded.distressUntil - now;
            if (Math.round(RocketStats.tripMillis(body, rocket) * Stations.tripFactor(stationLevel) * affinity.trip()) >
                    fades) {
                return "The distress signal will fade before a rescue could arrive (it has " +
                        net.phoenix.core.integration.continuum.common.ContinuumMissions.durationText(fades) + " left).";
            }
        }

        if (complex != null && !complex.drainEnergy(launchEnergy)) {
            return "The pad lost power before it could launch.";
        }

        ItemStack flown = stock.takeRocket();
        if (flown.isEmpty()) return "The rocket was taken before the launch could start.";
        if (carried > 0) stock.takeProbes(carried);
        if (kitsCarried > 0) stock.takeKits(kitsCarried);

        int haulProbes = 0;
        int haulCycles = 0;
        if (type == Mission.Type.HAUL && outpost != null) {
            haulProbes = outpost.probes();
            haulCycles = outpost.claim(now);
            data.setDirty();
        }

        float failureChance = RocketStats.failureChance(flown, stage);
        boolean succeeds = player.getRandom().nextFloat() >= failureChance;

        boolean kitRun = type == Mission.Type.REPAIR || type == Mission.Type.RESCUE;
        int payload = type == Mission.Type.HAUL ? haulProbes : kitRun ? kitsCarried : carried;
        MissionEvent event = MissionEvent.roll(type, payload, player.getRandom(), cfg.missionEventChance);
        List<String> events = new ArrayList<>();
        if (event != null) events.add(event.id);

        List<ItemStack> rewards = new ArrayList<>();
        if (succeeds && type == Mission.Type.EXTRACT) {
            int rolls = carried - (event == MissionEvent.MICROMETEOROIDS ? 1 : 0) +
                    (event == MissionEvent.DERELICT ? 1 : 0);
            rewards = rollRewards(team, body,
                    Affinities.scaled(Math.max(0, rolls), affinity.yield(), player.getRandom()),
                    player.getRandom());
        }
        if (succeeds && type == Mission.Type.HAUL) {
            int rolls = Affinities.scaled(haulProbes * haulCycles + (event == MissionEvent.DERELICT ? haulProbes : 0),
                    Stations.yieldFactor(stationLevel) * affinity.yield(), player.getRandom());
            rewards = rollRewards(team, body, rolls, player.getRandom());
        }

        long trip = Math.round(RocketStats.tripMillis(body, flown) * Stations.tripFactor(stationLevel) *
                affinity.trip());
        if (event == MissionEvent.TAILWIND) trip = Math.max(1000L, Math.round(trip * MissionEvent.TAILWIND_FACTOR));

        int shownProbes = type == Mission.Type.HAUL ? haulProbes : kitRun ? kitsCarried : carried;
        Mission mission = new Mission(UUID.randomUUID(), team, player.getUUID(), player.getGameProfile().getName(),
                destinationId, type, shownProbes, rewards, now, trip, succeeds,
                RocketStats.wearCost(body, flown) * Stations.wearFactor(stationLevel) * affinity.wear(), flown,
                Mission.State.ACTIVE, events);
        if (complex != null) {
            mission.padDimension = level.dimension().location().toString();
            mission.padPos = pad.asLong();
        }
        data.addMission(mission);
        if (stranded != null) {
            stranded.rescuing = true;
            data.setDirty();
        }
        return null;
    }

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

    private static int countRockets(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ContinuumRocketItem) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ContinuumRocketItem) total += stack.getCount();
        }
        return total;
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

    static List<ItemStack> rollRewards(UUID team, ContinuumBody body, int probes, RandomSource random) {
        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < probes; i++) {
            for (ContinuumBody.Yield yield : body.yields()) {
                if (random.nextFloat() >= yield.chance()) continue;
                Item item = BuiltInRegistries.ITEM.get(yield.item());
                if (item == Items.AIR) continue;
                int base = yield.min() + random.nextInt(yield.max() - yield.min() + 1);

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

            for (Mission mission : List.copyOf(data.missions(team))) {
                if (!mission.stranded() || mission.rescuing || now < mission.distressUntil) continue;
                ContinuumBody where = ContinuumData.body(mission.destination);
                data.removeMission(team, mission.id);
                notifyTeam(server, team, Component.literal("The distress signal from " +
                        (where != null ? where.name() : mission.destination.getPath()) +
                        " faded. The stranded rocket is lost.").withStyle(ChatFormatting.DARK_RED));
                changed = true;
            }
            if (changed) {
                data.setDirty();
                ContinuumServerEvents.sendStateToTeam(server, team);
            }
        }
    }

    public static int collectAll(ServerPlayer player) {
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        ContinuumTeamData data = ContinuumTeamData.get(player.server);
        int collected = 0;
        for (Mission mission : List.copyOf(data.missions(team))) {
            if (!mission.state.finished() || mission.stranded()) continue;
            if (collect(player, mission.id) == null) collected++;
        }
        if (collected == 0) {
            player.sendSystemMessage(Component.literal("Nothing is ready to collect.").withStyle(ChatFormatting.GRAY));
        }
        return collected;
    }

    private static @Nullable Mission strandedAt(ContinuumTeamData data, UUID team, ResourceLocation body) {
        return strandedAt(data, team, body, false);
    }

    private static @Nullable Mission strandedAt(ContinuumTeamData data, UUID team, ResourceLocation body,
                                                boolean beingRescued) {
        Mission best = null;
        for (Mission mission : data.missions(team)) {
            if (!mission.stranded() || !mission.destination.equals(body)) continue;
            if (!beingRescued && mission.rescuing) continue;
            if (beingRescued && !mission.rescuing) continue;
            if (best == null || mission.distressUntil < best.distressUntil) best = mission;
        }
        return best;
    }

    public static String durationText(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + String.format("%02dm", minutes);
        return minutes + "m " + String.format("%02ds", seconds % 60);
    }

    private static void land(MinecraftServer server, ContinuumTeamData data, Mission mission) {
        ContinuumBody destination = ContinuumData.body(mission.destination);
        String name = destination != null ? destination.name() : mission.destination.getPath();
        String probesText = mission.probes + " probe" + (mission.probes == 1 ? "" : "s");

        mission.events.removeIf(id -> {
            MissionEvent e = MissionEvent.byId(id);
            return e == null || (!mission.willSucceed && e != MissionEvent.SOLAR_FLARE);
        });
        boolean flare = mission.events.contains(MissionEvent.SOLAR_FLARE.id);
        boolean micro = mission.events.contains(MissionEvent.MICROMETEOROIDS.id);
        boolean echo = mission.events.contains(MissionEvent.SIGNAL_ECHO.id);

        float wear = RocketStats.wear(mission.rocket) + mission.wearCost +
                (flare ? MissionEvent.SOLAR_FLARE_WEAR : 0.0f);
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
                case STATION -> {
                    int built = data.stationLevel(mission.team, mission.destination) + 1;
                    data.setStationLevel(mission.team, mission.destination, built);
                    yield "Orbital station at " + name + " is now level " + Math.min(built, Stations.MAX_LEVEL) +
                            ". " + Stations.describe(built);
                }
                case RESCUE -> {
                    Mission saved = strandedAt(data, mission.team, mission.destination, true);
                    if (saved != null) {
                        saved.distressUntil = 0;
                        saved.rescuing = false;
                    }
                    yield "Rescue run to " + name + " landed: the stranded rocket is recovered and waiting for " +
                            "collection.";
                }
            };
            notifyTeam(server, mission.team,
                    Component.literal(message + eventText(mission)).withStyle(ChatFormatting.GREEN));
        } else {
            mission.state = Mission.State.FAILED;
            RocketStats.setWear(mission.rocket, wear + RocketStats.FAILURE_WEAR);

            String lost = switch (mission.type) {
                case SURVEY -> "";
                case EXTRACT, DEPLOY, STATION -> " The " + probesText + " were lost.";
                case HAUL -> " The haul was lost.";
                case REPAIR -> " The repair kits were lost and the outpost is still broken.";
                case RESCUE -> " The repair kits were lost and the stranded rocket is still waiting.";
            };

            if (mission.type == Mission.Type.RESCUE) {
                Mission waiting = strandedAt(data, mission.team, mission.destination, true);
                if (waiting != null) waiting.rescuing = false;
            }

            var cfg = PhoenixConfigs.INSTANCE.continuum;
            boolean canStrand = mission.type == Mission.Type.EXTRACT || mission.type == Mission.Type.DEPLOY ||
                    mission.type == Mission.Type.HAUL;
            if (canStrand && !Stations.preventsStranding(data.stationLevel(mission.team, mission.destination)) &&
                    RandomSource.create().nextDouble() < cfg.distressChance) {
                mission.distressUntil = System.currentTimeMillis() + cfg.distressWindowMinutes * 60_000L;
                notifyTeam(server, mission.team, Component.literal("DISTRESS SIGNAL from " + name + ": the " +
                        mission.type.label().toLowerCase() + " failed and the rocket is stranded." + lost +
                        " Send a rescue run with " + Math.max(1, cfg.rescueKits) + " repair kits within " +
                        durationText(cfg.distressWindowMinutes * 60_000L) + " or it is lost." + eventText(mission))
                        .withStyle(ChatFormatting.GOLD));
            } else {
                notifyTeam(server, mission.team,
                        Component.literal(mission.type.label() + " to " + name + " failed. The rocket came back " +
                                "damaged." + lost + eventText(mission)).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void reveal(ContinuumTeamData data, UUID team, ContinuumBody destination) {
        data.raise(team, destination.id(), DiscoveryStage.SURVEYED);

        for (ContinuumBody moon : ContinuumData.moonsOf(destination.id())) {
            data.raise(team, moon.id(), DiscoveryStage.DETECTED);
        }

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

    private static void revealExtra(ContinuumTeamData data, UUID team, ContinuumBody destination) {
        ContinuumBody nearest = null;
        for (ContinuumBody other : ContinuumData.bodiesOf(destination.system())) {
            if (other.isMoon() || other.id().equals(destination.id())) continue;
            if (data.storedStage(team, other.id()) != DiscoveryStage.UNKNOWN) continue;
            if (nearest == null || other.orbitAu() < nearest.orbitAu()) nearest = other;
        }
        if (nearest != null) data.raise(team, nearest.id(), DiscoveryStage.DETECTED);
    }

    public static @Nullable String collect(ServerPlayer player, UUID missionId) {
        MinecraftServer server = player.server;
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
        ContinuumTeamData data = ContinuumTeamData.get(server);

        Mission mission = data.mission(team, missionId).orElse(null);
        if (mission == null) return "That mission is gone.";
        if (!mission.state.finished()) return "That mission is still under way.";
        if (mission.stranded()) {
            return "That rocket is stranded. Send a rescue run (repair kits) before the signal fades in " +
                    durationText(mission.distressUntil - System.currentTimeMillis()) + ".";
        }

        LaunchPadMachine pad = launchComplexOf(server, mission);
        ItemStack rocket = mission.rocket.copy();
        int items = 0;
        int delivered = 0;
        if (pad != null) {
            rocket = pad.insertInput(rocket);
            if (!rocket.isEmpty()) rocket = pad.insertOutput(rocket);
        }
        if (!rocket.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, rocket);
        for (ItemStack reward : mission.rewards) {
            items += reward.getCount();
            ItemStack rest = pad != null ? pad.insertOutput(reward.copy()) : reward.copy();
            delivered += reward.getCount() - rest.getCount();
            if (!rest.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, rest);
        }
        data.removeMission(team, missionId);
        ContinuumServerEvents.sendStateToTeam(server, team);

        if (items > 0) {
            String where = delivered >= items ? "all of it in the launch complex's output buses" :
                    delivered > 0 ? delivered + " in the output buses, the rest in your inventory" :
                            "in your inventory";
            player.sendSystemMessage(
                    Component.literal("Collected the rocket and " + items + " resources (" + where + ").")
                            .withStyle(ChatFormatting.GREEN));
        }
        return null;
    }

    private static @Nullable LaunchPadMachine launchComplexOf(MinecraftServer server, Mission mission) {
        if (mission.padDimension == null) return null;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(mission.padDimension));
        var level = server.getLevel(key);
        BlockPos pos = BlockPos.of(mission.padPos);
        if (level == null || !level.isLoaded(pos)) return null;
        return MetaMachine.getMachine(level, pos) instanceof LaunchPadMachine machine && machine.isFormed() ? machine :
                null;
    }

    static void notifyTeam(MinecraftServer server, UUID team, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (TeamUtils.isPlayerOnTeam(player, team)) player.sendSystemMessage(message);
        }
    }
}
