package net.phoenix.core.common.machine.multiblock.forge;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.phoenix.core.rebirth.RebirthData;
import net.phoenix.core.rebirth.RebirthNetwork;
import net.phoenix.core.rebirth.S2CRebirthStatePacket;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PhoenixForgeMachine extends WorkableElectricMultiblockMachine {

    @SyncToClient
    private int phoenixPhase;

    private final ConditionalSubscriptionHandler phoenixHandler;

    public PhoenixForgeMachine(BlockEntityCreationInfo holder) {
        super(holder);
        this.phoenixHandler = new ConditionalSubscriptionHandler(this, this::phoenixTick, this::isFormed);
    }

    public int getPhoenixPhase() {
        return phoenixPhase;
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        phoenixHandler.updateSubscription();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        phoenixHandler.updateSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            net.phoenix.core.client.renderer.phoenix.PhoenixForgeWorldRenderer.add(this);
        }
    }

    @Override
    public void onUnload() {
        if (getLevel() != null && getLevel().isClientSide) {
            net.phoenix.core.client.renderer.phoenix.PhoenixForgeWorldRenderer.remove(this);
        }
        super.onUnload();
    }

    private void phoenixTick() {
        if (!(getLevel() instanceof ServerLevel level) || getOffsetTimer() % 20 != 0) return;
        int phase = 0;
        UUID team = RebirthNetwork.teamOf(this);
        if (team != null) {
            RebirthData.Team t = RebirthData.get(level).team(team);
            if (t.forgePos == getBlockPos().asLong()) phase = t.reborn ? 2 : t.closed ? 1 : 0;
        }
        if (phase != phoenixPhase) {
            phoenixPhase = phase;
            getSyncDataHolder().markClientSyncFieldDirty("phoenixPhase");
        }
    }

    public static @Nullable PhoenixForgeMachine at(Level level, BlockPos pos) {
        return MetaMachine.getMachine(level, pos) instanceof PhoenixForgeMachine m ? m : null;
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        if (isFormed() && !context.getPlayer().isShiftKeyDown()) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                S2CRebirthStatePacket.send(player, S2CRebirthStatePacket.of(player, getBlockPos(), true, false, ""));
            }
            return InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (getLevel() instanceof ServerLevel level) {
            UUID team = RebirthNetwork.teamOf(this);
            if (team == null || !RebirthNetwork.networkClosed(level, team)) return false;
            RebirthData.Team t = RebirthData.get(level).team(team);

            if (t.forgePos != getBlockPos().asLong()) return false;
        }
        return super.beforeWorking(recipe);
    }
}
