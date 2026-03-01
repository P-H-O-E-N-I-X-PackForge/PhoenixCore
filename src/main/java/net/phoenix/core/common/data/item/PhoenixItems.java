package net.phoenix.core.common.data.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;

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
    public static ItemEntry<Item> SOURCE_FIBER_MESH = REGISTRATE.item("source_fiber_mesh", Item::new)
            .lang("§dSource Fiber Mesh")
            .register();

    public static ItemEntry<TeslaStabilizerItem> LV_TESLA_STABILIZER = REGISTRATE
            .item("lv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("LV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "LV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for low-voltage wireless power.\nIs this even useful?");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/lv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> MV_TESLA_STABILIZER = REGISTRATE
            .item("mv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("MV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "MV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for medium-voltage wireless power.\nYou probably won't use this.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/mv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> HV_TESLA_STABILIZER = REGISTRATE
            .item("hv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("HV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "HV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for high-voltage wireless power.\nOne could say, this is quite shocking!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/hv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> EV_TESLA_STABILIZER = REGISTRATE
            .item("ev_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("EV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "EV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for extreme-voltage wireless power. \nRadical!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/ev_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> IV_TESLA_STABILIZER = REGISTRATE
            .item("iv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("IV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "IV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for insane-voltage wireless power.\nCrazy? I was crazy once.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/iv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> LuV_TESLA_STABILIZER = REGISTRATE
            .item("luv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("LuV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "LuV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ludicrous-voltage wireless power.\nYour progress is quite impressive! Keep going!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/luv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> ZPM_TESLA_STABILIZER = REGISTRATE
            .item("zpm_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("ZPM Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "ZPM Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for zero-point-module wireless power.\nWe getting sci-fi up in here.");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/zpm_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> UV_TESLA_STABILIZER = REGISTRATE
            .item("uv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("UV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "UV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ultimate-voltage wireless power.\nIs this the ultimate? Not quite!");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/uv_tesla_stabilizer")))
            .register();

    public static ItemEntry<TeslaStabilizerItem> UHV_TESLA_STABILIZER = REGISTRATE
            .item("uhv_tesla_stabilizer", TeslaStabilizerItem::new)
            .lang("UHV Tesla Stabilizer")
            .setData(ProviderType.LANG, (ctx, prov) -> {
                prov.add(ctx.get(), "UHV Tesla Stabilizer");
                prov.add(ctx.get().getDescriptionId() + ".tooltip",
                        "A stabilizing unit for ultra-high-voltage wireless power.\nIs this the peak of power? Or merely the beginning?");
            })
            .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/tesla_stabilizer/uhv_tesla_stabilizer")))
            .register();

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
