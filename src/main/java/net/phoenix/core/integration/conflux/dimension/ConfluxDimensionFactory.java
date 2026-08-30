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

/**
 * Each discipline is one shared, permanent dimension - like the vanilla Nether/End, every
 * team that picks a given discipline enters the same instance. These dimensions are
 * registered statically via datapack JSON at server start (see DisciplineChunkGenerator),
 * not created dynamically per team, because GT registers each ore vein against a fixed
 * dimension key at mod load time; a per-team dimension whose key isn't known until a team
 * actually creates one could never match a vein's registered dimension set, so under the
 * old per-team-instance design GT ore veins silently never generated at all.
 */
@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxDimensionFactory {

    public static ResourceKey<Level> getDimensionKey(String disciplineId) {
        return ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation("phoenixcore", "conflux/" + disciplineId.toLowerCase()));
    }

    /**
     * Sends a player into their discipline's shared dimension, generating the starting area
     * the first time anyone ever enters it. Shared by the ethereal spawn picker's initial
     * choice and by mid-game discipline switches (via research unlock).
     */
    public static void enterDisciplineDimension(ServerPlayer player, UUID teamId, String discipline) {
        MinecraftServer server = player.getServer();
        ServerLevel dimensionLevel = server.getLevel(getDimensionKey(discipline));
        if (dimensionLevel == null) return; // discipline dimension JSON missing/failed to load

        ServerLevel overworld = server.overworld();
        DisciplineProgressionData.get(overworld).getProgression(teamId);

        // The starting platform sits at a fixed spot in this SHARED dimension - generate it
        // once ever, not once per team, or the second team to arrive would regenerate the
        // same platform on top of whatever the first team already built there. Checking ANCHOR
        // itself for air doesn't work: with real terrain now generated here, ANCHOR sits inside
        // naturally solid ground for most disciplines (baseY 66-72) even before anyone's ever
        // visited, so the very first player in would find "not air", wrongly conclude the
        // platform already existed, skip building it, and get teleported straight into solid
        // rock. The starter chest can only exist if the platform actually got built, so checking
        // for it instead is a reliable "has this ever been initialized" signal.
        if (!dimensionLevel.getBlockState(DisciplineStartingArea.ANCHOR.offset(-3, 1, 0)).is(Blocks.CHEST)) {
            initializeStartingArea(dimensionLevel, discipline);
        }

        player.teleportTo(dimensionLevel, 0.5, 65, 0.5, 0, 0);

        // Players are teleported here directly rather than sleeping in a bed, so vanilla never
        // gets a respawn point set for this dimension on its own - without this, dying here
        // would respawn the player back in the overworld instead of keeping them where they
        // actually live, which defeats the "this is a real place to play in" design.
        player.setRespawnPosition(getDimensionKey(discipline), DisciplineStartingArea.ANCHOR, 0f, true, false);
    }

    public static void initializeStartingArea(ServerLevel dimensionLevel, String disciplineId) {
        DisciplineStartingArea startingArea = new DisciplineStartingArea(disciplineId);
        startingArea.generateStartingArea(dimensionLevel);
    }
}
