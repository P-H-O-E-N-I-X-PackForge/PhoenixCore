package net.phoenix.core.client.gui.forge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.S2CEndingPacket;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class EndingLook {

    private EndingLook() {}

    public enum Look {

        EMBER,

        ECHO,

        NULL,

        IRON,

        MIRROR
    }

    public record Plan(Look first, Look second, int fromColor, int toColor) {

        public static final Plan DEFAULT = new Plan(Look.EMBER, Look.EMBER, 0xFF8A1F, 0xFFD25A);

        public boolean morph() {
            return first != second;
        }
    }

    public static Look of(@Nullable String discipline) {
        if (discipline == null) return Look.EMBER;
        return switch (discipline) {
            case "sculk" -> Look.ECHO;
            case "void" -> Look.NULL;
            case "sealed_a" -> Look.IRON;
            case "sealed_b" -> Look.MIRROR;
            default -> Look.EMBER;
        };
    }

    public static Plan plan(@Nullable S2CEndingPacket ending) {
        if (ending == null) return Plan.DEFAULT;
        String id = ending.id();
        int to = 0xFF000000 | ending.accent();
        int from = 0xFF000000 | ending.accent2();
        if (id.startsWith("all_")) {
            Look look = of(id.substring(4));
            return new Plan(look, look, to, to);
        }
        int split = id.indexOf("_to_");
        if (split > 0) return new Plan(of(id.substring(0, split)), of(id.substring(split + 4)), from, to);
        return Plan.DEFAULT;
    }
}
