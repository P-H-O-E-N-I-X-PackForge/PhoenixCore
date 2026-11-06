package net.phoenix.core.integration.ars_nouveau.logistics;

import com.gregtechceu.gtceu.api.pipenet.IPipeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.phoenix.core.PhoenixCore;

import java.util.Locale;

/**
 * The tiers of Source pipe. A better pipe passes more Source in a tick and wastes less of it over a long run.
 */
public enum SourcePipeTier implements IPipeType<SourcePipeData>, StringRepresentable {

    BASIC("source_pipe", "Source Pipe", 0.25f, 256, 0.005f, 0xFFB070FF),
    GILDED("gilded_source_pipe", "Gilded Source Pipe", 0.3125f, 1024, 0.0025f, 0xFFFFD95C),
    ARCANE("arcane_source_pipe", "Arcane Source Pipe", 0.375f, 4096, 0.001f, 0xFF5CE1FF);

    private final String blockId;
    private final String displayName;
    private final float thickness;
    private final int throughput;
    private final float lossPerBlock;
    private final int color;

    SourcePipeTier(String blockId, String displayName, float thickness, int throughput, float lossPerBlock,
                   int color) {
        this.blockId = blockId;
        this.displayName = displayName;
        this.thickness = thickness;
        this.throughput = throughput;
        this.lossPerBlock = lossPerBlock;
        this.color = color;
    }

    public String blockId() {
        return blockId;
    }

    public String displayName() {
        return displayName;
    }

    /** Source passed in one tick. */
    public int throughput() {
        return throughput;
    }

    /** The share of Source lost for each pipe it passes through. */
    public float lossPerBlock() {
        return lossPerBlock;
    }

    /** ARGB. */
    public int color() {
        return color;
    }

    @Override
    public float getThickness() {
        return thickness;
    }

    @Override
    public SourcePipeData modifyProperties(SourcePipeData base) {
        return base;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public ResourceLocation type() {
        return PhoenixCore.id(blockId);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
