package net.phoenix.core.integration.continuum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.ContinuumRegistry;
import net.phoenix.core.integration.continuum.client.ContinuumSounds;
import net.phoenix.core.integration.continuum.common.RocketRefit;
import net.phoenix.core.integration.continuum.common.RocketStats;
import net.phoenix.core.integration.continuum.network.C2SRefitPacket;
import net.phoenix.core.network.PhoenixNetwork;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContinuumRefitScreen extends Screen {

    private static final int W = 380;
    private static final int H = 312;
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

        scale = Math.max(0.4f, Math.min(1.0f, Math.min((width - 40.0f) / W, (height - 40.0f) / H)));
        double mx = mouseX / scale;
        double my = mouseY / scale;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);
        int x = (Math.round(width / scale) - W) / 2;
        int y = (Math.round(height / scale) - H) / 2;
        g.fill(x, y, x + W, y + H, MapUi.PANEL_SOLID);
        g.renderOutline(x, y, W, H, MapUi.PANEL_LINE);

        var player = Minecraft.getInstance().player;
        ItemStack rocket = player == null ? ItemStack.EMPTY : RocketRefit.heldRocket(player);
        if (rocket.isEmpty()) {
            g.drawString(font, "Rocket refit", x + 10, y + 9, MapUi.TITLE);
            g.drawString(font, "Hold a Continuum Rocket to refit it.", x + 10, y + 28, MapUi.WARN);
            button(g, x + 10, y + H - 26, W - 20, "Close", true, "close", mx, my);
            g.pose().popPose();
            return;
        }

        int tier = RocketStats.tier(rocket);
        g.drawString(font, "Rocket refit  -  hull " + RocketStats.tierName(tier), x + 10, y + 9, MapUi.TITLE);

        float wear = RocketStats.wear(rocket);
        g.drawString(font, "Wear " + Math.round(wear * 100.0f) + "%", x + 10, y + 26, MapUi.TEXT);
        MapUi.bar(g, x + 70, y + 27, W - 80, 6, wear, MapUi.wearColor(wear));
        int sy = wrapped(g, String.format(Locale.ROOT, "Trips -%d%%   Wear -%d%%   Failure risk -%d%%",
                Math.round(RocketStats.tripReduction(rocket) * 100.0f),
                Math.round((1.0f - RocketStats.wearFactor(rocket)) * 100.0f),
                Math.round((1.0f - RocketStats.failureFactor(rocket)) * 100.0f)), x + 10, y + 42, W - 20,
                MapUi.GOOD);
        g.fill(x + 10, sy + 4, x + W - 10, sy + 5, MapUi.PANEL_LINE);

        int ty = sy + 14;
        g.drawString(font, "Hull", x + 10, ty + 5, MapUi.DIM);
        if (tier >= RocketStats.MAX_TIER) {
            g.drawString(font, RocketStats.tierName(tier) + "  (the best hull)", x + 70, ty + 5, MapUi.GOOD);
        } else {
            Item kit = RocketRefit.refitKit(tier + 1);
            int have = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, kit);
            g.drawString(font, RocketStats.tierName(tier) + " -> " + RocketStats.tierName(tier + 1), x + 70, ty + 5,
                    MapUi.TEXT);
            button(g, x + W - 150, ty, 140, "Refit  (kit: " + have + ")", have > 0, "tier", mx, my);
        }
        ty += 26;
        ty = wrapped(g, "A better hull flies faster, wears less, fails less and reaches higher-tier bodies.", x + 10,
                ty, W - 20, MapUi.DIM);
        ty += 8;
        g.fill(x + 10, ty, x + W - 10, ty + 1, MapUi.PANEL_LINE);
        ty += 9;

        g.drawString(font, "Modules", x + 10, ty, MapUi.TITLE);
        ty += 16;
        for (String upgrade : MODULES) {
            int level = RocketStats.level(rocket, upgrade);
            int cap = RocketStats.upgradeCap(tier, upgrade);
            Item module = RocketRefit.module(upgrade);
            int have = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, module);
            boolean canFit = cap > level && have > 0;

            g.drawString(font, moduleName(upgrade), x + 10, ty + 1, MapUi.TEXT);
            g.drawString(font, level + " / " + cap, x + 150, ty + 1, cap == 0 ? MapUi.BAD : MapUi.GOOD);
            g.drawString(font, moduleEffect(upgrade), x + 10, ty + 12, MapUi.DIM);
            String label = cap == 0 ? "needs a better hull" : level >= cap ? "full" : "Fit  (" + have + ")";
            button(g, x + W - 130, ty, 120, label, canFit, "fit:" + upgrade, mx, my);
            ty += 28;
        }

        g.fill(x + 10, ty, x + W - 10, ty + 1, MapUi.PANEL_LINE);
        ty += 9;
        Item repairKit = ContinuumRegistry.REPAIR_KIT.get();
        int kits = player.getAbilities().instabuild ? 99 : RocketRefit.count(player, repairKit);
        g.drawString(font, "Repair  (-" + Math.round(PhoenixConfigs.INSTANCE.continuum.repairKitWear * 100.0) +
                "% wear)", x + 10, ty + 5, MapUi.TEXT);
        button(g, x + W - 130, ty, 120, "Repair  (kits: " + kits + ")", wear > 0.0f && kits > 0, "repair", mx, my);

        button(g, x + 10, y + H - 26, W - 20, "Close", true, "close", mx, my);
        g.pose().popPose();
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
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SRefitPacket(RocketRefit.Action.TIER_UP, ""));
                } else if (id.equals("repair")) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SRefitPacket(RocketRefit.Action.REPAIR, ""));
                } else if (id.startsWith("fit:")) {
                    PhoenixNetwork.CHANNEL.sendToServer(new C2SRefitPacket(RocketRefit.Action.FIT, id.substring(4)));
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
