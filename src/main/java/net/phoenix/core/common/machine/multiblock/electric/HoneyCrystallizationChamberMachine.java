package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault

public class HoneyCrystallizationChamberMachine extends WorkableElectricMultiblockMachine {

    @Getter
    @Setter
    @SyncToClient
    @RerenderOnChanged
    private @NotNull Set<BlockPos> fluidOffsets = new HashSet<>();

    private static final FluidStack HONEY_STACK;
    static {
        Fluid honeyFluid = BuiltInRegistries.FLUID
                .get(ResourceLocation.fromNamespaceAndPath("productivebees", "honey"));
        if (honeyFluid != null) {
            HONEY_STACK = new FluidStack(honeyFluid, 1000);
        } else {
            HONEY_STACK = FluidStack.EMPTY;
        }
    }

    public HoneyCrystallizationChamberMachine(BlockEntityCreationInfo holder) {
        super(holder, new RecipeLogic());
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        this.fluidOffsets = saveOffsets();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        this.fluidOffsets.clear();
    }

    @NotNull
    public Set<BlockPos> saveOffsets() {
        Direction up = RelativeDirection.UP.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = getFrontFacing().getOpposite();
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());

        BlockPos pos = getBlockPos();
        Set<BlockPos> offsets = new HashSet<>();

        BlockPos startPos = pos
                .relative(up, -1)
                .relative(back, 3)
                .relative(right.getOpposite(), 2);

