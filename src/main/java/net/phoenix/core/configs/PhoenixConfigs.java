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
                "Default: 0.09"
        })
        public double poweredBoostMax = 0.09;

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
                "Default: 0.2"
        })
        public double creativeSpeedMax = 0.20;

        @Configurable
        @Configurable.Comment({
                "Min horizontal speed, in blocks/tick, for plain \"Creative\" mode's free-strafing",
                "flight (at speed slider = 0). This is a direct velocity, not creativeSpeedMin/Max's",
                "vanilla Abilities.flyingSpeed units - vanilla's own flight accumulates well beyond",
                "that value tick over tick via friction, but free-strafing sets velocity directly with",
                "no such buildup, so it needed its own, much larger-looking range to feel equivalent.",
                "Default: 0.2"
        })
        public double creativeFreeSpeedMin = 0.2;

        @Configurable
        @Configurable.Comment({
                "Max horizontal speed, in blocks/tick, for plain \"Creative\" mode's free-strafing",
                "flight (at speed slider = 10). See creativeFreeSpeedMin.",
                "Default: 1.6"
        })
        public double creativeFreeSpeedMax = 1.6;

        @Configurable
        @Configurable.Comment({
                "Min speed clamp for powered flight (at drift slider = 0, tightest handling).",
                "Default: 0.6"
        })
        public double poweredDriftMin = 0.6;

        @Configurable
        @Configurable.Comment({
                "Max speed clamp for powered flight (at drift slider = 10, loosest/floatiest).",
                "Default: 1.8"
        })
        public double poweredDriftMax = 1.8;

        @Configurable
        @Configurable.Comment({
                "How much horizontal momentum survives each tick you're airborne in a wing flight",
                "mode but NOT actively thrusting (not sprinting/sneaking-boosting), as a fraction",
                "kept per tick - at drift slider = 0 this is 0.0 (velocity snaps to zero the instant",
                "you stop thrusting, i.e. full inertia canceling); at drift slider = 10 it's 1.0",
                "(no damping at all, momentum carries over exactly like normal elytra gliding).",
                "The actual retention = driftSlider/10, this constant only exists so the mapping is",
                "documented in one place rather than a bare 0.0/1.0 buried in code.",
                "Default: 0.0"
        })
        public double coastRetentionMin = 0.0;

        @Configurable
        @Configurable.Comment({
                "See coastRetentionMin - the retention fraction at drift slider = 10.",
                "Default: 1.0"
        })
        public double coastRetentionMax = 1.0;

        @Configurable
        @Configurable.Comment({
                "Climb-speed multiplier for powered/sonic flight at vertical-speed slider = 5",
                "(the slider's \"normal\" midpoint, range 0-20) - matches the flat 8x climb boost",
                "this used to be hardcoded to, before the slider existed. Scales proportionally",
                "with the slider on both sides: 0 = none, 10 = double this, 20 = quadruple this.",
                "Default: 8.0"
        })
        public double poweredVerticalBase = 8.0;
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
