package net.phoenix.core.client.gui.forge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.S2CEndingPacket;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ClientEnding {

    private static S2CEndingPacket current;

    private ClientEnding() {}

    public static void set(S2CEndingPacket ending) {
        current = ending;
    }

    public static @Nullable S2CEndingPacket current() {
        return current;
    }
}
