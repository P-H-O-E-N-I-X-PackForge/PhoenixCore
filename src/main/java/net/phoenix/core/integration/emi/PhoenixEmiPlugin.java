package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackScreen;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.screen.EmiScreenBase;

@EmiEntrypoint
public class PhoenixEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory FISSION_FUEL = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "fission_fuel"),
            EmiStack.of(ChemicalHelper.get(TagPrefix.ingot, GTMaterials.Uranium235)));

    public static final EmiRecipeCategory FISSION_COOLANT = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "fission_coolant"),
            EmiStack.of(Items.WATER_BUCKET));

    public static final EmiRecipeCategory FISSION_BREEDING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "fission_breeding"),
            EmiStack.of(Items.CAULDRON));

    public static final EmiRecipeCategory ASTRAL_RITUAL = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "astral_ritual"),
            EmiStack.of(new ItemStack(net.phoenix.core.integration.astral.AstralBlocks.ASTRAL_RITUAL_PEDESTAL.get())));

    private static final java.util.List<java.util.regex.Pattern> HIDDEN_ITEMS = java.util.List.of(
            java.util.regex.Pattern.compile("advanced_[a-z_]+_canvas_hive"),
            java.util.regex.Pattern.compile("exspansion_box_[a-z_]+_canvas"),
            java.util.regex.Pattern.compile("exspansion_box_[a-z_]"),
            java.util.regex.Pattern.compile("advanced_[a-z_]+_beehive"),
            java.util.regex.Pattern.compile("volcanic_sourcelink"),
            java.util.regex.Pattern.compile("alchemical_sourcelink"),
            java.util.regex.Pattern.compile("mycelial_sourcelink"),
            java.util.regex.Pattern.compile("^productivebees:.*(exspansion|breeding_chamber|incubater|gene_indexer)"),
            java.util.regex.Pattern.compile("^expatternprovider:circuit_cutter$"));

    private static final java.util.Set<String> HIDDEN_CATEGORIES = java.util.Set.of("jei:information",
            "ae2:certus_growth");

    private static void hideFromScripts(EmiRegistry registry) {
        registry.removeEmiStacks(stack -> {
            ResourceLocation id = stack.getId();
            if (id == null) return false;
            String text = id.toString();
            for (java.util.regex.Pattern p : HIDDEN_ITEMS) {
                if (p.matcher(text).find()) return true;
            }
            return false;
        });
        registry.removeRecipes(recipe -> HIDDEN_CATEGORIES.contains(recipe.getCategory().getId().toString()));
    }

    @Override
    public void register(EmiRegistry registry) {
        hideFromScripts(registry);
        registry.removeRecipes(net.phoenix.core.integration.conflux.client.ConfluxEmiRefresh::hide);
        net.phoenix.core.integration.conflux.client.ConfluxRecipeVisibility.builtNow();
        registry.addRecipeDecorator(net.phoenix.core.integration.conflux.client.ConfluxEmiRefresh::decorate);
        guarded("Conflux research pages", () -> ConfluxEmi.register(registry));
        guarded("lapidary pages", () -> LapidaryEmi.register(registry));
        guarded("Cinder Forge pages", () -> CinderEmi.register(registry));
        guarded("chunk soul pages", () -> SoulEmi.register(registry));
        guarded("Tesla Network pages", () -> TeslaEmi.register(registry));
        guarded("Astral weaving pages", () -> AstralEmi.register(registry));
        guarded("Conflux ore pages", () -> OreEmi.register(registry));
        guarded("Conflux producer pages", () -> ProducerEmi.register(registry));
        boolean fissionLoaded = ModList.get().isLoaded("phoenix_fission");

        registry.addCategory(FISSION_FUEL);
        registry.addCategory(FISSION_COOLANT);
        registry.addCategory(FISSION_BREEDING);

        GrowthEmi.register(registry);

        registry.addCategory(ASTRAL_RITUAL);
        registerAstralRituals(registry);
        StellarEmi.register(registry);

        registry.addExclusionArea(RecipeBuilderScreen.class, (screen, consumer) -> consumer.accept(new Bounds(
                screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize())));
        registry.addDragDropHandler(RecipeBuilderScreen.class, new RecipeBuilderDragDrop());

        registry.addExclusionArea(OmniPackScreen.class, (screen, consumer) -> {
            Rect2i b = screen.getFullBoundsPx();
            consumer.accept(new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
        });
        EmiScreenBase.addScreenBoundsProvider(OmniPackScreen.class, screen -> {
            Rect2i b = screen.getFullBoundsPx();
            return new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        });

        registerMaterialFluidSearchAliases(registry);
    }

    private static void guarded(String what, Runnable register) {
        try {
            register.run();
        } catch (Throwable t) {
            net.phoenix.core.PhoenixCore.LOGGER.error("[PFT] The EMI {} could not be registered", what, t);
        }
    }

    private static void registerAstralRituals(EmiRegistry registry) {
        AstralRitualPedestalBlockEntity.RITUAL_TABLE
                .forEach((catalyst, result) -> registry.addRecipe(new AstralRitualEmiRecipe(catalyst, result)));
    }

    private static void registerMaterialFluidSearchAliases(EmiRegistry registry) {
        for (Material material : GTRegistries.MATERIALS) {
            if (!material.hasProperty(PropertyKey.FLUID)) continue;

            Fluid fluid = material.getFluid();
            if (fluid == null || fluid == Fluids.EMPTY) continue;

            EmiStack fluidStack = EmiStack.of(fluid);
            if (fluidStack.isEmpty()) continue;

            registry.addAlias(fluidStack, material.getLocalizedName());
        }
    }

    private static void addFormulaAliases(EmiRegistry registry, EmiStack stack, String... terms) {
        for (String term : terms) {
            registry.addAlias(stack, Component.literal(term));
        }
    }

    private static class RecipeBuilderDragDrop implements EmiDragDropHandler<RecipeBuilderScreen> {

        @Override
        public boolean dropStack(RecipeBuilderScreen screen, EmiIngredient ingredient, int x, int y) {
            if (ingredient.isEmpty()) return false;
            EmiStack first = ingredient.getEmiStacks().get(0);

            if (first instanceof ItemEmiStack itemEmi) {
                ItemStack mc = itemEmi.getItemStack();
                if (screen.itemInputPanel.isMouseOver(x, y))
                    return screen.itemInputPanel.acceptStack(mc, x, y);
                if (screen.itemOutputPanel.isMouseOver(x, y))
                    return screen.itemOutputPanel.acceptStack(mc, x, y);
                return screen.itemInputPanel.acceptStack(mc, x, y);
            }

            if (first instanceof FluidEmiStack fluidEmi) {
                ResourceLocation res = fluidEmi.getId();
                String id = (res != null) ? res.toString() : "minecraft:empty";
                int amount = (int) fluidEmi.getAmount();
                if (screen.fluidInputPanel.isMouseOver(x, y))
                    return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
                if (screen.fluidOutputPanel.isMouseOver(x, y))
                    return screen.fluidOutputPanel.acceptFluid(id, amount, x, y);
                return screen.fluidInputPanel.acceptFluid(id, amount, x, y);
            }

            return false;
        }
    }
}