        int width = 5;
        int depth = 5;
        int height = 1;

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                for (int dy = 0; dy < height; dy++) {

                    BlockPos currentPos = startPos.offset(
                            right.getStepX() * dx + back.getStepX() * dz,
                            up.getStepY() * dy,
                            right.getStepZ() * dx + back.getStepZ() * dz);

                    offsets.add(currentPos.subtract(pos));
                }
            }
        }

        return offsets;
    }

    private record PlasmaBoost(String name, double durationMultiplier, double eutMultiplier, int consumeAmount,
                               int ticksPerConsumption) {}

    private static final Map<Fluid, HoneyCrystallizationChamberMachine.PlasmaBoost> PLASMA_BOOSTS = new HashMap<>();
    static {
        PLASMA_BOOSTS.put(GTMaterials.Helium.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Helium Plasma", 0.9, 0.8, 1, 40));
        PLASMA_BOOSTS.put(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Iron Plasma", 0.7, 0.85, 200, 20));
        PLASMA_BOOSTS.put(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA),
                new HoneyCrystallizationChamberMachine.PlasmaBoost("Nickel Plasma", 0.6, 0.9, 50, 10));
    }

    @SyncToClient
    private boolean isPlasmaBoosted = false;

    @Nullable
    private HoneyCrystallizationChamberMachine.PlasmaBoost activeBoost = null;

    private int consumptionTimer = 0;

    @Override
    public boolean beforeWorking(@org.jetbrains.annotations.Nullable com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (!net.phoenix.core.rebirth.RebirthNetwork.allows(this)) return false;
        return super.beforeWorking(recipe);
    }

    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @SyncToClient
    private int latticeState;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @SyncToClient
    private int latticeTarget;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @SyncToClient
    private int latticeMoves;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @SyncToClient
    private int latticePar;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    @SyncToClient
    private int seedCharges;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    private int latticeSolves;
    @com.gregtechceu.gtceu.api.sync_system.annotations.SaveField
    private long latticeSeed;

    private static final double SEED_DURATION = 0.7;
    private static final double SEED_EUT = 0.9;

    public int getLatticeState() {
        return latticeState;
    }

    public int getLatticeTarget() {
        ensureLattice();
        return latticeTarget;
    }

    public int getLatticeMoves() {
        return latticeMoves;
    }

    public int getLatticePar() {
        ensureLattice();
        return latticePar;
    }

    public int getSeedCharges() {
        return seedCharges;
    }

    public int getLatticeSolves() {
        return latticeSolves;
    }

    public static @Nullable HoneyCrystallizationChamberMachine at(net.minecraft.world.level.Level level, BlockPos pos) {
        return MetaMachine.getMachine(level, pos) instanceof HoneyCrystallizationChamberMachine m ? m : null;
    }

    private void ensureLattice() {
        if (latticePar == 0) latticeNewPattern();
    }

    public void latticeNewPattern() {
        long time = getLevel() == null ? 0L : getLevel().getGameTime();
        latticeSeed = latticeSeed * 6364136223846793005L + 1442695040888963407L + time + getBlockPos().asLong();
        var pattern = net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice.generate(latticeSeed,
                latticeSolves);
        latticeTarget = pattern.target();
        latticePar = pattern.par();
        latticeState = 0;
        latticeMoves = 0;
        markDirty();
    }

    public void latticeClear() {
        ensureLattice();
        latticeState = 0;
        markDirty();
    }

    public record LatticeResult(boolean solved, String message) {}

    public LatticeResult latticePress(int cell) {
        ensureLattice();
        if (cell < 0 || cell >= net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice.CELLS) {
            return new LatticeResult(false, "");
        }
        latticeState = net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice.press(latticeState,
                cell);
        latticeMoves++;

        if ((latticeState & latticeTarget) != latticeTarget) {
            markDirty();
            return new LatticeResult(false, "");
        }
        int moves = latticeMoves;
        int par = latticePar;
        int reward = net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice.reward(moves, par);
        String grade = net.phoenix.core.common.machine.multiblock.electric.honey.HoneyLattice.grade(moves, par);
        seedCharges = Math.min(60, seedCharges + reward);
        latticeSolves++;
        latticeNewPattern();
        return new LatticeResult(true, grade + ": " + moves + " presses against a par of " + par + ". +" + reward +
                " seed charges.");
    }

    private void markDirty() {
        setChanged();
        getSyncDataHolder().markClientSyncFieldDirty("latticeState");
        getSyncDataHolder().markClientSyncFieldDirty("latticeTarget");
        getSyncDataHolder().markClientSyncFieldDirty("latticeMoves");
        getSyncDataHolder().markClientSyncFieldDirty("latticePar");
        getSyncDataHolder().markClientSyncFieldDirty("seedCharges");
    }

    @Override
    public net.minecraft.world.InteractionResult onUse(com.gregtechceu.gtceu.utils.ExtendedUseOnContext context) {
        if (isFormed() && context.getPlayer().isShiftKeyDown() &&
                net.phoenix.core.common.machine.multiblock.ScreenUse.opens(context.getItemInHand())) {
            if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
                ensureLattice();
                net.phoenix.core.common.machine.multiblock.electric.honey.S2CHoneyLatticePacket.send(player,
                        net.phoenix.core.common.machine.multiblock.electric.honey.S2CHoneyLatticePacket.of(this, true,
                                "", false));
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(isRemote());
        }
        return super.onUse(context);
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        if (seedCharges > 0 && !isRemote()) {
            seedCharges--;
            getSyncDataHolder().markClientSyncFieldDirty("seedCharges");
            setChanged();
        }
    }

    public Set<BlockPos> getFluidOffsets() {
        return this.fluidOffsets;
    }

    @Override
    public boolean onWorking() {
        if (this.consumptionTimer % (activeBoost == null ? 1 : activeBoost.ticksPerConsumption()) == 0) {
            isPlasmaBoosted = false;
            activeBoost = null;

            var fluidInputs = this.getCapabilitiesFlat(com.gregtechceu.gtceu.api.capability.recipe.IO.IN,
                    com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability.CAP);

            if (fluidInputs != null && !fluidInputs.isEmpty()) {

                if (fluidInputs.get(0) instanceof NotifiableFluidTank inputFluidHandler) {

                    for (var entry : PLASMA_BOOSTS.entrySet()) {
                        var fluid = entry.getKey();
                        var boost = entry.getValue();
                        FluidStack requiredStack = new FluidStack(fluid, boost.consumeAmount());

                        if (inputFluidHandler
                                .drain(requiredStack,
                                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
                                .getAmount() == boost.consumeAmount()) {

                            inputFluidHandler.drain(requiredStack,
                                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                            isPlasmaBoosted = true;
                            activeBoost = boost;
                            break;
                        }
                    }
                }
            }
        }

        boolean value = super.onWorking();

        this.consumptionTimer++;
        if (this.consumptionTimer > 72000) this.consumptionTimer = 0;

        return value;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HoneyCrystallizationChamberMachine furnace)) {
            return RecipeModifier.nullWrongType(HoneyCrystallizationChamberMachine.class, machine);
        }

        double duration = 1.0;
        double eut = 1.0;
        if (furnace.isPlasmaBoosted && furnace.activeBoost != null) {
            duration *= furnace.activeBoost.durationMultiplier;
            eut *= furnace.activeBoost.eutMultiplier;
        }
        if (furnace.seedCharges > 0) {
            duration *= SEED_DURATION;
            eut *= SEED_EUT;
        }
        if (duration == 1.0 && eut == 1.0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder().durationMultiplier(duration).eutMultiplier(eut).build();
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        if (!isFormed()) return widgets;
        widgets.add(new TextWidget<>(Text.dynamic(() -> isPlasmaBoosted ? Component.literal("§bPlasma Boost Active§r") :
                Component.literal("§7No Plasma Catalyst§r"))));
        widgets.add(new TextWidget<>(Text.dynamic(() -> seedCharges > 0 ?
                Component.literal("§6Seed crystals: " + seedCharges + " cycles left§r") :
                Component.literal("§7No seed crystals. Sneak-click with an empty hand to grow some.§r"))));
        return widgets;
    }

    public List<FluidStack> getRenderFluids() {
        return List.of(HONEY_STACK);
    }
}
