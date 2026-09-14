package net.phoenix.core.integration.gregvaults.common.registry;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.client.screen.VaultContainerMenu;
import net.phoenix.core.integration.gregvaults.client.screen.VaultTerminalMenu;
import net.phoenix.core.integration.gregvaults.common.blocks.CoreTier;
import net.phoenix.core.integration.gregvaults.common.blocks.VaultCoreBlock;
import net.phoenix.core.integration.gregvaults.common.items.VaultLinkables;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultInterfacePart;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public final class VaultRegistry {

    private VaultRegistry() {}

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK1 = REGISTRATE
            .block("vault_core_mk1", props -> new VaultCoreBlock(CoreTier.MK1, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                    prov.models().cubeAll(ctx.getName(), prov.modLoc("block/cores/vault_core_mk1"))))
            .lang("Vault Core MK I")
            .simpleItem()
            .register();

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK2 = REGISTRATE
            .block("vault_core_mk2", props -> new VaultCoreBlock(CoreTier.MK2, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                    prov.models().cubeAll(ctx.getName(), prov.modLoc("block/cores/vault_core_mk2"))))
            .lang("Vault Core MK II")
            .simpleItem()
            .register();

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK3 = REGISTRATE
            .block("vault_core_mk3", props -> new VaultCoreBlock(CoreTier.MK3, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(),
                    prov.models().cubeAll(ctx.getName(), prov.modLoc("block/cores/vault_core_mk3"))))
            .lang("Vault Core MK III")
            .simpleItem()
            .register();

    public static final ItemEntry<WirelessTerminalItem> WIRELESS_VAULT_TERMINAL = REGISTRATE
            .item("wireless_vault_terminal", WirelessTerminalItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES, PhoenixCore.MOD_ID);

    public static final RegistryObject<MenuType<VaultContainerMenu>> VAULT_MENU = MENU_TYPES.register("vault_menu",
            () -> IForgeMenuType.create(VaultContainerMenu::new));

    public static final RegistryObject<MenuType<VaultTerminalMenu>> VAULT_TERMINAL_MENU = MENU_TYPES
            .register("vault_terminal_menu", () -> IForgeMenuType.create(VaultTerminalMenu::new));

    public static PartAbility VAULT_INTERFACE_ABILITY;
    public static MachineDefinition VAULT_INTERFACE;

    public static void registerEventBus(IEventBus bus) {
        MENU_TYPES.register(bus);
    }

    public static void registerLinkables() {
        VaultLinkables.register(WIRELESS_VAULT_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
    }

    public static void initMachines() {
        VAULT_INTERFACE_ABILITY = new PartAbility("vault_interface");

        VAULT_INTERFACE = REGISTRATE
                .machine("vault_interface", VaultInterfacePart::new)
                .rotationState(RotationState.ALL)
                .abilities(VAULT_INTERFACE_ABILITY)
                .overlaySteamHullModel("vault_interface")
                .tooltips(Component.translatable("tooltip.gregtechvaults.vault_interface"))
                .register();
    }
}
