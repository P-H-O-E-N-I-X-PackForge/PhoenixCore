package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.network.chat.Component;
import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.phoenix.core.common.machine.multiblock.Shield;
import net.phoenix.core.common.machine.multiblock.ShieldedMachine;
import net.phoenix.core.common.machine.multiblock.part.fluid.PlasmaHatchPartMachine;
import net.phoenix.core.common.machine.trait.NotifiableShieldContainer;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class HighPressurePlasmaArcFurnaceMachine extends WorkableElectricMultiblockMachine implements ShieldedMachine {

    private static final int DECAY_TICK_RATE = 20;

    @SaveField
    @SyncToClient
    private Shield.ShieldTypes shieldType = Shield.ShieldTypes.INACTIVE;

    @SaveField
    @SyncToClient
    private int shieldHealth = 0;

    @SaveField
    @SyncToClient
    private int shieldCooldownTimer = 0;

    @SaveField
    @SyncToClient
    private int shieldDecayTimer = DECAY_TICK_RATE;

    private final ConditionalSubscriptionHandler shieldHandler;

    @SaveField
    private final NotifiableShieldContainer shieldContainer;

    @SyncToClient
    public boolean isPlasmaBoosted = false;

    @Nullable
    public PlasmaBoost activeBoost = null;

    private int consumptionTimer = 0;

    public HighPressurePlasmaArcFurnaceMachine(BlockEntityCreationInfo holder) {
        super(holder, new RecipeLogic());

        this.shieldHandler = new ConditionalSubscriptionHandler(this, this::shieldTick, this::isFormed);

        this.shieldContainer = attachTrait(new NotifiableShieldContainer());
    }

    public Shield.ShieldTypes getShieldType() {
        return this.shieldType;
    }

    public void updateShield(int pKey, boolean setCooldown) {
        this.shieldType = Shield.ShieldTypes.getShieldFromKey(pKey);
        this.shieldHealth = this.shieldType.shieldHealth;
        if (setCooldown) {
            this.shieldCooldownTimer = this.shieldType.shieldCooldownTicks;
        }
        if (this.getSyncDataHolder() != null) {
            this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
        }
    }

    @Override
    public void formStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.formStructure(substructureName);
        shieldHandler.updateSubscription();
    }

    @Override
    public void invalidateStructure(@org.jetbrains.annotations.NotNull String substructureName) {
        super.invalidateStructure(substructureName);
        shieldHandler.updateSubscription();
    }

    public void shieldTick() {
        if (this.shieldType == Shield.ShieldTypes.NORMAL) {

            this.shieldDecayTimer--;

            if (this.shieldDecayTimer <= 0) {
                this.shieldHealth -= this.shieldType.decayRate;
                this.shieldDecayTimer = DECAY_TICK_RATE;

                if (this.shieldHealth <= 0) {
                    this.shieldType = Shield.ShieldTypes.DECAYED;
                    this.shieldHealth = 0;
                    this.shieldCooldownTimer = this.shieldType.shieldCooldownTicks;
                    if (this.getSyncDataHolder() != null) {
                        this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
                    }
                }
                if (this.getSyncDataHolder() != null) {
                    this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
                }
            }
        } else {
            this.shieldDecayTimer = DECAY_TICK_RATE;
        }

        if (this.shieldCooldownTimer > 0) {
            this.shieldCooldownTimer--;
            if (this.getSyncDataHolder() != null) {
                this.getSyncDataHolder().markClientSyncFieldDirty("boundTeam");
            }
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe == null) return false;

        boolean isActivationRecipe = recipe.data.contains("shield_activation");

        if (this.shieldType != Shield.ShieldTypes.NORMAL) {
            if (!isActivationRecipe) {
                return false;
            }

            if (this.shieldCooldownTimer > 0) {
                return false;
            }
        } else {
            if (isActivationRecipe) {
                return false;
            }
        }

        return super.beforeWorking(recipe);
    }

    @Override
    public void afterWorking() {
        super.afterWorking();

        var activeRecipe = recipeLogic.getLastRecipe();
        if (activeRecipe != null && activeRecipe.data.contains("shield_activation")) {
            updateShield(activeRecipe.data.getInt("updated_shield_key"), true);
        }
    }

    @Override
    public boolean onWorking() {
        if (this.consumptionTimer % (activeBoost == null ? 1 : activeBoost.ticksPerConsumption()) == 0) {
            isPlasmaBoosted = false;
            activeBoost = null;

            for (var entry : PLASMA_BOOSTS.entrySet()) {
                var fluid = entry.getKey();
                var boost = entry.getValue();

                if (tryConsumePlasmaFromHatch(fluid, boost.consumeAmount())) {
                    isPlasmaBoosted = true;
                    activeBoost = boost;
                    break;
                }
            }
        }

        boolean value = super.onWorking();

        this.consumptionTimer++;
        if (this.consumptionTimer > 72000) this.consumptionTimer = 0;

        return value;
    }

    @Override
    public boolean isWorkingEnabled() {
        if (this.isPlasmaBoosted && this.activeBoost != null) {
            net.minecraft.world.level.material.Fluid currentFluid = null;
            for (var entry : PLASMA_BOOSTS.entrySet()) {
                if (entry.getValue().equals(this.activeBoost)) {
                    currentFluid = entry.getKey();
                    break;
                }
            }
            if (currentFluid == null || !hasPlasmaInHatch(currentFluid, this.activeBoost.consumeAmount())) {
                return false;
            }
        }
        return super.isWorkingEnabled();
    }

    private boolean hasPlasmaInHatch(Fluid fluid, int requiredAmount) {
        return getParts().stream()
                .filter(PlasmaHatchPartMachine.class::isInstance)
                .map(PlasmaHatchPartMachine.class::cast)
                .flatMap(hatch -> hatch.getRecipeHandlers().stream())
                .flatMap(handler -> handler.getCapability(FluidRecipeCapability.CAP).stream())
                .map(cap -> (NotifiableFluidTank) cap)
                .anyMatch(
                        tank -> !tank.isEmpty() && ((FluidStack) tank.getContents().get(0)).getFluid().isSame(fluid) &&
                                ((FluidStack) tank.getContents().get(0)).getAmount() >= requiredAmount);
    }

    private boolean tryConsumePlasmaFromHatch(Fluid fluid, int consumeAmount) {
        for (var hatch : getParts().stream()
                .filter(PlasmaHatchPartMachine.class::isInstance)
                .map(PlasmaHatchPartMachine.class::cast)
                .toList()) {

            var tank = (NotifiableFluidTank) hatch.getRecipeHandlers().get(0)
                    .getCapability(FluidRecipeCapability.CAP).get(0);

            if (!tank.isEmpty() && ((FluidStack) tank.getContents().get(0)).getFluid().isSame(fluid) &&
                    ((FluidStack) tank.getContents().get(0)).getAmount() >= consumeAmount) {

                tank.drain(consumeAmount, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }

    public record PlasmaBoost(String name, double durationMultiplier, double eutMultiplier, int consumeAmount,
                              int ticksPerConsumption) {}

    private static final Map<net.minecraft.world.level.material.Fluid, PlasmaBoost> PLASMA_BOOSTS = new HashMap<>();

    static {
        PLASMA_BOOSTS.put(GTMaterials.Helium.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Helium Plasma", 0.9, 0.8, 1, 40));

        PLASMA_BOOSTS.put(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Iron Plasma", 0.7, 0.85, 200, 20));

        PLASMA_BOOSTS.put(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA),
                new PlasmaBoost("Nickel Plasma", 0.6, 0.9, 50, 10));
    }

    /** A screen control pressed on the server. */
    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private HudModel hudModel() {
        return HudModel.of("High-Pressure Plasma Arc Furnace")
                .formed(this::isFormed)
                .badge(() -> HudRecipes.badge(this))
                .subtitle(() -> shieldType == Shield.ShieldTypes.NORMAL ? "Shield up" :
                        shieldType == Shield.ShieldTypes.DECAYED ? "Shield decayed: needs an activation recipe" :
                                "Shield down: run an activation recipe first")
                .cells(() -> {
                    var cells = HudRecipes.cells(this);
                    cells.add(HudModel.cell("SHIELD", shieldType == Shield.ShieldTypes.NORMAL ? shieldHealth + " HP" :
                            shieldType == Shield.ShieldTypes.DECAYED ? "decayed" : "inactive",
                            shieldType == Shield.ShieldTypes.NORMAL ? "decaying " + shieldType.decayRate + " / s" :
                                    shieldCooldownTimer > 0 ? "cooldown " + shieldCooldownTimer / 20 + "s" : "-",
                            shieldType == Shield.ShieldTypes.NORMAL ? HudModel.GREEN : HudModel.RED));
                    boolean boosted = isPlasmaBoosted && activeBoost != null;
                    cells.add(HudModel.cell("PLASMA BOOST", boosted ? activeBoost.name() : "none",
                            boosted ? String.format(java.util.Locale.ROOT, "x%.2f time, x%.2f EU",
                                    activeBoost.durationMultiplier(), activeBoost.eutMultiplier()) :
                                    "no plasma catalyst",
                            boosted ? HudModel.PURPLE : HudModel.DIM));
                    return cells;
                })
                .bars(() -> {
                    var bars = new java.util.ArrayList<HudModel.Bar>();
                    bars.add(new HudModel.Bar("Recipe", HudRecipes.progressPercent(this) / 100.0f,
                            String.format(java.util.Locale.ROOT, "%.0f%%", HudRecipes.progressPercent(this)),
                            HudModel.CYAN));
                    if (shieldType == Shield.ShieldTypes.NORMAL) {
                        bars.add(new HudModel.Bar("Shield", shieldHealth / (float) Math.max(1,
                                Shield.ShieldTypes.NORMAL.shieldHealth), shieldHealth + " HP", HudModel.GREEN));
                    }
                    return bars;
                })
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE,
                        HudModel.GOLD, this::isFormed)
                .notes(() -> java.util.List.of(HudModel.line(isPlasmaBoosted ?
                        "Plasma catalyst feeding the furnace." : "A plasma catalyst in a plasma hatch speeds it up.",
                        HudModel.MID)))
                .math(() -> java.util.List.of(
                        HudModel.heading("SHIELD"),
                        HudModel.line("health falls " + Shield.ShieldTypes.NORMAL.decayRate + " HP each second while up",
                                HudModel.MID),
                        HudModel.line("at 0 it decays, and needs " + Shield.ShieldTypes.DECAYED.shieldCooldownTicks / 20 +
                                "s to recover", HudModel.MID),
                        HudModel.line("shield now " + shieldHealth + " / " + Shield.ShieldTypes.NORMAL.shieldHealth,
                                HudModel.WHITE),
                        HudModel.heading("PLASMA CATALYSTS"),
                        HudModel.line("Helium: x0.90 time, x0.80 EU, 1 mB / 40 ticks", HudModel.MID),
                        HudModel.line("Iron:   x0.70 time, x0.85 EU, 200 mB / 20 ticks", HudModel.MID),
                        HudModel.line("Nickel: x0.60 time, x0.90 EU, 50 mB / 10 ticks", HudModel.MID),
                        HudModel.heading("RECIPE"),
                        HudModel.line(String.format(java.util.Locale.ROOT, "time %d / %d ticks",
                                getRecipeLogic().getProgress(), getRecipeLogic().getMaxProgress()), HudModel.WHITE)))
                .graph("progress %", HudModel.CYAN, () -> HudRecipes.progressPercent(this), 100.0f)
                .graph("shield HP", HudModel.GREEN, () -> (float) shieldHealth, Shield.ShieldTypes.NORMAL.shieldHealth)
                .graph("EU/t", HudModel.ORANGE, () -> HudRecipes.euPerTick(this), 0.0f);
    }

    public int getShieldHealth() {
        return this.shieldHealth;
    }

    public int getShieldCooldownTimer() {
        return this.shieldCooldownTimer;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HighPressurePlasmaArcFurnaceMachine furnace)) {
            return RecipeModifier.nullWrongType(HighPressurePlasmaArcFurnaceMachine.class, machine);
        }
        if (furnace.isPlasmaBoosted && furnace.activeBoost != null && furnace.shieldType == Shield.ShieldTypes.NORMAL) {
            PlasmaBoost boost = furnace.activeBoost;
            return ModifierFunction.builder()
                    .durationMultiplier(boost.durationMultiplier())
                    .eutMultiplier(boost.eutMultiplier())
                    .build();
        }
        return ModifierFunction.IDENTITY;
    }
}
