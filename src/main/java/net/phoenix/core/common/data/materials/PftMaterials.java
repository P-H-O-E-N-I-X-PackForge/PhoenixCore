package net.phoenix.core.common.data.materials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.MaterialBuilder;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;

public final class PftMaterials {

    private PftMaterials() {}

    private static void mat(String id, java.util.function.Consumer<MaterialBuilder> configure) {
        MaterialBuilder builder = new MaterialBuilder(net.phoenix.core.PhoenixCore.id(id));
        configure.accept(builder);
        builder.createMaterial();
    }

    public static void register() {
        quantumBinders();
        stabilizedAntimatter();
        phoenixEnrichedNeutronium();
        plasmas();
        fluids();
    }

    public static void modify() {
        GTMaterials.RutheniumTriniumAmericiumNeutronate.get().addFlags(GENERATE_FINE_WIRE);
        GTMaterials.Neutronium.get().addFlags(GENERATE_LONG_ROD, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR,
                GENERATE_SMALL_GEAR, GENERATE_BOLT_SCREW, GENERATE_DENSE);

        GTMaterials.HSLASteel.get().addFlags(GENERATE_SPRING, GENERATE_LONG_ROD);
        GTMaterials.Iridium.get().addFlags(GENERATE_FRAME);
        GTMaterials.Cobalt.get().addFlags(GENERATE_FINE_WIRE);

        for (var gem : new com.gregtechceu.gtceu.api.data.chemical.material.Material[] { GTMaterials.Diamond.get(),
                GTMaterials.Emerald.get(), GTMaterials.Ruby.get(), GTMaterials.Sapphire.get(),
                GTMaterials.GreenSapphire.get(), GTMaterials.Amethyst.get(), GTMaterials.Topaz.get(),
                GTMaterials.BlueTopaz.get(), GTMaterials.Opal.get() }) {
            gem.addFlags(PhoenixMaterialFlags.GENERATE_GEM_CUTS);
        }
    }

    private static void quantumBinders() {
        mat("crude_quantum_binder_dust", b -> b.dust().fluid().color(0xe47815)
                .components(GTMaterials.Calcium, 1, GTMaterials.Chlorine, 2, GTMaterials.Oxygen, 8)
                .flags(DISABLE_DECOMPOSITION));
        mat("advanced_quantum_binder_dust", b -> b.dust().fluid().color(0xFF0000)
                .components(GTMaterials.Calcium, 1, GTMaterials.Chlorine, 2)
                .flags(DISABLE_DECOMPOSITION));
        mat("super_advanced_quantum_binder_dust", b -> b.dust().fluid().color(0x8F00FF)
                .components(GTMaterials.Calcium, 1)
                .flags(DISABLE_DECOMPOSITION));
        MaterialLang.put("crude_quantum_binder_dust", "Crude Quantum Binder");
        MaterialLang.put("advanced_quantum_binder_dust", "Advanced Quantum Binder");
        MaterialLang.put("super_advanced_quantum_binder_dust", "Super Advanced Quantum Binder");
    }

    private static void stabilizedAntimatter() {
        mat("stabilized_antimatter", b -> b.ingot().fluid()
                .element(PhoenixElements.STABILIZED_ANTIMATTER)
                .color(0x000000).secondaryColor(0xFFFFFF)
                .iconSet(PhoenixMaterialSet.STABILIZED_ANTIMATTER_HOLDER)
                .cableProperties(GTValues.V[GTValues.MAX], 8192, 1024, true)
                .rotorStats(2800, 700, 30.0f, 2500000)
                .flags(GENERATE_PLATE, GENERATE_LONG_ROD, GENERATE_RING, GENERATE_ROUND, GENERATE_GEAR,
                        GENERATE_SMALL_GEAR, GENERATE_SPRING, PHOSPHORESCENT, GENERATE_ROD, GENERATE_BOLT_SCREW,
                        GENERATE_FRAME, GENERATE_DENSE, GENERATE_FINE_WIRE, GENERATE_FOIL, GENERATE_ROTOR));
        MaterialLang.put("stabilized_antimatter", "Stabilized Antimatter");
    }

    private static void phoenixEnrichedNeutronium() {
        mat("phoenix_enriched_neutronium", b -> b.ingot().fluid()
                .element(PhoenixElements.PHOENIX_ENRICHED_NEUTRONIUM)
                .color(0xffffff)
                .blastTemp(6800, BlastProperty.GasTier.HIGHEST)
                .iconSet(PhoenixMaterialSet.PHOENIX_ENRICHED_NEUTRONIUM_HOLDER)
                .flags(GENERATE_PLATE, GENERATE_FOIL, GENERATE_ROD, GENERATE_FRAME, GENERATE_DENSE));
        MaterialLang.put("phoenix_enriched_neutronium", "Phoenix Enriched Neutronium");
    }

    private static void plasma(String id, int color, int secondary) {
        mat(id, b -> b.plasma().color(color).secondaryColor(secondary));
        MaterialLang.put(id, title(id));
    }

