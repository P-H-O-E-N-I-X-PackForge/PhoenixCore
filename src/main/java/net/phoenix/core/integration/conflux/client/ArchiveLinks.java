package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.BiConsumer;

/**
 * Where Phoenix Archive plugs in to let the terminal open a lore entry. Archive has no 1.21.1 build yet, so this is
 * empty until its port sets {@link #opener}; the "Read lore" button only appears once it is.
 */
@OnlyIn(Dist.CLIENT)
public final class ArchiveLinks {

    private ArchiveLinks() {}

    /** Called with the terminal screen (to return to) and the {@code archive} unlock's entry id. */
    public static BiConsumer<Screen, String> opener;
}
