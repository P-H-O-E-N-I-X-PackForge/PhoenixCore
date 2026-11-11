package net.phoenix.core.integration.conflux.producer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.conflux.ConfluxDataType;

import java.util.List;

public final class ConfluxFuels {

    private ConfluxFuels() {}

    public static final TagKey<Item> BIOMATERIAL = TagKey.create(Registries.ITEM,
            PhoenixCore.id("conflux_biomaterial"));

    private static final List<TagKey<Item>> CIRCUIT_TIERS = List.of(CustomTags.ULV_CIRCUITS, CustomTags.LV_CIRCUITS,
            CustomTags.MV_CIRCUITS, CustomTags.HV_CIRCUITS, CustomTags.EV_CIRCUITS, CustomTags.IV_CIRCUITS,
            CustomTags.LuV_CIRCUITS, CustomTags.ZPM_CIRCUITS, CustomTags.UV_CIRCUITS, CustomTags.UHV_CIRCUITS,
            CustomTags.UEV_CIRCUITS, CustomTags.UIV_CIRCUITS, CustomTags.UXV_CIRCUITS, CustomTags.OpV_CIRCUITS,
            CustomTags.MAX_CIRCUITS);

    private static final float CIRCUIT_STEP = 0.75f;

    public static final int BASE_TICKS = 200;

    public static boolean usesItems(ConfluxDataType type) {
        return type == ConfluxDataType.MATERIAL || type == ConfluxDataType.BIOLOGICAL ||
                type == ConfluxDataType.COMPUTATIONAL;
    }

    public static boolean isFuel(ConfluxDataType type, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (type) {
            case MATERIAL -> !ChemicalHelper.getMaterialStack(stack).isEmpty();
            case BIOLOGICAL -> stack.is(BIOMATERIAL);
            case COMPUTATIONAL -> stack.is(CustomTags.CIRCUITS);
            default -> false;
        };
    }

    public static int ticks(ConfluxDataType type, ItemStack stack) {
        if (type == ConfluxDataType.MATERIAL) {
            long units = Math.round(ChemicalHelper.getMaterialStack(stack).amount() / (double) GTValues.M);
            return BASE_TICKS * (int) Math.max(1L, Math.min(9L, units));
        }
        return type == ConfluxDataType.COMPUTATIONAL ? BASE_TICKS * 2 : BASE_TICKS;
    }

    public static float quality(ConfluxDataType type, ItemStack stack) {
        if (type != ConfluxDataType.COMPUTATIONAL) return 1.0f;
        for (int i = CIRCUIT_TIERS.size() - 1; i >= 0; i--) {
            if (stack.is(CIRCUIT_TIERS.get(i))) return 1.0f + i * CIRCUIT_STEP;
        }
        return 1.0f;
    }
}
