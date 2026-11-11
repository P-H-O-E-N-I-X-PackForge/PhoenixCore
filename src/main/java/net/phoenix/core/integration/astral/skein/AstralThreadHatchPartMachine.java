package net.phoenix.core.integration.astral.skein;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.astral.api.machine.trait.NotifiableAstralThreadContainer;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.integration.astral.item.AstralWandItem;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import lombok.Getter;

@Getter
public class AstralThreadHatchPartMachine extends TieredIOPartMachine implements IMuiMachine {

    private final NotifiableAstralThreadContainer threadContainer;

    public AstralThreadHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.threadContainer = attachTrait(
                new NotifiableAstralThreadContainer(io, getMaxCapacity(tier), getMaxConsumption(tier)));
    }

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        if (!(context.getItemInHand().getItem() instanceof AstralWandItem)) return super.onUseWithItem(context);

        Player player = context.getPlayer();
        if (getLevel() == null || getLevel().isClientSide) return InteractionResult.SUCCESS;

        ItemStack cell = findThreadCell(player);
        if (cell.isEmpty()) {
            player.displayClientMessage(Component.literal("No Astral Thread Cell carried."), true);
            return InteractionResult.CONSUME;
        }

        if (threadContainer.getHandlerIO() == IO.OUT) {
            int moved = AstralThreadCellItem.addThread(cell, threadContainer.getThread());
            threadContainer.removeThread(moved);
            player.displayClientMessage(Component.literal("Drew " + moved + " Thread from hatch."), true);
        } else {
            int moved = threadContainer.addThread(AstralThreadCellItem.getThread(cell));
            AstralThreadCellItem.removeThread(cell, moved);
            player.displayClientMessage(Component.literal("Fed " + moved + " Thread into hatch."), true);
        }
        return InteractionResult.CONSUME;
    }

    private ItemStack findThreadCell(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AstralThreadCellItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public brachy.modularui.screen.ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager,
                                                           UISettings settings) {
        syncManager.syncValue("thread", 0, new brachy.modularui.value.sync.IntSyncValue(
                () -> threadContainer.getThread(), v -> {
                    int delta = v - threadContainer.getThread();
                    if (delta > 0) threadContainer.addThread(delta);
                    else if (delta < 0) threadContainer.removeThread(-delta);
                }));
        return net.phoenix.core.client.gui.hud.MachineHud.build(this, data, syncManager, settings, (id, value) -> {},
                this::hudModel);
    }

    private net.phoenix.core.client.gui.hud.HudModel hudModel() {
        boolean output = threadContainer.getHandlerIO() == IO.OUT;
        return net.phoenix.core.client.gui.hud.HudModel
                .of(com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Astral Thread " + (output ? "Output" : "Input") +
                        " Hatch")
                .theme(net.phoenix.core.client.gui.hud.HudTheme.ASTRAL)
                .formed(() -> true)
                .page(net.phoenix.core.client.gui.hud.ExoticPages.threadHatch(
                        com.gregtechceu.gtceu.api.GTValues.VNF[tier] + " Astral Thread " +
                                (output ? "Output" : "Input") + " Hatch",
                        output, () -> threadContainer.getThread(), () -> threadContainer.getMaxThread(),
                        () -> threadContainer.getTransferRate(), getAccentColor(), 0xFFE8D27A,
                        () -> "Astral Wand: " + (output ? "draw its thread into a Thread Cell." :
                                "feed a Thread Cell into it.")))
                .hero(net.phoenix.core.client.gui.hud.ExoticHeroes.threadLoom(
                        net.phoenix.core.client.gui.hud.Heroes.eased(
                                () -> threadContainer.getThread() /
                                        (float) Math.max(1, threadContainer.getMaxThread())),
                        !output, getAccentColor(), 0xFFE8D27A), 62)
                .badge(() -> output ?
                        new net.phoenix.core.client.gui.hud.HudModel.Badge("OUTPUT",
                                net.phoenix.core.client.gui.hud.HudModel.CYAN) :
                        new net.phoenix.core.client.gui.hud.HudModel.Badge("INPUT",
                                net.phoenix.core.client.gui.hud.HudModel.PURPLE))
                .subtitle(() -> output ? "Holds the thread a machine spins" : "Feeds thread to a machine")
                .cells(() -> {
                    var cells = new java.util.ArrayList<net.phoenix.core.client.gui.hud.HudModel.Cell>();
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("THREAD",
                            String.format(java.util.Locale.ROOT, "%,d", threadContainer.getThread()),
                            "of " + String.format(java.util.Locale.ROOT, "%,d", threadContainer.getMaxThread()),
                            net.phoenix.core.client.gui.hud.HudModel.PURPLE));
                    cells.add(net.phoenix.core.client.gui.hud.HudModel.cell("RATE",
                            String.format(java.util.Locale.ROOT, "%,d /t", threadContainer.getTransferRate()),
                            "most it moves", net.phoenix.core.client.gui.hud.HudModel.CYAN));
                    return cells;
                })
                .bars(() -> java.util.List.of(new net.phoenix.core.client.gui.hud.HudModel.Bar("Thread",
                        threadContainer.getThread() / (float) Math.max(1, threadContainer.getMaxThread()),
                        String.format(java.util.Locale.ROOT, "%,d / %,d", threadContainer.getThread(),
                                threadContainer.getMaxThread()),
                        net.phoenix.core.client.gui.hud.HudModel.PURPLE)))
                .notes(() -> java.util.List.of(net.phoenix.core.client.gui.hud.HudModel.line(output ?
                        "Astral Wand: draw its thread into a Thread Cell." :
                        "Astral Wand: feed a Thread Cell into it.", net.phoenix.core.client.gui.hud.HudModel.MID)));
    }

    private int getAccentColor() {
        return threadContainer.getHandlerIO() == IO.OUT ? 0xFFB07CFF : 0xFF5C2FB0;
    }

    public static int getMaxCapacity(int tier) {
        return 2000 * tier;
    }

    public static int getMaxConsumption(int tier) {
        return 500 * tier;
    }
}
