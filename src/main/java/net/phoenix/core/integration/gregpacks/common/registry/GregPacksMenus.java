package net.phoenix.core.integration.gregpacks.common.registry;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

public class GregPacksMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU,
            PhoenixCore.MOD_ID);

    public static final DeferredHolder<MenuType<OmniPackMenu>, MenuType<OmniPackMenu>> OMNIPACK_MENU = MENUS.register("omnipack_menu",
            () -> IMenuTypeExtension.create(OmniPackMenu::new));

    public static void init() {}
}
