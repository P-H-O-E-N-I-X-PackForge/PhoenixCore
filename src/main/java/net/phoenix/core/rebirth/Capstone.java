package net.phoenix.core.rebirth;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public enum Capstone {

    TESLA("phoenix_tesla_network", "tesla_tower", "Tesla Tower", "Tesla Tower", 0x59D8FF),
    HONEY("phoenixcore", "honey_crystallization_chamber", "Honey Crystallization Chamber", "Honey Chamber", 0xFFB52E),
    PLASMA("phoenixcore", "high_pressure_plasma_arc_furnace", "High-Pressure Plasma Arc Furnace", "Plasma Furnace",
            0xFF5CF0),
    ENGINE("phoenixconflux", "conflux_panoptic_engine", "Panoptic Engine", "Panoptic Engine", 0x6BE890),
    CONTINUUM("phoenix_continuum", "continuum_launch_complex", "Continuum Launch Complex", "Launch Complex", 0x7FA8FF),
    NULL_WELL("phoenixcore", "null_well", "Null Well", "Null Well", 0xC98CFF),
    TOKAMAK("phoenix_fission", "fusion_reactor", "Tokamak Fusion Reactor", "Tokamak", 0x00E5CC),
    STELLAR_FORGE("phoenixcore", "stellar_forge", "Stellar Forge", "Stellar Forge", 0xFFB347),
    AETHER_CRUCIBLE("phoenixcore", "aether_crucible", "Aether Crucible", "Aether Crucible", 0x6EF0C8),
    SCULK_CORTEX("phoenixcore", "sculk_cortex", "Sculk Cortex", "Sculk Cortex", 0x35C9E8),
    PHOENIX_INFUSER("phoenixcore", "phoenix_infuser", "Phoenix Infuser", "Phoenix Infuser", 0xFF8A3A),
    ANTIMATTER_COLLIDER("phoenixcore", "antimatter_collider", "Antimatter Collider", "Collider", 0xFF4D8A),
    ASCENSION_REACTOR("phoenix_fission", "ascension_reactor", "Ascension Reactor", "Ascension", 0x7CFFD8);

    public static final int COUNT = values().length;

    public static final Capstone[] PROGRESSION = { CONTINUUM, AETHER_CRUCIBLE, HONEY, TOKAMAK, TESLA, PLASMA,
            STELLAR_FORGE, SCULK_CORTEX, NULL_WELL, ANTIMATTER_COLLIDER, ASCENSION_REACTOR, PHOENIX_INFUSER, ENGINE };

    public final ResourceLocation machine;
    public final String displayName;
    public final String shortName;
    public final int color;

    Capstone(String namespace, String path, String displayName, String shortName, int color) {
        this.machine = ResourceLocation.fromNamespaceAndPath(namespace, path);
        this.displayName = displayName;
        this.shortName = shortName;
        this.color = color;
    }

    public static @Nullable Capstone of(ResourceLocation machineId) {
        for (Capstone c : values()) if (c.machine.equals(machineId)) return c;
        return null;
    }
}
