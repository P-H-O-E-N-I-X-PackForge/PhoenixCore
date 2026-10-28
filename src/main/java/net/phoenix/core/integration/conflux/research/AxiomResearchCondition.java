package net.phoenix.core.integration.conflux.research;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.recipe.gui.RecipeUIModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.widgets.TextWidget;
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

    @Getter
    private @NotNull String only = "";

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
        return Component.literal("Research: " + researchName(flag));
    }

    @Override
    public RecipeUIModifier modifyUI() {
        return (recipe, widget) -> {

            String mine = net.phoenix.core.integration.conflux.client.ClientResearchCache.getDisciplineInfo()
                    .disciplineId();
            if (!only.isEmpty() && mine != null && !listed(only, mine)) return;
            if (!except.isEmpty() && listed(except, mine)) return;
            if (flag.isEmpty()) {
                widget.textComponents.child(new TextWidget<>(Text.of(getTooltips())));
                return;
            }
            RichTooltip tip = new RichTooltip();
            tip.addLine(Text.of(Component.literal("§bResearch: §f" + researchName(flag))));
            String tree = treeName(flag);
            if (tree != null) tip.addLine(Text.of(Component.literal("§7Found in: §f" + tree)));
            if (!only.isEmpty()) tip.addLine(Text.of(Component.literal("§7Restricts: §f" + only)));
            if (!except.isEmpty()) tip.addLine(Text.of(Component.literal("§7Not required by: §f" + except)));
            tip.addLine(Text.of(Component.literal("§8Research it at a Conflux terminal.")));
            widget.textComponents.child(new TextWidget<>(Text.of(
                    Component.literal("Required Research").withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)))
                    .tooltip(tip));
        };
    }

    private static @org.jetbrains.annotations.Nullable String treeName(String flag) {
        for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
            for (ResearchNode node : tree.getNodes()) {
                for (ResearchUnlock unlock : node.unlocks) {
                    if ("flag".equals(unlock.type()) && flag.equals(unlock.value())) return tree.title;
                }
            }
        }
        return null;
    }

    private static String researchName(String flag) {
        for (ResearchNode node : ResearchTreeRegistry.INSTANCE.getAllNodes()) {
            for (ResearchUnlock unlock : node.unlocks) {
                if ("flag".equals(unlock.type()) && flag.equals(unlock.value())) return node.title;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String part : flag.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
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
