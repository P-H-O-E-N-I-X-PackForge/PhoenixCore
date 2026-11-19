package net.phoenix.core.integration.ars_nouveau.common.data.multiblock.part.source;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;

import net.minecraft.server.level.ServerLevel;
import net.phoenix.core.integration.ars_nouveau.api.capability.ISourceProviderCapability;
import net.phoenix.core.integration.ars_nouveau.api.machine.trait.NotifiableSourceContainer;
import net.phoenix.core.integration.ars_nouveau.common.event.SourceHatchTracker;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import lombok.Getter;

@Getter
public class SourceHatchPartMachine extends TieredIOPartMachine implements ISourceProviderCapability, IMuiMachine,
                                    com.hollingsworth.arsnouveau.api.item.IWandable {

    private final IO io;
    private final NotifiableSourceContainer sourceContainer;

    public SourceHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.io = io;
        this.sourceContainer = attachTrait(
                new NotifiableSourceContainer(io, getMaxCapacity(tier), getMaxConsumption(tier)));
    }

    @Override
    public ISourceTile getSource() {
        return sourceContainer;
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        syncManager.syncValue("source", 0, new brachy.modularui.value.sync.IntSyncValue(
                () -> sourceContainer.getSource(), v -> sourceContainer.setSource(v)));
        syncManager.syncValue("flow", 0, new brachy.modularui.value.sync.IntSyncValue(
                () -> sourceContainer.getCurrentFlow(),
                v -> sourceContainer.updateFlow(v - sourceContainer.getCurrentFlow())));
        return net.phoenix.core.util.PatternErrorUi.with(this, syncManager,
                net.phoenixvine.wiki.ui.hud.MachineHud.build(this, data, syncManager, settings, (id, value) -> {},
                        this::hudModel));
    }

    private net.phoenixvine.wiki.ui.hud.HudModel hudModel() {
        boolean input = io == IO.IN;
        int radius = net.phoenix.core.configs.PhoenixConfigs.INSTANCE.sourceHatch.sourceJarCheckRadius;
        return net.phoenixvine.wiki.ui.hud.HudModel
                .of(com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Source " + (input ? "Input" : "Output") + " Hatch")
                .theme(net.phoenixvine.wiki.ui.hud.HudTheme.ARCANE)
                .formed(() -> true)
                .page(net.phoenixvine.wiki.ui.hud.ExoticPages.sourceHatch(
                        com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Source " + (input ? "Input" : "Output") +
                                " Hatch",
                        input, () -> sourceContainer.getSource(), () -> sourceContainer.getMaxSource(),
                        () -> sourceContainer.getTransferRate(), () -> sourceContainer.getCurrentFlow(), radius,
                        getAccentColor(), () -> input ? "Draws Source from jars within " + radius + " blocks" :
                                "Fills jars within " + radius + " blocks"))
                .hero(net.phoenixvine.wiki.ui.hud.ExoticHeroes.sourceOrb(
                        net.phoenixvine.wiki.ui.hud.Heroes.eased(
                                () -> sourceContainer.getSource() /
                                        (float) Math.max(1, sourceContainer.getMaxSource())),
                        input, getAccentColor()), 62)
                .badge(() -> input ?
                        new net.phoenixvine.wiki.ui.hud.HudModel.Badge("INPUT",
                                net.phoenixvine.wiki.ui.hud.HudModel.PURPLE) :
                        new net.phoenixvine.wiki.ui.hud.HudModel.Badge("OUTPUT",
                                net.phoenixvine.wiki.ui.hud.HudModel.CYAN))
                .subtitle(() -> input ? "Draws Source from jars within " + radius + " blocks" :
                        "Fills jars within " + radius + " blocks")
                .cells(() -> {
                    var cells = new java.util.ArrayList<net.phoenixvine.wiki.ui.hud.HudModel.Cell>();
                    cells.add(net.phoenixvine.wiki.ui.hud.HudModel.cell("SOURCE",
                            String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getSource()),
                            "of " + String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getMaxSource()),
                            net.phoenixvine.wiki.ui.hud.HudModel.PURPLE));
                    cells.add(net.phoenixvine.wiki.ui.hud.HudModel.cell("RATE",
                            String.format(java.util.Locale.ROOT, "%,d /t", sourceContainer.getTransferRate()),
                            "most it moves", net.phoenixvine.wiki.ui.hud.HudModel.CYAN));
                    cells.add(net.phoenixvine.wiki.ui.hud.HudModel.cell("FLOW",
                            String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getCurrentFlow()),
                            "lately", net.phoenixvine.wiki.ui.hud.HudModel.GREEN));
                    cells.add(net.phoenixvine.wiki.ui.hud.HudModel.cell("RANGE", radius + " blocks",
                            "jar search", net.phoenixvine.wiki.ui.hud.HudModel.GOLD));
                    return cells;
                })
                .bars(() -> java.util.List.of(new net.phoenixvine.wiki.ui.hud.HudModel.Bar("Source",
                        sourceContainer.getSource() / (float) Math.max(1, sourceContainer.getMaxSource()),
                        String.format(java.util.Locale.ROOT, "%,d / %,d", sourceContainer.getSource(),
                                sourceContainer.getMaxSource()),
                        net.phoenixvine.wiki.ui.hud.HudModel.PURPLE)))
                .notes(() -> java.util.List.of(net.phoenixvine.wiki.ui.hud.HudModel.line(
                        "Source pipes and relays can feed it too.", net.phoenixvine.wiki.ui.hud.HudModel.MID)));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {

            SourceHatchTracker.add(serverLevel.dimension(), getBlockPos());
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() instanceof ServerLevel serverLevel) {

            SourceHatchTracker.remove(serverLevel.dimension(), getBlockPos());
        }
    }

    public static int getMaxCapacity(int tier) {
        return 2000 * tier;
    }

    public static int getMaxConsumption(int tier) {
        return 500 * tier;
    }

    private int getAccentColor() {
        return switch (io) {
            case IN -> 0xFF8F00FF;
            case OUT -> 0xFFD466FF;
            case BOTH -> 0xFFBC66FF;
            default -> 0xFF8080A0;
        };
    }

    private int getBorderColor() {
        int accent = getAccentColor();

        return (0xAA << 24) | (accent & 0x00FFFFFF);
    }
}
