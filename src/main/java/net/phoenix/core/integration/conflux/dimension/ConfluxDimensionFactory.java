package net.phoenix.core.integration.conflux.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxDimensionFactory {

    public static ResourceKey<Level> getDimensionKey(String disciplineId) {
        return ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation("phoenixcore", "conflux/" + disciplineId.toLowerCase()));
    }

    public static void enterDisciplineDimension(ServerPlayer player, UUID teamId, String discipline) {
        MinecraftServer server = player.getServer();
        ServerLevel dimensionLevel = server.getLevel(getDimensionKey(discipline));
        if (dimensionLevel == null) return;

        ServerLevel overworld = server.overworld();
        DisciplineProgressionData.get(overworld).getProgression(teamId);

        if (!dimensionLevel.getBlockState(DisciplineStartingArea.ANCHOR.offset(-3, 1, 0)).is(Blocks.CHEST)) {
            initializeStartingArea(dimensionLevel, discipline);
        }

        player.teleportTo(dimensionLevel, 0.5, 65, 0.5, 0, 0);

        player.setRespawnPosition(getDimensionKey(discipline), DisciplineStartingArea.ANCHOR, 0f, true, false);
    }

    public static void initializeStartingArea(ServerLevel dimensionLevel, String disciplineId) {
        DisciplineStartingArea startingArea = new DisciplineStartingArea(disciplineId);
        startingArea.generateStartingArea(dimensionLevel);
    }
}
