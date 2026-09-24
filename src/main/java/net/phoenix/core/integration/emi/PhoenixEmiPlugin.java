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
import net.minecraftforge.fml.ModList;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackScreen;
import net.phoenix.core.integration.gregvaults.client.screen.VaultScreen;
import net.phoenix.core.integration.gregvaults.client.screen.VaultTerminalScreen;
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

    @Override
    public void register(EmiRegistry registry) {
        boolean fissionLoaded = ModList.get().isLoaded("phoenix_fission");

        registry.addCategory(FISSION_FUEL);
        registry.addCategory(FISSION_COOLANT);
        registry.addCategory(FISSION_BREEDING);

        registry.addCategory(ASTRAL_RITUAL);
        registerAstralRituals(registry);

        registry.addExclusionArea(RecipeBuilderScreen.class, (screen, consumer) -> consumer.accept(new Bounds(
                screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize())));
        registry.addDragDropHandler(RecipeBuilderScreen.class, new RecipeBuilderDragDrop());

        // These three screens scale their whole layout to fit small windows (see each's own
        // getFullBoundsPx() doc) instead of just clipping/scrolling like RecipeBuilderScreen above, so
        // their real on-screen footprint isn't just vanilla's getGuiLeft()/getXSize() - those return
        // this screen's pre-scale coordinates, which only match real pixels at 1:1 scale. Without a
        // correct exclusion area EMI has no idea where these screens actually are once scaled down
        // (e.g. by a small window, or by EMI's own sidebar shrinking the space left for everything
        // else), so its sidebar/tooltips can render straight over them.
        registry.addExclusionArea(OmniPackScreen.class, (screen, consumer) -> {
            Rect2i b = screen.getFullBoundsPx();
            consumer.accept(new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
        });
        registry.addExclusionArea(VaultScreen.class, (screen, consumer) -> {
            Rect2i b = screen.getFullBoundsPx();
            consumer.accept(new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
        });
        registry.addExclusionArea(VaultTerminalScreen.class, (screen, consumer) -> {
            Rect2i b = screen.getFullBoundsPx();
            consumer.accept(new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
        });

        // The deeper half of the same bug, on a completely different EMI hook: EmiScreenBase.of(screen)
        // decides where EMI's own left/right/top/bottom sidebar regions get split *before* exclusion
        // areas are ever consulted. For any AbstractContainerScreen (all three of these are), that
        // decision falls back to the same raw leftPos/topPos/imageWidth/imageHeight (pre-scale, virtual
        // coordinates) unless a bounds provider is registered - reported directly via a Mixin accessor,
        // bypassing addExclusionArea entirely. At uiScale<1 (a window too small for 1:1, exactly the
        // "not fullscreen" case) the virtual canvas is *larger* than the real window, so that fallback's
        // right edge can exceed the real screen width outright - EMI then clamps its right-sidebar
        // region down to a sliver near the window's edge even though the real panel is smaller and
        // positioned differently, which is what collapses its layout into a cramped single row rather
        // than using the genuinely free space beside the (real, correctly-scaled) panel.
        EmiScreenBase.addScreenBoundsProvider(OmniPackScreen.class, screen -> {
            Rect2i b = screen.getFullBoundsPx();
            return new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        });
        EmiScreenBase.addScreenBoundsProvider(VaultScreen.class, screen -> {
            Rect2i b = screen.getFullBoundsPx();
            return new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        });
        EmiScreenBase.addScreenBoundsProvider(VaultTerminalScreen.class, screen -> {
            Rect2i b = screen.getFullBoundsPx();
            return new Bounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        });

        registerMaterialFluidSearchAliases(registry);
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
