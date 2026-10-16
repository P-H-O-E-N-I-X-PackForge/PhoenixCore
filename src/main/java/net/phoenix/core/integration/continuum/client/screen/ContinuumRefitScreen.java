package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.ContinuumRegistry;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.common.RocketRefit;
import net.phoenix.core.integration.continuum.common.RocketStats;
import net.phoenix.core.integration.continuum.network.C2SRefitPacket;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class ContinuumRefitScreen extends Screen {

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    private static final int W = 448;
    private static final int H = 260;
    private static final String[] MODULES = { RocketStats.OVERDRIVE, RocketStats.REINFORCED_HULL,
            RocketStats.HULL_INTERLOCK, RocketStats.STELLAR_SHIELD, RocketStats.SINGULARITY_SHIELD };

    private final List<Object[]> buttons = new ArrayList<>();
    private float scale = 1.0f;

    public ContinuumRefitScreen() {
        super(Component.literal("Rocket refit"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String moduleName(String upgrade) {
        return switch (upgrade) {
            case RocketStats.OVERDRIVE -> "Overdrive";
            case RocketStats.REINFORCED_HULL -> "Reinforced hull";
            case RocketStats.HULL_INTERLOCK -> "Hull interlock";
            case RocketStats.STELLAR_SHIELD -> "Stellar shielding";
            default -> "Singularity shielding";
        };
    }

    private static String moduleEffect(String upgrade) {
        return switch (upgrade) {
            case RocketStats.OVERDRIVE -> "-10% trip time a level";
            case RocketStats.REINFORCED_HULL -> "-25% wear a level";
            case RocketStats.HULL_INTERLOCK -> "refuses to launch worn out";
            case RocketStats.STELLAR_SHIELD -> "work at stars";
            default -> "work at black holes";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MapUi.refreshTheme();
        g.fill(0, 0, width, height, 0xCC000000);
        buttons.clear();

        scale = Math.max(0.5f, Math.min(1.5f, Math.min((width - 24.0f) / W, (height - 24.0f) / H)));
        double mx = mouseX / scale;
        double my = mouseY / scale;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);
        int x = (Math.round(width / scale) - W) / 2;
        int y = (Math.round(height / scale) - H) / 2;
        g.fill(x, y, x + W, y + H, MapUi.PANEL_SOLID);
        g.renderOutline(x, y, W, H, MapUi.PANEL_LINE);

        g.fill(x + 1, y + 1, x + W - 1, y + 25, MapUi.CHROME);
        g.fill(x + 1, y + 25, x + W - 1, y + 26, MapUi.PANEL_LINE);
        g.drawString(font, "Rocket refit", x + 10, y + 9, MapUi.TITLE);

        var player = Minecraft.getInstance().player;
        ItemStack rocket = player == null ? ItemStack.EMPTY : RocketRefit.heldRocket(player);
        if (rocket.isEmpty()) {
            RocketSprite.draw(g, x + W / 2.0f, y + 110, 70, 0.35f);
            g.drawCenteredString(font, "Hold a Continuum Rocket to refit it.", x + W / 2, y + 128, MapUi.WARN);
            g.drawCenteredString(font, "Sneak and use the rocket to open this screen.", x + W / 2, y + 142, MapUi.DIM);
            button(g, x + W / 2 - 60, y + H - 28, 120, "Close", true, "close", mx, my);
            g.pose().popPose();
            return;
        }

        int tier = RocketStats.tier(rocket);
        float wear = RocketStats.wear(rocket);
        String hull = RocketStats.tierName(tier);
        g.drawString(font, "Hull " + hull, x + W - 10 - font.width("Hull " + hull), y + 9, MapUi.GOOD);

        int top = y + 33;
        int bottom = y + H - 34;

        int lx = x + 10;
        int lw = 160;
        int lh = bottom - top;
        g.fill(lx, top, lx + lw, top + lh, MapUi.CHROME);
        g.renderOutline(lx, top, lw, lh, MapUi.BUTTON_LINE);
        RocketSprite.draw(g, lx + lw / 2.0f, top + 62, 48, 1.0f);
        g.drawCenteredString(font, hull, lx + lw / 2, top + 66, MapUi.TITLE);

        g.drawString(font, "Wear", lx + 8, top + 80, MapUi.DIM);
        String wearText = Math.round(wear * 100.0f) + "%";
        g.drawString(font, wearText, lx + lw - 8 - font.width(wearText), top + 80, MapUi.wearColor(wear));
        MapUi.bar(g, lx + 8, top + 91, lw - 16, 6, wear, MapUi.wearColor(wear));

        int ey = top + 104;
        ey = effect(g, lx + 8, ey, "Trips", Math.round(RocketStats.tripReduction(rocket) * 100.0f));
        ey = effect(g, lx + 8, ey, "Wear", Math.round((1.0f - RocketStats.wearFactor(rocket)) * 100.0f));
        effect(g, lx + 8, ey, "Failure risk", Math.round((1.0f - RocketStats.failureFactor(rocket)) * 100.0f));

        int hy = top + lh - 46;
        g.fill(lx + 6, hy - 4, lx + lw - 6, hy - 3, MapUi.PANEL_LINE);
        if (tier >= RocketStats.MAX_TIER) {
            g.drawCenteredString(font, "The best hull", lx + lw / 2, hy + 14, MapUi.GOOD);
        } else {
            Item kit = RocketRefit.refitKit(tier + 1);
            int have = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, kit);
            if (kit != null) g.renderItem(new ItemStack(kit), lx + 8, hy);
            g.drawString(font, hull + " -> " + RocketStats.tierName(tier + 1), lx + 28, hy + 4, MapUi.TEXT);
            button(g, lx + 8, hy + 20, lw - 16, "Refit hull  (kits: " + have + ")", have > 0, "tier", mx, my);
        }

        int rx = lx + lw + 10;
        int rw = x + W - 10 - rx;
        g.fill(rx, top, rx + rw, top + lh, MapUi.CHROME);
        g.renderOutline(rx, top, rw, lh, MapUi.BUTTON_LINE);
        g.drawString(font, "Modules", rx + 8, top + 7, MapUi.TITLE);
        int ty = top + 22;
        for (String upgrade : MODULES) {
            int level = RocketStats.level(rocket, upgrade);
            int cap = RocketStats.upgradeCap(tier, upgrade);
            Item module = RocketRefit.module(upgrade);
            int have = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, module);
            boolean canFit = cap > level && have > 0;

            g.fill(rx + 4, ty - 2, rx + rw - 4, ty - 1, MapUi.BUTTON_LINE);
            if (module != null) g.renderItem(new ItemStack(module), rx + 8, ty + 3);
            g.drawString(font, moduleName(upgrade), rx + 30, ty + 3, MapUi.TEXT);
            g.drawString(font, moduleEffect(upgrade), rx + 30, ty + 15, MapUi.DIM);

            int px = rx + 30 + font.width(moduleName(upgrade)) + 8;
            if (cap == 0) {
                g.drawString(font, "locked", px, ty + 3, MapUi.BAD);
            } else {
                for (int i = 0; i < cap; i++) {
                    int pxi = px + i * 9;
                    g.fill(pxi, ty + 3, pxi + 7, ty + 10, i < level ? MapUi.GOOD : MapUi.BUTTON_OFF);
                    g.renderOutline(pxi, ty + 3, 7, 7, MapUi.BUTTON_LINE);
                }
            }
            String label = cap == 0 ? "Needs hull" : level >= cap ? "Full" : "Fit  (" + have + ")";
            button(g, rx + rw - 82, ty + 4, 74, label, canFit, "fit:" + upgrade, mx, my);
            ty += 28;
        }

        int ry = top + lh - 26;
        g.fill(rx + 4, ry - 4, rx + rw - 4, ry - 3, MapUi.PANEL_LINE);
        Item repairKit = ContinuumRegistry.REPAIR_KIT.get();
        int kits = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, repairKit);
        g.renderItem(new ItemStack(repairKit), rx + 8, ry);
        g.drawString(font, "Repair  (-" + Math.round(PhoenixConfigs.INSTANCE.continuum.repairKitWear * 100.0) +
                "% wear)", rx + 30, ry + 4, MapUi.TEXT);
        button(g, rx + rw - 122, ry, 114, "Repair  (kits: " + kits + ")", wear > 0.0f && kits > 0, "repair", mx, my);

        button(g, x + W / 2 - 60, y + H - 28, 120, "Close", true, "close", mx, my);
        g.pose().popPose();
    }

    private int effect(GuiGraphics g, int x, int y, String label, int percent) {
        g.drawString(font, label, x, y, MapUi.DIM);
        String value = percent == 0 ? "-" : "-" + percent + "%";
        g.drawString(font, value, x + 144 - font.width(value), y, percent == 0 ? MapUi.DIM : MapUi.GOOD);
        return y + 12;
    }

    private int wrapped(GuiGraphics g, String text, int x, int y, int width, int color) {
        for (var line : font.split(Component.literal(text), width)) {
            g.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, boolean enabled, String id, double mx,
                        double my) {
        boolean hover = enabled && MapUi.inside(mx, my, x, y, w, 18);
        g.fill(x, y, x + w, y + 18, !enabled ? MapUi.BUTTON_OFF : hover ? MapUi.BUTTON_HOVER : MapUi.BUTTON);
        g.renderOutline(x, y, w, 18, enabled ? MapUi.PANEL_LINE : MapUi.BUTTON_LINE);
        g.drawCenteredString(font, label, x + w / 2, y + 5, enabled ? MapUi.TITLE : MapUi.DIM);
        if (enabled) buttons.add(new Object[] { id, x, y, w });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX / scale;
        double my = mouseY / scale;
        if (button == 0) {
            for (Object[] b : buttons) {
                if (!MapUi.inside(mx, my, (int) b[1], (int) b[2], (int) b[3], 18)) continue;
                ContinuumSounds.click();
                String id = (String) b[0];
                if (id.equals("close")) {
                    onClose();
                } else if (id.equals("tier")) {
                    PacketDistributor.sendToServer(new C2SRefitPacket(RocketRefit.Action.TIER_UP, ""));
                } else if (id.equals("repair")) {
                    PacketDistributor.sendToServer(new C2SRefitPacket(RocketRefit.Action.REPAIR, ""));
                } else if (id.startsWith("fit:")) {
                    PacketDistributor.sendToServer(new C2SRefitPacket(RocketRefit.Action.FIT, id.substring(4)));
                }
                return true;
            }
        } else if (button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
