package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.phoenix.core.PhoenixCore;

import java.util.ArrayList;
import java.util.List;

public final class PftRecipe {

    private static final List<String> PROBLEMS = new ArrayList<>();

    private final RecipeOutput output;
    private final String type;
    private final String id;
    private final GTRecipeBuilder builder;
    private final List<String> missing = new ArrayList<>();

    private PftRecipe(RecipeOutput output, String type, String id, GTRecipeBuilder builder) {
        this.output = output;
        this.type = type;
        this.id = id;
        this.builder = builder;
    }

    public static PftRecipe gt(RecipeOutput output, String type, String id) {
        GTRecipeType recipeType = lookupType(type);
        if (recipeType == null) {
            PROBLEMS.add("recipe " + id + ": no recipe type '" + type + "'");
            PhoenixCore.LOGGER.error("[PFT] recipe {}: no recipe type '{}'", id, type);
            return new PftRecipe(output, type, id, null);
        }
        return new PftRecipe(output, type, id, recipeType.recipeBuilder(PhoenixCore.id(id.replace(':', '/'))));
    }

    private static GTRecipeType lookupType(String name) {
        for (String namespace : new String[] { "gtceu", PhoenixCore.MOD_ID }) {
            RecipeType<?> found = BuiltInRegistries.RECIPE_TYPE
                    .get(ResourceLocation.fromNamespaceAndPath(namespace, name));
            if (found instanceof GTRecipeType gt) return gt;
        }
        return null;
    }

    private record Counted(String id, int count) {}

    private static Counted counted(String spec) {
        String s = spec.trim();
        int count = 1;
        int x = s.indexOf("x ");
        if (x > 0 && s.substring(0, x).matches("\\d+")) {
            count = Integer.parseInt(s.substring(0, x));
            s = s.substring(x + 2).trim();
        }
        return new Counted(s, count);
    }

    private static ResourceLocation resolve(String raw, java.util.function.Predicate<ResourceLocation> exists) {
        ResourceLocation id = ResourceLocation.parse(raw);
        if (exists.test(id)) return id;
        if (id.getNamespace().equals("gtceu") || id.getNamespace().equals("kubejs")) {
            ResourceLocation moved = ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, id.getPath());
            if (exists.test(moved)) return moved;
        }
        return null;
    }

    private ItemStack stack(Object spec) {
        if (spec instanceof ItemStack s) return s;
        Counted c = counted(String.valueOf(spec));
        if (c.id().startsWith("#")) {
            missing.add("item (a tag is not an item: " + c.id() + ")");
            return ItemStack.EMPTY;
        }
        ResourceLocation id = resolve(c.id(), BuiltInRegistries.ITEM::containsKey);
        if (id == null) {
            missing.add("item " + c.id());
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id), c.count());
    }

    private FluidStack fluid(Object spec) {
        if (spec instanceof FluidStack f) return f;
        String s = String.valueOf(spec).trim();
        int space = s.lastIndexOf(' ');
        int amount = 1000;
        if (space > 0 && s.substring(space + 1).matches("\\d+")) {
            amount = Integer.parseInt(s.substring(space + 1));
            s = s.substring(0, space).trim();
        }
        ResourceLocation id = resolve(s, BuiltInRegistries.FLUID::containsKey);
        if (id == null) {
            missing.add("fluid " + s);
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private static TagKey<Item> tag(String id) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(id));
    }

    public PftRecipe itemInputs(Object... specs) {
        for (Object spec : specs) {
            if (builder == null) return this;
            String text = String.valueOf(spec).trim();
            if (!(spec instanceof ItemStack) && text.contains("#")) {
                Counted c = counted(text);
                builder.inputItems(tag(c.id().substring(c.id().indexOf('#') + 1)), c.count());
                continue;
            }
            ItemStack stack = stack(spec);
            if (!stack.isEmpty()) builder.inputItems(stack);
        }
        return this;
    }

    public PftRecipe itemOutputs(Object... specs) {
        for (Object spec : specs) {
            if (builder == null) return this;
            ItemStack stack = stack(spec);
            if (!stack.isEmpty()) builder.outputItems(stack);
        }
        return this;
    }

    public PftRecipe inputFluids(Object... specs) {
        for (Object spec : specs) {
            if (builder == null) return this;
            FluidStack f = fluid(spec);
            if (!f.isEmpty()) builder.inputFluids(f);
        }
        return this;
    }

    public PftRecipe outputFluids(Object... specs) {
        for (Object spec : specs) {
            if (builder == null) return this;
            FluidStack f = fluid(spec);
            if (!f.isEmpty()) builder.outputFluids(f);
        }
        return this;
    }

    public PftRecipe notConsumable(Object spec) {
        if (builder == null) return this;
        String text = String.valueOf(spec).trim();
        if (!(spec instanceof ItemStack) && text.contains("#")) {
            builder.notConsumable(tag(text.substring(text.indexOf('#') + 1)));
            return this;
        }
        ItemStack stack = stack(spec);
        if (!stack.isEmpty()) builder.notConsumable(stack);
        return this;
    }

    public PftRecipe chancedOutput(Object spec, int chance) {
        if (builder == null) return this;
        ItemStack stack = stack(spec);
        if (!stack.isEmpty()) builder.chancedOutput(stack, chance);
        return this;
    }

    public PftRecipe circuit(int number) {
        if (builder != null) builder.circuitMeta(number);
        return this;
    }

    public PftRecipe duration(int ticks) {
        if (builder != null) builder.duration(ticks);
        return this;
    }

    public PftRecipe EUt(double eut) {
        return EUt((long) eut);
    }

    public PftRecipe EUt(long eut) {
        if (builder != null) builder.EUt(eut);
        return this;
    }

    public PftRecipe blastFurnaceTemp(int temperature) {
        if (builder != null) builder.blastFurnaceTemp(temperature);
        return this;
    }

    public PftRecipe CWUt(int cwut) {
        if (builder != null) builder.CWUt(cwut);
        return this;
    }

    public PftRecipe stationResearch(String researchItem, long eut, int cwut, int totalCwut, String dataItem) {
        if (builder == null) return this;
        ItemStack research = stack(researchItem);
        ItemStack data = dataItem == null ? ItemStack.EMPTY : stack(dataItem);
        if (research.isEmpty()) return this;
        builder.stationResearch(b -> {
            var configured = b.researchStack(research).EUt(eut).CWUt(cwut, totalCwut);
            return data.isEmpty() ? configured : configured.dataStack(data);
        });
        return this;
    }

    public PftRecipe fusionStartEU(long eu) {
        if (builder != null) builder.addData("eu_to_start", eu);
        return this;
    }

    public PftRecipe dimension(String dimension) {
        if (builder != null) builder.dimension(ResourceLocation.parse(dimension));
        return this;
    }

    public GTRecipeBuilder raw() {
        return builder;
    }

    public void save() {
        if (builder == null) return;
        if (!missing.isEmpty()) {
            String what = id + " (" + type + ") needs " + String.join(", ", missing);
            PROBLEMS.add(what);
            PhoenixCore.LOGGER.error("[PFT] skipped recipe {}", what);
            return;
        }
        builder.save(output);
    }

    public static void report(String problem) {
        PROBLEMS.add(problem);
    }

    public static List<String> problems() {
        return List.copyOf(PROBLEMS);
    }
}
