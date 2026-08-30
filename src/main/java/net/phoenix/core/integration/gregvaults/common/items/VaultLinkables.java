package net.phoenix.core.integration.gregvaults.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class VaultLinkables {

    private static final Map<Item, IVaultLinkableHandler> REGISTRY = new IdentityHashMap<>();

    private VaultLinkables() {}

    public static synchronized void register(Item item, IVaultLinkableHandler handler) {
        if (REGISTRY.containsKey(item)) {
            throw new IllegalStateException("Handler for " + item + " already registered");
        }
        REGISTRY.put(item, handler);
    }

    @Nullable
    public static synchronized IVaultLinkableHandler get(Item item) {
        return REGISTRY.get(item);
    }

    public static boolean canLink(ItemStack stack) {
        IVaultLinkableHandler handler = get(stack.getItem());
        return handler != null && handler.canLink(stack);
    }
}
