package net.phoenix.core.common.data.materials;

import net.phoenix.core.PhoenixCore;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MaterialLang {

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    private MaterialLang() {}

    public static void put(String id, String name) {
        NAMES.put(id, name);
    }

    public static void register() {
        NAMES.forEach((id, name) -> PhoenixCore.PHOENIX_REGISTRATE.addRawLang("material.phoenixcore." + id, name));
    }
}
