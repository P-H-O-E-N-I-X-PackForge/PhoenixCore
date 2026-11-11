package net.phoenix.core.integration.conflux.panoptic;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.common.machine.owner.PlayerOwner;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.hatch.ConfluxDataHatchPartMachine;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PanopticEngineMachine extends WorkableElectricMultiblockMachine {

    private static final int REFRESH_TICKS = 20;

    @SaveField
    @SyncToClient
    private float growth = 0f;
    @SaveField
    @SyncToClient
    private long treeSeed = 0L;

    @SaveField
    @SyncToClient
    private int disciplineIndex = 3;

    private float shownGrowth = -1f;
    private long lastShownNanos = 0L;

    public PanopticEngineMachine(BlockEntityCreationInfo holder) {
        super(holder);
        this.subscribeServerTick(this::refresh);
    }

    private void refresh() {
        if (isRemote() || !isFormed() || !(getLevel() instanceof ServerLevel level)) return;
        if (getOffsetTimer() % REFRESH_TICKS != 0) return;
        UUID team = team();
        if (team == null) return;

        PanopticData data = PanopticData.get(level);
        float g = data.growth(team);
        long seed = data.team(team).seed;
        int disc = indexOf(PanopticService.discipline(level, team));

        if (g != growth) {
            growth = g;
            getSyncDataHolder().markClientSyncFieldDirty("growth");
        }
        if (seed != treeSeed) {
            treeSeed = seed;
            getSyncDataHolder().markClientSyncFieldDirty("treeSeed");
        }
        if (disc != disciplineIndex) {
            disciplineIndex = disc;
            getSyncDataHolder().markClientSyncFieldDirty("disciplineIndex");
        }
        setChanged();
    }

    private @Nullable UUID team() {
        MachineOwner owner = getOwner();
        if (owner == null) return null;
        if (owner instanceof PlayerOwner) return TeamUtils.getTeamIdOrPlayerFallback(owner.getUUID());
        return owner.getUUID();
    }

    private static int indexOf(@Nullable String discipline) {
        if (discipline == null) return 3;
        return switch (discipline) {
            case "phoenix" -> 0;
            case "sculk" -> 1;
            case "void" -> 2;
            case "sealed_a" -> 4;
            case "sealed_b" -> 5;
            default -> 3;
        };
    }

    public @Nullable String pay(PanopticOffering offering) {
        if (offering.isFree()) return null;
        List<String> missing = new ArrayList<>();

        long[] stored = new long[ConfluxDataType.values().length];
        List<ConfluxDataHatchPartMachine> dataHatches = new ArrayList<>();
        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof ConfluxDataHatchPartMachine hatch && hatch.getIo() == IO.IN) {
                dataHatches.add(hatch);
                for (ConfluxDataType t : ConfluxDataType.values()) stored[t.ordinal()] += hatch.stored(t);
            }
        }
        for (ConfluxDataType t : ConfluxDataType.values()) {
            long need = offering.data()[t.ordinal()];
            if (need > stored[t.ordinal()]) {
                missing.add(String.format(java.util.Locale.ROOT, "%,d more %s", need - stored[t.ordinal()],
                        t.displayName));
            }
        }

        List<NotifiableFluidTank> tanks = new ArrayList<>();
        for (MultiblockPartMachine part : getParts()) {
            for (NotifiableFluidTank tank : part.getTraits(NotifiableFluidTank.class)) {
                if (tank.handlerIO == IO.IN || tank.handlerIO == IO.BOTH) tanks.add(tank);
            }
        }
        for (PanopticOffering.FluidCost cost : offering.fluids()) {
            Fluid fluid = fluidOf(cost.fluidId());
            int have = 0;
            for (NotifiableFluidTank tank : tanks) {
                have += tank.drainInternal(new FluidStack(fluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE)
                        .getAmount();
            }
            if (have < cost.millibuckets()) {
                missing.add(String.format(java.util.Locale.ROOT, "%,d more mB of %s", cost.millibuckets() - have,
                        new FluidStack(fluid, 1).getHoverName().getString()));
            }
        }
        if (!missing.isEmpty()) return "The Engine lacks " + String.join(", ", missing) + ".";

        for (ConfluxDataType t : ConfluxDataType.values()) {
            long need = offering.data()[t.ordinal()];
            for (ConfluxDataHatchPartMachine hatch : dataHatches) {
                if (need <= 0) break;
                need -= hatch.withdraw(t, need);
            }
        }
        for (PanopticOffering.FluidCost cost : offering.fluids()) {
            Fluid fluid = fluidOf(cost.fluidId());
            int need = cost.millibuckets();
            for (NotifiableFluidTank tank : tanks) {
                if (need <= 0) break;
                need -= tank.drainInternal(new FluidStack(fluid, need), IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        }
        return null;
    }

    private static Fluid fluidOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Fluid fluid = rl == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(rl);
        return fluid == Fluids.EMPTY ? Fluids.WATER : fluid;
    }

    public static @Nullable PanopticEngineMachine at(Level level, BlockPos pos) {
        return MetaMachine.getMachine(level, pos) instanceof PanopticEngineMachine e ? e : null;
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        if (context.getPlayer().isShiftKeyDown()) return super.onUse(context);
        if (!isFormed()) return super.onUse(context);
        if (context.getPlayer() instanceof ServerPlayer player) {
            S2CPanopticStatePacket.send(player, S2CPanopticStatePacket.of(player, getBlockPos(), true, null));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && getLevel().isClientSide) {
            net.phoenix.core.client.renderer.machine.PanopticWorldRenderer.add(this);
        }
    }

    @Override
    public void onUnload() {
        if (getLevel() != null && getLevel().isClientSide) {
            net.phoenix.core.client.renderer.machine.PanopticWorldRenderer.remove(this);
        }
        super.onUnload();
    }

    public float getGrowth() {
        return growth;
    }

    public long getTreeSeed() {
        return treeSeed;
    }

    public @Nullable String getDiscipline() {
        return switch (disciplineIndex) {
            case 0 -> "phoenix";
            case 1 -> "sculk";
            case 2 -> "void";
            case 4 -> "sealed_a";
            case 5 -> "sealed_b";
            default -> null;
        };
    }

    public float shownGrowth() {
        long now = System.nanoTime();
        if (shownGrowth < 0f) {
            shownGrowth = growth;
        } else {
            float dt = Math.min(0.25f, (now - lastShownNanos) / 1_000_000_000f);
            shownGrowth += (growth - shownGrowth) * Math.min(1f, dt * 0.9f);
            if (Math.abs(growth - shownGrowth) < 0.0005f) shownGrowth = growth;
        }
        lastShownNanos = now;
        return shownGrowth;
    }
}
