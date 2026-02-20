package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;

import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.ArrayList;
import java.util.List;

import static net.phoenix.core.PhoenixCore.PHOENIX_CREATIVE_TAB;
import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixItems {

    static {
        REGISTRATE.creativeModeTab(() -> PHOENIX_CREATIVE_TAB);
    }

    public static ItemEntry<ComponentItem> POWER_UNIT_LUV = REGISTRATE.item("luv_power_unit", ComponentItem::create)
            .lang("LuV Power Unit")
            .properties(p -> p.stacksTo(8))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tools/power_unit_luv")))
            .onRegister((c) -> c.attachComponents(ElectricStats.createElectricItem(102400000L, GTValues.LuV)))
            .register();
    public static ItemEntry<ComponentItem> POWER_UNIT_ZPM = REGISTRATE.item("zpm_power_unit", ComponentItem::create)
            .lang("ZPM Power Unit")
            .properties(p -> p.stacksTo(8))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tools/power_unit_zpm")))
            .onRegister((c) -> c.attachComponents(ElectricStats.createElectricItem(409600000L, GTValues.ZPM)))
            .register();
    public static ItemEntry<TeslaBinderItem> TESLA_BINDER = REGISTRATE
            .item("tesla_binder", TeslaBinderItem::new)
            .lang("Tesla Binder")
            .properties(p -> p.stacksTo(1))
            .onRegister(c -> c.attachComponents(c))
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tools/tesla_binder")))
            .register();
    public static ItemEntry<Item> HONEY_TREAT = REGISTRATE
            .item("honey_treat", Item::new)
            .lang("§6Honey Treat")
            .register();
    public static ItemEntry<Item> ROYAL_JELLY = REGISTRATE
            .item("royal_jelly", Item::new)
            .lang("§dRoyal Jelly")
            .register();
    public static ItemEntry<Item> HONEY_COMB_BASE = REGISTRATE
            .item("honey_comb_base", Item::new)
            .lang("§6Honeycomb Base")
            .register();
    public static ItemEntry<Item> HONEY_COMB_BASE_MOLD = REGISTRATE
            .item("honey_comb_base_mold", Item::new)
            .lang("§6Honeycomb Base Mold")
            .register();
    public static ItemEntry<Item> SOURCE_FIBERS = REGISTRATE.item("source_fibers", Item::new)
            .lang("§dRaw Source Fibers")
            .register();
    public static ItemEntry<Item> CARBON_MESH = REGISTRATE.item("source_fiber_mesh", Item::new)
            .lang("§dSource Fiber Mesh")
            .register();
    private static List<ItemEntry<Item>> registerAllStabilizers() {
        List<ItemEntry<Item>> entries = new ArrayList<>();
        for (int tier = GTValues.LV; tier <= GTValues.UHV; tier++) {
            int currentTier = tier;
            String tierName = GTValues.VN[currentTier].toLowerCase();

            entries.add(REGISTRATE.item(tierName + "_tesla_stabilizer", Item::new)
                    .lang(GTValues.VNF[currentTier] + " Tesla Stabilizer") // This sets the name
                    .setData(ProviderType.LANG, (ctx, prov) -> {
                        // 1. Re-confirm the Name (prevents it from being empty)
                        prov.add(ctx.get(), GTValues.VNF[currentTier] + " Tesla Stabilizer");

                        // 2. Add the Tooltip using a CUSTOM KEY
                        // We attach ".tooltip" to the end of the item's internal ID
                        prov.add(ctx.get().getDescriptionId() + ".tooltip",
                                "A stabilizing unit for " + getVoltageName(currentTier) + " wireless power.\n" + getFlavorText(currentTier));
                    })
                    .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/" + tierName + "_tesla_stabilizer")))
                    .register());
        }
        return entries;
    }

    private static String getVoltageName(int tier) {
        return switch (tier) {
            case GTValues.LV -> "low-voltage";
            case GTValues.MV -> "medium-voltage";
            case GTValues.HV -> "high-voltage";
            case GTValues.EV -> "extreme-voltage";
            case GTValues.IV -> "insane-voltage";
            case GTValues.LuV -> "ludicrous-voltage";
            case GTValues.ZPM -> "zero-point-module";
            case GTValues.UV -> "ultimate-voltage";
            case GTValues.UHV -> "ultra-high-voltage";
            default -> "unknown-voltage";
        };
    }

    private static String getFlavorText(int tier) {
        return switch (tier) {
            case GTValues.LV -> "Is this even useful?";
            case GTValues.MV -> "You probably won't use this.";
            case GTValues.HV -> "One could say, this is quite shocking!";
            case GTValues.EV -> "Radical!";
            case GTValues.IV -> "Crazy? I was crazy once.";
            case GTValues.LuV -> "Your progress is quite impressive! Keep going!";
            case GTValues.ZPM -> "We getting sci-fi up in here.";
            case GTValues.UV -> "Is this the ultimate? Not quite!";
            case GTValues.UHV -> "Is this the peak of power? Or merely the beginning";
            default -> "";
        };
    }


    /*
     * public static ItemEntry<ComponentItem> ENERGY_LAPOTRONIC_ORB = REGISTRATE
     * .item("lapotronic_energy_orb", ComponentItem::create)
     * .lang("Lapotronic Energy Orb")
     * .model(overrideModel(GTCEu.id("battery"), 8))
     * .onRegister(modelPredicate(GTCEu.id("battery"), ElectricStats::getStoredPredicate))
     * .onRegister(attach(ElectricStats.createRechargeableBattery(250_000_000L, GTValues.IV)))
     * .tag(CustomTags.IV_BATTERIES).register();
     */

    public static void init() {}
}
