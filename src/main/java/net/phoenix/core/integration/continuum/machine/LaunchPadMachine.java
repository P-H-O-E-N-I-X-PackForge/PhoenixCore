package net.phoenix.core.integration.continuum.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.phoenix.core.integration.continuum.network.S2COpenMapPacket;
import net.phoenix.core.network.PhoenixNetwork;

/**
 * The Continuum Launch Complex controller. A formed complex opens the Continuum map with launching enabled when it is
 * used; a sneaking use falls through to GT's normal behaviour (structure info, tools).
 *
 * <p>
 * Its tier is the highest tier of its energy input hatches, and launching draws energy from those hatches' buffers, so
 * a bigger power supply lets it send rockets to farther bodies (see {@code ContinuumMissions}).
 */
public class LaunchPadMachine extends MultiblockControllerMachine {

    public LaunchPadMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    /** The highest voltage tier among the energy input hatches, or 0 with none. */
    public int padTier() {
        int tier = 0;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch) tier = Math.max(tier, hatch.getTier());
        }
        return tier;
    }

    /** EU currently buffered across all energy input hatches. */
    public long storedEnergy() {
        long total = 0;
        for (var part : getParts()) {
            if (part instanceof EnergyHatchPartMachine hatch) total += hatch.energyContainer.getEnergyStored();
        }
        return total;
    }

    /**
     * Takes {@code amount} EU from the hatches' buffers.
     *
     * @return false (and takes nothing) if there is not that much stored
     */
    public boolean drainEnergy(long amount) {
        if (amount <= 0) return true;
        if (storedEnergy() < amount) return false;

        long remaining = amount;
        for (var part : getParts()) {
            if (remaining <= 0) break;
            if (!(part instanceof EnergyHatchPartMachine hatch)) continue;

            long take = Math.min(remaining, hatch.energyContainer.getEnergyStored());
            if (take <= 0) continue;
            hatch.energyContainer.changeEnergy(-take);
            remaining -= take;
        }
        return true;
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        Player player = context.getPlayer();
        if (!isFormed() || player == null || player.isShiftKeyDown()) return super.onUse(context);

        Level level = context.getLevel();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PhoenixNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new S2COpenMapPacket(getBlockPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
