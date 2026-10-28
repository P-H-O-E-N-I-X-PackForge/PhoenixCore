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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.phoenix.core.api.recipe.PhoenixRecipeModifier;
import net.phoenix.core.common.data.PhoenixRecipeTypes;
import net.phoenix.core.saveddata.SoulSavedData;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AlchemicalImbuerMachine extends WorkableElectricMultiblockMachine {

    private static final long HARMONIZATION_THRESHOLD = 72000L;

    @Getter
    @SaveField
    @SyncToClient
    private float cachedFloraBoost = 0.0f;

    @SaveField
    @SyncToClient
    private float cachedSoulDensity = 0.0f;

    @Getter
    @SaveField
    @SyncToClient
    private long totalWorkTicks = 0L;

    @SaveField
    private boolean wasWorking = false;

    private int scanTimer = 0;

    public AlchemicalImbuerMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    @Override
    public void formStructure(@NotNull String substructureName) {
        super.formStructure(substructureName);
        if (getLevel() instanceof ServerLevel level) {
            this.cachedFloraBoost = scanEnvironment(level, getBlockPos());
            this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
        }
    }

    @Override
    public boolean onWorking() {
        boolean isWorking = super.onWorking();

        if (isWorking) {

            if (totalWorkTicks < HARMONIZATION_THRESHOLD) {
                totalWorkTicks++;
            }

            scanTimer++;
            if (scanTimer >= 200) {
                if (getLevel() instanceof ServerLevel level) {
                    this.cachedFloraBoost = scanEnvironment(level, getBlockPos());
                    this.cachedSoulDensity = SoulSavedData.get(level).getMultiplier(new ChunkPos(getBlockPos()));
                }
                scanTimer = 0;
            }
        }

        wasWorking = isWorking;
        return isWorking;
    }

    private float scanEnvironment(ServerLevel level, BlockPos pos) {
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

                            boost += getBlockBoost(state);
                            if (boost >= 6.0f) return 6.0f;
                        }
                    }
                }
            }
        }
        return Math.min(boost, 6.0f);
    }

    private float getBlockBoost(BlockState state) {
        String registryName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        switch (registryName) {
            case "ars_nouveau:whirlisprig_flower":
                return 0.20f;
            case "ars_nouveau:magebloom_crop":
                return 0.05f;
            case "ars_nouveau:sourceberry_bush":
                return 0.01f;
            default:

                if (state.is(BlockTags.FLOWERS)) {
                    return 0.002f;
                }
                return 0.0f;
        }
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof AlchemicalImbuerMachine imbuer)) {
            return RecipeModifier.nullWrongType(AlchemicalImbuerMachine.class, machine);
        }

        float resonance = 1.0f;
        if (imbuer.getLevel() instanceof ServerLevel serverLevel) {
            resonance = SoulSavedData.get(serverLevel).getMultiplier(new ChunkPos(imbuer.getBlockPos())) +
                    imbuer.getCachedFloraBoost();
            if (imbuer.getTotalWorkTicks() >= HARMONIZATION_THRESHOLD) resonance += 0.5f;
        }

        double efficiencyFactor = 1.0 + ((resonance - 1.0) * 0.9);

        if (recipe.recipeType == PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.get()) {
            return PhoenixRecipeModifier.builder()
                    .durationMultiplier(0.8)
                    .sourceMultiplier(1.1 * efficiencyFactor)
                    .build();
        } else {
            return PhoenixRecipeModifier.builder()
                    .durationMultiplier(1.0 / efficiencyFactor)
                    .sourceMultiplier(1.0 / efficiencyFactor)
                    .build();
        }
    }

    private boolean extracting() {
        return getRecipeType() == PhoenixRecipeTypes.SOURCE_EXTRACTION_RECIPES.get();
    }

    private final net.phoenix.core.client.gui.hud.Heroes.Hero IMBUE_HERO = net.phoenix.core.client.gui.hud.Heroes.runes(() -> (float) Math.min(1.0,
            totalWorkTicks / (double) HARMONIZATION_THRESHOLD), net.phoenix.core.client.gui.hud.HudTheme.ARCANE.accent());
    private final net.phoenix.core.client.gui.hud.Heroes.Hero EXTRACT_HERO = net.phoenix.core.client.gui.hud.Heroes.pulseLine(() -> Math.min(1.0f, cachedSoulDensity / 2.0f),
            0xFF_FFC44D);

    private void hudAction(int id, int value) {
        HudRecipes.handle(this, id);
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData data,
                                                           PanelSyncManager syncManager,
                                                           brachy.modularui.screen.UISettings settings) {
        return MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel);
    }

    private float harmonicBonus() {
        return totalWorkTicks >= HARMONIZATION_THRESHOLD ? 0.5f : 0.0f;
    }

    private float totalPotency() {
        return cachedSoulDensity + cachedFloraBoost + harmonicBonus();
    }

    private HudModel hudModel() {
        return HudModel.of("Alchemical Imbuer")
                .formed(this::isFormed)
                .theme(net.phoenix.core.client.gui.hud.HudTheme.ARCANE)
                .hero((g, x, y, w, h, t) -> (extracting() ? EXTRACT_HERO : IMBUE_HERO).draw(g, x, y, w, h, t), 44)
                .badge(() -> HudRecipes.badge(this))
                .subtitle(() -> (extracting() ? "Siphoning source from the chunk: " : "Imbuing: ") +
                        (totalWorkTicks >= HARMONIZATION_THRESHOLD ? "chunk harmonized" : "harmonizing as it works"))
                .cells(() -> {
                    var cells = HudRecipes.cells(this);
                    cells.add(HudModel.cell("SOUL RESONANCE", String.format(java.util.Locale.ROOT, "%.2f", cachedSoulDensity),
                            "this chunk", HudModel.PURPLE));
                    cells.add(HudModel.cell("GARDEN BOOST", String.format(java.util.Locale.ROOT, "+%.2f", cachedFloraBoost),
                            "from flora nearby", HudModel.GREEN));
                    cells.add(HudModel.cell("TOTAL POTENCY", String.format(java.util.Locale.ROOT, "%.2fx", totalPotency()),
                            harmonicBonus() > 0 ? "harmonized +0.50" : "not harmonized", HudModel.GOLD));
                    return cells;
                })
                .bars(() -> {
                    var bars = new java.util.ArrayList<HudModel.Bar>();
                    bars.add(new HudModel.Bar("Recipe", HudRecipes.progressPercent(this) / 100.0f,
                            String.format(java.util.Locale.ROOT, "%.0f%%", HudRecipes.progressPercent(this)), HudModel.CYAN));
                    double harmony = Math.min(1.0, totalWorkTicks / (double) HARMONIZATION_THRESHOLD);
                    bars.add(new HudModel.Bar("Chunk harmonization", (float) harmony,
                            String.format(java.util.Locale.ROOT, "%.0f%%", harmony * 100.0), HudModel.GOLD));
                    return bars;
                })
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE,
                        HudModel.GOLD, this::isFormed)
                .math(() -> java.util.List.of(
                        HudModel.heading("POTENCY"),
                        HudModel.line("potency = soul + garden + harmony", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f + %.2f + %.2f = %.2fx", cachedSoulDensity,
                                cachedFloraBoost, harmonicBonus(), totalPotency()), HudModel.WHITE),
                        HudModel.heading("EFFICIENCY"),
                        HudModel.line("efficiency = 1 + (potency - 1) * 0.9", HudModel.MID),
                        HudModel.line(String.format(java.util.Locale.ROOT, "= %.2f", 1.0 + (totalPotency() - 1.0) * 0.9),
                                HudModel.WHITE),
                        HudModel.line("most recipes: time and source cost divided by it", HudModel.DIM),
                        HudModel.line("extraction: x0.8 time, source x1.1 * efficiency", HudModel.DIM),
                        HudModel.heading("HARMONY"),
                        HudModel.line(String.format(java.util.Locale.ROOT, "%,d of %,d work ticks", totalWorkTicks,
                                (long) HARMONIZATION_THRESHOLD), HudModel.WHITE)))
                .graph("potency", HudModel.GOLD, this::totalPotency, 0.0f)
                .graph("progress %", HudModel.CYAN, () -> HudRecipes.progressPercent(this), 100.0f)
                .graph("EU/t", HudModel.ORANGE, () -> HudRecipes.euPerTick(this), 0.0f);
    }
}
