package net.phoenix.core.configs;

import net.phoenix.core.PhoenixCore;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = PhoenixCore.MOD_ID)
public class PhoenixFissionConfigs {

    public static PhoenixFissionConfigs INSTANCE;
    public static ConfigHolder<PhoenixFissionConfigs> CONFIG_HOLDER;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(PhoenixFissionConfigs.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public FissionConfigs fission = new FissionConfigs();

    public static class FissionConfigs {

        @Configurable
        @Configurable.Comment("Enable the nuke block.")
        public boolean nukeEnabled = true;

        @Configurable
        @Configurable.Comment("Cube radius in blocks. Total affected volume is (2r+1)^3.")
        public int nukeCubeRadius = 16;

        @Configurable
        @Configurable.Comment("Hard cap to prevent insane values.")
        public int nukeCubeRadiusCap = 48;

        @Configurable
        @Configurable.Comment("Fuse time in ticks (20 ticks = 1 second). TNT is 80.")
        public int nukeFuseTicks = 120;

        @Configurable
        @Configurable.Comment("How many blocks to process per tick during cube wipe.")
        public int nukeBatchPerTick = 8000;

        @Configurable
        @Configurable.Comment("Skip blocks that have a BlockEntity (machines/chests).")
        public boolean nukeSkipBlockEntities = true;

        @Configurable
        @Configurable.Comment("Skip blocks in unloaded chunks (prevents chunk-forcing).")
        public boolean nukeSkipUnloadedChunks = true;

        @Configurable
        @Configurable.Comment("If true, replace removed blocks with fire instead of air.")
        public boolean nukeReplaceWithFire = false;

        // =========================
        // Heat model
        // =========================

        @Configurable
        @Configurable.Comment("Flat heat added per tick while running (before rods/moderators/parallels).")
        public double baseHeatPerTick = 0.0;

        @Configurable
        @Configurable.Comment("Max bonus percent from continuous running (power + breeder output). Example: 60 = up to +60%.")
        public double burnBonusMaxPercent = 60.0;

        @Configurable
        @Configurable.Comment("Seconds of continuous running required to reach the max burn bonus. Example: 1200 = 20 minutes.")
        public double burnBonusRampSeconds = 1200.0;

        @Configurable
        @Configurable.Comment("The maximum heat a reactor can hold before starting the meltdown timer.")
        public double maxSafeHeat = 10000.0;

        @Configurable
        @Configurable.Comment("Minimum heat clamp.")
        public double minHeat = 0.0;

        @Configurable
        @Configurable.Comment("Maximum heat clamp to prevent runaway numeric overflow (meltdown still happens).")
        public double maxHeatClamp = 250000.0;

        @Configurable
        @Configurable.Comment("Does the reactor naturally lose heat when not running?")
        public boolean passiveCooling = true;

        @Configurable
        @Configurable.Comment("Heat lost per tick when idling.")
        public double idleHeatLoss = 1.0;

        // =========================
        // Parallels model
        // =========================

        @Configurable
        @Configurable.Comment("Base parallels added per fuel rod (before heat-based parallels).")
        public int parallelsPerFuelRod = 1;

        @Configurable
        @Configurable.Comment("How much heat is required to add +1 to the recipe parallel multiplier.")
        public double heatPerParallel = 2000.0;

        @Configurable
        @Configurable.Comment("Hard cap for parallels.")
        public int maxParallels = 64;

        // =========================
        // Power model
        // =========================

        @Configurable
        @Configurable.Comment("How much EU/t is generated per unit of CURRENT heat (power scales with current heat).")
        public double euPerHeatUnit = 0.5;

        @Configurable
        @Configurable.Comment("Optional cap on generated EU/t. Set <= 0 for no cap.")
        public long maxGeneratedEUt = 0;

        // =========================
        // Fuel consumption model
        // =========================

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with parallels (recommended).")
        public boolean fuelUsageScalesWithParallels = true;

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with rod count (usually true).")
        public boolean fuelUsageScalesWithRodCount = true;

        @Configurable
        @Configurable.Comment("If true, blanket usage/output is additive across ALL blankets. If false, only the primary blanket is processed.")
        public boolean blanketUsageAdditive = true;

        @Configurable
        @Configurable.Comment("Clamp for total fuel discount percent from moderators.")
        public int maxFuelDiscountPercent = 90;

        @Configurable
        @Configurable.Comment("Clamp for total EU boost percent from moderators.")
        public int maxEUBoostPercent = 500;

        // =========================
        // Coolant model
        // =========================

        @Configurable
        @Configurable.Comment("If true, cooling only applies when coolant is present.")
        public boolean coolingRequiresCoolant = true;

        @Configurable
        @Configurable.Comment("If true, coolant usage is additive across all coolers. If false, uses primary cooler only.")
        public boolean coolantUsageAdditive = false;

        @Configurable
        @Configurable.Comment("Minimum EU/t produced while running (prevents 0). Set 0 to allow 0.")
        public long minGeneratedEUt = 8;

        @Configurable
        @Configurable.Comment("Exponent for heat->power curve. 1 = linear, >1 rewards high heat.")
        public double powerCurveExponent = 2.0;

        @Configurable
        @Configurable.Comment("Heat fraction of maxSafeHeat where generation begins. 0.0 = always, 0.1 = starts at 10% of maxSafeHeat.")
        public double powerStartFraction = 0.0;

        @Configurable
        public MeltdownConfigs meltdown = new MeltdownConfigs();

        @Configurable
        public ExplosionConfigs explosion = new ExplosionConfigs();

        @Configurable
        @Configurable.Comment("If true, all fuel rods in the multiblock must be the same tier.")
        public boolean restrictFuelRodTier = true;

        @Configurable
        @Configurable.Comment("If true, all coolers in the multiblock must be the same tier.")
        public boolean restrictCoolerTier = true;

        @Configurable
        @Configurable.Comment("If >= 0, all fuel rods must be exactly this tier. Set to -1 to disable.")
        public int requiredFuelRodTier = -1;

        @Configurable
        @Configurable.Comment("If >= 0, all coolers must be exactly this tier. Set to -1 to disable.")
        public int requiredCoolerTier = -1;
    }

    public static class MeltdownConfigs {

        @Configurable
        @Configurable.Comment("Base grace seconds when barely above safe heat.")
        public double baseGraceSeconds = 60.0;

        @Configurable
        @Configurable.Comment("Minimum grace seconds when extremely above safe heat.")
        public double minGraceSeconds = 10.0;

        @Configurable
        @Configurable.Comment("Severity multiplier: higher = faster meltdown when over safe heat.")
        public double excessHeatSeverity = 1.0;

        @Configurable
        @Configurable.Comment("If true, falling back under safe heat clears the timer.")
        public boolean clearTimerWhenSafe = true;
    }

    public static class ExplosionConfigs {

        @Configurable
        @Configurable.Comment("If true, replaces blocks with air/fire. If false, uses standard GT explosion (drops items).")
        public boolean destructiveExplosion = true;

        @Configurable
        @Configurable.Comment("Explosion power scales with fuel rod count: power = base + rods * multiplier.")
        public double explosionPowerPerFuelRod = 1.5;

        @Configurable
        @Configurable.Comment("The base power of the meltdown explosion.")
        public float baseExplosionPower = 10.0f;

        @Configurable
        @Configurable.Comment("Max radius used for destructive bypass block wiping.")
        public int maxDestructiveRadius = 6;
    }
}
