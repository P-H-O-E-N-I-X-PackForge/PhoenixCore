package net.phoenix.core.rebirth;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.common.machine.owner.PlayerOwner;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.phoenix.conflux.research.ResearchTeamHelper;
import net.phoenix.conflux.research.WorldResearchData;
import net.phoenixvine.wiki.util.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class RebirthNetwork {

    private RebirthNetwork() {}

    public static final ResourceLocation FORGE_ID = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "phoenix_forge");

    public static final String REBORN_FLAG = "rebirth_complete";

    private static final int ARRIVAL_DELAY_TICKS = 30 * 20;

    public record Result(boolean ok, String message) {}

    public static @Nullable UUID teamOf(MetaMachine machine) {
        MachineOwner owner = machine.getOwner();
        if (owner == null) return null;
        return owner instanceof PlayerOwner ? TeamUtils.getTeamIdOrPlayerFallback(owner.getUUID()) : owner.getUUID();
    }

    public static boolean isForge(MetaMachine machine) {
        return machine.getDefinition().getId().equals(FORGE_ID);
    }

    public static boolean allows(MetaMachine machine) {
        return true;
    }

    public static String denial(MetaMachine machine) {
        Level level = machine.getLevel();
        Capstone capstone = Capstone.of(machine.getDefinition().getId());
        if (!(level instanceof ServerLevel serverLevel) || capstone == null) return "";
        UUID team = teamOf(machine);
        if (team == null) return "It has no owner, so it cannot join a rebirth network.";
        RebirthData.Team t = RebirthData.get(serverLevel).team(team);
        if (t.forgePos == RebirthData.NONE) return "No Phoenix Forge has been attuned for your team.";
        if (!t.bound[capstone.ordinal()] || t.pos[capstone.ordinal()] != machine.getBlockPos().asLong()) {
            return "It is not bound to the Phoenix Forge. Use a Rebirth Binder on it.";
        }
        if (!t.closed) return "The ring is not closed. Order the capstones at the Phoenix Forge.";
        return "";
    }

    public static boolean networkClosed(ServerLevel level, UUID team) {
        RebirthData.Team t = RebirthData.get(level).team(team);
        return t.closed && t.allBound() && t.forgePos != RebirthData.NONE;
    }

    public static Result attuneForge(ServerPlayer player, ServerLevel level, BlockPos pos, MetaMachine forge) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        UUID owner = teamOf(forge);
        if (owner != null && !owner.equals(team) && !player.hasPermissions(2)) {
            return new Result(false, "That Forge belongs to another team.");
        }
        RebirthData data = RebirthData.get(level);
        RebirthData.Team t = data.team(team);
        t.forgePos = pos.asLong();
        t.forgeDim = level.dimension().location().toString();
        data.changed();
        return new Result(true, "Attuned to this Phoenix Forge. Bind the capstones to it.");
    }

    public static Result checkBind(ServerPlayer player, ServerLevel level, MetaMachine machine) {
        Capstone capstone = Capstone.of(machine.getDefinition().getId());
        if (capstone == null) return new Result(false, "That is not a capstone.");
        UUID team = ResearchTeamHelper.getTeamId(player);
        UUID owner = teamOf(machine);
        if (owner != null && !owner.equals(team) && !player.hasPermissions(2)) {
            return new Result(false, "That machine belongs to another team.");
        }
        if (RebirthData.get(level).team(team).forgePos == RebirthData.NONE) {
            return new Result(false, "Attune the Binder to your Phoenix Forge first.");
        }
        return new Result(true, "");
    }

    public static Result bindCapstone(ServerPlayer player, ServerLevel level, BlockPos pos, MetaMachine machine) {
        Result check = checkBind(player, level, machine);
        if (!check.ok()) return check;
        Capstone capstone = Capstone.of(machine.getDefinition().getId());
        UUID team = ResearchTeamHelper.getTeamId(player);
        RebirthData data = RebirthData.get(level);
        RebirthData.Team t = data.team(team);
        int i = capstone.ordinal();
        boolean wasBound = t.bound[i];
        t.bound[i] = true;
        t.pos[i] = pos.asLong();
        t.dim[i] = level.dimension().location().toString();

        t.closed = false;
        t.reborn = false;
        data.changed();
        return new Result(true,
                capstone.displayName + (wasBound ? " re-bound." : " bound to the Phoenix Forge.") + " " +
                        bound(t) + " of " + Capstone.COUNT + " bound.");
    }

    public static int bound(RebirthData.Team t) {
        int n = 0;
        for (boolean b : t.bound) if (b) n++;
        return n;
    }

    public static Result submitRing(ServerPlayer player, ServerLevel level, int[] ring) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        RebirthData data = RebirthData.get(level);
        RebirthData.Team t = data.team(team);
        if (!RebirthRing.isRing(ring)) return new Result(false, "Put all the capstones in the ring, each once.");
        if (!t.allBound())
            return new Result(false, "All the capstones have to be bound before the ring will take them.");
        int aligned = RebirthRing.aligned(RebirthRing.secret(t.seed), ring);
        t.ring = ring.clone();
        t.lastAligned = aligned;
        t.attempts++;
        boolean closed = aligned == Capstone.COUNT;
        t.closed = closed;
        data.changed();
        return new Result(closed, closed ? "The ring closes. The capstones wake." :
                aligned + " of " + Capstone.COUNT + " sit where the Forge wants them.");
    }

    public static Result ignite(ServerPlayer player, ServerLevel level) {
        UUID team = ResearchTeamHelper.getTeamId(player);
        RebirthData data = RebirthData.get(level);
        RebirthData.Team t = data.team(team);
        if (!t.allBound() || !t.closed) return new Result(false, "The ring is not closed.");
        boolean first = !t.reborn;
        t.reborn = true;
        data.changed();
        if (first) WorldResearchData.get(level).grantFlag(team, REBORN_FLAG);
        S2CEndingPacket.send(player);
        if (first) net.phoenix.conflux.ending.EpilogueService.scheduleArrival(player, ARRIVAL_DELAY_TICKS);
        return new Result(true, first ? "The Phoenix Forge is lit." : "The Phoenix Forge burns again.");
    }
}
