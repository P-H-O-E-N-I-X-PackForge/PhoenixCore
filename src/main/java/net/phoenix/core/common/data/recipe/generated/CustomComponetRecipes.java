package net.phoenix.core.common.data.recipe.generated;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.registrate.entry.MaterialRegistryEntry;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.materials.PhoenixProgressionMaterials;
import net.phoenix.core.common.machine.PhoenixMachines;

import java.util.Map;

import static com.gregtechceu.gtceu.api.GTValues.*;

public class CustomComponetRecipes {

    private static final int[] VA = GTValues.VA;

    private static final Map<Integer, MaterialRegistryEntry> MATERIAL_MAP = Map.of(
            LV, PhoenixProgressionMaterials.AURUM_STEEL,
            MV, PhoenixProgressionMaterials.ALUMINFROST,
            HV, PhoenixProgressionMaterials.FROST_REINFORCED_STAINED_STEEL,
            EV, PhoenixProgressionMaterials.SOURCE_IMBUED_TITANIUM,
            IV, PhoenixProgressionMaterials.VOID_TOUCHED_TUNGSTEN_STEEL,
            LuV, PhoenixProgressionMaterials.RESONANT_RHODIUM_ALLOY,
            ZPM, PhoenixProgressionMaterials.ADVANCED_QUIN_NAQUADIAN_ALLOY);

    private static ItemStack get(TagPrefix prefix, int tier) {
        MaterialRegistryEntry mat = MATERIAL_MAP.getOrDefault(tier, PhoenixProgressionMaterials.AURUM_STEEL);
        return ChemicalHelper.get(prefix, mat != null ? mat.get() : null);
    }

    private static final Map<Integer, TagKey<Item>> CIRCUIT_MAP = Map.of(
            LV, CustomTags.LV_CIRCUITS, MV, CustomTags.MV_CIRCUITS, HV, CustomTags.HV_CIRCUITS,
            EV, CustomTags.EV_CIRCUITS, IV, CustomTags.IV_CIRCUITS, LuV, CustomTags.LuV_CIRCUITS,
            ZPM, CustomTags.ZPM_CIRCUITS, UV, CustomTags.UV_CIRCUITS, UHV, CustomTags.UHV_CIRCUITS);

    public static ItemStack getFoilForTier(int tier) {
        return get(TagPrefix.foil.get(), tier);
    }

    public static ItemStack getPlateForTier(int tier) {
        return get(TagPrefix.plate.get(), tier);
    }

    public static ItemStack getDensePlateForTier(int tier) {
        return get(TagPrefix.plateDense.get(), tier);
    }

    public static ItemStack getSmallGearForTier(int tier) {
        return get(TagPrefix.gearSmall.get(), tier);
    }

    public static ItemStack getGearForTier(int tier) {
        return get(TagPrefix.gear.get(), tier);
    }

    public static ItemStack getRodForTier(int tier) {
        return get(TagPrefix.rod.get(), tier);
    }

    public static ItemStack getLongRodForTier(int tier) {
        return get(TagPrefix.rodLong.get(), tier);
    }

    public static ItemStack getFrameForTier(int tier) {
        return get(TagPrefix.frameGt.get(), tier);
    }

    public static ItemStack getQuadWireForTier(int tier) {
        return get(TagPrefix.wireGtQuadruple.get(), tier);
    }

    public static ItemStack getQuadCableForTier(int tier) {
        return get(TagPrefix.cableGtQuadruple.get(), tier);
    }

    public static ItemStack getOctalWireForTier(int tier) {
        return get(TagPrefix.wireGtOctal.get(), tier);
    }

    public static ItemStack getOctalCableForTier(int tier) {
        return get(TagPrefix.cableGtOctal.get(), tier);
    }

    public static ItemStack getHexWireForTier(int tier) {
        return get(TagPrefix.wireGtHex.get(), tier);
    }

    public static TagKey<Item> getCircuitForTier(int tier) {
        return CIRCUIT_MAP.getOrDefault(tier, CustomTags.LV_CIRCUITS);
    }

    public static ItemStack getCrateForTier(int tier) {
        return getMachineStack(tier, "CRATE");
    }

    public static ItemStack getDrumForTier(int tier) {
        return getMachineStack(tier, "DRUM");
    }

    private static ItemStack getMachineStack(int tier, String type) {
        return switch (tier) {
            case LV -> type.equals("CRATE") ? PhoenixMachines.AURUM_STEEL_CRATE.asStack() :
                    PhoenixMachines.AURUM_STEEL_DRUM.asStack();
            case MV -> type.equals("CRATE") ? PhoenixMachines.ALUMINFROST_CRATE.asStack() :
                    PhoenixMachines.ALUMINFROST_DRUM.asStack();
            case HV -> type.equals("CRATE") ? PhoenixMachines.FROST_REINFORCED_STAINED_STEEL_CRATE.asStack() :
                    PhoenixMachines.FROST_REINFORCED_STAINED_STEEL_DRUM.asStack();
            case EV -> type.equals("CRATE") ? PhoenixMachines.SOURCE_IMBUED_TITANIUM_CRATE.asStack() :
                    PhoenixMachines.SOURCE_IMBUED_TITANIUM_DRUM.asStack();
            case IV -> type.equals("CRATE") ? PhoenixMachines.VOID_TOUCHED_TUNGSTEN_STEEL_CRATE.asStack() :
                    PhoenixMachines.VOID_TOUCHED_TUNGSTEN_STEEL_DRUM.asStack();
            case LuV -> type.equals("CRATE") ? PhoenixMachines.RESONANT_RHODIUM_ALLOY_CRATE.asStack() :
                    PhoenixMachines.RESONANT_RHODIUM_ALLOY_DRUM.asStack();
            default -> type.equals("CRATE") ? PhoenixMachines.ALUMINFROST_CRATE.asStack() :
                    PhoenixMachines.AURUM_STEEL_DRUM.asStack();
        };
    }
}
