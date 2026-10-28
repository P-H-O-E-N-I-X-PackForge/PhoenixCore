package net.phoenix.core.client.gui.hud;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What every recipe machine can show without knowing anything about it: status, progress, energy. */
public final class HudRecipes {

    private HudRecipes() {}

    /** The id of the "turn the machine on or off" control. */
    public static final int TOGGLE = 1000;

    public static HudModel.Badge badge(WorkableElectricMultiblockMachine m) {
        if (!m.isFormed()) return new HudModel.Badge("NOT FORMED", HudModel.RED);
        RecipeLogic logic = m.getRecipeLogic();
        if (!logic.isWorkingEnabled()) return new HudModel.Badge("PAUSED", HudModel.GOLD);
        return switch (logic.getStatus().name()) {
            case "WORKING" -> new HudModel.Badge("WORKING", HudModel.GREEN);
            case "WAITING" -> new HudModel.Badge("WAITING", HudModel.GOLD);
            case "SUSPEND" -> new HudModel.Badge("SUSPENDED", HudModel.RED);
            default -> new HudModel.Badge("IDLE", HudModel.DIM);
        };
    }

    /** The cells every recipe machine has: progress, energy use, stored energy. */
    public static List<HudModel.Cell> cells(WorkableElectricMultiblockMachine m) {
        List<HudModel.Cell> cells = new ArrayList<>();
        RecipeLogic logic = m.getRecipeLogic();
        double pct = logic.getMaxProgress() <= 0 ? 0.0 : logic.getProgress() * 100.0 / logic.getMaxProgress();
        cells.add(HudModel.cell("PROGRESS", String.format(Locale.ROOT, "%.0f%%", pct),
                logic.getProgress() / 20 + "s / " + logic.getMaxProgress() / 20 + "s", HudModel.CYAN));
        var recipe = logic.getLastRecipe();
        long eut = recipe == null ? 0L : recipe.getInputEUt().getTotalEU();
        cells.add(HudModel.cell("ENERGY USE", String.format(Locale.ROOT, "%,d EU/t", eut),
                logic.isWorking() ? "drawing" : "not drawing", HudModel.ORANGE));
        var container = m.getEnergyContainer();
        cells.add(HudModel.cell("STORED", String.format(Locale.ROOT, "%,d EU", container.getEnergyStored()),
                String.format(Locale.ROOT, "of %,d", container.getEnergyCapacity()), HudModel.BLUE));
        return cells;
    }

    public static float progressPercent(WorkableElectricMultiblockMachine m) {
        RecipeLogic logic = m.getRecipeLogic();
        return logic.getMaxProgress() <= 0 ? 0.0f : logic.getProgress() * 100.0f / logic.getMaxProgress();
    }

    public static float euPerTick(WorkableElectricMultiblockMachine m) {
        var recipe = m.getRecipeLogic().getLastRecipe();
        return recipe == null || !m.getRecipeLogic().isWorking() ? 0.0f : recipe.getInputEUt().getTotalEU();
    }

    /** The server side of the on/off control. */
    public static boolean handle(WorkableElectricMultiblockMachine m, int id) {
        if (id != TOGGLE) return false;
        m.getRecipeLogic().setWorkingEnabled(!m.getRecipeLogic().isWorkingEnabled());
        return true;
    }
}
