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
public class SourceHatchPartMachine extends TieredIOPartMachine implements ISourceProviderCapability, IMuiMachine {

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
        return net.phoenix.core.client.gui.hud.MachineHud.build(this, data, syncManager, settings, (id, value) -> {},
                this::hudModel);
    }

    private net.phoenix.core.client.gui.hud.HudModel hudModel() {
        boolean input = io == IO.IN;
        int radius = net.phoenix.core.configs.PhoenixConfigs.INSTANCE.sourceHatch.sourceJarCheckRadius;
        return net.phoenix.core.client.gui.hud.HudModel
                .of(com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Source " + (input ? "Input" : "Output") + " Hatch")
                .theme(net.phoenix.core.client.gui.hud.HudTheme.ARCANE)
                .formed(() -> true)
                .page(net.phoenix.core.client.gui.hud.ExoticPages.sourceHatch(
                        com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Source " + (input ? "Input" : "Output") +
                                " Hatch",
                        input, () -> sourceContainer.getSource(), () -> sourceContainer.getMaxSource(),
                        () -> sourceContainer.getTransferRate(), () -> sourceContainer.getCurrentFlow(), radius,
                        getAccentColor(), () -> input ? "Draws Source from jars within " + radius + " blocks" :
                                "Fills jars within " + radius + " blocks"))
                .hero(net.phoenix.core.client.gui.hud.ExoticHeroes.sourceOrb(
                        net.phoenix.core.client.gui.hud.Heroes.eased(
                                () -> sourceContainer.getSource() /
                                        (float) Math.max(1, sourceContainer.getMaxSource())),
                        input, getAccentColor()), 62)
                .badge(() -> input ?
                        new net.phoenix.core.client.gui.hud.HudModel.Badge("INPUT",
                                net.phoenix.core.client.gui.hud.HudModel.PURPLE) :
                        new net.phoenix.core.client.gui.hud.HudModel.Badge("OUTPUT",
                                net.phoenix.core.client.gui.hud.HudModel.CYAN))
                .subtitle(() -> input ? "Draws Source from jars within " + radius + " blocks" :
                        "Fills jars within " + radius + " blocks")
                .cells(() -> {
                    var cells = new java.util.ArrayList<net.phoenix.core.client.gui.hud.HudModel.Cell>();
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("SOURCE",
                            String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getSource()),
                            "of " + String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getMaxSource()),
                            net.phoenix.core.client.gui.hud.HudModel.PURPLE));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("RATE",
                            String.format(java.util.Locale.ROOT, "%,d /t", sourceContainer.getTransferRate()),
                            "most it moves", net.phoenix.core.client.gui.hud.HudModel.CYAN));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("FLOW",
                            String.format(java.util.Locale.ROOT, "%,d", sourceContainer.getCurrentFlow()),
                            "lately", net.phoenix.core.client.gui.hud.HudModel.GREEN));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("RANGE", radius + " blocks",
                            "jar search", net.phoenix.core.client.gui.hud.HudModel.GOLD));
                    return cells;
                })
                .bars(() -> java.util.List.of(new net.phoenix.core.client.gui.hud.HudModel.Bar("Source",
                        sourceContainer.getSource() / (float) Math.max(1, sourceContainer.getMaxSource()),
                        String.format(java.util.Locale.ROOT, "%,d / %,d", sourceContainer.getSource(),
                                sourceContainer.getMaxSource()),
                        net.phoenix.core.client.gui.hud.HudModel.PURPLE)))
                .notes(() -> java.util.List.of(net.phoenix.core.client.gui.hud.HudModel.line(
                        "Source pipes and relays can feed it too.", net.phoenix.core.client.gui.hud.HudModel.MID)));
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
