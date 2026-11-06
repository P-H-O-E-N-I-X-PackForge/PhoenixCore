package net.phoenix.core.integration.conflux.signature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SignatureMechanics {

    private static final Map<String, List<SignatureMechanic>> BY_ID = new HashMap<>();

    private SignatureMechanics() {}

    public static void register(String signatureId, SignatureMechanic mechanic) {
        BY_ID.computeIfAbsent(signatureId, k -> new ArrayList<>()).add(mechanic);
    }

    static List<SignatureMechanic> of(String signatureId) {
        return BY_ID.getOrDefault(signatureId, List.of());
    }
}
