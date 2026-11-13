package net.phoenix.core.api;

import com.gregtechceu.gtceu.api.sound.SoundEntry;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixSounds {

    public static final RegistryEntry<SoundEntry, SoundEntry> MICROVERSE = REGISTRATE.sound("microverse").register();

    public static void init() {}
}
