package net.phoenix.core.api;

import net.phoenix.core.PhoenixCore;
import net.phoenix.core.configs.PhoenixConfigs;

import java.util.HashMap;
import java.util.Map;

public class PhoenixColors {
    public static final Map<Character, Integer> CUSTOM_FORMATTING = new HashMap<>();
    public static final ThreadLocal<Character> LAST_CODE = ThreadLocal.withInitial(() -> ' ');

    public static void registerCustomColor(char code, int hex) {
        CUSTOM_FORMATTING.put(Character.toLowerCase(code), hex);
    }
    public static void loadColorsFromConfig() {
        // Access the String array instead of the List
        String[] colorDefinitions = PhoenixConfigs.INSTANCE.colors.customColors;

        for (String entry : colorDefinitions) {
            if (entry == null || !entry.contains(":")) continue;

            try {
                String[] split = entry.split(":");
                if (split.length == 2) {
                    char codeChar = split[0].charAt(0);
                    String hexPart = split[1].replace("#", "").trim();
                    int colorInt = Integer.parseInt(hexPart, 16);

                    registerCustomColor(codeChar, colorInt);
                    PhoenixCore.LOGGER.info("Registered custom color '§{}' as #{}", codeChar, hexPart);
                }
            } catch (Exception e) {
                PhoenixCore.LOGGER.error("Failed to parse color config entry: {}", entry);
            }
        }
    }
}