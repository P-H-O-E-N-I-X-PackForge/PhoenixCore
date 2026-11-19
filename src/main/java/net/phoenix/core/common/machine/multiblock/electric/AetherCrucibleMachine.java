package net.phoenix.core.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine;
import net.phoenix.core.common.machine.multiblock.api.TierAwareMultiblockMachine.TierConditions;
import net.phoenixvine.wiki.ui.hud.HudModel;
import net.phoenixvine.wiki.ui.hud.HudRecipes;
import net.phoenixvine.wiki.ui.hud.MachineHud;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class AetherCrucibleMachine extends TierAwareMultiblockMachine {

    @SaveField
    @SyncToClient
    private int catalystTicks = 0;

    @SaveField
    @SyncToClient
    private int catalystMax = 0;

    @SaveField
    @SyncToClient
    private String resonanceMsg = "";

    public AetherCrucibleMachine(BlockEntityCreationInfo info) {
        super(info, 2);

        registerTierCondition(1, TierConditions.hasPartOfClass(FluidHatchPartMachine.class));
    }

    public static boolean usesDrillingFluid(GTRecipe recipe) {
        try {
            for (var content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
                for (var fluid : FluidRecipeCapability.CAP.of(content.content()).getFluids()) {
                    if (BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath().endsWith("drilling_fluid")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean resonant() {
        return getFormationTier() >= 2 && catalystTicks > 0;
    }

    public void startResonance(int ticks) {
        catalystTicks = ticks;
        catalystMax = ticks;
        resonanceMsg = "Resonance for " + ticks / 1200 + " minutes of work.";
        markResonanceDirty();
    }

    public static ModifierFunction recipeModifier(com.gregtechceu.gtceu.api.machine.MetaMachine machine,
                                                  GTRecipe recipe) {
        if (!(machine instanceof AetherCrucibleMachine crucible)) {
            return ModifierFunction.IDENTITY;
        }
        double time = 1.0;
        int parallels = 1;
        if (crucible.resonant()) {
            time = 0.70;
            parallels = 2;
        }
        if (crucible.getFormationTier() >= 1 && usesDrillingFluid(recipe)) time *= 0.85;
        if (time == 1.0 && parallels == 1) return ModifierFunction.IDENTITY;
        var builder = ModifierFunction.builder().durationMultiplier(time);
        if (parallels > 1) builder.parallels(parallels);
        return builder.build();
    }

    @Override
    public boolean onWorking() {
        boolean value = super.onWorking();
        if (catalystTicks > 0) {
            catalystTicks--;
            if (catalystTicks == 0) {
                catalystMax = 0;
                resonanceMsg = "The resonance has faded.";
                markResonanceDirty();
            } else if (catalystTicks % 20 == 0) {
                getSyncDataHolder().markClientSyncFieldDirty("catalystTicks");
            }
        }
        return value;
    }

    private void markResonanceDirty() {
        setChanged();
        var holder = getSyncDataHolder();
        if (holder == null) return;
        holder.markClientSyncFieldDirty("catalystTicks");
        holder.markClientSyncFieldDirty("catalystMax");
        holder.markClientSyncFieldDirty("resonanceMsg");
    }

    private void hudAction(int id, int value) {
        if (isRemote()) return;
        HudRecipes.handle(this, id);
    }

    @Override
    public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return net.phoenix.core.util.PatternErrorUi.with(this, syncManager,
                MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel));
    }

    private String modeName() {
        if (resonant()) return "Resonant";
        return getFormationTier() >= 1 ? "Aetheric" : "Base";
    }

    static String clock(int ticks) {
        int s = ticks / 20;
        return s / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60);
    }

    private HudModel hudModel() {
        return HudModel.of("Aether Crucible")
                .formed(this::isFormed)
                .theme(net.phoenixvine.wiki.ui.hud.HudTheme.TEAL)
                .badge(() -> HudRecipes.badge(this))
                .subtitle(() -> resonant() ? "Resonant: " + clock(catalystTicks) + " of work left" :
                        getFormationTier() >= 2 ? "Catalyst hatch ready: click it with a catalyst" :
                                getFormationTier() >= 1 ? "Aetheric: faster drilling-fluid recipes" :
                                        "Base: add a fluid hatch, or a catalyst hatch")
                .cells(() -> {
                    var cells = HudRecipes.cells(this);
                    cells.add(HudModel.cell("MODE", modeName(),
                            resonant() ? "x0.70 time, 2 parallel" :
                                    getFormationTier() >= 1 ? "x0.85 time on drilling fluid" : "no bonus",
                            resonant() ? HudModel.PURPLE : getFormationTier() >= 1 ? HudModel.CYAN : HudModel.DIM));
                    cells.add(HudModel.cell("CATALYST", catalystTicks > 0 ? clock(catalystTicks) : "none",
                            catalystTicks > 0 ? "counts while working" : getFormationTier() >= 2 ?
                                    "click the hatch with one" : "needs a catalyst hatch",
                            catalystTicks > 0 ? HudModel.GREEN : HudModel.DIM));
                    return cells;
                })
                .bars(() -> {
                    var bars = new ArrayList<HudModel.Bar>();
                    bars.add(new HudModel.Bar("Recipe", HudRecipes.progressPercent(this) / 100.0f,
                            String.format(java.util.Locale.ROOT, "%.0f%%", HudRecipes.progressPercent(this)),
                            HudModel.CYAN));
                    if (catalystMax > 0) {
                        bars.add(new HudModel.Bar("Resonance", catalystTicks / (float) catalystMax,
                                clock(catalystTicks), HudModel.PURPLE));
                    }
                    return bars;
                })
                .button(() -> getRecipeLogic().isWorkingEnabled() ? "PAUSE" : "RESUME", HudRecipes.TOGGLE,
                        HudModel.GOLD, this::isFormed)
                .notes(() -> {
                    var lines = new ArrayList<HudModel.Line>();
                    if (!resonanceMsg.isEmpty()) lines.add(HudModel.line(resonanceMsg, HudModel.MID));
                    lines.add(HudModel.line(getFormationTier() >= 2 ?
                            "Click the catalyst hatch holding a catalyst to crack its code." :
                            "A catalyst hatch lets the Crucible resonate.", HudModel.MID));
                    return lines;
                })
                .math(() -> {
                    var lines = new ArrayList<HudModel.Line>();
                    lines.add(HudModel.heading("FLUID HATCH (AETHERIC)"));
                    lines.add(HudModel.line("recipes that burn a blazing or voidic drilling fluid run x0.85 time",
                            HudModel.MID));
                    lines.add(HudModel.heading("CATALYST HATCH (RESONANT)"));
                    lines.add(HudModel.line("a cracked catalyst: x0.70 time and 2 parallel on every recipe",
                            HudModel.MID));
                    lines.add(HudModel.line("it counts down only while the Crucible works", HudModel.MID));
                    return lines;
                })
                .graph("progress %", HudModel.CYAN, () -> HudRecipes.progressPercent(this), 100.0f)
                .graph("EU/t", HudModel.ORANGE, () -> HudRecipes.euPerTick(this), 0.0f);
    }

    public static class AetherCatalystHatch extends MultiblockPartMachine
                                            implements
                                            net.phoenix.core.common.machine.multiblock.api.IMultiblockTierProvider,
                                            IMuiMachine {

        private static final int GLYPH = 10;
        private static final int DELETE = 20;

        @SaveField
        @SyncToClient
        private int puzzleTier = 0;

        @SaveField
        @SyncToClient
        private int attemptsLeft = 0;

        @SaveField
        @SyncToClient
        private String puzzleLog = "";

        @SaveField
        @SyncToClient
        private String puzzleCur = "";

        @SaveField
        @SyncToClient
        private String puzzleMsg = "";

        @SaveField
        private String puzzleSecret = "";

        @SaveField
        private String puzzlePlayer = "";

        @SaveField
        private long puzzleSeed = 0L;

        public AetherCatalystHatch(BlockEntityCreationInfo info, int tier) {
            super(info);
        }

        @Override
        public int getFormationTier() {
            return 2;
        }

        private AetherCrucibleMachine crucible() {
            for (var controller : getControllers()) {
                if (controller instanceof AetherCrucibleMachine c && c.isFormed()) return c;
            }
            return null;
        }

        @Override
        public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
            return AetherCatalysts.tierOf(player.getItemInHand(hand)) > 0 || puzzleTier > 0 &&
                    AetherCatalysts.tierOf(player.getMainHandItem()) > 0;
        }

        @Override
        public InteractionResult onUse(ExtendedUseOnContext context) {
            int tier = AetherCatalysts.tierOf(context.getItemInHand());
            if (tier > 0 && !isRemote()) {
                if (crucible() == null) {
                    context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component
                            .literal("The Aether Crucible is not formed."), true);
                    return InteractionResult.sidedSuccess(false);
                }
                begin(tier, context.getPlayer().getUUID());
            }
            return super.onUse(context);
        }

        private void begin(int tier, UUID player) {
            if (puzzleTier != 0) return;
            var grade = AetherCatalysts.grade(tier);
            puzzleSeed = puzzleSeed * 6364136223846793005L + 1442695040888963407L +
                    (getLevel() == null ? 0L : getLevel().getGameTime()) + getBlockPos().asLong();
            Random rng = new Random(puzzleSeed);
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < grade.codeLength(); i++) {
                code.append(AetherCatalysts.GLYPHS.charAt(rng.nextInt(grade.glyphs())));
            }
            puzzleSecret = code.toString();
            puzzleTier = tier;
            puzzlePlayer = player.toString();
            attemptsLeft = grade.attempts();
            puzzleLog = "";
            puzzleCur = "";
            puzzleMsg = grade.name() + " catalyst: crack its code. It is eaten only if you do.";
            markDirty();
        }

        private void markDirty() {
            setChanged();
            var holder = getSyncDataHolder();
            if (holder == null) return;
            for (String field : new String[] { "puzzleTier", "attemptsLeft", "puzzleLog", "puzzleCur",
                    "puzzleMsg" }) {
                holder.markClientSyncFieldDirty(field);
            }
        }

        private boolean eat(int tier) {
            if (getLevel() == null || puzzlePlayer.isEmpty()) return false;
            Player player;
            try {
                player = getLevel().getPlayerByUUID(UUID.fromString(puzzlePlayer));
            } catch (IllegalArgumentException e) {
                return false;
            }
            if (player == null) return false;
            for (var hand : InteractionHand.values()) {
                var held = player.getItemInHand(hand);
                if (AetherCatalysts.tierOf(held) == tier) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    return true;
                }
            }
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (AetherCatalysts.tierOf(inv.getItem(i)) == tier) {
                    if (!player.getAbilities().instabuild) inv.getItem(i).shrink(1);
                    return true;
                }
            }
            return false;
        }

        private void glyph(int g) {
            if (puzzleTier == 0) return;
            var grade = AetherCatalysts.grade(puzzleTier);
            if (g < 0 || g >= grade.glyphs() || puzzleCur.length() >= grade.codeLength()) return;
            puzzleCur += AetherCatalysts.GLYPHS.charAt(g);
            if (puzzleCur.length() == grade.codeLength()) submit(grade);
            markDirty();
        }

        private void submit(AetherCatalysts.Grade grade) {
            int exact = 0;
            int[] secretCount = new int[6];
            int[] guessCount = new int[6];
            for (int i = 0; i < grade.codeLength(); i++) {
                int s = AetherCatalysts.GLYPHS.indexOf(puzzleSecret.charAt(i));
                int g = AetherCatalysts.GLYPHS.indexOf(puzzleCur.charAt(i));
                if (s == g) {
                    exact++;
                } else {
                    secretCount[s]++;
                    guessCount[g]++;
                }
            }
            int near = 0;
            for (int i = 0; i < 6; i++) near += Math.min(secretCount[i], guessCount[i]);
            puzzleLog += (puzzleLog.isEmpty() ? "" : ";") + puzzleCur + ":" + exact + ":" + near;
            puzzleCur = "";
            attemptsLeft--;
            if (exact == grade.codeLength()) {
                var crucible = crucible();
                if (crucible == null) {
                    puzzleMsg = "Cracked, but the Crucible is no longer formed. The catalyst is kept.";
                } else if (eat(puzzleTier)) {
                    crucible.startResonance(grade.boostTicks());
                    puzzleMsg = "Cracked: the catalyst is eaten. Resonance for " + grade.boostTicks() / 1200 +
                            " minutes of work.";
                } else {
                    puzzleMsg = "Cracked, but the catalyst is no longer with you.";
                }
                puzzleTier = 0;
            } else if (attemptsLeft <= 0) {
                puzzleMsg = "The code held (it was " + puzzleSecret + "). The catalyst is kept: click again.";
                puzzleTier = 0;
            }
        }

        private void deleteGlyph() {
            if (puzzleTier == 0 || puzzleCur.isEmpty()) return;
            puzzleCur = puzzleCur.substring(0, puzzleCur.length() - 1);
            markDirty();
        }

        private void hudAction(int id, int value) {
            if (isRemote()) return;
            if (id >= GLYPH && id < GLYPH + 6) glyph(id - GLYPH);
            else if (id == DELETE) deleteGlyph();
        }

        @Override
        public ModularPanel<?> buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
            return net.phoenix.core.util.PatternErrorUi.with(this, syncManager,
                    MachineHud.build(this, data, syncManager, settings, this::hudAction, this::hudModel));
        }

        private static String spaced(String s) {
            StringBuilder out = new StringBuilder();
            for (char c : s.toCharArray()) out.append(c).append(' ');
            return out.toString().trim();
        }

        private HudModel hudModel() {
            HudModel model = HudModel.of("Aether Catalyst Hatch")
                    .theme(net.phoenixvine.wiki.ui.hud.HudTheme.TEAL)
                    .subtitle(() -> puzzleTier > 0 ?
                            AetherCatalysts.grade(puzzleTier).name() + " catalyst: crack the code" :
                            "Click the hatch holding a catalyst to start a code")
                    .cells(() -> {
                        var cells = new ArrayList<HudModel.Cell>();
                        if (puzzleTier > 0) {
                            var g = AetherCatalysts.grade(puzzleTier);
                            cells.add(HudModel.cell("CODE", g.codeLength() + " glyphs", "from " +
                                    AetherCatalysts.GLYPHS.substring(0, g.glyphs()), HudModel.CYAN));
                            cells.add(HudModel.cell("TRIES", Integer.toString(attemptsLeft), "left",
                                    attemptsLeft <= 2 ? HudModel.RED : HudModel.GREEN));
                            cells.add(HudModel.cell("RESONANCE", g.boostTicks() / 1200 + " min", "if cracked",
                                    HudModel.PURPLE));
                        }
                        return cells;
                    });
            for (int g = 0; g < 6; g++) {
                final int glyph = g;
                model.button("Glyph " + AetherCatalysts.GLYPHS.charAt(g), GLYPH + g, HudModel.CYAN,
                        () -> puzzleTier > 0 && glyph < AetherCatalysts.grade(puzzleTier).glyphs());
            }
            return model
                    .button("Delete glyph", DELETE, HudModel.RED, () -> puzzleTier > 0 && !puzzleCur.isEmpty())
                    .notes(() -> {
                        var lines = new ArrayList<HudModel.Line>();
                        if (puzzleTier > 0) {
                            var grade = AetherCatalysts.grade(puzzleTier);
                            for (String guess : puzzleLog.isEmpty() ? new String[0] : puzzleLog.split(";")) {
                                String[] p = guess.split(":");
                                lines.add(HudModel.line(spaced(p[0]) + "   " + p[1] + " right place, " + p[2] +
                                        " wrong place", HudModel.WHITE));
                            }
                            StringBuilder cur = new StringBuilder(puzzleCur);
                            while (cur.length() < grade.codeLength()) cur.append('_');
                            lines.add(HudModel.line(spaced(cur.toString()), HudModel.GOLD));
                        }
                        if (!puzzleMsg.isEmpty()) lines.add(HudModel.line(puzzleMsg, HudModel.MID));
                        return lines;
                    })
                    .math(() -> {
                        var lines = new ArrayList<HudModel.Line>();
                        lines.add(HudModel.heading("CATALYSTS"));
                        for (var g : AetherCatalysts.GRADES) {
                            lines.add(HudModel.line(g.name() + ": " + AetherCatalysts.itemOf(g.tier()) + ", " +
                                    g.codeLength() + " of " + g.glyphs() + ", " + g.attempts() + " tries, " +
                                    g.boostTicks() / 1200 + " min", HudModel.WHITE));
                        }
                        lines.add(HudModel.line("the catalyst is eaten only if you crack the code", HudModel.MID));
                        return lines;
                    });
        }
    }
}
