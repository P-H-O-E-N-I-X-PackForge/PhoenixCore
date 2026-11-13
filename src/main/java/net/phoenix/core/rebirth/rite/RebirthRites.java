package net.phoenix.core.rebirth.rite;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.rebirth.Capstone;
import net.phoenix.core.rebirth.RebirthNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RebirthRites {

    private RebirthRites() {}

    private record Session(Rite rite, BlockPos pos) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static RebirthNetwork.Result begin(ServerPlayer player, ServerLevel level, BlockPos pos,
                                              MetaMachine machine) {
        Capstone capstone = Capstone.of(machine.getDefinition().getId());
        if (capstone == null) return new RebirthNetwork.Result(false, "That is not a capstone.");
        if (!(machine instanceof MultiblockControllerMachine controller) || !controller.isFormed()) {
            return new RebirthNetwork.Result(false, "The multiblock has to be formed before it can be bound.");
        }
        RebirthNetwork.Result check = RebirthNetwork.checkBind(player, level, machine);
        if (!check.ok()) return check;

        Rite rite = Rite.create(capstone, level.getGameTime() * 31L + pos.asLong() + player.getUUID().hashCode());
        SESSIONS.put(player.getUUID(), new Session(rite, pos));
        S2CRitePacket.send(player, S2CRitePacket.open(capstone, pos, rite, "The " + capstone.displayName +
                " answers the Binder. Prove the link."));
        return new RebirthNetwork.Result(true, "The binding rite begins.");
    }

    public static void act(ServerPlayer player, BlockPos pos, int action, int[] args) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || !s.pos.equals(pos)) return;
        ServerLevel level = player.serverLevel();
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        Capstone capstone = s.rite.capstone;
        Rite.Reply reply = s.rite.act(action, args);
        switch (reply.outcome()) {
            case SUCCESS -> {
                RebirthNetwork.Result bound = RebirthNetwork.bindCapstone(player, level, pos, machine);
                SESSIONS.remove(player.getUUID());
                S2CRitePacket.send(player, S2CRitePacket.of(capstone, pos, S2CRitePacket.BOUND, s.rite.data(),
                        reply.extra(), reply.message() + " " + bound.message()));
            }
            case FAIL -> {
                Rite fresh = Rite.create(capstone, s.rite.seed * 6364136223846793005L + level.getGameTime());
                SESSIONS.put(player.getUUID(), new Session(fresh, pos));
                S2CRitePacket.send(player, S2CRitePacket.of(capstone, pos, S2CRitePacket.FAILED, fresh.data(),
                        reply.extra(), reply.message()));
            }
            default -> S2CRitePacket.send(player, S2CRitePacket.of(capstone, pos, S2CRitePacket.ACTIVE,
                    s.rite.data(), reply.extra(), reply.message()));
        }
    }

    public static void drop(UUID player) {
        SESSIONS.remove(player);
    }
}
