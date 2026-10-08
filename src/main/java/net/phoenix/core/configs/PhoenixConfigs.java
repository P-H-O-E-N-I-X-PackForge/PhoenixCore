package net.phoenix.core.configs;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.common.blocks.CoreTier;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = PhoenixCore.MOD_ID)
public class PhoenixConfigs {

    public static PhoenixConfigs INSTANCE;
    public static ConfigHolder<PhoenixConfigs> CONFIG_HOLDER;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(PhoenixConfigs.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public FeatureConfigs features = new FeatureConfigs();

    @Configurable
    public static WingFlightConfigs wingFlight = new WingFlightConfigs();

    @Configurable
    public SourceHatchConfig sourceHatch = new SourceHatchConfig();
    @Configurable
    public PhoenixConfigs.CoreValues coreValues = new PhoenixConfigs.CoreValues();

    @Configurable
    public PhoenixConfigs.VaultValues vaultValues = new PhoenixConfigs.VaultValues();

    @Configurable
    public PhoenixConfigs.WirelessTerminal wirelessTerminal = new PhoenixConfigs.WirelessTerminal();

    @Configurable
    @Configurable.Comment("Whether buying a shop entry requires winning a quick timing minigame first.")
    public boolean shopMinigameEnabled = true;

    @Configurable
    public ContinuumConfigs continuum = new ContinuumConfigs();

    /** Continuum space missions: pacing, rocket wear and the launch pad. */
    public static class ContinuumConfigs {

        public enum RenderQuality {
            LOW,
            MEDIUM,
            HIGH
        }

        public enum PlanetStyle {
            SPHERE,
            CUBE
        }

        @Configurable
        @Configurable.Comment({
                "Visual quality of the Continuum space views (this client only). LOW uses far fewer mesh",
                "triangles, fewer noise layers and a simpler backdrop, for weaker GPUs. Pressing Q on the map",
                "changes it for that session without touching this setting.",
                "Default: MEDIUM"
        })
        public RenderQuality renderQuality = RenderQuality.MEDIUM;

        @Configurable
        @Configurable.Comment({
                "How planets and moons are drawn. SPHERE is the smooth shaded globe. CUBE draws every world as a",
                "blocky cube whose surface is built from chunky square cells (8 / 12 / 16 per face at",
                "LOW / MEDIUM / HIGH), for a more Minecraft look. Stars and black holes stay round.",
                "Pressing C on the map flips it for that session.",
                "Default: SPHERE"
        })
        public PlanetStyle planetStyle = PlanetStyle.SPHERE;

        @Configurable
        @Configurable.Comment({
                "Scales every mission's real-time length. 1.0 = the trip times in the body definitions",
                "(5 minutes for the nearest body up to 2 hours for the farthest). Lower it to test.",
                "Default: 1.0"
        })
        @Configurable.DecimalRange(min = 0.001, max = 100.0)
        public double tripTimeMultiplier = 1.0;

        @Configurable
        @Configurable.Comment({
                "The most trip time rocket upgrades can remove in total (0.5 = up to half).",
                "Default: 0.5"
        })
        @Configurable.DecimalRange(min = 0.0, max = 0.9)
        public double maxTripReduction = 0.5;

        @Configurable
        @Configurable.Comment({
                "Rocket wear (0 = new, 1 = ruined) at or above which a rocket with a Hull Interlock upgrade",
                "refuses to launch. Without the interlock a worn rocket launches anyway and may fail.",
                "Default: 0.7"
        })
        @Configurable.DecimalRange(min = 0.1, max = 1.0)
        public double interlockWearThreshold = 0.7;

        @Configurable
        @Configurable.Comment({ "Scales how much wear each mission puts on the rocket.", "Default: 1.0" })
        @Configurable.DecimalRange(min = 0.0, max = 10.0)
        public double wearMultiplier = 1.0;

        @Configurable
        @Configurable.Comment({ "Missions a team can have in flight (or waiting to be collected) at once.",
                "Default: 8" })
        @Configurable.Range(min = 1, max = 64)
        public int maxMissionsPerTeam = 8;

        @Configurable
        @Configurable.Comment({
                "Real minutes one outpost production cycle takes. Each probe at an outpost rolls the body's",
                "yield table once per cycle.", "Default: 10"
        })
        @Configurable.DecimalRange(min = 0.05, max = 1440.0)
        public double outpostCycleMinutes = 10.0;

        @Configurable
        @Configurable.Comment({
                "How many finished cycles an outpost stockpiles before it stops producing (144 cycles of",
                "10 minutes is a day). A Haul mission empties it.", "Default: 144"
        })
        @Configurable.Range(min = 1, max = 100000)
        public int outpostMaxStoredCycles = 144;

        @Configurable
        @Configurable.Comment({
                "Chance that a successful survey turns up an anomaly: a surface cache of resources, a vein",
                "signature that maps another deposit, or (rarely) ancient ruins with a large cache.",
                "Default: 0.25"
        })
        @Configurable.DecimalRange(min = 0.0, max = 1.0)
        public double surveyAnomalyChance = 0.25;

        @Configurable
        @Configurable.Comment({
                "Chance that a mission has a random event on the way (solar flare, micrometeoroids, a tailwind,",
                "a derelict cache, a signal echo). At most one per mission, revealed when it lands. 0 turns events",
                "off. Default: 0.3"
        })
        @Configurable.DecimalRange(min = 0.0, max = 1.0)
        public double missionEventChance = 0.3;

        @Configurable
        @Configurable.Comment({
                "Chance, per finished production cycle, that something breaks an outpost. A broken outpost",
                "stops producing (and stops costing upkeep) until a repair run fixes it; what it had already",
                "stockpiled is kept. 0 turns incidents off. Default: 0.05 (about once per 3 hours of 10 min cycles)"
        })
        @Configurable.DecimalRange(min = 0.0, max = 1.0)
        public double outpostIncidentChance = 0.05;

        @Configurable
        @Configurable.Comment({
                "Repair kits a repair run uses up (they are taken from the launching player's inventory).",
                "Default: 1"
        })
        @Configurable.Range(min = 1, max = 16)
        public int outpostRepairKits = 1;

        @Configurable
        @Configurable.Comment({
                "Whether outposts need power. If true, every production cycle costs upkeep drawn from the owning",
                "team's Tesla Network; a cycle that cannot be paid for is lost, so an unpowered outpost idles.",
                "Default: true"
        })
        public boolean outpostRequiresPower = true;

        @Configurable
        @Configurable.Comment({
                "EU one probe costs per production cycle. With the default 10 minute cycle, 500,000 EU is about",
                "42 EU/t per probe (8 probes ~ 330 EU/t). 0 turns upkeep off. Default: 500000"
        })
        @Configurable.DecimalRange(min = 0.0, max = 1.0E12)
        public double outpostUpkeepEUPerProbe = 500_000.0;

        @Configurable
        @Configurable.Comment({ "The most probes one outpost can hold.", "Default: 8" })
        @Configurable.Range(min = 1, max = 64)
        public int maxProbesPerOutpost = 8;

        @Configurable
        @Configurable.Comment({
                "If true, missions can only be launched from a fully built Continuum Launch Complex. If false the plain",
                "Continuum Launch Pad block also works (it ignores tier and power) - meant for testing and creative.",
                "Default: true"
        })
        public boolean requireMultiblockPad = true;

        @Configurable
        @Configurable.Comment({
                "Energy a launch draws from the Launch Complex's input hatches, in amp-ticks of the pad's own voltage",
                "(1200 = 1 amp for a minute). A MV pad pays 128 * this in EU. Default: 1200"
        })
        @Configurable.Range(min = 0, max = 10000000)
        public int launchEnergyAmpTicks = 1200;

        @Configurable
        @Configurable.Comment({
                "The pad tier (GT voltage tier: 1 = LV, 2 = MV, 3 = HV, 4 = EV, 5 = IV, 6 = LuV, 7 = ZPM, 8 = UV)",
                "a body needs, by its gate.min_tier. Defaults: start MV, early HV, mid EV, late IV, endgame LuV."
        })
        @Configurable.Range(min = 0, max = 14)
        public int padTierStart = 2;

        @Configurable
        @Configurable.Range(min = 0, max = 14)
        public int padTierEarly = 3;

        @Configurable
        @Configurable.Range(min = 0, max = 14)
        public int padTierMid = 4;

        @Configurable
        @Configurable.Range(min = 0, max = 14)
        public int padTierLate = 5;

        @Configurable
        @Configurable.Range(min = 0, max = 14)
        public int padTierEndgame = 6;

        @Configurable
        @Configurable.Comment({ "The most extraction probes one mission can carry.", "Default: 16" })
        @Configurable.Range(min = 1, max = 64)
        public int maxProbesPerMission = 16;

        @Configurable
        @Configurable.Comment({ "How much wear one Repair Kit removes from a rocket (0.3 = 30%).", "Default: 0.3" })
        @Configurable.DecimalRange(min = 0.01, max = 1.0)
        public double repairKitWear = 0.3;

        @Configurable
        @Configurable.Comment({ "How close to a Launch Pad a player has to be to launch from it, in blocks.",
                "Default: 12" })
        @Configurable.Range(min = 2, max = 64)
        public int launchRangeBlocks = 12;
    }

    public static class CoreValues {

        @Configurable
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK I", "Default: 100" })
        public int mk1SlotValue = 100;

        @Configurable
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK II", "Default: 200" })
        public int mk2SlotValue = 200;

        @Configurable
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK III", "Default: 500" })
        public int mk3SlotValue = 500;
    }

    public static class VaultValues {

        @Configurable
        public PhoenixConfigs.VaultValues.BronzeVault bronzeVault = new PhoenixConfigs.VaultValues.BronzeVault();

        @Configurable
        public PhoenixConfigs.VaultValues.SteelVault steelVault = new PhoenixConfigs.VaultValues.SteelVault();

        @Configurable
        public PhoenixConfigs.VaultValues.TitaniumVault titaniumVault = new PhoenixConfigs.VaultValues.TitaniumVault();

        public static class BronzeVault {

            @Configurable
            @Configurable.Comment({ "Base number of item slots for the Large Bronze Vault", "Default: 36" })
            public int bronzeBaseSlots = 36;

            @Configurable
            @Configurable.Comment({ "Maximum number of interfaces for the Large Bronze Vault", "Default: 2" })
            public int bronzeInterfaceLimit = 2;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large  Vault", "Default: true" })
            public boolean bronzeWireless = true;
        }

        public static class SteelVault {

            @Configurable
            @Configurable.Comment({ "Base number of item slots for the Large Steel Vault", "Default: 72" })
            public int steelBaseSlots = 72;

            @Configurable
            @Configurable.Comment({ "Maximum number of interfaces for the Large  Vault", "Default: 4" })
            public int steelInterfaceLimit = 4;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large Steel Vault",
                    "Default: true" })
            public boolean steelWireless = true;
        }

