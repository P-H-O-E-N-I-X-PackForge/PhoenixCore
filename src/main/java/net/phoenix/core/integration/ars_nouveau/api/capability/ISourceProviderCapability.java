package net.phoenix.core.integration.ars_nouveau.api.capability;

import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.CapabilityManager;
import net.neoforged.neoforge.capabilities.CapabilityToken;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;

public interface ISourceProviderCapability {

    Capability<ISourceProviderCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    ISourceTile getSource();
}
