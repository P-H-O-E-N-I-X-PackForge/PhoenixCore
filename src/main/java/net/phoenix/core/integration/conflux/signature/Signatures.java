package net.phoenix.core.integration.conflux.signature;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Signatures {

    private static final Set<UUID> DIRTY = new HashSet<>();

    private Signatures() {}

    public static double get(ServerLevel level, UUID team, String id) {
        return SignatureRegistry.INSTANCE.get(id).map(def -> SignatureData.get(level).get(team, def)).orElse(0.0);
    }

    public static void set(ServerLevel level, UUID team, String id, double value) {
        SignatureRegistry.INSTANCE.get(id).ifPresent(def -> {
            if (SignatureData.get(level).set(team, def, value)) DIRTY.add(team);
        });
    }

    public static void add(ServerLevel level, UUID team, String id, double amount) {
        SignatureRegistry.INSTANCE.get(id).ifPresent(def -> {
            SignatureData data = SignatureData.get(level);
            if (data.set(team, def, data.get(team, def) + amount)) DIRTY.add(team);
        });
    }

    public static int level(ServerLevel level, UUID team, String id) {
        return SignatureRegistry.INSTANCE.get(id).map(def -> def.levelIndex(SignatureData.get(level).get(team, def)))
                .orElse(0);
    }

    public static boolean reached(UUID team, String flag) {
        if (!flag.startsWith("sig:")) return false;
        String[] parts = flag.split(":");
        if (parts.length != 3) return false;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        Optional<SignatureDefinition> def = SignatureRegistry.INSTANCE.get(parts[1]);
        if (def.isEmpty()) return false;
        try {
            int wanted = Integer.parseInt(parts[2]);
            return def.get().levelIndex(SignatureData.get(server.overworld()).get(team, def.get())) >= wanted;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Set<String> flagsOf(UUID team, String discipline) {
        Set<String> out = new HashSet<>();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || discipline == null) return out;
        SignatureData data = SignatureData.get(server.overworld());
        for (SignatureDefinition def : SignatureRegistry.INSTANCE.forDiscipline(discipline)) {
            int at = def.levelIndex(data.get(team, def));
            for (int i = 0; i <= at; i++) out.add(def.flag(i));
        }
        return out;
    }

    static void takeDirtyAdd(UUID team) {
        DIRTY.add(team);
    }

    static Set<UUID> takeDirty() {
        Set<UUID> copy = new HashSet<>(DIRTY);
        DIRTY.clear();
        return copy;
    }
}
