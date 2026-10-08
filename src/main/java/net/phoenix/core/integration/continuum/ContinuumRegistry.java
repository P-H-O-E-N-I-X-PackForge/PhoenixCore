package net.phoenix.core.integration.continuum;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.core.integration.continuum.block.LaunchPadBlock;
import net.phoenix.core.integration.continuum.common.RocketStats;
import net.phoenix.core.integration.continuum.item.ContinuumProbeItem;
import net.phoenix.core.integration.continuum.item.ContinuumRepairKitItem;
import net.phoenix.core.integration.continuum.item.ContinuumRocketItem;
import net.phoenix.core.integration.continuum.item.ContinuumUpgradeItem;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

/** Continuum's items and blocks. Called from {@code PhoenixCore} so they register with everything else. */
public final class ContinuumRegistry {

    private ContinuumRegistry() {}

    public static final ItemEntry<ContinuumRocketItem> ROCKET = REGISTRATE
            .item("continuum_rocket", ContinuumRocketItem::new)
            .lang("Continuum Rocket")
            .register();

    /** Payload for extraction missions. */
    public static final ItemEntry<ContinuumProbeItem> PROBE = REGISTRATE
            .item("continuum_probe", ContinuumProbeItem::new)
            .lang("Extraction Probe")
            .register();

    public static final ItemEntry<ContinuumUpgradeItem> MODULE_OVERDRIVE = REGISTRATE
            .item("continuum_module_overdrive",
                    p -> new ContinuumUpgradeItem(p, RocketStats.OVERDRIVE, 5, "Each level: -10% trip time"))
            .lang("Overdrive Module")
            .register();

    public static final ItemEntry<ContinuumUpgradeItem> MODULE_HULL = REGISTRATE
            .item("continuum_module_hull",
                    p -> new ContinuumUpgradeItem(p, RocketStats.REINFORCED_HULL, 3, "Each level: -25% wear per trip"))
            .lang("Reinforced Hull Plating")
            .register();

    public static final ItemEntry<ContinuumUpgradeItem> MODULE_INTERLOCK = REGISTRATE
            .item("continuum_module_interlock",
                    p -> new ContinuumUpgradeItem(p, RocketStats.HULL_INTERLOCK, 1,
                            "Refuses to launch when the rocket is too worn"))
            .lang("Hull Interlock Module")
            .register();

    public static final ItemEntry<ContinuumUpgradeItem> MODULE_STELLAR_SHIELD = REGISTRATE
            .item("continuum_module_stellar_shield",
                    p -> new ContinuumUpgradeItem(p, RocketStats.STELLAR_SHIELD, 1,
                            "Lets the rocket extract from, and build outposts on, a star"))
            .lang("Stellar Shielding Module")
            .register();

    public static final ItemEntry<ContinuumUpgradeItem> MODULE_SINGULARITY_SHIELD = REGISTRATE
            .item("continuum_module_singularity_shield",
                    p -> new ContinuumUpgradeItem(p, RocketStats.SINGULARITY_SHIELD, 1,
                            "Lets the rocket extract from, and build outposts on, a black hole or quasar"))
            .lang("Singularity Shielding Module")
            .register();

    public static final ItemEntry<ContinuumRepairKitItem> REPAIR_KIT = REGISTRATE
            .item("continuum_repair_kit", ContinuumRepairKitItem::new)
            .lang("Rocket Repair Kit")
            .register();

    public static final BlockEntry<LaunchPadBlock> LAUNCH_PAD = REGISTRATE
            .block("continuum_launch_pad", LaunchPadBlock::new)
            .initialProperties(() -> Blocks.POLISHED_BLACKSTONE)
            .properties(p -> p.strength(4.0f, 12.0f))
            .lang("Test Launch Pad (no power or tier)")
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().cubeBottomTop(ctx.getName(),
                    prov.mcLoc("block/chiseled_polished_blackstone"), prov.mcLoc("block/polished_blackstone"),
                    prov.mcLoc("block/polished_blackstone"))))
            .item(BlockItem::new)
            .build()
            .register();

    public static void init() {}
}
