package net.phoenix.core.integration.gregvaults.common.registry;



import com.gregtechceu.gtceu.api.registry.registrate.entry.MachineEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
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
            BuiltInRegistries.MENU, PhoenixCore.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<VaultContainerMenu>> VAULT_MENU = MENU_TYPES.register("vault_menu",
            () -> IMenuTypeExtension.create(VaultContainerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<VaultTerminalMenu>> VAULT_TERMINAL_MENU = MENU_TYPES
            .register("vault_terminal_menu", () -> IMenuTypeExtension.create(VaultTerminalMenu::new));

    public static PartAbility VAULT_INTERFACE_ABILITY;
    public static MachineEntry<MachineDefinition> VAULT_INTERFACE;

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
