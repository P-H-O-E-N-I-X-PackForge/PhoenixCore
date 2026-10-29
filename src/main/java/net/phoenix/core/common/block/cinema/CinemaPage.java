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
 */
public record CinemaPage(String title, String body, List<Button> buttons) {

    public record Button(String label, String action) {}

    public static CinemaPage parse(String raw) {
        String text = raw == null ? "" : raw.replace("\\n", "\n");
        String[] parts = text.split("\n", -1);
        String title = "";
        StringBuilder body = new StringBuilder();
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith("@button ")) {
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
        return new CinemaPage(title.trim(), body.toString().trim(), buttons);
    }
}
