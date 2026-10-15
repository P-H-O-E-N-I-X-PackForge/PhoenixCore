package net.phoenix.core.integration.conflux.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.StructureManager;
import net.phoenix.core.PhoenixCore;

public class ConfluxPiece extends StructurePiece {

    private static final ResourceLocation LOOT = PhoenixCore.id("chests/ethereal_spawn");

    public enum Kind {

        WATCHTOWER("watchtower", 4, 4, 3, 17),
        
        COMPOUND("compound", 11, 11, 4, 11),
        
        SHRINE("shrine", 8, 8, 3, 12),
        
        HALL("hall", 8, 5, 3, 13);

        private final String id;
        private final int radiusX;
        private final int radiusZ;
        private final int maxSlope;
        private final int height;

        Kind(String id, int radiusX, int radiusZ, int maxSlope, int height) {
            this.id = id;
            this.radiusX = radiusX;
            this.radiusZ = radiusZ;
            this.maxSlope = maxSlope;
            this.height = height;
        }

        public int radiusX() {
            return radiusX;
        }

        public int radiusZ() {
            return radiusZ;
        }

        public int maxSlope() {
            return maxSlope;
        }

        public static Kind byName(String name) {
            for (Kind k : values()) if (k.id.equals(name)) return k;
            return WATCHTOWER;
        }
    }

    private record Theme(BlockState wall, BlockState wallAlt, BlockState floor, BlockState pillar, BlockState slab,
                         BlockState light, BlockState accent, BlockState window, BlockState focus,
                         BlockState hanging) {}

    private static Theme themeOf(String id) {
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        return switch (id) {
            case "sculk" -> new Theme(Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(), Blocks.SCULK.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(), Blocks.VERDANT_FROGLIGHT.defaultBlockState(), soulLantern);
            case "void" -> new Theme(Blocks.END_STONE_BRICKS.defaultBlockState(),
                    Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.PURPUR_PILLAR.defaultBlockState(), Blocks.PURPUR_SLAB.defaultBlockState(),
                    Blocks.END_ROD.defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    Blocks.PURPLE_STAINED_GLASS_PANE.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    lantern);
            case "sealed_a" -> new Theme(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
                    Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.SMOOTH_STONE.defaultBlockState(),
                    Blocks.IRON_BLOCK.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(), Blocks.COPPER_BLOCK.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(), Blocks.COPPER_BLOCK.defaultBlockState(), lantern);
            case "sealed_b" -> new Theme(Blocks.NETHER_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(),
                    Blocks.WARPED_STEM.defaultBlockState(), Blocks.NETHER_BRICK_SLAB.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(), Blocks.WARPED_WART_BLOCK.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(), Blocks.SOUL_CAMPFIRE.defaultBlockState(), soulLantern);
            default -> new Theme(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BASALT.defaultBlockState(),
                    Blocks.BLACKSTONE_SLAB.defaultBlockState(), Blocks.LANTERN.defaultBlockState(),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.IRON_BARS.defaultBlockState(),
                    Blocks.LAVA_CAULDRON.defaultBlockState(), lantern);
        };
    }

    private final Kind kind;
    private final String themeId;
    private final BlockPos origin;
    private final long seed;

    public ConfluxPiece(Kind kind, String themeId, BlockPos origin, long seed) {
        super(ConfluxStructures.PIECE.get(), 0, boxFor(kind, origin));
        this.kind = kind;
        this.themeId = themeId;
        this.origin = origin;
        this.seed = seed;
    }

