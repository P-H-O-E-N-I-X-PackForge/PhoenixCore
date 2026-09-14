package net.phoenix.core.integration.gregpacks.common;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackInventory;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackItem;
import net.phoenix.core.integration.gregpacks.common.item.OmniPackTier;
import net.phoenix.core.integration.gregpacks.common.registry.OmniPackBlockItem;
import net.phoenix.core.integration.gregpacks.common.upgrade.UpgradeEffects;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MaintenanceModuleHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack packStack = findPackWithMaintenance(player);
        if (packStack == null) return;

        BlockEntity be = player.level().getBlockEntity(event.getPos());
        if (!(be instanceof MetaMachine machine)) return;
        if (!(machine instanceof MaintenanceHatchPartMachine hatch)) return;
        if (!hatch.hasMaintenanceProblems()) return;

        byte problems = hatch.getMaintenanceProblems();
        OmniPackTier tier = getTier(packStack);
        OmniPackInventory packInv = OmniPackInventory.fromItem(packStack,
                new UpgradeEffects(tier,
                        OmniPackInventory.fromUpgradeItem(packStack, tier.defaultMaxUpgrades)).totalSlots);

        boolean allFixed = true;
        for (int i = 0; i < 6; i++) {
            if (((problems >> i) & 1) == 1) continue;

            GTToolType required = getToolForProblem(i);
            ItemStack toolFound = findToolInPack(packInv, required);
            if (toolFound == null) {
                allFixed = false;
                continue;
            }
            hatch.setMaintenanceFixed(i);
            toolFound.setDamageValue(toolFound.getDamageValue() + 1);
            if (toolFound.getDamageValue() >= toolFound.getMaxDamage()) {
                toolFound = ItemStack.EMPTY;
            }
        }

        boolean anyToolFound = false;
        for (int i = 0; i < 6; i++) {
            if (((problems >> i) & 1) == 1) continue;
            GTToolType required = getToolForProblem(i);
            if (findToolInPack(packInv, required) != null) {
                anyToolFound = true;
                break;
            }
        }
        if (!anyToolFound) return;

        event.setCanceled(true);

        if (allFixed) {
            player.displayClientMessage(
                    Component.translatable("message.gregpacks.maintenance_fixed"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.gregpacks.maintenance_failed"), true);
        }
    }

    private static GTToolType getToolForProblem(int index) {
        return switch (index) {
            case 0 -> GTToolType.WRENCH;
            case 1 -> GTToolType.SCREWDRIVER;
            case 2 -> GTToolType.SOFT_MALLET;
            case 3 -> GTToolType.HARD_HAMMER;
            case 4 -> GTToolType.WIRE_CUTTER;
            case 5 -> GTToolType.CROWBAR;
            default -> null;
        };
    }

    private static ItemStack findToolInPack(OmniPackInventory inv, GTToolType toolType) {
        if (toolType == null) return null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && ToolHelper.is(stack, toolType)) {
                return stack;
            }
        }
        return null;
    }

    private static ItemStack findPackWithMaintenance(Player player) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            ItemStack curiosPack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .map(inv -> {
                        var slots = inv.getCurios();
                        if (!slots.containsKey("back")) return ItemStack.EMPTY;
                        var handler = slots.get("back").getStacks();
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack s = handler.getStackInSlot(i);
                            if (hasMaintenance(s)) return s;
                        }
                        return ItemStack.EMPTY;
                    }).orElse(ItemStack.EMPTY);
            if (!curiosPack.isEmpty()) return curiosPack;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (hasMaintenance(s)) return s;
        }
        return null;
    }

    private static boolean hasMaintenance(ItemStack stack) {
        OmniPackTier tier = getTier(stack);
        if (tier == null) return false;
        OmniPackInventory upgInv = OmniPackInventory.fromUpgradeItem(stack, tier.defaultMaxUpgrades);
        return new UpgradeEffects(tier, upgInv).hasMaintenance;
    }

    private static OmniPackTier getTier(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof OmniPackItem item) return item.getTier();
        if (stack.getItem() instanceof OmniPackBlockItem item) return item.getBlock().getTier();
        return null;
    }
}
