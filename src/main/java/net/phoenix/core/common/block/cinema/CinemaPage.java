package net.phoenix.core.common.block.cinema;

import java.util.ArrayList;
import java.util.List;

public record CinemaPage(String title, String body, List<Button> buttons, String condition, List<Picture> pictures,
                         boolean hint, float scroll) {

    public record Button(String label, String action) {}

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
                    pictures.add(
                            new Picture(false, "continuum:" + words[0].toLowerCase() + ":" + colour, caption, action));
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
