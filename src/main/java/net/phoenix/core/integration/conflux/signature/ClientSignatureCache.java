package net.phoenix.core.integration.conflux.signature;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClientSignatureCache {

    private static List<S2CSignaturePacket.Entry> entries = List.of();

    private ClientSignatureCache() {}

    public static void accept(List<S2CSignaturePacket.Entry> next) {
        entries = List.copyOf(next);
    }

    public static List<S2CSignaturePacket.Entry> entries() {
        return entries;
    }

    public static void clear() {
        entries = List.of();
    }
}
