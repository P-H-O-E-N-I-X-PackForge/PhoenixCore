package net.phoenix.core.integration.ae2;

import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ExpandedProxyContext {

    private static final Map<Object, ExpandedPatternBufferProxyPartMachine> OWNERS = Collections
            .synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<ExpandedPatternBufferProxyPartMachine> CURRENT = new ThreadLocal<>();

    private ExpandedProxyContext() {}

    public static void register(Object handlerList, ExpandedPatternBufferProxyPartMachine proxy) {
        if (handlerList != null) OWNERS.put(handlerList, proxy);
    }

    public static void enter(Object handlerList) {
        ExpandedPatternBufferProxyPartMachine proxy = OWNERS.get(handlerList);
        if (proxy == null) CURRENT.remove();
        else CURRENT.set(proxy);
    }

    public static void exit() {
        CURRENT.remove();
    }

    public static boolean blocked(MEPatternBufferPartMachine.InternalSlot slot) {
        var proxy = CURRENT.get();
        if (proxy == null || !(proxy.getBuffer() instanceof ExpandedPatternBufferPartMachine buffer)) return false;
        int index = buffer.indexOf(slot);
        return index >= 0 && !buffer.mayUse(proxy, index);
    }

    public static void wanted(MEPatternBufferPartMachine.InternalSlot slot) {
        var proxy = CURRENT.get();
        if (proxy == null || !(proxy.getBuffer() instanceof ExpandedPatternBufferPartMachine buffer)) return;
        int index = buffer.indexOf(slot);
        if (index >= 0) buffer.markWanted(proxy, index);
    }
}
