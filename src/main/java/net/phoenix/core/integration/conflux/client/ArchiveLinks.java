package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public final class ArchiveLinks {

    private ArchiveLinks() {}

    public static BiConsumer<Screen, String> opener;
}
