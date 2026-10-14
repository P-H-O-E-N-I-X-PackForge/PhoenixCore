package net.phoenix.core.integration.astral.puzzle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.astral.puzzle.AstralCalibratedMachine;
import net.phoenix.core.integration.astral.puzzle.AstralSurveyItem;
import net.phoenix.core.integration.astral.puzzle.StarChart;

@OnlyIn(Dist.CLIENT)
public class SurveyScreen extends Screen {

    private final int[] stars;
    private final int[] known;
    private final int total;
    private final StarChart.Kind kind;
    private final String where;
    private final ItemStack stack;
    private StarMap map;

    public SurveyScreen(ItemStack stack) {
        super(Component.literal("Survey"));
        this.stack = stack;
        this.stars = AstralSurveyItem.stars(stack);
        this.known = AstralSurveyItem.known(stack);
        this.total = AstralSurveyItem.total(stack);
        this.kind = AstralSurveyItem.kind(stack);
        this.where = AstralSurveyItem.where(stack);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int size = Math.max(120, Math.min(Math.min(width - 60, height - 90), 300));
        map = new StarMap((width - size) / 2, 34, size);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE60A0A18);
        g.drawCenteredString(font, (kind == StarChart.Kind.CHAIN ? "Spinning Wheel" : "Loom") + " survey  -  " + where,
                width / 2, 12, 0xFFE8D8FF);
        g.drawCenteredString(font, known.length + " of " + total +
                " lines revealed. Channel the Ritual Pedestal with this in your inventory to learn more.", width / 2,
                23, 0xFF9A94B8);
        map.background(g);
        for (int packed : known) {
            int from = (packed >> 4) & 15;
            int to = packed & 15;
            if (from >= stars.length || to >= stars.length) continue;
            int x1 = map.px((stars[from] >> 4) & 15);
            int y1 = map.py(stars[from] & 15);
            int x2 = map.px((stars[to] >> 4) & 15);
            int y2 = map.py(stars[to] & 15);
            StarMap.line(g, x1, y1, x2, y2, StarMap.KNOWN_COLOR, kind == StarChart.Kind.CHAIN);
            if (kind == StarChart.Kind.CHAIN) {
                g.drawString(font, String.valueOf((packed >> 8) + 1), (x1 + x2) / 2 + 3, (y1 + y2) / 2 - 9, 0xFFFFE9B0,
                        false);
            }
        }
        for (int star : stars) {
            map.star(g, map.px((star >> 4) & 15), map.py(star & 15), StarMap.STAR_COLOR, false);
        }
        boolean phase = AstralSurveyItem.phaseKnown(stack);
        g.drawCenteredString(font, phase ?
                "Favoured moon: " + AstralCalibratedMachine.phaseName(AstralSurveyItem.favouredPhase(stack)) +
                        "  -  calibrate under it to align the machine" :
                "Favoured moon: unknown  -  reveal every line, then channel once more",
                width / 2, map.top() + map.size() + 12, phase ? 0xFFE0B0FF : 0xFF6C6C8A);
        g.drawCenteredString(font, "Esc to close", width / 2, map.top() + map.size() + 26, 0xFF6C6C8A);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