    public ConfluxPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ConfluxStructures.PIECE.get(), tag);
        this.kind = Kind.byName(tag.getString("Kind"));
        this.themeId = tag.getString("Theme");
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox boxFor(Kind kind, BlockPos o) {
        return new BoundingBox(o.getX() - kind.radiusX - 1, o.getY() - 14, o.getZ() - kind.radiusZ - 1,
                o.getX() + kind.radiusX + 1, o.getY() + kind.height, o.getZ() + kind.radiusZ + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", kind.id);
        tag.putString("Theme", themeId);
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Pen pen = new Pen(level, box, random, themeOf(themeId));
        pen.prepare(kind);
        switch (kind) {
            case WATCHTOWER -> tower(pen);
            case COMPOUND -> compound(pen);
            case SHRINE -> shrine(pen);
            case HALL -> hall(pen);
        }
    }

    private int hash(int x, int y, int z) {
        long h = seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (int) (h >>> 28) & 0x7FFFFFFF;
    }

    private final class Pen {

        final WorldGenLevel level;
        final BoundingBox box;
        final RandomSource random;
        final Theme t;

        Pen(WorldGenLevel level, BoundingBox box, RandomSource random, Theme theme) {
            this.level = level;
            this.box = box;
            this.random = random;
            this.t = theme;
        }

        void set(BlockState state, int dx, int dy, int dz) {
            BlockPos p = origin.offset(dx, dy, dz);
            if (box.isInside(p)) level.setBlock(p, state, 2);
        }

        void air(int dx, int dy, int dz) {
            set(Blocks.AIR.defaultBlockState(), dx, dy, dz);
        }

        boolean isGround(int dx, int dy, int dz) {
            BlockPos p = origin.offset(dx, dy, dz);
            BlockState s = level.getBlockState(p);
            return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced();
        }

        void chest(int dx, int dy, int dz) {
            BlockPos p = origin.offset(dx, dy, dz);
            createChest(level, box, random, p, LOOT, null);
        }

        void prepare(Kind k) {
            for (int dx = -k.radiusX; dx <= k.radiusX; dx++) {
                for (int dz = -k.radiusZ; dz <= k.radiusZ; dz++) {
                    BlockPos column = origin.offset(dx, 0, dz);
                    if (column.getX() < box.minX() || column.getX() > box.maxX() || column.getZ() < box.minZ() ||
                            column.getZ() > box.maxZ()) {
                        continue;
                    }
                    for (int y = 0; y <= k.height; y++) air(dx, y, dz);
                    for (int y = -1; y >= -12; y--) {
                        if (isGround(dx, y, dz)) break;
                        set(y == -1 ? t.floor : t.wall, dx, y, dz);
                    }
                }
            }
        }
    }

    private static int abs(int v) {
        return Math.abs(v);
    }

    private void tower(Pen p) {
        int door = (int) (seed & 3); 
        int ladderX = door == 0 ? -2 : door == 1 ? 2 : 0;
        int ladderZ = door == 2 ? -2 : door == 3 ? 2 : 0;
        Direction ladderFacing = door == 0 ? Direction.EAST : door == 1 ? Direction.WEST :
                door == 2 ? Direction.SOUTH : Direction.NORTH;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int ring = Math.max(abs(dx), abs(dz));
                p.set(p.t.floor, dx, -1, dz);

                if (ring == 3) {
                    boolean doorCell = switch (door) {
                        case 0 -> dx == 3 && dz == 0;
                        case 1 -> dx == -3 && dz == 0;
                        case 2 -> dz == 3 && dx == 0;
                        default -> dz == -3 && dx == 0;
                    };
                    for (int y = 0; y <= 13; y++) {
                        if (doorCell && y <= 1) {
                            p.air(dx, y, dz);
                        } else if (y % 4 == 2 && (dx == 0 || dz == 0) && !doorCell) {
                            p.set(p.t.window, dx, y, dz);
                        } else {
                            p.set(hash(dx, y, dz) % 6 == 0 ? p.t.wallAlt : p.t.wall, dx, y, dz);
                        }
                    }
                }
                if (ring <= 2 && !(dx == ladderX && dz == ladderZ)) p.set(p.t.floor, dx, 6, dz);
                if (ring <= 4 && !(dx == ladderX && dz == ladderZ)) p.set(p.t.floor, dx, 13, dz);
                if (ring == 4 && ((dx + dz) & 1) == 0) p.set(p.t.wall, dx, 14, dz);
                if (abs(dx) == 4 && abs(dz) == 4) {
                    p.set(p.t.pillar, dx, 14, dz);
                    p.set(p.t.pillar, dx, 15, dz);
                }
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing);
        for (int y = 0; y <= 13; y++) p.set(ladder, ladderX, y, ladderZ);

        p.set(p.t.accent, 0, 14, 0);
        p.set(p.t.light, 0, 15, 0);
        p.chest(1, 7, 1);
        p.chest(-2, 14, 2);
    }

    private void compound(Pen p) {
        int gate = (int) ((seed >>> 8) & 3);
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -11; dz <= 11; dz++) {
                int ring = Math.max(abs(dx), abs(dz));
                boolean plaza = abs(dx) <= 3 && abs(dz) <= 3;
                boolean path = (dx == 0 || dz == 0) && ring <= 10;
                if (plaza || path) p.set(p.t.floor, dx, -1, dz);

                if (ring == 10) {
                    
                    int along = gate < 2 ? dz : dx;
                    boolean onGateWall = switch (gate) {
                        case 0 -> dx == 10;
                        case 1 -> dx == -10;
                        case 2 -> dz == 10;
                        default -> dz == -10;
                    };
                    boolean gap = onGateWall && abs(along) <= 1;
                    boolean post = onGateWall && abs(along) == 2;
                    for (int y = 0; y <= 4; y++) {
                        if (gap && y <= 3) {
                            p.air(dx, y, dz);
                        } else if (!gap && y >= 3 && hash(dx, y, dz) % 5 == 0) {
                            
                        } else {
                            p.set(hash(dx, y, dz) % 5 == 1 ? p.t.wallAlt : p.t.wall, dx, y, dz);
                        }
                    }
                    if (post) {
                        for (int y = 0; y <= 6; y++) p.set(p.t.pillar, dx, y, dz);
                        p.set(p.t.light, dx, 7, dz);
                    } else if (!gap && ((dx + dz) & 1) == 0) {
                        p.set(p.t.wall, dx, 5, dz);
                    }
                }
            }
        }

        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = sx * 10;
                int cz = sz * 10;
                for (int ex = -2; ex <= 2; ex++) {
                    for (int ez = -2; ez <= 2; ez++) {
                        int ring = Math.max(abs(ex), abs(ez));
                        int x = cx + ex;
                        int z = cz + ez;
                        p.set(p.t.floor, x, -1, z);
                        if (ring == 2) {
                            for (int y = 0; y <= 8; y++) {
                                boolean slit = y == 5 && (ex == 0 || ez == 0);
                                boolean entry = y <= 1 && ex == -sx * 2 && ez == 0;
                                if (entry) p.air(x, y, z);
                                else p.set(slit ? p.t.window : p.t.wall, x, y, z);
                            }
                        }
                        p.set(p.t.slab, x, 9, z);
                    }
                }
                p.set(p.t.light, cx, 10, cz);
            }
        }
        p.chest(-9, 0, -9);

        for (int ex = -1; ex <= 1; ex++) {
            for (int ez = -1; ez <= 1; ez++) {
                p.set(p.t.floor, ex, -1, ez);
                if (abs(ex) == 1 && abs(ez) == 1) {
                    for (int y = 0; y <= 2; y++) p.set(p.t.pillar, ex, y, ez);
                } else if (ex != 0 || ez != 0) {
                    p.set(p.t.wall, ex, 0, ez);
                }
                p.set(p.t.slab, ex, 3, ez);
            }
        }
        p.set(p.t.focus, 0, 0, 0);
        p.set(p.t.hanging, 0, 2, 0);

        hut(p, 6, 6);
        hut(p, -6, -6);
    }

    private void hut(Pen p, int cx, int cz) {
        int toward = cx > 0 ? -1 : 1;
        for (int ex = -3; ex <= 3; ex++) {
            for (int ez = -3; ez <= 3; ez++) {
                int ring = Math.max(abs(ex), abs(ez));
                int x = cx + ex;
                int z = cz + ez;
                if (ring <= 2) p.set(p.t.floor, x, -1, z);
                if (ring == 2) {
                    for (int y = 0; y <= 3; y++) {
                        boolean doorway = ex == toward * 2 && ez == 0 && y <= 1;
                        boolean win = y == 2 && ez == 0 && ex == -toward * 2;
                        if (doorway) p.air(x, y, z);
                        else p.set(win ? p.t.window : (hash(x, y, z) % 6 == 0 ? p.t.wallAlt : p.t.wall), x, y, z);
                    }
                }
                if (ring <= 3) p.set(p.t.slab, x, 4, z);
            }
        }
        p.chest(cx - toward, 0, cz + 1);
        p.set(p.t.hanging, cx, 3, cz);
    }

    private void shrine(Pen p) {
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 5.3) p.set(p.t.floor, dx, -1, dz);
                if (d <= 3.4) p.set(p.t.floor, dx, 0, dz);
            }
        }
        
        for (int k = 0; k < 8; k++) {
            int px = (int) Math.round(6 * Math.cos(k * Math.PI / 4.0));
            int pz = (int) Math.round(6 * Math.sin(k * Math.PI / 4.0));
            boolean broken = hash(px, 1, pz) % 10 < 3;
            int height = 4 + hash(px, 2, pz) % 3 - (broken ? 2 : 0);
            for (int y = 0; y < height; y++) p.set(p.t.pillar, px, y, pz);
            if (!broken) p.set((k & 1) == 0 ? p.t.light : p.t.accent, px, height, pz);
        }
        
        for (int y = 1; y <= 8; y++) p.set(p.t.pillar, 0, y, 0);
        p.set(p.t.accent, 0, 9, 0);
        p.set(p.t.light, 0, 10, 0);
        p.set(p.t.wallAlt, 1, 1, 0);
        p.set(p.t.wallAlt, -1, 1, 0);
        p.set(p.t.wallAlt, 0, 1, 1);
        p.set(p.t.wallAlt, 0, 1, -1);

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = -6; y <= -3; y++) {
                    boolean shell = abs(dx) == 3 || abs(dz) == 3 || y == -6 || y == -3;
                    p.set(shell ? (hash(dx, y, dz) % 4 == 0 ? p.t.wallAlt : p.t.wall) : Blocks.AIR.defaultBlockState(),
                            dx, y, dz);
                }
            }
        }
        p.set(p.t.light, 0, -5, 0);
        p.chest(-2, -5, -2);
        p.chest(2, -5, 2);
    }

    private void hall(Pen p) {
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                p.set(p.t.floor, dx, -1, dz);
                boolean wallCell = abs(dx) == 8 || abs(dz) == 5;
                if (wallCell) {
                    for (int y = 0; y <= 5; y++) {
                        boolean doorway = dz == 5 && abs(dx) <= 1 && y <= 2;
                        boolean win = y == 3 && ((abs(dz) == 5 && Math.floorMod(dx, 4) == 2) ||
                                (abs(dx) == 8 && dz == 0));
                        if (doorway) p.air(dx, y, dz);
                        else p.set(win ? p.t.window : (hash(dx, y, dz) % 7 == 0 ? p.t.wallAlt : p.t.wall), dx, y, dz);
                    }
                }
                
                p.set(p.t.slab, dx, 6, dz);
                if (abs(dz) <= 3 && abs(dx) <= 7) p.set(p.t.slab, dx, 7, dz);
                if (abs(dz) <= 1 && abs(dx) <= 6) p.set(p.t.slab, dx, 8, dz);
            }
        }
        
        BlockState furnace = Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, Direction.SOUTH);
        p.set(furnace, -3, 0, -4);
        p.set(furnace, -2, 0, -4);
        p.set(p.t.focus, 0, 0, -4);
        p.set(Blocks.ANVIL.defaultBlockState(), 2, 0, -4);
        
        for (int y = 2; y <= 11; y++) p.set(p.t.pillar, -3, y, -5);
        p.set(Blocks.CAMPFIRE.defaultBlockState(), -3, 12, -5);
        p.set(p.t.hanging, 4, 5, 0);
        p.set(p.t.hanging, -4, 5, 0);
        p.set(p.t.hanging, 0, 5, 2);
        p.chest(6, 0, -3);
        p.chest(-6, 0, 3);
    }
}
