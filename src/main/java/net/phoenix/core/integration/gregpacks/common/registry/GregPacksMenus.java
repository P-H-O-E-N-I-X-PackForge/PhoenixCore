package net.phoenix.core.integration.gregpacks.common.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

public class GregPacksMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            PhoenixCore.MOD_ID);

    public static final RegistryObject<MenuType<OmniPackMenu>> OMNIPACK_MENU = MENUS.register("omnipack_menu",
            () -> IForgeMenuType.create(OmniPackMenu::new));

    public static void init() {}
}
