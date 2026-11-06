package net.phoenix.core.integration.conflux.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.phoenix.core.integration.conflux.research.GatedRecipeRegistry;
import net.phoenix.core.integration.conflux.research.GatedRecipeRule;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class ConfluxRecipeVisibility {

    private static String builtFor = "";

    private ConfluxRecipeVisibility() {}

    private static String signature(@Nullable String discipline) {
        StringBuilder sb = new StringBuilder();
        sb.append(showOthers() ? "show;" : "hide;")
                .append(net.phoenix.core.integration.conflux.research.ResearchTreeRegistry.INSTANCE.getAllNodes()
                        .size())
                .append(';').append(discipline).append(';');
        List<GatedRecipeRule> rules = GatedRecipeRegistry.INSTANCE.getRules();
        for (int i = 0; i < rules.size(); i++) {
            GatedRecipeRule rule = rules.get(i);
            if (rule.hides() && rule.inScope(discipline)) sb.append(i).append(',');
        }
        return sb.toString();
    }

    private static boolean showOthers() {
        var cfg = net.phoenix.core.configs.PhoenixConfigs.INSTANCE;
        return cfg != null && cfg.showOtherDisciplineRecipes;
    }

    public static List<net.minecraft.network.chat.Component> describe(@Nullable ResourceLocation id,
                                                                      java.util.List<ItemStack> outputs,
                                                                      boolean craftingRecipe) {
        List<net.minecraft.network.chat.Component> out = new java.util.ArrayList<>();
        for (GatedRecipeRule rule : GatedRecipeRegistry.INSTANCE.getRules()) {
            if (rule.onlyDisciplines().isEmpty() && rule.exceptDisciplines().isEmpty()) continue;
            if (!matches(rule, id, outputs, craftingRecipe)) continue;
            if (out.isEmpty()) out.add(net.minecraft.network.chat.Component.literal("\u00a7dDiscipline recipe"));
            String who = !rule.onlyDisciplines().isEmpty() ? String.join(", ", rule.onlyDisciplines()) :
                    String.join(", ", rule.exceptDisciplines());
            boolean closes = !rule.onlyDisciplines().isEmpty();
            String text = rule.flag().isEmpty() ? (closes ? "Closed to: " + who : "Only for: " + who) :
                    (closes ? "Needs research for: " + who : "Needs research, except for: " + who);
            out.add(net.minecraft.network.chat.Component.literal("\u00a77" + text));
        }
        return out;
    }

    private static boolean matches(GatedRecipeRule rule, @Nullable ResourceLocation id,
                                   java.util.List<ItemStack> outputs, boolean craftingRecipe) {
        if (craftingRecipe) {
            if (id != null && rule.namesCraftingId(id)) return true;
            for (ItemStack stack : outputs) {
                if (rule.namesCraftingRecipe(id == null ? ResourceLocation.withDefaultNamespace("none") : id, stack)) {
                    return true;
                }
            }
            return false;
        }
        if (id != null && rule.recipeIds().contains(id)) return true;
        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) continue;
            if (rule.outputItems().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
            for (var tag : rule.outputTags()) {
                if (stack.is(tag)) return true;
            }
        }
        return false;
    }

    public static boolean hidden(@Nullable ResourceLocation id, java.util.List<ItemStack> outputs,
                                 boolean craftingRecipe) {
        if (showOthers()) return false;
        String discipline = ClientResearchCache.getDisciplineInfo().disciplineId();
        for (GatedRecipeRule rule : GatedRecipeRegistry.INSTANCE.getRules()) {
            if (!rule.hides() || !rule.inScope(discipline)) continue;
            if (craftingRecipe) {
                for (ItemStack stack : outputs) {
                    if (rule.namesCraftingRecipe(
                            Objects.requireNonNullElse(id, ResourceLocation.withDefaultNamespace("none")),
                            stack)) {
                        return true;
                    }
                }
                if (outputs.isEmpty() && id != null && rule.namesCraftingRecipe(id, ItemStack.EMPTY)) return true;
            } else {
                if (id != null && rule.recipeIds().contains(id)) return true;
                for (ItemStack stack : outputs) {
                    if (stack.isEmpty()) continue;
                    if (rule.outputItems().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
                    for (var tag : rule.outputTags()) {
                        if (stack.is(tag)) return true;
                    }
                }
            }
        }
        return false;
    }

    public static void onDisciplineMaybeChanged() {
        String now = signature(ClientResearchCache.getDisciplineInfo().disciplineId());
        if (now.equals(builtFor)) return;
        builtFor = now;
        if (ModList.get().isLoaded("emi")) ConfluxEmiRefresh.reloadRecipes();
    }

    public static void builtNow() {
        builtFor = signature(ClientResearchCache.getDisciplineInfo().disciplineId());
    }
}
