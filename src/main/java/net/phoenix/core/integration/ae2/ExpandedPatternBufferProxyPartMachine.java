package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

public class ExpandedPatternBufferProxyPartMachine extends MEPatternBufferProxyPartMachine {

    public static final int[][] RANGES = { { 0, 71 }, { 0, 23 }, { 24, 47 }, { 48, 71 }, { 0, 35 }, { 36, 71 } };

    @SaveField
    @SyncToClient
    private int priority = 0;

    @SaveField
    @SyncToClient
    private int rangeIndex = 0;

    public ExpandedPatternBufferProxyPartMachine(BlockEntityCreationInfo info) {
        super(info);
        getProxySlotRecipeHandler().getProxySlotHandlers()
                .forEach(handlers -> ExpandedProxyContext.register(handlers, this));
    }

    public int getPriority() {
        return priority;
    }

    public boolean allows(int slot) {
        int[] range = RANGES[Math.floorMod(rangeIndex, RANGES.length)];
        return slot >= range[0] && slot <= range[1];
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return super.onScrewdriverClick(context);
        if (!isRemote()) {
            if (player.isCrouching()) {
                priority = (priority + 1) % 10;
                player.displayClientMessage(
                        Component.translatable("gui.phoenixcore.expanded_proxy.priority", priority), true);
            } else {
                rangeIndex = (rangeIndex + 1) % RANGES.length;
                int[] range = RANGES[rangeIndex];
                player.displayClientMessage(Component.translatable("gui.phoenixcore.expanded_proxy.slots",
                        range[0] + 1, range[1] + 1), true);
            }
        }
        return InteractionResult.sidedSuccess(isRemote());
    }
}
