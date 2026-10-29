package net.phoenix.core.common.block.cinema;

import java.util.ArrayList;
import java.util.List;

/**
 * What one page of a cinema screen holds, read from the text of the page. The text is plain, so it can be typed in the
 * game; {@code \n} (a backslash and an n, or a real line break) starts a new line:
 *
 * <pre>
 * The title\nThe body, which wraps to fit the screen.\n@button Open the picker | open_discipline_picker
 * </pre>
 *
 * The first line is the title when the page has more than one line, {@code # } at the start of the first line makes it
 * one even when it does not, and each {@code @button Label | action} line is a button (see CinemaClientActions for what an
 * action can be). A page of one plain line is just its text.
 * <p>
 * {@code @image namespace:path/to/texture.png | caption} puts a picture (a texture from a resource pack or mod) in a row
 * under the title, {@code @item minecraft:blaze_rod | caption} an item's icon; several lines make a row, each with its
 * caption under it.
 * <p>
 * A second {@code |} after the caption gives the action of a click on the picture ({@code @image mod:tex.png | Map | hud:solaris}).
 * <p>
 * {@code @continuum star ffcc66 | caption} is a live picture of a Continuum body ({@code star}, {@code black_hole},
 * {@code gas_cloud} or {@code protostar}, then an optional colour) that turns to follow the player looking at it; it is
 * kept as a picture whose id is {@code continuum:<body>:<colour>}.
 * <p>
 * {@code @scroll} (or {@code @scroll 0.4}, in blocks a second) makes the body rise slowly through the free part of the screen
 * and fade out at the top, like the poem at the end of a game, then start again. The title, pictures and buttons stay put.
 * <p>
 * The first page of a screen says "right-click to turn the page" under its arrows; a {@code @nohint} line takes that away
 * (for all but the first screen someone sees, say).
 * <p>
 * An {@code @if} line makes the page show only when its condition holds for the player looking (see CinemaConditions):
 * {@code @if no_discipline}, {@code @if discipline:phoenix}, {@code @if !committed}, {@code @if flag:px_awakened}.
 */
public record CinemaPage(String title, String body, List<Button> buttons, String condition, List<Picture> pictures,
                         boolean hint, float scroll) {

    public record Button(String label, String action) {}

    /** A picture on the page: a texture ({@code item} false) or an item's icon, what it is captioned, and what clicking it does (may be empty). */
    public record Picture(boolean item, String id, String caption, String action) {}

    public static CinemaPage parse(String raw) {
        String text = raw == null ? "" : raw.replace("\\n", "\n");
        String[] parts = text.split("\n", -1);
        String title = "";
        StringBuilder body = new StringBuilder();
        List<Button> buttons = new ArrayList<>();
        String condition = "";
        List<Picture> pictures = new ArrayList<>();
        boolean hint = true;
        float scroll = 0.0f;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("@scroll") || part.startsWith("@scroll ")) {
                scroll = 0.3f;
                try {
                    if (part.length() > 8) scroll = Float.parseFloat(part.substring(8).trim());
                } catch (NumberFormatException ignored) {}
            } else if (part.equals("@nohint")) {
                hint = false;
            } else if (part.startsWith("@continuum ")) {
                String spec = part.substring("@continuum ".length());
                int bar = spec.indexOf('|');
                String[] words = (bar < 0 ? spec : spec.substring(0, bar)).trim().split("\\s+");
                String[] tail = bar < 0 ? new String[] { "" } : spec.substring(bar + 1).split("\\|", 2);
                String caption = tail[0].trim();
                String action = tail.length > 1 ? tail[1].trim() : "";
                if (!words[0].isEmpty()) {
                    String colour = words.length > 1 ? words[1].replace("#", "") : "-";
                    pictures.add(new Picture(false, "continuum:" + words[0].toLowerCase() + ":" + colour, caption, action));
                }
            } else if (part.startsWith("@image ") || part.startsWith("@item ")) {
                boolean item = part.startsWith("@item ");
                String spec = part.substring(part.indexOf(' ') + 1);
                int bar = spec.indexOf('|');
                String id = (bar < 0 ? spec : spec.substring(0, bar)).trim();
                String[] tail = bar < 0 ? new String[] { "" } : spec.substring(bar + 1).split("\\|", 2);
                String caption = tail[0].trim();
                String action = tail.length > 1 ? tail[1].trim() : "";
                if (!id.isEmpty()) pictures.add(new Picture(item, id, caption, action));
            } else if (part.startsWith("@if ")) {
                condition = part.substring("@if ".length()).trim();
            } else if (part.startsWith("@button ")) {
                String spec = part.substring("@button ".length());
                int bar = spec.indexOf('|');
                String label = (bar < 0 ? spec : spec.substring(0, bar)).trim();
                String action = bar < 0 ? "next" : spec.substring(bar + 1).trim();
                if (!label.isEmpty()) buttons.add(new Button(label, action));
            } else if (i == 0 && (part.startsWith("# ") || parts.length > 1)) {
                title = part.startsWith("# ") ? part.substring(2) : part;
            } else {
                if (body.length() > 0) body.append('\n');
                body.append(part);
            }
        }
        return new CinemaPage(title.trim(), body.toString().trim(), buttons, condition, pictures, hint, scroll);
    }
}
