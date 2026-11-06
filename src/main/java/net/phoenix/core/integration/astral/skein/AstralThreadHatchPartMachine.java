package net.phoenix.core.integration.astral.skein;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.ars_nouveau.client.gui.SourceHatchBackground;
import net.phoenix.core.integration.astral.api.machine.trait.NotifiableAstralThreadContainer;
import net.phoenix.core.integration.astral.item.AstralThreadCellItem;
import net.phoenix.core.integration.astral.item.AstralWandItem;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;
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
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        if (isRemote()) {
            mainWidget.background(new SourceHatchBackground(getAccentColor()));
        }

        mainWidget.child(Flow.col()
                .coverChildren()
                .margin(8, 6)
                .child(Text.dynamic(() -> Component.literal("ASTRAL THREAD HATCH")
                        .withStyle(style -> style.withColor(getAccentColor())))
                        .asWidget()
                        .marginBottom(8))
                .child(Text.dynamic(() -> Component.literal("MODE: " + threadContainer.getHandlerIO().name())
                        .withStyle(ChatFormatting.WHITE))
                        .asWidget())
                .child(Text.dynamic(() -> {
                    int cur = threadContainer.getThread();
                    int max = Math.max(1, threadContainer.getMaxThread());
                    int pct = (int) ((cur * 100L) / max);
                    return Component.literal("THREAD: " + cur + " / " + max + " (" + pct + "%)")
                            .withStyle(ChatFormatting.WHITE);
                }).asWidget())
                .child(Text.dynamic(() -> Component.literal("RATE: " + threadContainer.getTransferRate() + "/s")
                        .withStyle(ChatFormatting.WHITE))
                        .asWidget()));
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
