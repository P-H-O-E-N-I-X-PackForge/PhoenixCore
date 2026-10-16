package net.phoenix.core.integration.conflux.research;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@NoArgsConstructor
public class AxiomResearchCondition extends RecipeCondition<AxiomResearchCondition> {

    public static final String CONDITION_ID = "phoenixcore:axiom_research";

    public static final MapCodec<AxiomResearchCondition> CODEC = RecordCodecBuilder
            .mapCodec(instance -> RecipeCondition.isReverse(instance)
                    .and(Codec.STRING.fieldOf("flag").forGetter(AxiomResearchCondition::getFlag))
                    .and(Codec.STRING.optionalFieldOf("only", "").forGetter(AxiomResearchCondition::getOnly))
                    .and(Codec.STRING.optionalFieldOf("except", "").forGetter(AxiomResearchCondition::getExcept))
                    .apply(instance, AxiomResearchCondition::new));

    public static com.tterrag.registrate.util.entry.RegistryEntry<RecipeConditionType<?>, RecipeConditionType<AxiomResearchCondition>> TYPE;

    private static final double CHECK_RADIUS = 24.0;

    @Getter
    private @NotNull String flag = "";

    /** Comma separated disciplines the restriction applies to; empty means all. */
    @Getter
    private @NotNull String only = "";

    /** Comma separated disciplines the restriction does not apply to. */
    @Getter
    private @NotNull String except = "";

    public AxiomResearchCondition(boolean isReverse, @NotNull String flag, @NotNull String only,
                                  @NotNull String except) {
        super(isReverse);
        this.flag = flag;
        this.only = only;
        this.except = except;
    }

    public static AxiomResearchCondition of(String flag) {
        return new AxiomResearchCondition(false, flag, "", "");
    }

    public static AxiomResearchCondition of(String flag, String only, String except) {
        return new AxiomResearchCondition(false, flag, only, except);
    }

    private static boolean listed(String csv, String discipline) {
        if (discipline == null) return false;
        for (String entry : csv.split(",")) if (entry.trim().equals(discipline)) return true;
        return false;
    }

    @Override
    protected boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        MetaMachine machine = recipeLogic.getMachine();
        if (!(machine.getLevel() instanceof ServerLevel level)) return false;

        ServerPlayer nearestPlayer = level.getNearestPlayer(
                machine.getBlockPos().getX() + 0.5,
                machine.getBlockPos().getY() + 0.5,
                machine.getBlockPos().getZ() + 0.5,
                CHECK_RADIUS,
                p -> !p.isSpectator() && p instanceof ServerPlayer) instanceof ServerPlayer sp ? sp : null;

        if (nearestPlayer == null) return false;

        UUID teamId = ResearchTeamHelper.getTeamId(nearestPlayer);
        WorldResearchData data = WorldResearchData.get(level);
        // "only"/"except" scope who the restriction applies to; outside that scope the recipe is unrestricted.
        // Inside it the flag is required, and with no flag the recipe is simply closed to that discipline.
        String discipline = data.getDiscipline(teamId);
        boolean inScope = (only.isEmpty() || listed(only, discipline)) && !listed(except, discipline);
        if (!inScope) return true;
        return !flag.isEmpty() && data.hasFlag(teamId, flag);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getTooltips() {
        if (flag.isEmpty()) {
            return Component.literal(!only.isEmpty() ? "Unavailable to: " + only : "Only for: " + except);
        }
        return Component.translatable("phoenixcore.condition.axiom_research", flag);
    }

    @Override
    public AxiomResearchCondition createTemplate() {
        return new AxiomResearchCondition();
    }

    @Override
    public RecipeConditionType<AxiomResearchCondition> getType() {
        if (TYPE == null) throw new IllegalStateException("AxiomResearchCondition.TYPE not registered yet!");
        return TYPE.get();
    }
}
