package net.phoenix.core.saveddata;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;

public class TeslaTeamEnergyData extends SavedData {

    private static final String DATA_NAME = "phoenix_tesla_team_energy";
    private final Map<UUID, TeamEnergy> networks = new HashMap<>();
    private final Set<UUID> onlineNetworks = new HashSet<>();

    // --- DATA TRANSFER OBJECTS ---

    public static class HatchInfo {

        public final BlockPos pos;
        public final ResourceKey<Level> dimension;
        public BigInteger input = BigInteger.ZERO;
        public BigInteger output = BigInteger.ZERO;
        public BigInteger buffered = BigInteger.ZERO;
        public long displayFlow = 0;
        public boolean isPhysicalOutput = false;
        public boolean isSoulLinked = false;

        public HatchInfo(BlockPos pos, ResourceKey<Level> dimension) {
            this.pos = pos;
            this.dimension = dimension;
        }
    }

    // --- INTERNAL TEAM CLASS ---

    public static class TeamEnergy {

        public long lastNetInput = 0;
        public long lastNetOutput = 0;

        public BigInteger stored = BigInteger.ZERO;
        public BigInteger capacity = BigInteger.ZERO;

        // Persisted UI and Math Maps
        public final Map<BlockPos, Long> machineDisplayFlow = new HashMap<>();
        public final Map<BlockPos, Long> lastSeen = new HashMap<>();
        public final Map<BlockPos, BigInteger> energyBuffered = new HashMap<>();
        public final Map<BlockPos, Boolean> hatchIsOutput = new HashMap<>();

        // Dimension Registry (Fixes Cross-Dim Logic)
        public final Map<BlockPos, ResourceKey<Level>> posToDimension = new HashMap<>();
        public final Set<BlockPos> soulLinkedMachines = new HashSet<>();

        // Real-time accumulators (Not Persisted)
        public final Map<BlockPos, Long> machineCurrentFlow = new HashMap<>();
        public final Map<BlockPos, BigInteger> energyInput = new HashMap<>();
        public final Map<BlockPos, BigInteger> energyOutput = new HashMap<>();

        public void markHatchActive(BlockPos pos, long gameTime) {
            lastSeen.put(pos, gameTime);
        }

        public ResourceKey<Level> getMachineDimension(BlockPos pos) {
            return posToDimension.getOrDefault(pos, Level.OVERWORLD);
        }

        public int getLiveHatchCount(long currentGameTime) {
            lastSeen.entrySet().removeIf(entry -> (currentGameTime - entry.getValue()) > 40);
            return lastSeen.size();
        }

        public BigInteger fill(BigInteger amount) {
            if (amount.signum() <= 0) return BigInteger.ZERO;
            BigInteger space = capacity.subtract(stored).max(BigInteger.ZERO);
            BigInteger toFill = amount.min(space);
            stored = stored.add(toFill);
            return toFill;
        }

        public BigInteger drain(BigInteger amount) {
            if (amount.signum() <= 0) return BigInteger.ZERO;
            BigInteger toDrain = stored.min(amount);
            stored = stored.subtract(toDrain);
            return toDrain;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Stored", stored.toString());
            tag.putString("Capacity", capacity.toString());

            // Save Hatches with Dimensions
            ListTag hatchIOList = new ListTag();
            hatchIsOutput.forEach((pos, isOut) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("p", pos.asLong());
                entry.putBoolean("o", isOut);
                entry.putString("d", posToDimension.getOrDefault(pos, Level.OVERWORLD).location().toString());
                hatchIOList.add(entry);
            });
            tag.put("HatchIO", hatchIOList);

            // Save Soul Links with Dimensions
            ListTag soulList = new ListTag();
            soulLinkedMachines.forEach(pos -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("p", pos.asLong());
                entry.putString("d", posToDimension.getOrDefault(pos, Level.OVERWORLD).location().toString());
                soulList.add(entry);
            });
            tag.put("SoulLinks", soulList);

