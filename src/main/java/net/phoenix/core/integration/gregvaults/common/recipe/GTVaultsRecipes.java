package net.phoenix.core.integration.gregvaults.common.recipe;

import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.common.items.WirelessTerminalItem;
import net.phoenix.core.integration.gregvaults.common.registry.VaultRegistry;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachineDefinition.*;

@SuppressWarnings("all")
public class GTVaultsRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("large_bronze_vault"),
                BRONZE_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Bronze),
                'B', CustomTags.ULV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.BRONZE_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("large_steel_vault"),
                STEEL_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Steel),
                'B', CustomTags.LV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.STEEL_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("large_titanium_vault"),
                TITANIUM_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Titanium),
                'B', CustomTags.HV_CIRCUITS, 'C', new MaterialEntry(rod, StainlessSteel), 'D',
                GTMachines.TITANIUM_CRATE.asStack(), 'E', new MaterialEntry(plateDouble, StainlessSteel));

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("mk1_core"),
                VaultRegistry.VAULT_CORE_MK1.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Bronze),
                'B', new MaterialEntry(rodLong, Bronze), 'C', GTMachines.BRONZE_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("mk2_core"),
                VaultRegistry.VAULT_CORE_MK2.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Steel),
                'B', new MaterialEntry(rodLong, Steel), 'C', GTMachines.STEEL_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("mk3_core"),
                VaultRegistry.VAULT_CORE_MK3.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Titanium),
                'B', new MaterialEntry(rodLong, Titanium), 'C', GTMachines.TITANIUM_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("wireless_vault_terminal"),
                VaultRegistry.WIRELESS_VAULT_TERMINAL.asStack(), "AAA", "CBD", "EdE", 'A',
                new MaterialEntry(plate, Steel), 'B', new MaterialEntry(plate, Glass), 'C', GTItems.EMITTER_LV,
                'D', GTItems.SENSOR_LV, 'E', new MaterialEntry(screw, Steel));

        VanillaRecipeHelper.addShapedRecipe(provider, true, PhoenixCore.id("vault_interface"),
                VaultRegistry.VAULT_INTERFACE.asStack(), "w", "B", "A", 'A', GTBlocks.BRONZE_HULL,
                'B', new MaterialEntry(pipeNormalFluid, Bronze));

        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.LV, GTItems.EMITTER_LV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.MV, GTItems.EMITTER_MV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.HV, GTItems.EMITTER_HV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.EV, GTItems.EMITTER_EV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.IV, GTItems.EMITTER_IV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.LUV, GTItems.EMITTER_LuV.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.ZPM, GTItems.EMITTER_ZPM.get());
        addEmitterUpgrade(provider, WirelessTerminalItem.EmitterTier.UV, GTItems.EMITTER_UV.get());
    }

    private static void addEmitterUpgrade(Consumer<FinishedRecipe> provider,
                                          WirelessTerminalItem.EmitterTier tier, net.minecraft.world.item.Item emitterItem) {
        ResourceLocation recipeId = PhoenixCore.id("terminal_emitter_" + tier.name().toLowerCase());
        ResourceLocation emitterId = ForgeRegistries.ITEMS.getKey(emitterItem);

        provider.accept(new FinishedRecipe() {

            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("tier", tier.level);
                json.addProperty("emitter", emitterId.toString());
            }

            @Override
            public ResourceLocation getId() {
                return recipeId;
            }

            @Override
            public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
                return VaultRecipes.EMITTER_UPGRADE_SERIALIZER.get();
            }

            @Override
            public @org.jetbrains.annotations.Nullable JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public @org.jetbrains.annotations.Nullable ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }
}
