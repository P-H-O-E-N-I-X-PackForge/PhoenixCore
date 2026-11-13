package net.phoenix.core.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FxRegistry {

    private static final Map<String, FxRender> RENDERS = new LinkedHashMap<>();

    private FxRegistry() {}

    static synchronized void register(FxRender render) {
        RENDERS.put(render.id(), render);
    }

    public static synchronized FxRender get(String id) {
        return RENDERS.get(id);
    }

    public static synchronized List<FxRender> all() {
        return new ArrayList<>(RENDERS.values());
    }

    public static synchronized List<String> ids() {
        return new ArrayList<>(RENDERS.keySet());
    }

    public static synchronized void resetAll() {
        for (FxRender r : RENDERS.values()) r.reset();
    }
}
