package net.phoenix.core.integration.gregpacks.common.registry;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.phoenix.core.integration.gregpacks.common.item.UpgradeItem;
import net.phoenix.core.integration.gregpacks.common.item.UpgradeType;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

@SuppressWarnings("all")
public class GregPacksUpgrades {

    public static final ItemEntry<UpgradeItem> ITEM_CAPACITY_I = REGISTRATE
            .item("item_module_1",
                    p -> new UpgradeItem(UpgradeType.ITEM_CAPACITY_I, "item.gregpacks.item_module_1.tooltip"))
            .lang("Item Capacity Module I").register();

    public static final ItemEntry<UpgradeItem> ITEM_CAPACITY_II = REGISTRATE
            .item("item_module_2",
                    p -> new UpgradeItem(UpgradeType.ITEM_CAPACITY_II, "item.gregpacks.item_module_2.tooltip"))
            .lang("Item Capacity Module II").register();

    public static final ItemEntry<UpgradeItem> ITEM_CAPACITY_III = REGISTRATE
            .item("item_module_3",
                    p -> new UpgradeItem(UpgradeType.ITEM_CAPACITY_III, "item.gregpacks.item_module_3.tooltip"))
            .lang("Item Capacity Module III").register();

    public static final ItemEntry<UpgradeItem> FLUID_CAPACITY_I = REGISTRATE
            .item("fluid_module_1",
                    p -> new UpgradeItem(UpgradeType.FLUID_CAPACITY_I, "item.gregpacks.fluid_module_1.tooltip"))
            .lang("Fluid Capacity Module I").register();

    public static final ItemEntry<UpgradeItem> FLUID_CAPACITY_II = REGISTRATE
            .item("fluid_module_2",
                    p -> new UpgradeItem(UpgradeType.FLUID_CAPACITY_II, "item.gregpacks.fluid_module_2.tooltip"))
            .lang("Fluid Capacity Module II").register();

    public static final ItemEntry<UpgradeItem> FLUID_CAPACITY_III = REGISTRATE
            .item("fluid_module_3",
                    p -> new UpgradeItem(UpgradeType.FLUID_CAPACITY_III, "item.gregpacks.fluid_module_3.tooltip"))
            .lang("Fluid Capacity Module III").register();

    public static final ItemEntry<UpgradeItem> ENERGY_CAPACITY_I = REGISTRATE
            .item("energy_module_1",
                    p -> new UpgradeItem(UpgradeType.ENERGY_CAPACITY_I, "item.gregpacks.energy_module_1.tooltip"))
            .lang("Energy Capacity Module I").register();

    public static final ItemEntry<UpgradeItem> ENERGY_CAPACITY_II = REGISTRATE
            .item("energy_module_2",
                    p -> new UpgradeItem(UpgradeType.ENERGY_CAPACITY_II, "item.gregpacks.energy_module_2.tooltip"))
            .lang("Energy Capacity Module II").register();

    public static final ItemEntry<UpgradeItem> ENERGY_CAPACITY_III = REGISTRATE
            .item("energy_module_3",
                    p -> new UpgradeItem(UpgradeType.ENERGY_CAPACITY_III, "item.gregpacks.energy_module_3.tooltip"))
            .lang("Energy Capacity Module III").register();

    public static final ItemEntry<UpgradeItem> CRAFTING_MODULE = REGISTRATE
            .item("crafting_module",
                    p -> new UpgradeItem(UpgradeType.CRAFTING, "item.gregpacks.crafting_module.tooltip"))
            .lang("Crafting Module").register();

    public static final ItemEntry<UpgradeItem> FEEDING_MODULE = REGISTRATE
            .item("feeding_module", p -> new UpgradeItem(UpgradeType.FEEDING, "item.gregpacks.feeding_module.tooltip"))
            .lang("Feeding Module").register();

    public static final ItemEntry<UpgradeItem> JETPACK_MODULE_I = REGISTRATE
            .item("jetpack_module_1",
                    p -> new UpgradeItem(UpgradeType.JETPACK_I, "item.gregpacks.jetpack_module_1.tooltip"))
            .lang("Jetpack Module I").register();

    public static final ItemEntry<UpgradeItem> JETPACK_MODULE_II = REGISTRATE
            .item("jetpack_module_2",
                    p -> new UpgradeItem(UpgradeType.JETPACK_II, "item.gregpacks.jetpack_module_2.tooltip"))
            .lang("Jetpack Module II").register();

    public static final ItemEntry<UpgradeItem> MAINTENANCE_MODULE = REGISTRATE
            .item("maintenance_module",
                    p -> new UpgradeItem(UpgradeType.MAINTENANCE, "item.gregpacks.maintenance_module.tooltip"))
            .lang("Maintenance Module").register();

    public static final ItemEntry<UpgradeItem> MAGNET_MODULE_I = REGISTRATE
            .item("magnet_module_1",
                    p -> new UpgradeItem(UpgradeType.MAGNET_I, "item.gregpacks.magnet_module_1.tooltip"))
            .lang("Magnet Module I").register();

    public static final ItemEntry<UpgradeItem> MAGNET_MODULE_II = REGISTRATE
            .item("magnet_module_2",
                    p -> new UpgradeItem(UpgradeType.MAGNET_II, "item.gregpacks.magnet_module_2.tooltip"))
            .lang("Magnet Module II").register();

    public static final ItemEntry<UpgradeItem> PROCESSING_MODULE = REGISTRATE
            .item("processing_module",
                    p -> new UpgradeItem(UpgradeType.PROCESSING, "item.gregpacks.processing_module.tooltip"))
            .lang("Processing Module").register();

    public static void init() {}
}
