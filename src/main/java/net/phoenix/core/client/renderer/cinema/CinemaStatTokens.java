package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets a cinema screen line embed a live vanilla stat for whoever's looking at it (e.g.
 * "Deaths: {stat:deaths}") instead of only ever showing static text. Resolved per-viewer at
 * render time, client-side only - the stored line text is just the raw token, so each viewer sees
 * their OWN value, the same way a scoreboard sidebar would.
 */
public final class CinemaStatTokens {

    private CinemaStatTokens() {}

    private static final Pattern TOKEN = Pattern.compile("\\{stat:([a-zA-Z0-9_]+)}");

    private record StatEntry(Stat<?> stat, IntFunction<String> format) {}

    private static String formatCount(int value) {
        return String.valueOf(value);
    }

    private static String formatTicksAsTime(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    // Vanilla tracks distance-walked/swum/flown stats in centimeters (1 block = 100).
    private static String formatCmAsBlocks(int cm) {
        return String.format(Locale.ROOT, "%.0f blocks", cm / 100.0);
    }

    // Damage stats are tracked in tenths of a point (matching the float damage amounts used
    // elsewhere, e.g. a zombie's 2.5-damage hit is stored as 25).
    private static String formatTenthsAsDecimal(int tenths) {
        return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }

    private static final Map<String, StatEntry> STATS = Map.ofEntries(
            Map.entry("playtime", new StatEntry(Stats.CUSTOM.get(Stats.PLAY_TIME), CinemaStatTokens::formatTicksAsTime)),
            Map.entry("deaths", new StatEntry(Stats.CUSTOM.get(Stats.DEATHS), CinemaStatTokens::formatCount)),
            Map.entry("mobkills", new StatEntry(Stats.CUSTOM.get(Stats.MOB_KILLS), CinemaStatTokens::formatCount)),
            Map.entry("playerkills",
                    new StatEntry(Stats.CUSTOM.get(Stats.PLAYER_KILLS), CinemaStatTokens::formatCount)),
            Map.entry("jumps", new StatEntry(Stats.CUSTOM.get(Stats.JUMP), CinemaStatTokens::formatCount)),
            Map.entry("walked", new StatEntry(Stats.CUSTOM.get(Stats.WALK_ONE_CM), CinemaStatTokens::formatCmAsBlocks)),
            Map.entry("flown", new StatEntry(Stats.CUSTOM.get(Stats.FLY_ONE_CM), CinemaStatTokens::formatCmAsBlocks)),
            Map.entry("swum", new StatEntry(Stats.CUSTOM.get(Stats.SWIM_ONE_CM), CinemaStatTokens::formatCmAsBlocks)),
            Map.entry("damagedealt",
                    new StatEntry(Stats.CUSTOM.get(Stats.DAMAGE_DEALT), CinemaStatTokens::formatTenthsAsDecimal)),
            Map.entry("damagetaken",
                    new StatEntry(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN), CinemaStatTokens::formatTenthsAsDecimal)),
            Map.entry("animalsbred", new StatEntry(Stats.CUSTOM.get(Stats.ANIMALS_BRED), CinemaStatTokens::formatCount)),
            Map.entry("fishcaught", new StatEntry(Stats.CUSTOM.get(Stats.FISH_CAUGHT), CinemaStatTokens::formatCount)),
            Map.entry("trades",
                    new StatEntry(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER), CinemaStatTokens::formatCount)));

    public static String resolve(String text, LocalPlayer player) {
        if (player == null || text.indexOf('{') < 0) return text;

        Matcher matcher = TOKEN.matcher(text);
        if (!matcher.find()) return text;

        StringBuilder out = new StringBuilder();
        int last = 0;
        do {
            out.append(text, last, matcher.start());
            StatEntry entry = STATS.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (entry != null) {
                int value = player.getStats().getValue(entry.stat());
                out.append(entry.format().apply(value));
            } else {
                out.append(matcher.group());
            }
            last = matcher.end();
        } while (matcher.find());
        out.append(text, last, text.length());
        return out.toString();
    }
}
