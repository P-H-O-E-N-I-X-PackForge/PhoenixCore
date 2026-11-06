package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenixvine.wiki.client.rich.RichBlock;
import net.phoenixvine.wiki.client.rich.WikiMarkdownParser;
import net.phoenixvine.wiki.client.rich.WikiRichTextRenderer;

import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class TextEditorScreen extends Screen {

    private static final int C_BTN = 0xFF1A1A24;
    private static final int C_BTN_HOV = 0xFF22222E;
    private static final int C_GREEN = 0xFF1A2A1A;
    private static final int MAX_LENGTH = 4000;

    private static final char[] COLOR_CODES = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f', 'r' };
    private static final int[] COLOR_VALUES = { 0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA,
            0xFFFFAA00, 0xFFAAAAAA, 0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55,
            0xFFFFFFFF, 0xFFFFFFFF };

    private static final String[] MD_LABELS = { "B", "I", "H", "-", "[]", "T", "R", "PB" };
    private static final String[] MD_INSERTS = { "**", "*", "# ", "- ", "[](url)", "[](tip:msg)", "{reset}",
            "\n---\n" };
    private static final String[] MD_TIPS = { "Bold: wrap text in ** **", "Italic: wrap text in * *", "Heading",
            "Bullet list", "Link", "Hover tooltip: [text](tip:message)", "Reset formatting", "Page break" };

    private static final String[] MD2_LABELS = { "1.", "[x]", ">", "`c`", "```", "Tbl", "!", ":::" };
    private static final String[] MD2_INSERTS = { "1. ", "- [ ] ", "> ", "`code`", "\n```\n\ncode\n```\n",
            "\n| A | B |\n|---|---|\n| a | b |\n", ":::warning Title\n\n:::\n", ":::spoiler Title\n\n:::\n" };
    private static final String[] MD2_TIPS = { "Numbered list", "Checklist", "Quote", "Inline code", "Code block",
            "Table", "Warning box", "Spoiler" };

    private final Screen parent;
    private final String heading;
    private final Consumer<String> onConfirm;
    private final String initial;
    private final int accent;

    private MultilineTextArea inputBox;
    private EditBox hexBox;

    private int pw, ph, px, py, btnY;
    private int editorW;

    private String previewSource = null;
    private List<RichBlock> previewBlocks = List.of();
    private int previewScroll = 0;

    public TextEditorScreen(Screen parent, String heading, String initial, Consumer<String> onConfirm, int accent) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.initial = initial;
        this.onConfirm = onConfirm;
        this.accent = accent;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    protected void init() {
        String keptText = inputBox != null ? inputBox.getValue() : initial;
        String keptHex = hexBox != null ? hexBox.getValue() : "";

        pw = Math.min(1000, width - 60);
        ph = Math.min(700, height - 60);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        btnY = py + ph - 24;
        editorW = pw * 11 / 20;

        inputBox = addRenderableWidget(new MultilineTextArea(font, px + 8, py + 26, editorW - 12, ph - 100,
                MAX_LENGTH));
        inputBox.setAccent(accent);
        inputBox.setValue(keptText);
        inputBox.seekToStart();
        setInitialFocus(inputBox);

        hexBox = new EditBox(font, px + 8 + font.width("Hex: "), btnY - 36, 58, 12, Component.empty());
        hexBox.setMaxLength(7);
        hexBox.setHint(Component.literal("#RRGGBB"));
        hexBox.setValue(keptHex);
        addRenderableWidget(hexBox);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        ConfluxTheme.refresh();
        g.fill(0, 0, width, height, ConfluxTheme.BG);
        g.fill(px, py, px + pw, py + ph, ConfluxTheme.PANEL);
        g.renderOutline(px, py, pw, ph, ConfluxTheme.BORDER);
        g.fill(px + 1, py, px + pw - 1, py + 2, accent);
        g.fillGradient(px + 1, py + 2, px + pw - 1, py + 22, ConfluxTheme.withAlpha(accent, 50), 0x00000000);

        g.drawString(font, "§eEditing §f" + heading, px + 10, py + 9, 0xFFFFFF, false);
        int used = inputBox.getValue().length();
        String count = used + " / " + MAX_LENGTH;
        g.drawString(font, (used >= MAX_LENGTH ? "§c" : "§8") + count, px + editorW - 8 - font.width(count), py + 9,
                0xFFFFFF, false);

        super.render(g, mx, my, pt);

        renderPreview(g);
        renderHexRow(g);
        renderColorAndFormats(g, mx, my);
        renderFormatButtons2(g, mx, my);

        int half = pw / 2 - 6;
        button(g, mx, my, px + 6, btnY, half, 16, "§a✓ Confirm", C_GREEN);
        button(g, mx, my, px + pw / 2 + 3, btnY, half, 16, "§c✕ Cancel", C_BTN);
    }

    private void renderPreview(GuiGraphics g) {
        int x = px + editorW + 4;
        int w = pw - editorW - 12;
        int top = py + 26, h = ph - 100;
        g.fill(x, top, x + w, top + h, ConfluxTheme.PANEL_DEEP);
        g.renderOutline(x, top, w, h, ConfluxTheme.BORDER);
        g.drawString(font, "§8Preview", x + 6, py + 12, 0xFFFFFF, false);

        String text = inputBox.getValue();
        if (!text.equals(previewSource)) {
            previewSource = text;
            try {
                previewBlocks = text.isEmpty() ? List.of() : WikiMarkdownParser.parse(text, false, true);
            } catch (RuntimeException e) {
                previewBlocks = List.of();
            }
        }
        int contentH = WikiRichTextRenderer.measureBlocksHeight(font, previewBlocks, w - 12, new HashSet<>());
        previewScroll = Math.max(0, Math.min(previewScroll, Math.max(0, contentH - (h - 8))));
        g.enableScissor(x + 1, top + 1, x + w - 1, top + h - 1);
        WikiRichTextRenderer.renderBlocks(g, font, previewBlocks, x + 6, top + 4, w - 12, previewScroll, top + 1,
                top + h - 1, accent, new HashSet<>());
        g.disableScissor();
    }

    private void renderHexRow(GuiGraphics g) {
        int rowY = btnY - 36;
        g.drawString(font, "§8Hex: ", px + 8, rowY + 2, 0xFFFFFF, false);
        int insX = px + 8 + font.width("Hex: ") + 60;
        String hexVal = hexBox.getValue().trim();
        if (hexVal.startsWith("#") && hexVal.length() == 7 && hexVal.substring(1).matches("[0-9a-fA-F]{6}")) {
            g.fill(insX + 44, rowY, insX + 58, rowY + 12, (int) Long.parseLong(hexVal.substring(1), 16) | 0xFF000000);
        }
        g.drawString(font, "§8{#RRGGBB} colours work too", insX + 62, rowY + 2, 0xFFFFFF, false);
    }

    private void renderColorAndFormats(GuiGraphics g, int mx, int my) {
        int rowY = btnY - 22;
        int boxX = px + 8 + font.width("Colors: ");
        g.drawString(font, "§8Colors: ", px + 8, rowY + 2, 0xFFFFFF, false);
        for (int i = 0; i < COLOR_CODES.length; i++) {
            int cx = boxX + i * 13;
            boolean hov = mx >= cx && mx < cx + 11 && my >= rowY && my < rowY + 11;
            g.fill(cx, rowY, cx + 11, rowY + 11, COLOR_VALUES[i]);
            g.renderOutline(cx, rowY, 11, 11, hov ? accent : 0xFF333333);
            if (hov) g.renderTooltip(font, Component.literal("§" + COLOR_CODES[i] + "§" + COLOR_CODES[i]), mx, my);
        }
        for (int i = 0; i < MD_LABELS.length; i++) {
            int bx = fmtX(i);
            boolean hov = mx >= bx && mx < bx + 18 && my >= rowY && my < rowY + 12;
            g.fill(bx, rowY, bx + 18, rowY + 12, hov ? C_BTN_HOV : C_BTN);
            if (hov) {
                g.fill(bx, rowY, bx + 18, rowY + 1, accent);
                g.renderTooltip(font, Component.literal(MD_TIPS[i]), mx, my);
            }
            String label = i == 0 ? "§l" + MD_LABELS[i] : i == 1 ? "§o" + MD_LABELS[i] : MD_LABELS[i];
            g.drawCenteredString(font, label, bx + 9, rowY + 2, hov ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
    }

    private int fmtX(int i) {
        int total = MD_LABELS.length * 20 - 2;
        return px + pw - 8 - total + i * 20;
    }

    private void renderFormatButtons2(GuiGraphics g, int mx, int my) {
        int rowY = btnY - 50;
        for (int i = 0; i < MD2_LABELS.length; i++) {
            int bx = md2X(i), bw = md2W(i);
            boolean hov = mx >= bx && mx < bx + bw && my >= rowY && my < rowY + 12;
            g.fill(bx, rowY, bx + bw, rowY + 12, hov ? C_BTN_HOV : C_BTN);
            if (hov) {
                g.fill(bx, rowY, bx + bw, rowY + 1, accent);
                g.renderTooltip(font, Component.literal(MD2_TIPS[i]), mx, my);
            }
            g.drawCenteredString(font, MD2_LABELS[i], bx + bw / 2, rowY + 2, hov ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
    }

    private int md2W(int i) {
        return Math.max(16, font.width(MD2_LABELS[i]) + 6);
    }

    private int md2X(int index) {
        int x = px + 8;
        for (int i = 0; i < index; i++) x += md2W(i) + 2;
        return x;
    }

    private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, int bg) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hov ? C_BTN_HOV : bg);
        if (hov) g.fill(x, y, x + w, y + 1, accent);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hov ? accent : ConfluxTheme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int half = pw / 2 - 6;
        if (mx >= px + 6 && mx < px + 6 + half && my >= btnY && my < btnY + 16) {
            confirm();
            return true;
        }
        if (mx >= px + (double) pw / 2 + 3 && mx < px + pw - 3 && my >= btnY && my < btnY + 16) {
            onClose();
            return true;
        }

        int hexRowY = btnY - 36;
        int insX = px + 8 + font.width("Hex: ") + 60;
        if (mx >= insX && mx < insX + 40 && my >= hexRowY - 1 && my < hexRowY + 13) {
            String hex = hexBox.getValue().trim();
            if (!hex.startsWith("#")) hex = "#" + hex;
            if (hex.length() == 7) {
                setInitialFocus(inputBox);
                inputBox.forceInsert("{" + hex.toUpperCase() + "}");
            }
            return true;
        }

        int rowY = btnY - 22;
        int boxX = px + 8 + font.width("Colors: ");
        for (int i = 0; i < COLOR_CODES.length; i++) {
            int cx = boxX + i * 13;
            if (mx >= cx && mx < cx + 11 && my >= rowY && my < rowY + 11) {
                setInitialFocus(inputBox);
                inputBox.forceInsert("§" + COLOR_CODES[i]);
                return true;
            }
        }
        for (int i = 0; i < MD_LABELS.length; i++) {
            int bx = fmtX(i);
            if (mx >= bx && mx < bx + 18 && my >= rowY && my < rowY + 12) {
                setInitialFocus(inputBox);
                inputBox.forceInsert(MD_INSERTS[i]);
                return true;
            }
        }
        int row2 = btnY - 50;
        for (int i = 0; i < MD2_LABELS.length; i++) {
            int bx = md2X(i), bw = md2W(i);
            if (mx >= bx && mx < bx + bw && my >= row2 && my < row2 + 12) {
                setInitialFocus(inputBox);
                inputBox.forceInsert(MD2_INSERTS[i]);
                return true;
            }
        }
        if (mx < px || mx >= px + pw || my < py || my >= py + ph) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double delta) {
        if (mx >= px + editorW) {
            previewScroll = Math.max(0, previewScroll - (int) (delta * 18));
            return true;
        }
        if (inputBox.scrollBy(delta)) return true;
        return super.mouseScrolled(mx, my, 0.0, delta);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (kc == GLFW.GLFW_KEY_ENTER && (mod & GLFW.GLFW_MOD_CONTROL) != 0) {
            confirm();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    private void confirm() {
        try {
            onConfirm.accept(inputBox.getValue());
        } finally {
            Minecraft.getInstance().setScreen(parent);
        }
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
