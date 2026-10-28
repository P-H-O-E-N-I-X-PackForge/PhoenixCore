package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import net.phoenix.core.saveddata.SoulSavedData;

import brachy.modularui.value.sync.PanelSyncManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BioAethericEngineMachine extends WorkableElectricMultiblockMachine implements ITieredMachine {

    @Getter
    @SaveField
    @SyncToClient
    private float lastBotanicalBoost = 0.0f;

    @SaveField
    @SyncToClient
    private float cachedSoulDensity = 0.0f;

    @SaveField
    private int dynamoTier = GTValues.HV;
    private long maxHatchOutput = 0;

    private int scanTimer = 0;

    public BioAethericEngineMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public int getTier() {
        return isFormed() ? dynamoTier : GTValues.LV;
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        detectDynamoTier();
        if (getLevel() instanceof ServerLevel serverLevel) {
            this.lastBotanicalBoost = calculateFloraBoost(serverLevel, getBlockPos());
            this.cachedSoulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getBlockPos()));
        }
    }

    private void detectDynamoTier() {
        int detectedTier = GTValues.ULV;
        long totalPower = 0;

        var parts = getParts();
        if (parts == null) return;

        for (MultiblockPartMachine part : parts) {
            var handlers = part.getRecipeHandlers();
            if (handlers == null) continue;

            for (var handler : handlers) {
                Object capObject = handler.getCapability(EURecipeCapability.CAP);
                if (handler.getHandlerIO() == IO.OUT && capObject instanceof IEnergyContainer container) {
                    long voltage = container.getOutputVoltage();
                    long amperage = container.getOutputAmperage();
                    detectedTier = Math.max(detectedTier, GTUtil.getFloorTierByVoltage(voltage));
                    totalPower += (voltage * amperage);
                }
            }
        }

        this.dynamoTier = detectedTier;
        this.maxHatchOutput = totalPower;
    }

    private float calculateFloraBoost(ServerLevel level, BlockPos pos) {
        float boost = 0.0f;
        ChunkPos centerChunk = new ChunkPos(pos);

        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - 40);
        int maxY = Math.min(level.getMaxBuildHeight(), pos.getY() + 40);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                var chunk = level.getChunkSource().getChunkNow(centerChunk.x + x, centerChunk.z + z);
                if (chunk == null) continue;

                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int by = minY; by <= maxY; by++) {
                            BlockState state = chunk.getBlockState(
                                    new BlockPos((centerChunk.x + x) << 4 | bx, by, (centerChunk.z + z) << 4 | bz));
                            if (state.isAir()) continue;

                            boost += getFloraBoost(state);

                            if (boost >= 6.0f) return 6.0f;
                        }
                    }
                }
            }
        }
        return Math.min(boost, 6.0f);
    }

    private float getFloraBoost(BlockState state) {
        String registryName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        switch (registryName) {
            case "ars_nouveau:whirlisprig_flower":
                return 0.18f;
            case "ars_nouveau:magebloom_crop":
                return 0.5f;
            case "ars_nouveau:sourceberry_bush":
                return 0.05f;
            default:
                if (state.is(BlockTags.FLOWERS)) {
                    return 0.01f;
                }
                return 0.0f;
        }
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof BioAethericEngineMachine engine)) {
            return RecipeModifier.nullWrongType(BioAethericEngineMachine.class, machine);
        }

        float baseSoul = 1.0f;
        if (engine.getLevel() instanceof ServerLevel serverLevel) {
            baseSoul = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(engine.getBlockPos()));
        }

        float totalResonance = baseSoul + engine.getLastBotanicalBoost();

        double durationMultiplier = 1.0 / (1.0 + (totalResonance * 0.1));

        if (engine.getLevel() != null && engine.getLevel().isNight()) {
            durationMultiplier /= 1.25;
        }

        double euBoost = (double) totalResonance;

        return PhoenixRecipeModifier.builder()
                .durationMultiplier(durationMultiplier)
                .euOutputMultiplier(euBoost)
                .sourceMultiplier(1.0 + (totalResonance * 0.05))
                .build();
    }

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.lastBotanicalBoost = calculateFloraBoost(level, getBlockPos());
                    this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
                }
                scanTimer = 0;
            }
        }
        return isWorking;
    }

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private float multiplier() {
        return cachedSoulDensity + lastBotanicalBoost;
    }

    private HudModel hudModel() {
        return HudModel.of("Bio-Aetheric Engine")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.ARCANE)
                .hero(net.phoenix.core.client.gui.hud.Heroes.pulseLine(() -> Math.min(1.0f, multiplier() / 4.0f),
                        net.phoenix.core.client.gui.hud.HudTheme.ARCANE.accent()), 30)
                .badge(() -> lastBotanicalBoost > 1.0f && isFormed() ?
                        new HudModel.Badge("SOUL SATURATED", HudModel.CYAN) :
                        HudRecipes.badge(this))
                .subtitle(() -> "Generates from the soul of the chunk and the flora round it")
                .cells(() -> {
                    var cells = HudRecipes.cells(this);
                    boolean night = getLevel() != null && getLevel().isNight();
                    cells.add(
                            HudModel.cell("CHUNK SOUL", String.format(java.util.Locale.ROOT, "%.2f", cachedSoulDensity),
                                    "base", HudModel.PURPLE));
                    cells.add(HudModel.cell("FLORA BONUS",
                            String.format(java.util.Locale.ROOT, "+%.2f", lastBotanicalBoost),
                            "from plants nearby", HudModel.GREEN));
                    cells.add(HudModel.cell("VEIL", night ? "active" : "dormant",
                            night ? "x1.25 faster" : "wakes at night",
                            night ? HudModel.CYAN : HudModel.DIM));
                    return cells;
                })
                .bars(() -> java.util.List.of(
                        new HudModel.Bar("Recipe", HudRecipes.progressPercent(this) / 100.0f,
                                String.format(java.util.Locale.ROOT, "%.0f%%", HudRecipes.progressPercent(this)),
                                HudModel.CYAN),
                        new HudModel.Bar("EU multiplier", Math.min(1.0f, multiplier() / 4.0f),
                                String.format(java.util.Locale.ROOT, "%.2fx", multiplier()), HudModel.GOLD)))
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE,
                        HudModel.GOLD, this::isFormed)
                .math(() -> java.util.List.of(
                        HudModel.heading("RESONANCE"),
                        HudModel.line("resonance = chunk soul + flora bonus", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f + %.2f = %.2f", cachedSoulDensity,
                                lastBotanicalBoost, multiplier()), HudModel.WHITE),
                        HudModel.heading("OUTPUT"),
                        HudModel.line("time = 1 / (1 + resonance * 0.1), / 1.25 at night", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= x%.3f", 1.0 / (1.0 + multiplier() * 0.1)),
                                HudModel.WHITE),
                        HudModel.line("EU output = x resonance", HudModel.MID),
                        HudModel.line("source cost = 1 + resonance * 0.05", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= x%.3f", 1.0 + multiplier() * 0.05),
                                HudModel.WHITE)))
                .graph("EU multiplier", HudModel.GOLD, this::multiplier, 0.0f)
                .graph("progress %", HudModel.CYAN, () -> HudRecipes.progressPercent(this), 100.0f)
                .graph("EU/t", HudModel.ORANGE, () -> HudRecipes.euPerTick(this), 0.0f);
    }
}
