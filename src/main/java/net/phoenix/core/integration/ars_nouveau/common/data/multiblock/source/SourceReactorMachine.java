package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.source;

import net.phoenix.core.client.gui.hud.HudModel;
import net.phoenix.core.client.gui.hud.HudRecipes;
import net.phoenix.core.client.gui.hud.MachineHud;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.saveddata.SoulSavedData;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SourceReactorMachine extends WorkableElectricMultiblockMachine {

    @Getter
    @SaveField
    @SyncToClient
    private float reactorStability = 1.0f;

    @SaveField
    @SyncToClient
    private float cachedSoulDensity = 1.0f;

    private int scanTimer = 0;

    public SourceReactorMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        if (getLevel() instanceof ServerLevel serverLevel) {
            this.reactorStability = calculateStability(serverLevel, getBlockPos());
            this.cachedSoulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(getBlockPos()));
        }
    }

    private float calculateStability(ServerLevel level, BlockPos pos) {
        float stabilizers = 0.0f;
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

                            String id = state.getBlock().getDescriptionId();
                            if (id.contains("source_gem_block")) stabilizers += 0.04f;
                            else if (id.contains("magebloom_block")) stabilizers += 0.06f;
                            else if (id.contains("arcane_core")) stabilizers += 0.12f;

                            if (stabilizers >= 4.0f) return 5.0f;
                        }
                    }
                }
            }
        }
        return 1.0f + stabilizers;
    }

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {
            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.reactorStability = calculateStability(level, getBlockPos());
                    this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
                }
                scanTimer = 0;
            }
        }
        return isWorking;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SourceReactorMachine reactor)) {
            return RecipeModifier.nullWrongType(SourceReactorMachine.class, machine);
        }

        float soulDensity = 1.0f;

        if (reactor.getLevel() instanceof ServerLevel serverLevel) {
            soulDensity = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(reactor.getBlockPos()));
        }

        double speedBoost = 1.0 + (soulDensity * 0.15);
        double costReduction = 1.0 + (reactor.getReactorStability() * 0.1);

        return PhoenixRecipeModifier.builder()
                .durationMultiplier(1.0 / speedBoost)
                .sourceMultiplier(1.0 / costReduction)
                .build();
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

    private HudModel hudModel() {
        return HudModel.of("Source Reactor")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.REACTOR)
                .hero(net.phoenix.core.client.gui.hud.Heroes.arcGauge(() -> reactorStability / 2.5f, net.phoenix.core.client.gui.hud.HudTheme.REACTOR.accent()), 38)
                .badge(() -> reactorStability < 1.1f && isFormed() ? new HudModel.Badge("LOW STABILITY", HudModel.RED) :
                        reactorStability > 2.2f ? new HudModel.Badge("CRITICAL EFFICIENCY", HudModel.CYAN) :
                                HudRecipes.badge(this))
                .subtitle(() -> "Reaction pressure rises with soul density and stability")
                .cells(() -> {
                    var cells = HudRecipes.cells(this);
                    cells.add(HudModel.cell("SOUL DENSITY", String.format(java.util.Locale.ROOT, "%.2f", cachedSoulDensity),
                            "this chunk", HudModel.PURPLE));
                    cells.add(HudModel.cell("STABILITY", String.format(java.util.Locale.ROOT, "%.2fx", reactorStability),
                            reactorStability > 2.0f ? "excellent" : reactorStability > 1.5f ? "good" : "poor",
                            reactorStability > 1.5f ? HudModel.GREEN : HudModel.GOLD));
                    cells.add(HudModel.cell("PRESSURE", String.format(java.util.Locale.ROOT, "%.2fx",
                            cachedSoulDensity * reactorStability), "soul x stability", HudModel.ORANGE));
                    return cells;
                })
                .bars(() -> java.util.List.of(
                        new HudModel.Bar("Recipe", HudRecipes.progressPercent(this) / 100.0f,
                                String.format(java.util.Locale.ROOT, "%.0f%%", HudRecipes.progressPercent(this)),
                                HudModel.CYAN),
                        new HudModel.Bar("Stability", Math.min(1.0f, reactorStability / 2.5f),
                                String.format(java.util.Locale.ROOT, "%.2fx", reactorStability), HudModel.GREEN)))
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE,
                        HudModel.GOLD, this::isFormed)
                .notes(() -> java.util.List.of(reactorStability < 1.1f ?
                        HudModel.line("Low stability: source waste imminent.", HudModel.RED) :
                        reactorStability > 2.2f ? HudModel.line("Critical efficiency.", HudModel.CYAN) :
                                HudModel.line("Stable.", HudModel.MID)))
                .math(() -> java.util.List.of(
                        HudModel.heading("SPEED AND COST"),
                        HudModel.line("speed = 1 + soul * 0.15", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f", 1.0 + cachedSoulDensity * 0.15),
                                HudModel.WHITE),
                        HudModel.line("cost divisor = 1 + stability * 0.1", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f", 1.0 + reactorStability * 0.1),
                                HudModel.WHITE),
                        HudModel.heading("PRESSURE"),
                        HudModel.line("pressure = soul density * stability", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f * %.2f = %.2f", cachedSoulDensity,
                                reactorStability, cachedSoulDensity * reactorStability), HudModel.WHITE)))
                .graph("stability", HudModel.GREEN, () -> reactorStability, 2.5f)
                .graph("pressure", HudModel.ORANGE, () -> cachedSoulDensity * reactorStability, 0.0f)
                .graph("progress %", HudModel.CYAN, () -> HudRecipes.progressPercent(this), 100.0f);
    }
}
