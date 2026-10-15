package net.phoenix.core.integration.conflux.dimension.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.common.data.GTOres;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.phoenix.core.integration.conflux.dimension.ConfluxDimensionFactory;

import java.util.Set;
import java.util.function.Consumer;

public class GTVeinPlacement {

    public static ResourceKey<Level> disciplineDimension(String disciplineId) {
        return ConfluxDimensionFactory.getDimensionKey(disciplineId);
    }

    public static GTOreDefinition registerVein(String disciplineId, String veinName, Consumer<GTOreDefinition> config) {
        GTOreDefinition def = GTOres.blankOreDefinition();

        def.dimensions(Set.of(disciplineDimension(disciplineId)));
        config.accept(def);
        def.register(new ResourceLocation("phoenixcore", disciplineId + "_" + veinName));
        return def;
    }
}
