package net.phoenix.core.client.render.structure;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Ported from Phantasia's {@code net.phoenixvine.phantasia.client.render.PhantasiaDummyWorld}
 * (verbatim - pure vanilla {@code Level} shim, no Phantasia-specific logic). A fake {@code Level}
 * that reports {@code Blocks.AIR} everywhere by default (subclasses override with real data) and
 * proxies everything else (registries, recipe manager, biomes, ticks) to a real backing level.
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class PhoenixDummyWorld extends Level {

    protected final PhoenixDummyChunkSource chunkSource;
    protected final LevelLightEngine lightEngine;
    protected final BiomeManager biomeManager;
    public final WeakReference<Level> proxyLevel;

    public PhoenixDummyWorld() {
        this(Minecraft.getInstance().level);
    }

    public PhoenixDummyWorld(Level proxy) {
        super(
                (WritableLevelData) proxy.getLevelData(),
                proxy.dimension(),
                proxy.registryAccess(),
                proxy.dimensionTypeRegistration(),
                proxy::getProfiler,
                true,
                false,
                0L,
                0);
        this.proxyLevel = new WeakReference<>(proxy);
        this.chunkSource = new PhoenixDummyChunkSource(this);
        this.lightEngine = new LevelLightEngine(chunkSource, true, false);
        this.biomeManager = new BiomeManager(this, 0L);
    }

    protected Level getProxy() {
        Level l = proxyLevel.get();
        if (l == null) {
            l = Minecraft.getInstance().level;
        }
        return l;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        return false;
    }

    @Override
    public void setBlockEntity(BlockEntity be) {}

    @Override
    public LevelLightEngine getLightEngine() {
        Level proxy = proxyLevel.get();
        return proxy != null ? proxy.getLightEngine() : lightEngine;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int skyDarken) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return true;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return switch (direction) {
            case DOWN, UP -> 0.9f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
        };
    }

    @Override
    public ChunkSource getChunkSource() {
        return chunkSource;
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return true;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        return super.getBiome(pos.offset(Vec3i.ZERO));
    }

    @Override
    public BiomeManager getBiomeManager() {
        return biomeManager;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return getProxy().getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return getProxy().getNoiseBiome(x, y, z);
    }

    @Override
    public RegistryAccess registryAccess() {
        return getProxy().registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return getProxy().enabledFeatures();
    }

    @Override
    public RecipeManager getRecipeManager() {
        return getProxy().getRecipeManager();
    }

    @Override
    public Scoreboard getScoreboard() {
        return getProxy().getScoreboard();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return getProxy().getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return getProxy().getFluidTicks();
    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return new LevelEntityGetter<>() {

            @Override
            public @Nullable Entity get(int id) {
                return null;
            }

            @Override
            public @Nullable Entity get(UUID uuid) {
                return null;
            }

            @Override
            public Iterable<Entity> getAll() {
                return Collections.emptyList();
            }

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test,
                                               AbortableIterationConsumer<U> consumer) {}

            @Override
            public void get(AABB box, Consumer<Entity> consumer) {}

            @Override
            public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AABB box,
                                               AbortableIterationConsumer<U> consumer) {}
        };
    }

    @Override
    public void playSound(@Nullable Player player, double x, double y, double z, SoundEvent sound, SoundSource cat,
                          float vol, float pitch) {}

    @Override
    public void playSound(@Nullable Player player, Entity entity, SoundEvent sound, SoundSource cat, float vol,
                          float pitch) {}

    @Override
    public void playSeededSound(@Nullable Player player, double x, double y, double z, Holder<SoundEvent> sound,
                                SoundSource cat, float vol, float pitch, long seed) {}

    @Override
    public void playSeededSound(@Nullable Player player, double x, double y, double z, SoundEvent sound,
                                SoundSource cat, float vol, float pitch, long seed) {}

    @Override
    public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> sound, SoundSource cat,
                                float vol, float pitch, long seed) {}

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState now, int flags) {}

    @Override
    public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {}

    @Override
    public void gameEvent(GameEvent event, Vec3 pos, GameEvent.Context ctx) {}

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos) {}

    @Override
    public void addParticle(ParticleOptions data, double x, double y, double z, double vx, double vy, double vz) {}

    @Override
    public void addParticle(ParticleOptions data, boolean force, double x, double y, double z, double vx, double vy,
                            double vz) {}

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions data, double x, double y, double z, double vx, double vy,
                                         double vz) {}

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions data, boolean force, double x, double y, double z, double vx,
                                         double vy, double vz) {}

    @Override
    public int getFreeMapId() {
        return 0;
    }

    @Override
    @Nullable
    public MapItemSavedData getMapData(String name) {
        return null;
    }

    @Override
    public void setMapData(String id, MapItemSavedData data) {}

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}

    @Override
    public String gatherChunkSourceStats() {
        return "";
    }
}
