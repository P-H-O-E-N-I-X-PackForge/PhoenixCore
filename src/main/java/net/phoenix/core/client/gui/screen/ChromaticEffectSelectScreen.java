package net.phoenix.core.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.phoenix.chromatic_codes.api.ChromaticEffectsRegistry;
import net.phoenix.chromatic_codes.config.ModConfig;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.SelectChromaticCodePacket;
import net.phoenixvine.wiki.theme.PhoenixTheme;

import java.util.ArrayList;
import java.util.List;

public class ChromaticEffectSelectScreen extends Screen {

    private final InteractionHand hand;
    private final List<Character> availableCodes = new ArrayList<>();
    private static final int ENTRY_HEIGHT = 18;
    private static final int LIST_TOP = 50;

    // Refreshed from the shared Phoenix theme at the top of every render() call.
    private int cAccent, cDim, cText, cHoverFill;

    // The list's content height is fully known up front (entries.size() is fixed once the config
    // is read in the constructor) - rather than clamp/scroll, we size the pose scale so every row
    // is guaranteed to fit on-screen at once, same idea as the rest of the Phoenix Suite.
    private float uiScale = 1f;
    private int vw, vh;

    public ChromaticEffectSelectScreen(InteractionHand hand) {
        super(Component.literal("Select Chromatic Effect"));
        this.hand = hand;

        for (String entry : ModConfig.INSTANCE.colors.customColors) {
            String codeStr = entry.split(":")[0];
            if (!codeStr.isEmpty()) {
                availableCodes.add(codeStr.charAt(0));
            }
        }

        for (String entry : ModConfig.INSTANCE.colors.customGradients) {
            String codeStr = entry.split(":")[0];
            if (!codeStr.isEmpty()) {
                availableCodes.add(codeStr.charAt(0));
            }
        }
    }

    @Override
    protected void init() {
        int neededH = LIST_TOP + availableCodes.size() * ENTRY_HEIGHT + 20;
        float neededW = 230f;
        uiScale = (width < neededW || height < neededH) ?
                Math.min(width / neededW, height / neededH) : 1f;
        uiScale = Math.max(0.1f, uiScale);
        vw = Math.round(width / uiScale);
        vh = Math.round(height / uiScale);

        refreshTheme();
    }

    private void refreshTheme() {
        PhoenixTheme t = PhoenixTheme.current();
        cAccent = t.accent.getColor();
        cDim = t.textDim.getColor();
        cText = t.text.getColor();
        cHoverFill = (t.accent.getColor() & 0x00FFFFFF) | 0x22000000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int rmx, int rmy, float partialTicks) {
        this.renderBackground(guiGraphics);
        refreshTheme();

        int mouseX = Math.round(rmx / uiScale);
        int mouseY = Math.round(rmy / uiScale);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1f);

        int centerX = vw / 2;
        int x = centerX - 100;
        int y = LIST_TOP;

        guiGraphics.drawCenteredString(this.font, "§lCHROMATIC_SPRAY_INTERFACE", centerX, 20, cAccent);
        guiGraphics.drawCenteredString(this.font, "§7Select active flux code", centerX, 32, cDim);

        for (Character code : availableCodes) {

            Component preview = ChromaticEffectsRegistry.parseCustomEffects("&" + code + " CODE_TYPE: " + code);

            boolean hovering = mouseX >= x && mouseX <= x + 200 && mouseY >= y && mouseY <= y + (ENTRY_HEIGHT - 2);

            if (hovering) {
                guiGraphics.fill(x - 5, y - 2, x + 205, y + ENTRY_HEIGHT - 2, cHoverFill);
            }

            guiGraphics.drawString(this.font, preview, x, y, cText);
            y += ENTRY_HEIGHT;
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double rmx, double rmy, int button) {
        double mouseX = rmx / uiScale;
        double mouseY = rmy / uiScale;
        int x = vw / 2 - 100;
        int yStart = LIST_TOP;

        for (int i = 0; i < availableCodes.size(); i++) {
            int entryY = yStart + (i * ENTRY_HEIGHT);

            if (mouseX >= x && mouseX <= x + 200 && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                char selectedCode = availableCodes.get(i);

                PhoenixNetwork.CHANNEL.sendToServer(new SelectChromaticCodePacket(hand, selectedCode));

                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
