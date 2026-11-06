package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.research.GatedRecipeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchUnlock;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class UnlockPickerScreen extends Screen {

    private static final String[] TYPES = { "item", "multiblock", "recipe", "recipe_tag", "flag", "archive",
            "world_stage", "recipe_modifier" };
    private static final String[] LABELS = { "Item", "Multiblock", "Recipe", "Recipe tag", "Flag", "Archive",
            "World stage", "Speed" };
    private static final String[] HELP = { "A reward item. Add *N to give more than one.",
            "A multiblock machine the research unlocks.",
            "One recipe, kept locked until this is researched: a machine recipe, or a crafting table one.",
            "A recipe tag that opens recipes.",
            "A research flag; recipe gates and nodes can look for it.", "An archive entry that becomes readable.",
            "A world stage that changes the discipline's world.",
            "Makes machines faster or slower, or change their power: 'duration 0.8 gtceu:macerator', 'eut 0.9 *', 'speed 1.25 *'. 0.8 is 20% less; -20% works too." };

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 280;
    private static final int ROW_H = 13;
    private static final int C_BG = 0xF00F0F1C;
    private static final int C_BORDER = 0xFF252538;

    private final Screen parent;
    private final Consumer<ResearchUnlock> onPick;
    private int typeIdx = 0;
    private List<String> candidates = List.of();
    private EditBox search;
    private int scroll = 0;
    private int px, py;

    public UnlockPickerScreen(Screen parent, Consumer<ResearchUnlock> onPick) {
        super(Component.literal("Pick an unlock"));
        this.parent = parent;
        this.onPick = onPick;
        refresh();
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        String keep = search == null ? "" : search.getValue();
        search = new EditBox(font, px + 10, py + 52, PANEL_W - 20, 14, Component.empty());
        search.setHint(Component.literal("search, or type a value"));
        search.setMaxLength(200);
        search.setValue(keep);
        search.setResponder(v -> scroll = 0);
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    private String type() {
        return TYPES[typeIdx];
    }

    private void refresh() {
        Set<String> out = new TreeSet<>();
        String type = type();
        for (ResearchNode n : ResearchTreeRegistry.INSTANCE.getAllNodes()) {
            for (ResearchUnlock u : n.unlocks) {
                if (u.type().equals(type) && !u.value().isBlank()) out.add(u.value());
            }
        }
        switch (type) {
            case "item" -> BuiltInRegistries.ITEM.keySet().forEach(id -> out.add(id.toString()));
            case "multiblock" -> com.gregtechceu.gtceu.api.registry.GTRegistries.MACHINES.entrySet().forEach(e -> {
                if (e.getValue() instanceof com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition) {
                    out.add(e.getKey().location().toString());
                }
            });
            case "recipe" -> {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    level.getRecipeManager().getRecipes().forEach(holder -> {
                        if (holder.value() instanceof com.gregtechceu.gtceu.api.recipe.GTRecipe ||
                                holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe) {
                            out.add(holder.id().toString());
                        }
                    });
                }
            }
            case "recipe_tag" -> out.addAll(ResearchTreeRegistry.INSTANCE.getGatedRecipeTags());
            case "flag" -> GatedRecipeRegistry.INSTANCE.getRules().forEach(rule -> out.add(rule.flag()));
            case "world_stage" -> out.add("initial");
            case "recipe_modifier" -> {
                out.add("duration 0.9 *");
                out.add("duration 0.8 *");
                out.add("eut 0.9 *");
                out.add("speed 1.25 *");
                BuiltInRegistries.RECIPE_TYPE.keySet().forEach(id -> {
                    out.add("duration 0.8 " + id);
                    out.add("eut 0.9 " + id);
                });
            }
            default -> {}
        }
        candidates = new ArrayList<>(out);
        scroll = 0;
    }

    private List<String> matches() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase();
        List<String> starts = new ArrayList<>(), contains = new ArrayList<>();
        for (String c : candidates) {
            String l = c.toLowerCase();
            if (q.isEmpty() || l.startsWith(q) || l.substring(l.indexOf(':') + 1).startsWith(q)) starts.add(c);
            else if (l.contains(q)) contains.add(c);
        }
        starts.addAll(contains);
        return starts;
    }

    private int listTop() {
        return py + 76;
    }

    private int listRows() {
        return (PANEL_H - 76 - 30) / ROW_H;
    }

    private int[] tabBounds(int i) {
        int w = PANEL_W / TYPES.length;
        return new int[] { px + i * w, py + 22, px + (i + 1) * w, py + 40 };
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xC0000000);
        g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, ConfluxTheme.accent());
        g.fill(px, py, px + PANEL_W, py + PANEL_H, C_BG);
        g.drawString(font, "§ePick what this node unlocks", px + 10, py + 8, 0xFFFFFF, false);

        for (int i = 0; i < TYPES.length; i++) {
            int[] b = tabBounds(i);
            boolean active = i == typeIdx;
            boolean hov = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            g.fill(b[0], b[1], b[2], b[3], active ? 0xFF22223A : hov ? 0xFF1A1A2C : 0xFF121222);
            g.renderOutline(b[0], b[1], b[2] - b[0], b[3] - b[1], active ? ConfluxTheme.accent() : C_BORDER);
            g.drawCenteredString(font, (active ? "§f" : "§7") + LABELS[i], (b[0] + b[2]) / 2, b[1] + 5, 0xFFFFFF);
        }
        g.drawString(font, "§8" + HELP[typeIdx], px + 10, py + 43, 0xFFFFFF, false);

        List<String> list = matches();
        String typed = search.getValue().trim();
        boolean custom = !typed.isEmpty() && !list.contains(typed);
        int rows = listRows();
        int top = listTop();
        g.fill(px + 8, top - 2, px + PANEL_W - 8, top + rows * ROW_H + 2, 0xFF0A0A14);

        int shown = 0;
        int total = list.size() + (custom ? 1 : 0);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rows)));
        for (int i = scroll; i < total && shown < rows; i++, shown++) {
            int y = top + shown * ROW_H;
            boolean hov = mx >= px + 8 && mx < px + PANEL_W - 8 && my >= y && my < y + ROW_H;
            if (hov) g.fill(px + 8, y, px + PANEL_W - 8, y + ROW_H, 0xFF22223A);
            if (custom && i == 0) {
                g.drawString(font, "§a+ use \"" + typed + "\" as typed", px + 12, y + 3, 0xFFFFFF, false);
                continue;
            }
            String value = list.get(custom ? i - 1 : i);
            int textX = px + 12;
            if (type().equals("item")) {
                var item = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.tryParse(value));
                if (item.isPresent()) {
                    g.pose().pushPose();
                    g.pose().translate(px + 10, y - 1, 0);
                    g.pose().scale(0.75f, 0.75f, 1f);
                    g.renderItem(new ItemStack(item.get()), 0, 0);
                    g.pose().popPose();
                    textX = px + 26;
                }
            }
            g.drawString(font, font.plainSubstrByWidth(value, PANEL_W - 40 - (textX - px - 12)), textX, y + 3,
                    0xFFFFFF, false);
        }
        if (total == 0) g.drawString(font, "§8nothing known for this type yet: type a value", px + 12, top + 3,
                0xFFFFFF, false);
        g.drawString(font, "§8" + total + " entries · click to add · Esc to cancel", px + 10, py + PANEL_H - 16,
                0xFFFFFF, false);

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn != 0) return false;
        for (int i = 0; i < TYPES.length; i++) {
            int[] b = tabBounds(i);
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                typeIdx = i;
                search.setValue("");
                refresh();
                return true;
            }
        }
        List<String> list = matches();
        String typed = search.getValue().trim();
        boolean custom = !typed.isEmpty() && !list.contains(typed);
        int total = list.size() + (custom ? 1 : 0);
        if (mx >= px + 8 && mx < px + PANEL_W - 8 && my >= listTop() && my < listTop() + listRows() * ROW_H) {
            int i = scroll + (int) ((my - listTop()) / ROW_H);
            if (i < total) {
                String value = custom ? (i == 0 ? typed : list.get(i - 1)) : list.get(i);
                finish(value);
                return true;
            }
        }
        return false;
    }

    private void finish(String value) {
        onPick.accept(new ResearchUnlock(type(), value));
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        return true;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if ((kc == GLFW.GLFW_KEY_ENTER || kc == GLFW.GLFW_KEY_KP_ENTER) && !search.getValue().isBlank()) {
            List<String> list = matches();
            finish(list.size() == 1 ? list.get(0) : search.getValue().trim());
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
