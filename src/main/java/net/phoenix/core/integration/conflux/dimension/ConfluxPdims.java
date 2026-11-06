package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.continuum.api.PdimWorldgen;
import net.phoenix.core.PhoenixCore;

public final class ConfluxPdims {

    private ConfluxPdims() {}

    public static PdimWorldgen.Spec discipline(ResourceLocation body, String disciplineId) {
        return discipline(body, disciplineId, PhoenixCore.id(disciplineId));
    }

    public static PdimWorldgen.Spec discipline(ResourceLocation body, String disciplineId, ResourceLocation biome) {
        String json = """
                {
                  "type": "phoenixcore:discipline",
                  "discipline_id": "%s",
                  "seed": %d,
                  "biome_source": { "type": "minecraft:fixed", "biome": "%s" }
                }
                """.formatted(disciplineId, body.toString().hashCode() & 0x7fffffff, biome);
        return PdimWorldgen.of(body).generator(json).dimensionType(PhoenixCore.id("discipline"));
    }
}
