package net.phoenix.core.integration.conflux.pipe;

import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.CapabilityManager;
import net.neoforged.neoforge.capabilities.CapabilityToken;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ConfluxDataCapability {

    public static final Capability<IConfluxDataHandler> DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private ConfluxDataCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IConfluxDataHandler.class);
    }
}
