package net.phoenix.core.integration.gregvaults.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class VaultClientModelHooks {

    private VaultClientModelHooks() {}

    public static void addVaultOverlayRenderer(Object builder) {
        try {
            builder.getClass()
                    .getMethod("addDynamicRenderer", java.util.function.Supplier.class)
                    .invoke(builder, (java.util.function.Supplier<?>) () -> VaultOverlayRender.INSTANCE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to add Vault dynamic renderer", e);
        }
    }
}
