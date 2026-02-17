package net.phoenix.core.configs;

import net.phoenix.core.PhoenixCore;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = PhoenixCore.MOD_ID)
public class PhoenixConfigs {

    public static PhoenixConfigs INSTANCE;

    public static ConfigHolder<PhoenixConfigs> CONFIG_HOLDER;
    public static FeatureConfigs.TeslaConnectionMode TeslaConnectionMode;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(PhoenixConfigs.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public FeatureConfigs features = new FeatureConfigs();

    @Configurable
    public CleanroomConfig cleanroom = new CleanroomConfig();

    @Configurable
    public SourceHatchConfig sourceHatch = new SourceHatchConfig();

    public static class SourceHatchConfig {

        @Configurable
        @Configurable.Comment({
                "The radius (in blocks) in which a Source Hatch will scan for nearby Source Jars to pull from." })
        public int sourceJarCheckRadius = 12;
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

        // Features
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
        @Configurable.Comment({
                "Whether the Custom HPCA componets are enabled" })
        public boolean HPCAComponetsEnabled = true;
        @Configurable
        @Configurable.Comment({
                "Whether the Custom Phoenix HPCA multiblock is enabled" })
        public boolean PHPCAEnabled = true;
        @Configurable
        @Configurable.Comment({ "Whether recipes for the machines are enabled" })
        public boolean recipesEnabled = true;

        // COMPUTATION

        @Configurable
        @Configurable.Comment({ "How powerful the normal Phoenix Computation Unit is (CWU/t)" })
        public int BasicPCUStrength = 32;
        @Configurable
        @Configurable.Comment({ "How powerful the Advanced Phoenix Computation Unit is (CWU/t)" })
        public int PCUStrength = 64;
        @Configurable
        @Configurable.Comment({
                "How much coolant the basic Phoenix Computation Unit uses" })
        public int BasicPCUCoolantUsed = 4;
        @Configurable
        @Configurable.Comment({
                "How much coolant the Advanced Phoenix Computation Unit uses" })
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

        // COOLING
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
                "What Stronger Coolant the Active Phoenix Cooler uses while in the PHPCA  (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant1 = "pcb_coolant";
        @Configurable
        @Configurable.Comment({
                "What Strongest Coolant the Active Phoenix Cooler uses when in the PHPCA (Gt or GT Kubejs Material)" })
        public String ActiveCoolerCoolant2 = "sodium_potassium";
        @Configurable
        @Configurable.Comment({
                "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
        public double BaseCoolantBoost = 1.0;
        @Configurable
        @Configurable.Comment({
                "How much ActiveCoolerCoolant1 boosts base CWU/t ()" })
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

        public TeslaConnectionMode teslaConnectionMode = FeatureConfigs.TeslaConnectionMode.DATA_STICK;

        public enum TeslaConnectionMode {
            TEAM_AUTO,
            DATA_STICK
        }
    }
}
