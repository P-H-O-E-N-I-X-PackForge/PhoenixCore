package net.phoenix.core.integration.gregpacks.common.inventory;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IForgeMenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;

public class GregPacksMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            PhoenixCore.MOD_ID);

    public static final RegistryObject<MenuType<OmniPackMenu>> OMNIPACK_MENU = MENUS.register("omnipack_menu",
            () -> IForgeMenuType.create(OmniPackMenu::new));

    public static void init() {}
}
