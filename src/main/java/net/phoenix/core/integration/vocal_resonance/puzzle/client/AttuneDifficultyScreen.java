package net.phoenix.core.integration.vocal_resonance.puzzle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.vocal_resonance.puzzle.AttuneDifficulty;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SAttuneStartPacket;

@OnlyIn(Dist.CLIENT)
public class AttuneDifficultyScreen extends Screen {

    private static final int ROW_H = 40;
    private static final int ROW_W = 320;

    private final BlockPos pos;
    private final int tuneLevel;

    public AttuneDifficultyScreen(BlockPos pos, int tuneLevel) {
        super(Component.literal("Tune the lock"));
        this.pos = pos;
        this.tuneLevel = tuneLevel;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int top() {
        return Math.max(40, (height - AttuneDifficulty.values().length * (ROW_H + 6)) / 2);
    }

    private int left() {
        return (width - ROW_W) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE60A0A18);
        g.drawCenteredString(font, "Tune the Harmonic Lock", width / 2, 12, 0xFFE8D8FF);
        g.drawCenteredString(font, tuneLevel == 0 ? "Clear Easy to wake it. Then come back for harder levels and boosts." :
                "Tuned to " + AttuneDifficulty.fromLevel(tuneLevel).label() +
                        ". A harder level clears with bigger boosts; failing one changes nothing.",
                width / 2, 25, 0xFF9A94B8);

        AttuneDifficulty[] all = AttuneDifficulty.values();
        for (int i = 0; i < all.length; i++) {
            AttuneDifficulty d = all[i];
            int y = top() + i * (ROW_H + 6);
            boolean open = d.level() <= tuneLevel + 1;
            boolean cleared = d.level() <= tuneLevel;
            boolean hover = open && mouseX >= left() && mouseX < left() + ROW_W && mouseY >= y && mouseY < y + ROW_H;

            g.fill(left(), y, left() + ROW_W, y + ROW_H, hover ? 0xFF2C2760 : open ? 0xFF1D1A40 : 0xFF121122);
            g.renderOutline(left(), y, ROW_W, ROW_H, open ? d.color() : 0xFF33304A);
            g.fill(left(), y, left() + 4, y + ROW_H, open ? d.color() : 0xFF33304A);

            g.drawString(font, d.label(), left() + 12, y + 6, open ? d.color() : 0xFF6C6C8A, false);
            String status = cleared ? "cleared" : open ? "next" : "locked";
            g.drawString(font, status, left() + ROW_W - 10 - font.width(status), y + 6,
                    cleared ? 0xFF7AFFB0 : open ? 0xFFE8D8FF : 0xFF6C6C8A, false);
            g.drawString(font, d.boost(), left() + 12, y + 20, open ? 0xFFB8B0D8 : 0xFF55556A, false);
        }
        g.drawCenteredString(font, "Esc to close", width / 2, top() + all.length * (ROW_H + 6) + 4, 0xFF6C6C8A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            AttuneDifficulty[] all = AttuneDifficulty.values();
            for (int i = 0; i < all.length; i++) {
                int y = top() + i * (ROW_H + 6);
                if (all[i].level() <= tuneLevel + 1 && mx >= left() && mx < left() + ROW_W && my >= y &&
                        my < y + ROW_H) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SAttuneStartPacket(pos, all[i].level()));
                    
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
