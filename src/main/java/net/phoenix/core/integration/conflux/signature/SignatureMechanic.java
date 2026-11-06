package net.phoenix.core.integration.conflux.signature;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public interface SignatureMechanic {

    default void onSecond(ServerLevel level, UUID team, SignatureDefinition definition) {}
}
