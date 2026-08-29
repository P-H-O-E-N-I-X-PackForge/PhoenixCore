package net.phoenix.core.integration.drone.mui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.drone.network.DroneTargetView;

import brachy.modularui.value.sync.SyncHandler;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DroneTargetListSyncHandler extends SyncHandler<DroneTargetListSyncHandler> {

    private final BooleanSupplier isDirty;
    private final Supplier<List<DroneTargetView>> targetsSupplier;

    private List<DroneTargetView> targets = List.of();
    private Runnable onChanged;

    public DroneTargetListSyncHandler(BooleanSupplier isDirty, Supplier<List<DroneTargetView>> targetsSupplier) {
        this.isDirty = isDirty;
        this.targetsSupplier = targetsSupplier;
        allowC2S(false);
    }

    public List<DroneTargetView> getTargets() {
        return targets;
    }

    public DroneTargetListSyncHandler onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        if (!init && !isDirty.getAsBoolean()) return;
        List<DroneTargetView> current = targetsSupplier.get();
        syncToClient(0, buf -> DroneTargetView.encodeList(current, buf));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readOnClient(int id, FriendlyByteBuf buf) {
        this.targets = DroneTargetView.decodeList(buf);
        if (onChanged != null) onChanged.run();
    }

    @Override
    public void readOnServer(int id, FriendlyByteBuf buf) {}
}
