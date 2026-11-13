package net.phoenix.core.common.machine.multiblock.electric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class AetherCatalysts {

    private AetherCatalysts() {}

    public record Grade(int tier, String name, int codeLength, int glyphs, int attempts, int boostTicks) {}

    public static final Grade[] GRADES = {
            new Grade(1, "Lesser", 3, 4, 8, 20 * 60 * 5),
            new Grade(2, "Greater", 4, 5, 8, 20 * 60 * 20),
            new Grade(3, "Perfect", 5, 6, 9, 20 * 60 * 60),
    };

    private static final Map<String, Integer> ITEMS = Map.of(
            "minecraft:amethyst_shard", 1,
            "minecraft:echo_shard", 2,
            "minecraft:nether_star", 3);

    public static Grade grade(int tier) {
        return GRADES[Math.max(1, Math.min(GRADES.length, tier)) - 1];
    }

    public static int tierOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return ITEMS.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 0);
    }

    public static String itemOf(int tier) {
        for (var e : ITEMS.entrySet()) if (e.getValue() == tier) return e.getKey();
        return "?";
    }

    public static final String GLYPHS = "ABCDEF";
}
