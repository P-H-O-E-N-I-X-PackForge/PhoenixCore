package net.phoenix.core.common.data.recipeConditions;

import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.phoenix.conflux.common.research.WorldResearchData;
import net.phoenixvine.wiki.util.TeamUtils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DisciplineCondition extends RecipeCondition<DisciplineCondition> {

    public static final MapCodec<DisciplineCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> RecipeCondition.isReverse(instance)
                    .and(Codec.STRING.fieldOf("discipline").forGetter(DisciplineCondition::getDiscipline))
                    .apply(instance, DisciplineCondition::new));

    @Getter
    private String discipline = "";

    public DisciplineCondition() {
        super(false);
    }

    public DisciplineCondition(boolean isReverse, String discipline) {
        super(isReverse);
        this.discipline = discipline;
    }

    public DisciplineCondition(String discipline) {
        this(false, discipline);
    }

    @Override
    protected boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        var machine = recipeLogic.getMachine();
        if (!(machine.getLevel() instanceof ServerLevel level)) return false;
        UUID owner = machine.getOwnerUUID();
        if (owner == null) return false;
        UUID team = TeamUtils.getTeamIdOrPlayerFallback(owner);
        String chosen = WorldResearchData.get(level).disciplineOf(team);
        return discipline.equals(chosen);
    }

    @Override
    public Component getTooltips() {
        String name = discipline.isEmpty() ? "?" : discipline.replace('_', ' ');
        return Component.literal((isReverse ? "§dNot for the discipline: " : "§dOnly for the discipline: ") + name);
    }

    /** The short label for recipe pages, for example "Only for sculk". */
    public String pillText() {
        String name = discipline.isEmpty() ? "?" : discipline.replace('_', ' ');
        return (isReverse ? "Not for " : "Only for ") + name;
    }

    @Override
    public ViewerPill viewerPill() {
        // just the discipline's name; the full sentence is in the tooltip
        String name = discipline.isEmpty() ? "?" : discipline.replace('_', ' ');
        return new ViewerPill(name, 0xFFB890FF,
                java.util.List.of(net.minecraft.network.chat.Component.literal("§dDiscipline recipe"), getTooltips()));
    }

    @Override
    public boolean hiddenInViewer(com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        var cfg = net.phoenix.conflux.ConfluxConfigs.INSTANCE;
        if (cfg != null && cfg.showOtherDisciplineRecipes) return false;
        return !discipline.equals(net.phoenix.conflux.api.ConfluxApi.clientDiscipline());
    }

    @Override
    public DisciplineCondition createTemplate() {
        return new DisciplineCondition();
    }

    @Override
    public RecipeConditionType<DisciplineCondition> getType() {
        return TYPE.get();
    }

    public static com.tterrag.registrate.util.entry.RegistryEntry<RecipeConditionType<?>, RecipeConditionType<DisciplineCondition>> TYPE;
}
