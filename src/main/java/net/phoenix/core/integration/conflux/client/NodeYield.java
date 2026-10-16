package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.research.ConfluxRewards;
import net.phoenix.core.integration.conflux.research.GatedRecipeRegistry;
import net.phoenix.core.integration.conflux.research.GatedRecipeRule;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public record NodeYield(List<ItemStack> rewards, List<ItemStack> recipeItems, List<String> recipeLines,
                        List<ItemStack> machines, List<String> otherLines, List<ResearchNode> leadsTo) {

    public boolean isEmpty() {
        return rewards.isEmpty() && recipeItems.isEmpty() && recipeLines.isEmpty() && machines.isEmpty() &&
                otherLines.isEmpty() && leadsTo.isEmpty();
    }

    public static NodeYield of(ResearchNode node, boolean showFlags) {
        var registries = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : null;
        List<ItemStack> rewards = new ArrayList<>();
        List<ItemStack> recipeItems = new ArrayList<>();
        List<String> recipeLines = new ArrayList<>();
        List<ItemStack> machines = new ArrayList<>();
        List<String> other = new ArrayList<>();

        for (ResearchUnlock u : node.unlocks) {
            switch (u.type()) {
                case "item" -> {
                    ItemStack s = ConfluxRewards.parse(u.value(), registries);
                    if (!s.isEmpty()) rewards.add(s);
                }
                case "flag" -> {
                    boolean gates = false;
                    for (GatedRecipeRule rule : GatedRecipeRegistry.INSTANCE.getRules()) {
                        if (!rule.flag().equals(u.value())) continue;
                        gates = true;
                        for (ResourceLocation id : rule.outputItems()) addItem(recipeItems, id);
                        for (var tag : rule.outputTags()) recipeLines.add("Any item tagged #" + tag.location());
                        for (var gate : rule.recipeTypes()) {
                            recipeLines.add(pretty(gate.recipeType().getPath()) + " recipes, " +
                                    gate.minTier().toUpperCase(Locale.ROOT) + " and up");
                        }
                        for (ResourceLocation id : rule.recipeIds())
                            recipeLines.add("Recipe: " + pretty(lastSegment(id)));
                    }
                    if (showFlags && !gates) other.add("§b⚑ " + u.value());
                }
                case "multiblock" -> {
                    ResourceLocation id = ResourceLocation.tryParse(u.value());
                    Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                    if (item != null) machines.add(new ItemStack(item));
                    else other.add("§6⬡ " + u.value());
                }
                case "recipe_tag" -> recipeLines.add("Recipes: " + u.value());
                case "archive" -> other.add("§e❖ Lore: " + u.value());
                case "world_stage" -> other.add("§a▲ Stage: " + u.value());
                default -> other.add("§7• " + u.value());
            }
        }

        for (var body : net.phoenix.core.integration.continuum.data.ContinuumData.allBodies()) {
            if (node.id.equals(body.surveyedResearch())) other.add("§b◎ Surveys " + body.name());
            else if (node.id.equals(body.detectedResearch())) other.add("§e◎ Detects " + body.name());
        }
        for (var system : net.phoenix.core.integration.continuum.data.ContinuumData.systems()) {
            if (node.id.equals(system.surveyedResearch())) other.add("§b◎ Surveys the " + system.name() + " system");
            else if (node.id.equals(system.detectedResearch()))
                other.add("§e◎ Detects the " + system.name() + " system");
        }

        List<ResearchNode> leads = new ArrayList<>();
        for (ResearchNode n : ResearchTreeRegistry.INSTANCE.getAllNodes()) {
            if (n.prerequisites.contains(node.id) || n.prerequisitesAny.contains(node.id)) leads.add(n);
        }
        return new NodeYield(rewards, recipeItems, recipeLines, machines, other, leads);
    }

    private static void addItem(List<ItemStack> out, ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) return;
        for (ItemStack s : out) if (s.is(item)) return;
        out.add(new ItemStack(item));
    }

    private static String lastSegment(ResourceLocation id) {
        String p = id.getPath();
        return p.substring(p.lastIndexOf('/') + 1);
    }

    static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.replace('/', ' ').split("[_ ]")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