        public static class TitaniumVault {

            @Configurable
            @Configurable.Comment({ "Base number of item slots for the Large Titanium Vault", "Default: 108" })
            public int titaniumBaseSlots = 108;

            @Configurable
            @Configurable.Comment({ "Maximum number of interfaces for the Large Titanium Vault", "Default: 8" })
            public int titaniumInterfaceLimit = 8;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large Titanium Vault",
                    "Default: true" })
            public boolean titaniumWireless = true;
        }
    }

    public static class WirelessTerminal {

        @Configurable
        @Configurable.Comment({ "Base distance in blocks that the wireless terminal can connect to a vault",
                "Default: 64" })
        public int connectionDistance = 64;

        @Configurable
        @Configurable.Comment({
                "Whether infinite range is enabled for the wireless terminal, also enables cross-dimension connection",
                "If true, connectionDistance will be ignored entirely", "Default: false" })
        public boolean infiniteRange = false;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the LV emitter", "Default: 1.5" })
        public double lvEmitterBonus = 1.5;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the MV emitter", "Default: 2.0" })
        public double mvEmitterBonus = 2.0;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the HV emitter", "Default: 2.5" })
        public double hvEmitterBonus = 2.5;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the EV emitter", "Default: 3.0" })
        public double evEmitterBonus = 3.0;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the IV emitter", "Default: 4.0" })
        public double ivEmitterBonus = 4.0;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the LuV emitter", "Default: 5.0" })
        public double luvEmitterBonus = 5.0;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the ZPM emitter", "Default: 6.0" })
        public double zpmEmitterBonus = 6.0;

        @Configurable
        @Configurable.Comment({ "The range multiplier applied by the UV emitter", "Default: 8.0" })
        public double uvEmitterBonus = 8.0;
    }

    public static int getSlotValue(CoreTier tier) {
        return switch (tier) {
            case MK1 -> INSTANCE.coreValues.mk1SlotValue;
            case MK2 -> INSTANCE.coreValues.mk2SlotValue;
            case MK3 -> INSTANCE.coreValues.mk3SlotValue;
        };
    }

    public static class SourceHatchConfig {

        @Configurable
        @Configurable.Comment({
                "The radius (in blocks) in which a Source Hatch will scan for nearby Source Jars to pull from." })
        public int sourceJarCheckRadius = 12;
    }

    public static class WingFlightConfigs {

        @Configurable
        @Configurable.Comment({
                "EU/t drained from the Tesla network during powered elytra/sonic flight.",
                "Speed and boost scale proportionally with this value.",
                "Default: 5000"
        })
        public long poweredFlightEUt = 5_000L;

        @Configurable
        @Configurable.Comment({
                "EU/t drained from the Tesla network during creative flight mode.",
                "Set to 0 for truly free creative flight.",
                "Fly speed scales proportionally with this value.",
                "Default: 1000"
        })
        public long creativeFlightEUt = 1_000L;

        @Configurable
        @Configurable.Comment({
                "Base boost scale for powered elytra flight at minimum speed setting.",
                "The actual boost = boostMin + (speedSlider * (boostMax - boostMin))",
                "Default: 0.01"
        })
        public double poweredBoostMin = 0.01;

        @Configurable
        @Configurable.Comment({
                "Max boost scale for powered elytra flight at maximum speed setting.",
                "Scales further with poweredFlightEUt so higher drain = faster top speed.",
                "Default: 0.16"
        })
        public double poweredBoostMax = 0.16;

        @Configurable
        @Configurable.Comment({
                "Min creative fly speed (at speed slider = 0).",
                "Default: 0.05"
        })
        public double creativeSpeedMin = 0.05;

        @Configurable
        @Configurable.Comment({
                "Max creative fly speed (at speed slider = 10).",
                "Scales further with creativeFlightEUt so higher drain = faster top speed.",
                "Default: 0.35"
        })
        public double creativeSpeedMax = 0.35;

        @Configurable
        @Configurable.Comment({
                "Horizontal speed, in blocks/tick, for plain \"Creative\" mode's free-strafing flight",
                "at speed slider = 5 (the slider's \"normal\" midpoint, range 0-20, matching vanilla",
                "creative-fly speed) - not creativeSpeedMin/Max's vanilla Abilities.flyingSpeed units,",
                "since vanilla's own flight accumulates well beyond that raw value tick over tick via",
                "friction, but free-strafing sets velocity directly with no such buildup, so it needs",
                "its own, much larger-looking value to reach an equivalent actual speed. Scales",
                "proportionally with the slider: 0 = stopped, 10 = 3.5x this, 20 = 7x this (raised from a",
                "4x ceiling - it capped out well below what \"creative flight\" should feel like at max).",
                "Default: 0.55 (matches vanilla creative-fly speed at slider = 5, the old flat default)"
        })
        public double creativeFreeSpeedBase = 0.55;

        @Configurable
        @Configurable.Comment({
                "Min speed clamp for powered flight, at speed slider = 0 (the SPEED slider now drives",
                "this cap - previously only the Drift slider affected it at all, so cranking Speed to max",
                "got you to the same ceiling faster without ever actually raising it).",
                "Default: 0.6"
        })
        public double poweredDriftMin = 0.6;

        @Configurable
        @Configurable.Comment({
                "Max speed clamp for powered flight, at speed slider = 20. Drift then applies on top of",
                "this as a loosening multiplier (up to +50% at drift slider = 10) - see poweredDriftMin.",
                "Default: 4.0"
        })
        public double poweredDriftMax = 4.0;

        @Configurable
        @Configurable.Comment({
                "Coasting half-life, in seconds, at drift slider = 0: velocity snaps to zero the instant",
                "you stop thrusting (full inertia canceling) as long as this is left at 0 - kept",
                "configurable only for consistency with coastHalfLifeMax, not meant to be changed.",
                "Default: 0.0"
        })
        public double coastHalfLifeMin = 0.0;

        @Configurable
        @Configurable.Comment({
                "Coasting half-life, in seconds, at drift slider = 9 (how long it takes to bleed off",
                "half your speed while airborne and not thrusting). Drift slider = 10 is still a hard",
                "special case with ZERO decay (matches real elytra exactly); slider 0-9 now scales this",
                "half-life linearly instead of a raw per-tick retention fraction, because a linear",
                "retention scale feels like instant death almost everywhere: even 0.7 retention/tick",
                "(70% \"kept\", which sounds generous) compounds down to under 0.1% of your speed within",
                "a single second, so nearly the entire slider used to feel identical to drift = 0.",
                "Default: 8.0"
        })
        public double coastHalfLifeMax = 8.0;

        @Configurable
        @Configurable.Comment({
                "Climb-speed multiplier for powered/sonic flight at vertical-speed slider = 5",
                "(the slider's \"normal\" midpoint, range 0-20) - matches the flat 8x climb boost",
                "this used to be hardcoded to, before the slider existed. Scales proportionally",
                "with the slider on both sides: 0 = none, 10 = double this, 20 = quadruple this.",
                "Default: 8.0"
        })
        public double poweredVerticalBase = 8.0;

        @Configurable
        @Configurable.Comment({
                "Min forward-accel boost for the leggings' sprint boost (at sprint-speed slider = 0).",
                "The actual boost = sprintAccelMin + ((sprintSpeed/20) * (sprintAccelMax - sprintAccelMin))",
                "Default: 0.03"
        })
        public double sprintAccelMin = 0.03;

        @Configurable
        @Configurable.Comment({
                "Max forward-accel boost for the leggings' sprint boost (at sprint-speed slider = 20).",
                "Default: 0.25 (slider = 5, this system's default, works out to the old hardcoded 0.085)"
        })
        public double sprintAccelMax = 0.25;

        @Configurable
        @Configurable.Comment({
                "Min upward-impulse strength for the boots' boosted jump (at jump-height slider = 0).",
                "The actual impulse = jumpHeightMin + ((jumpHeight/20) * (jumpHeightMax - jumpHeightMin))",
                "Default: 0.21"
        })
        public double jumpHeightMin = 0.21;

        @Configurable
        @Configurable.Comment({
                "Max upward-impulse strength for the boots' boosted jump (at jump-height slider = 20).",
                "Default: 0.65 (slider = 5, this system's default, works out to the old hardcoded 0.32)"
        })
        public double jumpHeightMax = 0.65;
    }

    public static class CleanroomConfig {

        @Configurable
        @Configurable.Comment({
                "Whether the cleanroom deals lethal damage to players when active and at max cleanliness." })
        public boolean lethal = true;

        @Configurable
        @Configurable.Comment({ "The maximum cleanliness level of the cleanroom." })
        public int maxCleanliness = 1000;

        @Configurable
        @Configurable.Comment({ "The amount of pollution each player adds per tick inside the cleanroom." })
        public int playerPollution = 5;

        @Configurable
        @Configurable.Comment({ "The amount of cleanliness regenerated per tick when no players are inside." })
        public int regenRate = 1;

        @Configurable
        @Configurable.Comment({ "The amount of sterilizing gas consumed per tick." })
        public int fluidConsumption = 1;
    }

    public static class FeatureConfigs {

        @Configurable
        @Configurable.Comment("The maximum Prismatic Paint capacity of the Chameleon Spray Can (in mB).")
        public int chameleonSprayCanCapacity = 8000;

        @Configurable
        @Configurable.Comment("The amount of Prismatic Paint consumed per block/entity recolored (in mB).")
        public int chameleonSprayCanCostPerOperation = 50;

        @Configurable
        @Configurable.Comment("The fluid consumption multiplier applied when chain-painting/bulk-painting blocks (e.g. 0.85 equals a 15% discount). Set to 1.0 to disable discounts.")
        public double chameleonSprayCanBulkMultiplier = 0.85;

        @Configurable.Comment({ "Whether the ME Tag Input Bus and Hatch are enabled" })
        public boolean tagInputsEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Creative Energy Multiblock is enabled" })
        public boolean creativeEnergyEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Blazing Maintenance Hatch is enabled" })
        public boolean blazingHatchEnabled = true;

        @Configurable
        @Configurable.Comment({
                "Whether the Blazing Cleanroom is enabled (This just disables the casings, you can have the hatch on with this off just fine)" })
        public boolean blazingCleanroomEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Custom HPCA components are enabled" })
        public boolean HPCAComponetsEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether the Custom Phoenix HPCA multiblock is enabled" })
        public boolean PHPCAEnabled = true;

        @Configurable
        @Configurable.Comment({ "Whether recipes for the machines are enabled" })
        public boolean recipesEnabled = true;

        @Configurable
        @Configurable.Comment({ "How powerful the normal Phoenix Computation Unit is (CWU/t)" })
        public int BasicPCUStrength = 32;

        @Configurable
        @Configurable.Comment({ "How powerful the Advanced Phoenix Computation Unit is (CWU/t)" })
        public int PCUStrength = 64;

        @Configurable
        @Configurable.Comment({ "How much coolant the basic Phoenix Computation Unit uses" })
        public int BasicPCUCoolantUsed = 4;

        @Configurable
        @Configurable.Comment({ "How much coolant the Advanced Phoenix Computation Unit uses" })
        public int PCUCoolantUsed = 8;

        @Configurable
        @Configurable.Comment({ "How powerful the normal Phoenix Computation Unit is (CWU/t) when damaged" })
        public int damagedBasicPCUStrength = 16;

        @Configurable
        @Configurable.Comment({ "How powerful the advanced Phoenix Computation Unit is (CWU/t) when damaged" })
        public int damagedPCUStrength = 32;

        @Configurable
        @Configurable.Comment({
                "How much EU the normal Phoenix Computation uses per tick while not providing CWU/t (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int basicPCUEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the normal Phoenix Computation can use at max (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int basicPCUMaxEUt = 10;

        @Configurable
        @Configurable.Comment({
                "How much EU the advanced Phoenix Computation uses per tick while not providing CWU/t (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int PCUEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the advanced Phoenix Computation can use at max (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int PCUMaxEUt = 10;

        @Configurable
        @Configurable.Comment({ "How powerful the Phoenix Heat Sink is (Cooling Provided)" })
        public int HeatSinkStrength = 4;

        @Configurable
        @Configurable.Comment({ "How powerful the Phoenix Active Cooler is (Cooling Provided)" })
        public int ActiveCoolerStrength = 8;

        @Configurable
        @Configurable.Comment({
                "How much EU the Phoenix Heat Sink uses per tick (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int HeatSinkEutUpkeep = 0;

        @Configurable
        @Configurable.Comment({
                "How much EU the Active Phoenix Cooler uses per tick (Goes off GTValues, ULV is 0, LV is 1, MV is 2, etc)" })
        public int ActiveCoolerEutUpkeep = 8;

        @Configurable
        @Configurable.Comment({ "How much coolant the Active Phoenix Cooler can use at max in milibuckets" })
        public int ActiveCoolerCoolantUse = 10;

        @Configurable
        @Configurable.Comment({
                "What Base Coolant the Active Phoenix Cooler uses while in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolantBase = "copper";

        @Configurable
        @Configurable.Comment({
                "What Stronger Coolant the Active Phoenix Cooler uses while in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant1 = "pcb_coolant";

        @Configurable
        @Configurable.Comment({
                "What Strongest Coolant the Active Phoenix Cooler uses when in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant2 = "sodium_potassium";

        @Configurable
        @Configurable.Comment({ "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
        public double BaseCoolantBoost = 1.0;

        @Configurable
        @Configurable.Comment({ "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
        public double CoolantBoost1 = 1.1;

        @Configurable
        @Configurable.Comment({
                "What Strongest Coolant the Active Phoenix Cooler uses when in the PHPCA (Gt or GT Kubejs Material)" })
        public double CoolantBoost2 = 1.2;

        @Configurable
        @Configurable.Comment({ "Whether the Tech Suite's on-screen HUD (flight mode, tuning sliders, network",
                "status, rebirth cooldown, energy bar) is drawn at all. Set to false to hide it entirely." })
        public boolean techSuiteHUDEnabled = true;

        @Configurable
        @Configurable.Comment({
                "The connection mode for Tesla Towers.",
                "TEAM_AUTO: All towers under a team/player share the same cloud automatically.",
                "DATA_STICK: Towers must be manually linked to hatches using a Data Stick."
        })
        public TeslaConnectionMode teslaConnectionMode = TeslaConnectionMode.DATA_STICK;

        public enum TeslaConnectionMode {
            TEAM_AUTO,
            DATA_STICK
        }
    }

    @Configurable
    @Configurable.Comment({ "Config options for OmniPacks base values" })
    public PhoenixConfigs.PackValueConfigs OmniPackBaseValues = new PhoenixConfigs.PackValueConfigs();
    @Configurable
    @Configurable.Comment({ "Config options for Upgrade Modules" })
    public PhoenixConfigs.UpgradeConfigs ModuleValues = new PhoenixConfigs.UpgradeConfigs();

    public static class PackValueConfigs {

        @Configurable
        @Configurable.Comment({ "Basic OmniPack Base Values" })
        public PhoenixConfigs.PackValueConfigs.BasicPack basicPack = new PhoenixConfigs.PackValueConfigs.BasicPack();
        @Configurable
        @Configurable.Comment({ "Advanced OmniPack Base Values" })
        public PhoenixConfigs.PackValueConfigs.AdvancedPack advancedPack = new PhoenixConfigs.PackValueConfigs.AdvancedPack();
        @Configurable
        @Configurable.Comment({ "Elite OmniPack Base Values" })
        public PhoenixConfigs.PackValueConfigs.ElitePack elitePack = new PhoenixConfigs.PackValueConfigs.ElitePack();

        public static class BasicPack {

            @Configurable
            @Configurable.Comment({ "Base number of slots for the Basic OmniPack", "Default: 27" })
            public int basicPackItemSlots = 27;
            @Configurable
            @Configurable.Comment({ "Base fluid capacity for the Basic OmniPack in millibuckets", "Default: 32000" })
            public int basicPackFluidStorage = 32_000;
            @Configurable
            @Configurable.Comment({ "Base EU capacity for the Basic OmniPack", "Default: 200000" })
            public int basicPackEUStorage = 200_000;
            @Configurable
            @Configurable.Comment({ "Number of upgrade slots on the Basic OmniPack", "Default: 6" })
            public int basicPackUpgradeSlots = 6;
        }

        public static class AdvancedPack {

            @Configurable
            @Configurable.Comment({ "Base number of slots for the Advanced OmniPack", "Default: 45" })
            public int advancedPackItemSlots = 45;
            @Configurable
            @Configurable.Comment({ "Base fluid capacity for the Advanced OmniPack in millibuckets",
                    "Default: 128000" })
            public int advancedPackFluidStorage = 128_000;
            @Configurable
            @Configurable.Comment({ "Base EU capacity for the Advanced OmniPack", "Default: 1000000" })
            public int advancedPackEUStorage = 1_000_000;
            @Configurable
            @Configurable.Comment({ "Number of upgrade slots on the Advanced OmniPack", "Default: 10" })
            public int advancedPackUpgradeSlots = 10;
        }

        public static class ElitePack {

            @Configurable
            @Configurable.Comment({ "Base number of slots for the Elite OmniPack", "Default: 90" })
            public int elitePackItemSlots = 90;
            @Configurable
            @Configurable.Comment({ "Base fluid capacity for the Elite OmniPack in millibuckets", "Default: 512000" })
            public int elitePackFluidStorage = 512_000;
            @Configurable
            @Configurable.Comment({ "Base EU capacity for the Elite OmniPack", "Default: 200000000" })
            public int elitePackEUStorage = 200_000_000;
            @Configurable
            @Configurable.Comment({ "Number of upgrade slots on the Elite OmniPack", "Default: 16" })
            public int elitePackUpgradeSlots = 16;
        }
    }

    public static class UpgradeConfigs {

        @Configurable
        @Configurable.Comment("Number of slots added by the Item Capacity Module I")
        public int itemModule1Bonus = 9;
        @Configurable
        @Configurable.Comment("Number of slots added by the Item Capacity Module II")
        public int itemModule2Bonus = 18;
        @Configurable
        @Configurable.Comment("Number of slots added by the Item Capacity Module III")
        public int itemModule3Bonus = 27;
        @Configurable
        @Configurable.Comment("Tank multiplier for the Fluid Capacity Module I")
        public double fluidModule1Bonus = 2.00;
        @Configurable
        @Configurable.Comment("Tank multiplier for the Fluid Capacity Module II")
        public double fluidModule2Bonus = 4.00;
        @Configurable
        @Configurable.Comment("Tank multiplier for the Fluid Capacity Module III")
        public double fluidModule3Bonus = 8.00;
        @Configurable
        @Configurable.Comment("EU capacity multiplier for the Energy Capacity Module I")
        public double energyModule1Bonus = 1.25;
        @Configurable
        @Configurable.Comment("EU capacity multiplier for the Energy Capacity Module II")
        public double energyModule2Bonus = 1.50;
        @Configurable
        @Configurable.Comment("EU capacity multiplier for the Energy Capacity Module III")
        public double energyModule3Bonus = 2.00;
        @Configurable
        @Configurable.Comment("EU per tick cost of the Jetpack Module I")
        public int jetpackModule1EUCost = 90;
        @Configurable
        @Configurable.Comment("EU per tick cost of the Jetpack Module II")
        public int jetpackModule2EUCost = 30;
        @Configurable
        @Configurable.Comment("Pickup radius in blocks of the Magnet Module I")
        public int magnetModule1Radius = 5;
        @Configurable
        @Configurable.Comment("Pickup radius in blocks of the Magnet Module II")
        public int magnetModule2Radius = 10;
    }
}