    private static void plasmas() {
        plasma("duck_plasma", 0xFFA500, 0xA9423F);
        plasma("superheated_plasma", 0xFFA500, 0xA9423F);
        plasma("dormant_phoenix_based_plasma", 0xcd8c19, 0xA9423F);
        plasma("quantum_stable_plasma", 0xd8d6d6, 0xA9423F);
        plasma("true_phoenix_based_plasma", 0x8d00fc, 0x00FFFF);
        plasma("active_phoenix_based_plasma", 0xfc4f4f, 0xA9423F);
        plasma("singularity_reactive_plasma", 0x5500AA, 0xFF00FF);
        plasma("chrono_stabilized_plasma", 0xADD8E6, 0x4682B4);
    }

    private static void fluid(String id, boolean thin, int color, int bucket, String name) {
        mat(id, b -> b.fluid().color(color).secondaryColor(bucket)
                .iconSet(MaterialIconSet.DULL).flags(DISABLE_DECOMPOSITION));
        MaterialLang.put(id, name);
    }

    private static void fluids() {
        fluid("chaos", false, 0x000000, 0x1E1E1E, "Chaos");
        fluid("grass_water", true, 0x00ff11, 0x00ff11, "Grass Water");
        fluid("dimensional_flux_resin", false, 0x8A2BE2, 0x9400D3, "Dimensional Flux Resin");
        fluid("plasma_stabilized_mercury", false, 0xE0FFFF, 0x808080, "Plasma-Stabilized Mercury");
        fluid("aetheric_conductive_gel", false, 0xFFFF00, 0xF0E68C, "Aetheric Conductive Gel");
        fluid("void_reactive_coolant", false, 0x000000, 0x1E1E1E, "Void-Reactive Coolant");
        fluid("quantum_adhesive_solution", false, 0x00FFFF, 0xAFEEEE, "Quantum Adhesive Solution");
        fluid("multi_phase_dimensional_gel", false, 0xFFA07A, 0xFF7F50, "Multi-Phase Dimensional Gel");
        fluid("neutrino_laced_superfluid", true, 0x4682B4, 0x6495ED, "Neutrino-Laced Superfluid");
        fluid("exo_temporal_stabilizer", false, 0x3CB371, 0x2E8B57, "Exo-Temporal Stabilizer");
        fluid("hypercharged_void_extract", false, 0xFFD700, 0xFFA500, "Hypercharged Void Extract");
        fluid("reality_woven_nanotubes", false, 0x800080, 0x9400D3, "Reality-Woven Nanotubes");
        fluid("phoenix_fire_solution", false, 0xFF4500, 0xFF8C00, "Phoenix Fire Solution");
        fluid("eldritch_catalytic_mist", true, 0x483D8B, 0x6A5ACD, "Eldritch Catalytic Mist");
        fluid("quantum_locked_fluid", false, 0x00BFFF, 0x1E90FF, "Quantum-Locked Fluid");
        fluid("void_charged_ionic_gel", false, 0x40E0D0, 0x00CED1, "Void-Charged Ionic Gel");
        fluid("glitched_anomalous_resin", false, 0xFF69B4, 0xFF1493, "Glitched Anomalous Resin");
        fluid("entropy_diffusion_mist", true, 0x778899, 0x696969, "Entropy-Diffusion Mist");
        fluid("dark_matter_suspension", false, 0x4B0082, 0x191970, "Dark Matter Suspension");
        fluid("reality_fractured_isotope", false, 0xDC143C, 0xFF6347, "Reality-Fractured Isotope");
        fluid("cosmic_flux_liquid", false, 0x008080, 0x008B8B, "Cosmic Flux Liquid");
        fluid("stellar_compression_dust", true, 0xF5F5DC, 0xFFFFE0, "Stellar Compression Dust");
        fluid("graviton_dense_slurry", false, 0x2F4F4F, 0x000080, "Graviton-Dense Slurry");
        fluid("event_horizon_condensate", false, 0x000000, 0x4F4F4F, "Event Horizon Condensate");
        fluid("quantum_star_infusion", false, 0xFFFFE0, 0xFFDAB9, "Quantum-Star Infusion");
        fluid("eon_fused_catalyst", false, 0xB8860B, 0xD2B48C, "Eon-Fused Catalyst");
        fluid("singularity_condensed_matter", false, 0x000000, 0x1E1E1E, "Singularity-Condensed Matter");
        fluid("primordial_chaos_fluid", false, 0x800000, 0xA52A2A, "Primordial Chaos Fluid");
        fluid("multiversal_binding_agent", false, 0xDA70D6, 0xFF00FF, "Multiversal Binding Agent");
        fluid("absolute_zero_etherium", false, 0xAFEEEE, 0xE0FFFF, "Absolute-Zero Etherium");
        fluid("cognitive_dissonance_compound", false, 0x8B008B, 0x4B0082, "Cognitive Dissonance Compound");
        fluid("memory_scrambler_toxin", true, 0xFF8C00, 0xFFA07A, "Memory Scrambler Toxin");
        fluid("emotional_override_agent", false, 0xFF0000, 0x8B0000, "Emotional Override Agent");
        fluid("sensory_distortion_fluid", false, 0x00FFFF, 0x8FBC8F, "Sensory Distortion Fluid");
        fluid("neural_decay_catalyst", true, 0x696969, 0x000000, "Neural Decay Catalyst");
        fluid("bio_electric_discharge_amplifier", false, 0xFFFF00, 0xF0E68C, "Bio-Electric Discharge Amplifier");
        fluid("synaptic_corrosion_agent", true, 0xA0522D, 0x8B4513, "Synaptic Corrosion Agent");
        fluid("wetware_integration_disruptor", false, 0xDC143C, 0xFF6347, "Wetware Integration Disruptor");
        fluid("nanite_aggregation_catalyst", true, 0x708090, 0x4682B4, "Nanite Aggregation Catalyst");
        fluid("accelerated_cellular_degenerate", false, 0x808000, 0x556B2F, "Accelerated Cellular Degenerate");
        fluid("uncontrolled_growth_stimulant", false, 0x00FF00, 0x3CB371, "Uncontrolled Growth Stimulant");
        fluid("genetic_corruption_fluid", false, 0xDA70D6, 0x9932CC, "Genetic Corruption Fluid");
        fluid("chimeric_integration_fluid_corrupted", false, 0xFFD700, 0xB8860B,
                "Chimeric Integration Fluid (Corrupted)");
        fluid("neural_binding_compound", false, 0x0000FF, 0x4169E1, "Neural Binding Compound");
        fluid("behavioral_imprinting_solution", true, 0xFF69B4, 0xFF1493, "Behavioral Imprinting Solution");
        fluid("pain_amplification_serum", true, 0xFF4500, 0x8B0000, "Pain Amplification Serum");
        fluid("bio_iridescent_decay", true, 0x40E0D0, 0x00CED1, "Bio-Iridescent Decay");
        fluid("crystalline_proliferation", true, 0xE0FFFF, 0xAFEEEE, "Crystalline Proliferation");
        fluid("pulsating_organic_slime", false, 0xFA8072, 0xE9967A, "Pulsating Organic Slime");
        fluid("entangled_state_conduit_fluid", true, 0x00FFFF, 0xADD8E6, "Entangled State Conduit Fluid");
        fluid("cryogenic_processing_gel", false, 0xADD8E6, 0x87CEEB, "Cryogenic Processing Gel");
        fluid("adaptive_logic_matrix_fluid", true, 0x90EE90, 0x98FB98, "Adaptive Logic Matrix Fluid");
        fluid("resonant_frequency_amplifier_fluid", true, 0xFFA07A, 0xFFDAB9, "Resonant Frequency Amplifier Fluid");
        fluid("cognitive_interface_alloy_fluid", true, 0xD3D3D3, 0xC0C0C0, "Cognitive Interface Alloy Fluid");

        fluid("akashic_resonance_stream", true, 0xFFD700, 0xAFEEEE, "Akashic Resonance Stream");
        fluid("zeronium_temporal_essence", false, 0xC0C0C0, 0x808080, "Zeronium Temporal Essence");
        fluid("multiversal_singularity_catalyst", false, 0x191970, 0x4B0082, "Multiversal Singularity Catalyst");
        fluid("chronarium_infusion_fluid", true, 0xADD8E6, 0x87CEEB, "Chronarium Infusion Fluid");
        fluid("ethereal_knowledge_nexus", true, 0xF0FFFF, 0xE0FFFF, "Ethereal Knowledge Nexus");
        fluid("fading_ember_residue", false, 0xFF8C00, 0x8B4513, "Fading Ember Residue");
        fluid("true_heat_conductor_unstable", true, 0xFFFFFF, 0xFFD700, "True Heat Conductor (Unstable)");
        fluid("phoenix_ash_suspension", false, 0x696969, 0xFF4500, "Phoenix Ash Suspension");
        fluid("infernal_overload_emulsion", false, 0x8B0000, 0xFF6347, "Infernal Overload Emulsion");
        fluid("scorched_matrix_fluid", false, 0x36454F, 0xFFA07A, "Scorched Matrix Fluid");
        fluid("phase_instability_fluid", true, 0xCD5C5C, 0xBC8F8F, "Phase Instability Fluid");
        fluid("echoes_of_the_past_resonance_fluid", true, 0xC0C0C0, 0xD3D3D3, "Echoes of the Past Resonance Fluid");
        fluid("multi_spectral_sensor_array_fluid", true, 0x228B22, 0x3CB371, "Multi-Spectral Sensor Array Fluid");
        fluid("information_processor_fluid_sensory", true, 0x00BFFF, 0x1E90FF, "Information Processor Fluid (Sensory)");
        fluid("adaptive_camouflage_coating_tech", true, 0x8FBC8F, 0xA9A9A9, "Adaptive Camouflage Coating (Tech)");
        fluid("directional_energy_receiver_fluid", true, 0xF0E68C, 0xFFD700, "Directional Energy Receiver Fluid");
        fluid("neural_interface_emulator_digital_fluid", true, 0x6A5ACD, 0x8A2BE2,
                "Neural Interface Emulator (Digital Fluid)");
    }

    private static String title(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.split("_")) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
