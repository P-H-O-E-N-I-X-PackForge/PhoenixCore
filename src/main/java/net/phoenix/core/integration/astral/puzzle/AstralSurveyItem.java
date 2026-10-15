package net.phoenix.core.integration.astral.puzzle;


import net.phoenix.core.util.StackNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.phoenix.core.integration.astral.AstralItems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AstralSurveyItem extends Item {

    private static final String DIM = "Dim";
    private static final String POS = "Pos";
    private static final String SEED = "Seed";
    private static final String KIND = "Kind";
    private static final String STARS = "Stars";
    private static final String TOTAL = "Total";
    private static final String KNOWN = "Known";
    private static final String PHASE_KNOWN = "PhaseKnown";

    public AstralSurveyItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(AstralCalibratedMachine machine, Level level) {
        ItemStack stack = new ItemStack(AstralItems.ASTRAL_SURVEY.get());
        StarChart chart = machine.chart();
        CompoundTag tag = StackNbt.getOrCreateTag(stack);
        tag.putString(DIM, level.dimension().location().toString());
        tag.putLong(POS, machine.getBlockPos().asLong());
        tag.putLong(SEED, machine.getChartSeed());
        tag.putInt(KIND, chart.kind().ordinal());
        tag.putIntArray(STARS, chart.packedStars());
        tag.putInt(TOTAL, chart.lineCount());
        tag.putIntArray(KNOWN, new int[0]);
        return stack;
    }

    public static boolean isFor(ItemStack stack, Level level, BlockPos pos, long seed) {
        CompoundTag tag = StackNbt.getTag(stack);
        return tag != null && tag.getLong(POS) == pos.asLong() && tag.getLong(SEED) == seed &&
                tag.getString(DIM).equals(level.dimension().location().toString());
    }

    public static boolean isSurvey(ItemStack stack) {
        return stack.getItem() instanceof AstralSurveyItem && StackNbt.hasTag(stack);
    }

    public static int[] stars(ItemStack stack) {
        return StackNbt.hasTag(stack) ? StackNbt.getTag(stack).getIntArray(STARS) : new int[0];
    }

    public static int[] known(ItemStack stack) {
        return StackNbt.hasTag(stack) ? StackNbt.getTag(stack).getIntArray(KNOWN) : new int[0];
    }

    public static int total(ItemStack stack) {
        return StackNbt.hasTag(stack) ? StackNbt.getTag(stack).getInt(TOTAL) : 0;
    }

    public static StarChart.Kind kind(ItemStack stack) {
        int ordinal = StackNbt.hasTag(stack) ? StackNbt.getTag(stack).getInt(KIND) : 0;
        StarChart.Kind[] values = StarChart.Kind.values();
        return values[Math.max(0, Math.min(ordinal, values.length - 1))];
    }

    public static String where(ItemStack stack) {
        if (!StackNbt.hasTag(stack)) return "?";
        BlockPos pos = BlockPos.of(StackNbt.getTag(stack).getLong(POS));
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static boolean phaseKnown(ItemStack stack) {
        return StackNbt.hasTag(stack) && StackNbt.getTag(stack).getBoolean(PHASE_KNOWN);
    }

    public static void revealPhase(ItemStack stack) {
        if (isSurvey(stack)) StackNbt.getTag(stack).putBoolean(PHASE_KNOWN, true);
    }

    public static int favouredPhase(ItemStack stack) {
        return StackNbt.hasTag(stack) ? AstralCalibratedMachine.favouredPhaseOf(StackNbt.getTag(stack).getLong(SEED)) : 0;
    }

    public static boolean complete(ItemStack stack) {
        return known(stack).length >= total(stack);
    }

    public static int reveal(ItemStack stack, RandomSource random, int count) {
        if (!isSurvey(stack)) return 0;
        CompoundTag tag = StackNbt.getTag(stack);
        StarChart chart = StarChart.generate(tag.getLong(SEED), kind(stack));
        int[] known = known(stack);
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < chart.lineCount(); i++) {
            boolean seen = false;
            for (int k : known) {
                if (k >> 8 == i) {
                    seen = true;
                    break;
                }
            }
            if (!seen) hidden.add(i);
        }
        int revealed = Math.min(count, hidden.size());
        int[] grown = Arrays.copyOf(known, known.length + revealed);
        for (int n = 0; n < revealed; n++) {
            int pick = random.nextInt(hidden.size());
            grown[known.length + n] = chart.packedLine(hidden.remove(pick));
        }
        Arrays.sort(grown);
        tag.putIntArray(KNOWN, grown);
        return revealed;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && isSurvey(stack)) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) { net.phoenix.core.integration.astral.puzzle.client.AstralPuzzleClient.openSurvey(stack); }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        if (!isSurvey(stack)) return;
        tooltip.add(Component.literal((kind(stack) == StarChart.Kind.CHAIN ? "Spinning Wheel" : "Loom") + " at " +
                where(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(known(stack).length + " of " + total(stack) + " lines revealed")
                .withStyle(complete(stack) ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        if (phaseKnown(stack)) {
            tooltip.add(Component.literal("Favoured moon: " + AstralCalibratedMachine.phaseName(favouredPhase(stack)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.literal("Right-click to read. Channel the Ritual Pedestal to reveal more.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