            // Save Buffers
            ListTag bufferList = new ListTag();
            energyBuffered.forEach((pos, buf) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("p", pos.asLong());
                entry.putString("b", buf.toString());
                bufferList.add(entry);
            });
            tag.put("Buffers", bufferList);

            // Save Flow Data
            ListTag flowList = new ListTag();
            machineDisplayFlow.forEach((pos, flow) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("p", pos.asLong());
                entry.putLong("f", flow);
                flowList.add(entry);
            });
            tag.put("FlowData", flowList);

            return tag;
        }

        // Inside TeslaTeamEnergyData.TeamEnergy class
        public final Set<BlockPos> activeChargers = new HashSet<>();

        public void addCharger(BlockPos pos) {
            activeChargers.add(pos);
        }

        public void removeCharger(BlockPos pos) {
            activeChargers.remove(pos);
        }

        public static TeamEnergy load(CompoundTag tag) {
            TeamEnergy e = new TeamEnergy();
            e.stored = new BigInteger(tag.getString("Stored").isEmpty() ? "0" : tag.getString("Stored"));
            e.capacity = new BigInteger(tag.getString("Capacity").isEmpty() ? "0" : tag.getString("Capacity"));

            if (tag.contains("HatchIO")) {
                ListTag list = tag.getList("HatchIO", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    BlockPos p = BlockPos.of(entry.getLong("p"));
                    ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                            new ResourceLocation(entry.getString("d")));
                    e.hatchIsOutput.put(p, entry.getBoolean("o"));
                    e.posToDimension.put(p, dim);
                }
            }

            if (tag.contains("SoulLinks")) {
                ListTag list = tag.getList("SoulLinks", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    BlockPos p = BlockPos.of(entry.getLong("p"));
                    ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                            new ResourceLocation(entry.getString("d")));
                    e.soulLinkedMachines.add(p);
                    e.posToDimension.put(p, dim);
                }
            }

            if (tag.contains("Buffers")) {
                ListTag list = tag.getList("Buffers", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    e.energyBuffered.put(BlockPos.of(entry.getLong("p")), new BigInteger(entry.getString("b")));
                }
            }

            if (tag.contains("FlowData")) {
                ListTag list = tag.getList("FlowData", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    e.machineDisplayFlow.put(BlockPos.of(entry.getLong("p")), entry.getLong("f"));
                }
            }
            return e;
        }
    }

    // --- WRAPPER & HELPER METHODS ---

    public Map<UUID, TeamEnergy> getNetworksView() {
        return Collections.unmodifiableMap(networks);
    }

    public TeamEnergy getOrCreate(UUID team) {
        return networks.computeIfAbsent(team, k -> {
            setDirty();
            return new TeamEnergy();
        });
    }

    public void setEnergyBuffered(UUID team, Level level, BlockPos pos, BigInteger stored, boolean isOutput) {
        TeamEnergy e = getOrCreate(team);
        e.energyBuffered.put(pos, stored);
        e.hatchIsOutput.put(pos, isOutput);
        e.posToDimension.put(pos, level.dimension());
        setDirty();
    }

    public boolean toggleSoulLink(UUID team, Level level, BlockPos pos) {
        if (MetaMachine.getMachine(level, pos) instanceof BatteryBufferMachine) {
            return false;
        }

        TeamEnergy e = getOrCreate(team);
        boolean removed = e.soulLinkedMachines.remove(pos);
        if (!removed) {
            e.soulLinkedMachines.add(pos.immutable());
            e.posToDimension.put(pos, level.dimension());
        } else {
            e.posToDimension.remove(pos);
        }
        setDirty();
        return !removed;
    }

    public Collection<HatchInfo> getHatches(UUID team) {
        TeamEnergy e = networks.get(team);
        if (e == null) return List.of();

        Map<BlockPos, HatchInfo> map = new HashMap<>();

        e.energyBuffered.forEach((pos, buf) -> {
            HatchInfo info = map.computeIfAbsent(pos, p -> new HatchInfo(p, e.getMachineDimension(p)));
            info.buffered = buf;
            info.isPhysicalOutput = e.hatchIsOutput.getOrDefault(pos, false);
        });

        e.soulLinkedMachines.forEach(pos -> {
            HatchInfo info = map.computeIfAbsent(pos, p -> new HatchInfo(p, e.getMachineDimension(p)));
            info.isSoulLinked = true;
            info.displayFlow = e.machineDisplayFlow.getOrDefault(pos, 0L);
        });

        return map.values();
    }

    public void removeMachineFromAllTeams(BlockPos pos) {
        boolean changed = false;
        for (TeamEnergy team : networks.values()) {
            // Sets return boolean
            boolean r1 = team.soulLinkedMachines.remove(pos);
            boolean r2 = team.activeChargers.remove(pos);
            boolean r3 = team.soulLinkedMachines.remove(pos);

            // Maps return null if the key wasn't present
            boolean r4 = team.lastSeen.remove(pos) != null;
            boolean r5 = team.machineDisplayFlow.remove(pos) != null;
            boolean r6 = team.energyBuffered.remove(pos) != null;
            boolean r7 = team.hatchIsOutput.remove(pos) != null;
            boolean r8 = team.posToDimension.remove(pos) != null;
            boolean r9 = team.energyInput.remove(pos) != null;
            boolean r10 = team.energyOutput.remove(pos) != null;

            if (r1 || r2 || r3 || r4 || r5 || r6 || r7 || r8 || r9 || r10) {
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    // --- ONLINE STATE METHODS ---

    public void setOnline(UUID team, boolean online) {
        if (online) {
            if (onlineNetworks.add(team)) setDirty();
        } else {
            if (onlineNetworks.remove(team)) setDirty();
        }
    }

    public boolean isOnline(UUID team) {
        return onlineNetworks.contains(team);
    }

    // --- SAVED DATA OVERRIDES ---

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        networks.forEach((uuid, energy) -> {
            CompoundTag teamTag = new CompoundTag();
            teamTag.putUUID("Team", uuid);
            teamTag.put("Energy", energy.save());
            teamTag.putBoolean("Online", onlineNetworks.contains(uuid));
            list.add(teamTag);
        });
        tag.put("Networks", list);
        return tag;
    }

    public static TeslaTeamEnergyData load(CompoundTag tag) {
        TeslaTeamEnergyData data = new TeslaTeamEnergyData();
        ListTag list = tag.getList("Networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag teamTag = list.getCompound(i);
            UUID teamUUID = teamTag.getUUID("Team");
            data.networks.put(teamUUID, TeamEnergy.load(teamTag.getCompound("Energy")));
            if (teamTag.getBoolean("Online")) data.onlineNetworks.add(teamUUID);
        }
        return data;
    }

    public static TeslaTeamEnergyData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return (overworld == null ? level : overworld).getDataStorage().computeIfAbsent(
                TeslaTeamEnergyData::load, TeslaTeamEnergyData::new, DATA_NAME);
    }

    /**
     * Removes a specific endpoint (Hatch or Machine) from a team's network records.
     */
    public void removeEndpoint(UUID team, BlockPos pos) {
        TeamEnergy e = networks.get(team);
        if (e != null) {
            e.energyInput.remove(pos);
            e.energyOutput.remove(pos);
            e.energyBuffered.remove(pos);
            e.hatchIsOutput.remove(pos);
            e.soulLinkedMachines.remove(pos);
            e.posToDimension.remove(pos); // Clean up the dimension registry
            e.machineDisplayFlow.remove(pos);
            e.lastSeen.remove(pos);
            setDirty();
        }
    }
}
