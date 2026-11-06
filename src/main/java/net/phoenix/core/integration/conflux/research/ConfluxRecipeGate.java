package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class ConfluxRecipeGate {

    private ConfluxRecipeGate() {}

    public static boolean blocks(ServerPlayer player, net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
        if (player.isCreative()) return false;
        if (blocks(player, holder.id())) return true;
        var rules = GatedRecipeRegistry.INSTANCE.getRules();
        if (rules.isEmpty()) return false;
        UUID team = ResearchTeamHelper.getTeamId(player);
        WorldResearchData data = WorldResearchData.get(player.serverLevel());
        String discipline = data.getDiscipline(team);
        net.minecraft.world.item.ItemStack result = null;
        for (GatedRecipeRule rule : rules) {
            if (!rule.crafting() || !rule.inScope(discipline)) continue;
            if (result == null) result = holder.value().getResultItem(player.serverLevel().registryAccess());
            if (rule.namesCraftingRecipe(holder.id(), result) && rule.blocks(discipline, f -> data.hasFlag(team, f))) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocks(ServerPlayer player, ResourceLocation recipeId) {
        String flag = ResearchTreeRegistry.INSTANCE.getGatedRecipes().get(recipeId);
        if (flag == null || player.isCreative()) return false;
        UUID team = ResearchTeamHelper.getTeamId(player);
        return !WorldResearchData.get(player.serverLevel()).hasFlag(team, flag);
    }
}
