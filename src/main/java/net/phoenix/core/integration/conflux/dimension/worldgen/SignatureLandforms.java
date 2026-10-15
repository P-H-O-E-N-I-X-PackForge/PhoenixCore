package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.function.IntBinaryOperator;

/**
 * The signature landforms of the four non-Phoenix dimensions, to sit beside Phoenix's volcanoes:
 *
 * <ul>
 * <li>Sculk: cenotes (sinkholes into a flooded cavern), giant fungus towers, rib arches.
 * <li>Void: floating islands with waterfalls and chains, half-buried amethyst geodes, impact craters.
 * <li>Sealed A: open-pit mines with a spiral haul road, cooling towers and smokestacks, collapsed overpasses.
 * <li>Sealed B: basalt column fields, geyser vents, bone-rib canyons.
 * </ul>
 *
 * Like the volcanoes these are plain arithmetic over the columns of one chunk, worked out from the seed alone: no noise,
 * no features, no block entities, nothing that ticks.
 */
final class SignatureLandforms {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();
    private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING = Blocks.CRYING_OBSIDIAN.defaultBlockState();

    private SignatureLandforms() {}

    static boolean handles(Landforms.Kind kind) {
        return switch (kind) {
            case CENOTE, FUNGUS, RIBS, SKY_ISLAND, GEODE, IMPACT, PIT_MINE, TOWERS, OVERPASS, COLUMNS, GEYSER -> true;
            default -> false;
        };
    }

    // ---------------------------------------------------------------- choosing

    private static int average(IntBinaryOperator ground, int cx, int cz, int r) {
        return (ground.applyAsInt(cx, cz) * 2 + ground.applyAsInt(cx + r, cz) + ground.applyAsInt(cx - r, cz) +
                ground.applyAsInt(cx, cz + r) + ground.applyAsInt(cx, cz - r)) / 6;
    }

    static Landforms.Instance create(Landforms.Kind kind, String dimension, int cx, int cz, RandomSource rnd,
                                     long seed, int seaLevel, IntBinaryOperator ground) {
        int base = average(ground, cx, cz, 14);
        // everything but the floating islands wants dry land
        if (kind != Landforms.Kind.SKY_ISLAND && base < seaLevel + 3) return null;

        switch (kind) {
            case CENOTE: {
                int radius = 11 + rnd.nextInt(8);
                int depth = 24 + rnd.nextInt(10);
                return new Landforms.Instance(kind, cx, cz, (int) (radius * 1.5) + 6, seed, base, 5, radius, depth,
                        null, null, null, 0, 0);
            }
            case FUNGUS: {
                int count = 3 + rnd.nextInt(3);
                Landforms.Pillar[] stalks = new Landforms.Pillar[count];
                int reach = 0;
                for (int i = 0; i < count; i++) {
                    int px = cx + rnd.nextInt(37) - 18;
                    int pz = cz + rnd.nextInt(37) - 18;
                    int stalk = 2 + rnd.nextInt(2);
                    int top = ground.applyAsInt(px, pz) + 16 + rnd.nextInt(18);
                    stalks[i] = new Landforms.Pillar(px, pz, stalk, top);
                    reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + stalk * 4 + 4));
                }
                return new Landforms.Instance(kind, cx, cz, reach, seed, base, 0, 0, 0, null, stalks, null, 0, 0);
            }
            case RIBS: {
                boolean canyon = dimension.equals("sealed_b");
                int count = 5 + rnd.nextInt(4);
                int spacing = 5;
                int arch = canyon ? 9 + rnd.nextInt(3) : 7 + rnd.nextInt(5);
                double theta = rnd.nextDouble() * Math.PI;
                int length = count * spacing + (canyon ? 34 : 0);
                return new Landforms.Instance(kind, cx, cz, length / 2 + arch + 8, seed, base, arch, count, spacing,
                        new double[] { theta }, null, null, 0, canyon ? 1 : 0);
            }
            case SKY_ISLAND: {
                int count = 3 + rnd.nextInt(3);
                Landforms.Pillar[] islands = new Landforms.Pillar[count];
                int reach = 0;
                for (int i = 0; i < count; i++) {
                    int px = cx + rnd.nextInt(81) - 40;
                    int pz = cz + rnd.nextInt(81) - 40;
                    int radius = 7 + rnd.nextInt(10);
                    int top = base + 38 + rnd.nextInt(30);
                    islands[i] = new Landforms.Pillar(px, pz, radius, top);
                    reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + radius + 6));
                }
                return new Landforms.Instance(kind, cx, cz, reach, seed, base, 0, 0, 0, null, islands, null, 0, 0);
            }
            case GEODE: {
                int radius = 11 + rnd.nextInt(6);
                return new Landforms.Instance(kind, cx, cz, radius + 3, seed, base, radius, 0, 0,
                        new double[] { rnd.nextDouble() * Math.PI * 2.0 }, null, null, 0, 0);
            }
            case IMPACT: {
                int radius = 14 + rnd.nextInt(12);
                int depth = 7 + rnd.nextInt(5);
                return new Landforms.Instance(kind, cx, cz, (int) (radius * 2.3), seed, base, 0, radius, depth,
                        new double[] { rnd.nextDouble() * Math.PI * 2.0 }, null, null, 0, 0);
            }
            case PIT_MINE: {
                if (base < seaLevel + 8) return null;
                int radius = 36 + rnd.nextInt(15);
                int depth = 28 + rnd.nextInt(13);
                return new Landforms.Instance(kind, cx, cz, (int) (radius * 1.2), seed, base, 0, radius, depth,
                        new double[] { rnd.nextDouble() * Math.PI * 2.0 }, null, null, 0, 0);
            }
            case TOWERS: {
                int towers = 1 + rnd.nextInt(3);
                int stacks = 1 + rnd.nextInt(2);
                Landforms.Pillar[] list = new Landforms.Pillar[towers + stacks];
                int reach = 0;
                for (int i = 0; i < list.length; i++) {
                    boolean stack = i >= towers;
                    int px = cx;
                    int pz = cz;
                    // keep clear of the ones already placed
                    for (int attempt = 0; attempt < 12; attempt++) {
                        px = cx + rnd.nextInt(97) - 48;
                        pz = cz + rnd.nextInt(97) - 48;
                        boolean clear = true;
                        for (int j = 0; j < i; j++) {
                            if (Math.hypot(px - list[j].x(), pz - list[j].z()) < 30) clear = false;
                        }
                        if (clear) break;
                    }
                    int radius = stack ? 3 : 12 + rnd.nextInt(3);
                    int height = stack ? 55 + rnd.nextInt(11) : 40 + rnd.nextInt(13);
                    list[i] = new Landforms.Pillar(px, pz, radius, base + height);
                    reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + radius + 6));
                }
                return new Landforms.Instance(kind, cx, cz, reach, seed, base, 0, 0, 0, null, list, null, 0, 0);
            }
            case OVERPASS: {
                int spans = 6 + rnd.nextInt(4);
                int spacing = 13;
                return new Landforms.Instance(kind, cx, cz, spans * spacing / 2 + 10, seed, base, 12, spans, spacing,
                        new double[] { rnd.nextDouble() * Math.PI }, null, null, 0, 0);
            }
            case COLUMNS: {
                int radius = 34 + rnd.nextInt(18);
                return new Landforms.Instance(kind, cx, cz, radius + 4, seed, base, 9 + rnd.nextInt(7), radius, 0,
                        null, null, null, 0, 0);
            }
            default: { // GEYSER
                int count = 2 + rnd.nextInt(3);
                Landforms.Pillar[] vents = new Landforms.Pillar[count];
                int reach = 0;
                for (int i = 0; i < count; i++) {
                    int px = cx + rnd.nextInt(41) - 20;
                    int pz = cz + rnd.nextInt(41) - 20;
                    int radius = 7 + rnd.nextInt(3);
                    vents[i] = new Landforms.Pillar(px, pz, radius, 0);
                    reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + radius + 3));
                }
                return new Landforms.Instance(kind, cx, cz, reach, seed, base, 0, 0, 0, null, vents, null, 0, 0);
            }
        }
    }

    // ---------------------------------------------------------------- building

    static void apply(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        switch (v.kind()) {
            case CENOTE -> cenote(v, chunk, minX, minZ, minY, maxY);
            case FUNGUS -> fungus(v, chunk, minX, minZ, minY, maxY);
            case RIBS -> ribs(v, chunk, minX, minZ, minY, maxY);
            case SKY_ISLAND -> skyIslands(v, chunk, minX, minZ, minY, maxY);
            case GEODE -> geode(v, chunk, minX, minZ, minY, maxY);
            case IMPACT -> impact(v, chunk, minX, minZ, minY, maxY);
            case PIT_MINE -> pitMine(v, chunk, minX, minZ, minY, maxY);
            case TOWERS -> towers(v, chunk, minX, minZ, minY, maxY);
            case OVERPASS -> overpass(v, chunk, minX, minZ, minY, maxY);
            case COLUMNS -> columns(v, chunk, minX, minZ, minY, maxY);
            case GEYSER -> geysers(v, chunk, minX, minZ, minY, maxY);
            default -> {}
        }
    }

    private static void set(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState state,
                            int minY, int maxY) {
        if (y > minY && y < maxY - 1) chunk.setBlockState(pos.set(x, y, z), state, false);
    }

    private static void fill(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int z, int from, int to,
                             BlockState state, int minY, int maxY) {
        for (int y = Math.max(from, minY + 1); y <= Math.min(to, maxY - 2); y++) {
            chunk.setBlockState(pos.set(x, y, z), state, false);
        }
    }

    private static double angleDiff(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
    }

    // ---- sculk: the cenote ----

    private static void cenote(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bottom = v.baseY() - v.craterDepth();
        int pool = bottom + v.height();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double dx = x - v.cx();
                double dz = z - v.cz();
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > v.reach()) continue;
                // a ragged lip, so it is not a perfect circle
                double rim = v.craterRadius() + ((Landforms.hash2(v.seed(), x >> 1, z >> 1) & 7) - 3.5) * 0.4;
                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);

                if (r < rim) {
                    int floor = bottom + (int) Math.round(3.0 * (r / rim) * (r / rim));
                    fill(chunk, pos, x, z, floor + 1, ground, AIR, minY, maxY);
                    set(chunk, pos, x, floor, z, Blocks.CLAY.defaultBlockState(), minY, maxY);
                    fill(chunk, pos, x, z, floor + 1, pool, WATER, minY, maxY);
                } else if (r < rim * 1.45) {
                    // the cavern widens below the shaft, like a bell, under a roof of rock
                    double t = (r - rim) / (rim * 0.45);
                    int ceiling = bottom + 8 - (int) (t * t * 8.0);
                    int floor = bottom + (int) (t * 3.0);
                    if (ceiling > floor + 1 && ceiling < ground - 3) {
                        fill(chunk, pos, x, z, floor + 1, ceiling, AIR, minY, maxY);
                        set(chunk, pos, x, floor, z, Blocks.CLAY.defaultBlockState(), minY, maxY);
                        fill(chunk, pos, x, z, floor + 1, Math.min(ceiling, pool), WATER, minY, maxY);
                    }
                }
                if (r >= rim && r < rim + 2.5 && ground > minY) {
                    set(chunk, pos, x, ground, z, MOSS, minY, maxY);
                }
            }
        }
    }

    // ---- sculk: giant fungus ----

    private static void fungus(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        BlockState cap = Blocks.WARPED_WART_BLOCK.defaultBlockState();
        BlockState glow = Blocks.SHROOMLIGHT.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int ground = -1;
                for (Landforms.Pillar p : v.pillars()) {
                    double d = Math.hypot(x - p.x(), z - p.z());
                    double capRadius = p.radius() * 4 + 2;
                    if (d > capRadius) continue;
                    if (ground < 0) ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                    if (d <= p.radius() + 0.5) fill(chunk, pos, x, z, ground + 1, p.top(), stem, minY, maxY);

                    double f = d / capRadius;
                    int under = p.top() - (int) (4.0 * f * f);
                    int over = p.top() + (int) Math.round(capRadius * 0.5 * Math.sqrt(Math.max(0.0, 1.0 - f * f)));
                    for (int y = under; y <= over; y++) {
                        int roll = Landforms.hash2(v.seed(), x + y * 31, z) & 15;
                        BlockState s = y == under && roll < 5 ? glow : y == over && roll == 0 ? glow : cap;
                        set(chunk, pos, x, y, z, s, minY, maxY);
                    }
                }
            }
        }
    }

    // ---- sculk arches and sealed B's bone-rib canyon ----

    private static void ribs(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double theta = v.rivuletAngles()[0];
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        int count = v.craterRadius();
        int spacing = v.craterDepth();
        boolean canyon = v.variant() == 1;
        BlockState foot = canyon ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.SCULK.defaultBlockState();
        double canyonHalf = (count * spacing + 34) * 0.5;

        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                if (Math.hypot(dx, dz) > v.reach()) continue;
                double u = dx * cos + dz * sin;
                double w = -dx * sin + dz * cos;
                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);

                if (canyon) {
                    double half = 5.5 + 1.5 * Math.sin(u * 0.21 + (v.seed() & 63));
                    double taper = Landforms.smooth((canyonHalf - Math.abs(u)) / 10.0);
                    half *= taper;
                    if (half > 1.0 && Math.abs(w) < half) {
                        double q = Math.abs(w) / half;
                        int floor = v.baseY() - 14 + (int) Math.round(3.0 * q * q);
                        fill(chunk, pos, x, z, floor + 1, ground, AIR, minY, maxY);
                        set(chunk, pos, x, floor, z, BASALT, minY, maxY);
                        fill(chunk, pos, x, z, floor + 1, floor + 3, LAVA, minY, maxY);
                    }
                }

                for (int i = 0; i < count; i++) {
                    double mid = (i - (count - 1) / 2.0);
                    double s = mid * spacing;
                    if (u < s - 1.0 || u >= s + 1.0) continue;
                    double size = v.height() * (1.0 - 0.45 * (mid / (count / 2.0)) * (mid / (count / 2.0)));
                    double outer = size;
                    double inner = size - 1.6;
                    if (Math.abs(w) > outer) continue;
                    int yOuter = v.baseY() + (int) Math.ceil(Math.sqrt(outer * outer - w * w));
                    int yInner = Math.abs(w) < inner ?
                            v.baseY() + (int) Math.floor(Math.sqrt(inner * inner - w * w)) : v.baseY() - 1;
                    int from = Math.abs(w) < inner ? yInner : Math.min(ground, v.baseY()) + 1;
                    if (yOuter <= from) continue;
                    fill(chunk, pos, x, z, from, yOuter, BONE, minY, maxY);
                    if (Math.abs(w) >= inner) set(chunk, pos, x, Math.min(ground, v.baseY()), z, foot, minY, maxY);
                }
            }
        }
    }

    // ---- void: floating islands ----

    private static void skyIslands(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.END_STONE.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int ground = Integer.MIN_VALUE;
                for (Landforms.Pillar p : v.pillars()) {
                    double d = Math.hypot(x - p.x(), z - p.z());
                    double radius = p.radius();
                    long ph = Landforms.mix(v.seed(), p.x(), p.z());

                    // a chain tethers the bigger ones to the ground
                    if (p.radius() >= 10 && x == p.x() && z == p.z()) {
                        if (ground == Integer.MIN_VALUE) ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                        int tip = p.top() - (int) (radius * 0.95) - 3;
                        fill(chunk, pos, x, z, ground + 1, tip, chain, minY, maxY);
                        set(chunk, pos, x, ground, z, OBSIDIAN, minY, maxY);
                    }

                    // a waterfall off one edge
                    if ((ph & 1) == 0) {
                        double a = ((ph >>> 8) & 0xFFFF) / 65535.0 * Math.PI * 2.0;
                        int lx = p.x() + (int) Math.round((radius + 1.0) * Math.cos(a));
                        int lz = p.z() + (int) Math.round((radius + 1.0) * Math.sin(a));
                        if (x == lx && z == lz) {
                            if (ground == Integer.MIN_VALUE) {
                                ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                            }
                            fill(chunk, pos, x, z, ground + 1, p.top() - 1, WATER, minY, maxY);
                        }
                    }

                    if (d > radius) continue;
                    double q = d / radius;
                    int surface = p.top() + (int) Math.round(1.5 * (1.0 - q * q));
                    int depth = (int) Math.round(radius * 0.95 * Math.pow(1.0 - q, 0.7)) + 2;
                    int bottom = p.top() - depth;
                    for (int y = bottom; y <= surface; y++) {
                        BlockState s = y == surface ? grass : y > surface - 3 ? dirt : stone;
                        set(chunk, pos, x, y, z, s, minY, maxY);
                    }
                    // a pond at the middle, held level by the grass around it
                    if (d < radius * 0.28) {
                        set(chunk, pos, x, surface, z, AIR, minY, maxY);
                        set(chunk, pos, x, surface - 1, z, WATER, minY, maxY);
                        set(chunk, pos, x, surface - 2, z, Blocks.CLAY.defaultBlockState(), minY, maxY);
                    }
                }
            }
        }
    }

    // ---- void: amethyst geode ----

    private static void geode(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double radius = v.height();
        int centreY = v.baseY() + (int) Math.round(radius * 0.35);
        double doorway = v.rivuletAngles()[0];
        BlockState outer = SMOOTH_BASALT;
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();

        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                double dxz = Math.sqrt(dx * dx + dz * dz);
                if (dxz > radius + 1) continue;
                boolean inDoor = dxz > 2.0 && angleDiff(Math.atan2(dz, dx), doorway) < 0.33;
                for (int y = v.baseY() - 1; y <= centreY + (int) radius; y++) {
                    double dy = y - centreY;
                    double d = Math.sqrt(dxz * dxz + dy * dy);
                    if (d > radius + 0.5) continue;
                    if (d > radius - 4.0) {
                        if (inDoor && y >= v.baseY() && y <= v.baseY() + 3) {
                            set(chunk, pos, x, y, z, AIR, minY, maxY);
                            continue;
                        }
                        int roll = Landforms.hash2(v.seed(), x + y * 17, z) & 63;
                        BlockState s;
                        if (d > radius - 1.0) s = outer;
                        else if (d > radius - 3.0) s = calcite;
                        else s = roll < 5 ? budding : roll == 6 ? lantern : amethyst;
                        set(chunk, pos, x, y, z, s, minY, maxY);
                    } else if (y == v.baseY() - 1) {
                        set(chunk, pos, x, y, z, amethyst, minY, maxY);
                    } else {
                        set(chunk, pos, x, y, z, AIR, minY, maxY);
                    }
                }
            }
        }
    }

    // ---- void: impact crater ----

    private static void impact(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double radius = v.craterRadius();
        double phase = v.rivuletAngles()[0];
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r >= v.reach()) continue;
                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                double bowl = r < radius ? -v.craterDepth() * (1.0 - (r / radius) * (r / radius)) : 0.0;
                double lip = 3.0 * Math.exp(-Math.pow((r - radius * 1.1) / (radius * 0.25), 2.0));
                double blend = Landforms.smooth((1.0 - r / v.reach()) / 0.35);
                int top = (int) Math.round(ground + (v.baseY() + bowl + lip - ground) * blend);
                top = Math.max(minY + 1, Math.min(maxY - 2, top));

                BlockState surface = null;
                int roll = Landforms.hash2(v.seed(), x, z) & 31;
                if (r < radius) {
                    surface = r < radius * 0.3 ? (roll < 6 ? CRYING : OBSIDIAN) :
                            roll < 14 ? blackstone : roll < 22 ? SMOOTH_BASALT : roll < 24 ? CRYING : OBSIDIAN;
                } else if (r < radius * 2.2) {
                    // ejecta thrown out in rays
                    double ray = Math.abs(Math.sin(Math.atan2(dz, dx) * 7.0 + phase));
                    if (ray > 0.92) surface = roll < 16 ? GRAVEL : OBSIDIAN;
                    else if (lip > 1.0) surface = blackstone;
                }
                if (top > ground) fill(chunk, pos, x, z, ground + 1, top, blackstone, minY, maxY);
                else if (top < ground) fill(chunk, pos, x, z, top + 1, ground, AIR, minY, maxY);
                if (surface != null) set(chunk, pos, x, top, z, surface, minY, maxY);
                // the fallen star at the heart of it
                if (r < 2.2) {
                    set(chunk, pos, x, top + 1, z, r < 1.0 ? lantern : CRYING, minY, maxY);
                }
            }
        }
    }

    // ---- sealed A: open-pit mine ----

    /** Half the width of the haul road: it is 11 blocks across, wide enough for a truck and easy to find from the rim. */
    static final double PIT_ROAD_HALF = 5.5;
    private static final double PIT_TURNS = 2.0;

    private static void pitMine(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double radius = v.craterRadius();
        double phase = v.rivuletAngles()[0];
        // how fast the road winds: radians of turn per block of radius
        double wind = PIT_TURNS * 2.0 * Math.PI / radius;
        BlockState road = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState step = Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState();
        BlockState dash = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState edge = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState post = Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
        BlockState lamp = Blocks.LANTERN.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState rawIron = Blocks.RAW_IRON_BLOCK.defaultBlockState();
        BlockState rawCopper = Blocks.RAW_COPPER_BLOCK.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r >= v.reach()) continue;
                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                int roll = Landforms.hash2(v.seed(), x, z) & 31;

                if (r < radius) {
                    // The road is a spiral. Along any bearing it crosses at one radius, the centreline; the pit wall is
                    // steep, so the road is a LEVEL SHELF at the height of that centreline, not a strip that tilts with
                    // the wall. How far a column is from the road is measured across it, not along it.
                    double along = (1.0 - r / radius) * PIT_TURNS * 2.0 * Math.PI + phase;
                    double offset = Math.atan2(Math.sin(Math.atan2(dz, dx) - along), Math.cos(Math.atan2(dz, dx) - along));
                    double centre = r - offset / wind;
                    double across = Math.abs(centre - r) * (wind * r) / Math.sqrt(1.0 + wind * wind * r * r);
                    boolean onRoad = r > 7.0 && centre > 7.0 && centre < radius && across < PIT_ROAD_HALF;

                    if (onRoad) {
                        double depthHere = v.craterDepth() * (1.0 - centre / radius);
                        double height = v.baseY() - depthHere;
                        int level = (int) Math.floor(height);
                        if (level >= ground) continue;
                        fill(chunk, pos, x, z, level + 1, ground, AIR, minY, maxY);
                        set(chunk, pos, x, level, z, road, minY, maxY);
                        // a half block step every so often, so the slope is walkable without jumping
                        if (height - level >= 0.5) {
                            set(chunk, pos, x, level + 1, z, step, minY, maxY);
                        } else if (across > PIT_ROAD_HALF - 1.0) {
                            set(chunk, pos, x, level, z, edge, minY, maxY);
                        } else if (across < 0.8 && (int) Math.floor((1.0 - centre / radius) * 70.0) % 2 == 0) {
                            set(chunk, pos, x, level, z, dash, minY, maxY);
                        }
                        // lamp posts along the edge, so the road reads from the rim
                        if (across > PIT_ROAD_HALF - 1.0 && roll == 0) {
                            set(chunk, pos, x, level + 1, z, post, minY, maxY);
                            set(chunk, pos, x, level + 2, z, post, minY, maxY);
                            set(chunk, pos, x, level + 3, z, lamp, minY, maxY);
                        }
                        continue;
                    }

                    // the benches step down the wall, rounded so that they sit at or below the road beside them
                    double depth = v.craterDepth() * (1.0 - r / radius);
                    int surface = v.baseY() - (int) Math.round(Math.ceil(depth / 4.0) * 4.0);
                    if (surface >= ground) continue;
                    fill(chunk, pos, x, z, surface + 1, ground, AIR, minY, maxY);
                    set(chunk, pos, x, surface, z, roll < 16 ? GRAVEL : roll < 24 ? coarse : andesite, minY, maxY);
                    // an ore stockpile on the floor
                    if (r < 4.5) {
                        int heap = (int) Math.round(3.0 * (1.0 - r / 4.5));
                        fill(chunk, pos, x, z, surface + 1, surface + heap, (roll & 1) == 0 ? rawIron : rawCopper, minY,
                                maxY);
                    }
                } else {
                    // a berm of spoil around the rim
                    double q = (r - radius) / (v.reach() - radius);
                    int heap = (int) Math.round(2.0 * (1.0 - q));
                    if (heap > 0) fill(chunk, pos, x, z, ground + 1, ground + heap, andesite, minY, maxY);
                }
            }
        }
    }

    private static void towers(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState light = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState gray = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState red = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState pad = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int ground = Integer.MIN_VALUE;
                for (Landforms.Pillar p : v.pillars()) {
                    double dx = x - p.x();
                    double dz = z - p.z();
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > p.radius() + 4) continue;
                    if (ground == Integer.MIN_VALUE) ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                    boolean stack = p.radius() <= 4;
                    int base = v.baseY();
                    int height = p.top() - base;
                    double angle = Math.atan2(dz, dx);
                    double damage = ((Landforms.mix(v.seed(), p.x(), p.z()) >>> 12) & 0xFF) / 255.0 * Math.PI * 2.0;

                    // a level pad for it to stand on
                    if (d <= p.radius() + 3) {
                        if (ground < base) fill(chunk, pos, x, z, ground + 1, base - 1, pad, minY, maxY);
                        else if (ground > base) fill(chunk, pos, x, z, base + 1, ground, AIR, minY, maxY);
                        set(chunk, pos, x, base, z, pad, minY, maxY);
                    }

                    for (int y = base + 1; y <= p.top(); y++) {
                        double r = towerRadius(p, base, y);
                        double inner = stack ? r - 1.1 : r - 1.5;
                        if (d > r + 0.5 || d < inner) continue;

                        int top = p.top();
                        int hash = Landforms.hash2(v.seed(), x + y * 13, z);
                        // a broken lip on the damaged side
                        if (y > top - 9 && angleDiff(angle, damage) < 0.7 && (hash & 3) != 0 &&
                                y > top - 3 - (hash >> 3 & 7)) {
                            continue;
                        }
                        if (!stack && y <= base + 5 && angleDiff(angle, damage + Math.PI) < 0.16) {
                            continue; // an arched entrance at the foot
                        }
                        BlockState s;
                        if (stack) s = (y / 4) % 2 == 0 ? red : white;
                        else s = (y / 6) % 3 == 1 ? gray : light;
                        set(chunk, pos, x, y, z, s, minY, maxY);
                    }
                }
            }
        }
    }

    /** The radius of a cooling tower's wall at a height: wide at the foot, narrow at the throat, flaring at the mouth. */
    static double towerRadius(Landforms.Pillar p, int base, int y) {
        if (p.radius() <= 4) return p.radius();
        double f = (y - base) / (double) (p.top() - base);
        double throat = p.radius() * 0.66;
        double mouth = throat * 1.22;
        return f < 0.62 ? throat + (p.radius() - throat) * Math.pow((0.62 - f) / 0.62, 2.0) :
                throat + (mouth - throat) * Math.pow((f - 0.62) / 0.38, 2.0);
    }

    // ---- sealed A: collapsed overpass ----

    private static void overpass(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double theta = v.rivuletAngles()[0];
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        int spans = v.craterRadius();
        int spacing = v.craterDepth();
        int deckY = v.baseY() + v.height();
        BlockState road = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState under = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState line = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState rail = Blocks.ANDESITE_WALL.defaultBlockState();
        BlockState rebar = Blocks.IRON_BARS.defaultBlockState();
        BlockState rubble = Blocks.COBBLESTONE.defaultBlockState();
        double start = -spans * spacing / 2.0;

        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                if (Math.hypot(dx, dz) > v.reach()) continue;
                double u = dx * cos + dz * sin;
                double w = -dx * sin + dz * cos;
                if (Math.abs(w) > 6.0 || u < start - 2 || u > start + spans * spacing + 2) continue;
                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);

                int span = (int) Math.floor((u - start) / spacing);
                span = Math.max(0, Math.min(spans - 1, span));
                double t = (u - (start + span * spacing)) / spacing;
                long sh = Landforms.mix(v.seed(), span, 77);
                boolean broken = (sh & 0xFF) % 100 < 40;
                double frac = 0.25 + ((sh >>> 8) & 7) / 16.0;
                boolean fromLeft = ((sh >>> 12) & 1) == 0;
                double k = fromLeft ? t / frac : (1.0 - t) / frac;
                boolean deckHere = !broken || k <= 1.0;
                int sag = broken ? (int) Math.round(Math.max(0.0, k) * Math.max(0.0, k) * 5.0) : 0;

                if (Math.abs(w) <= 3.5 && deckHere) {
                    int y = deckY - sag;
                    set(chunk, pos, x, y - 1, z, under, minY, maxY);
                    set(chunk, pos, x, y, z, Math.abs(w) < 0.6 && Math.floorMod((int) Math.floor(u / 3.0), 2) == 0 ?
                            line : road, minY, maxY);
                } else if (Math.abs(w) <= 4.5 && Math.abs(w) > 3.5 && deckHere && (Landforms.hash2(v.seed(), x, z) & 3) != 0) {
                    set(chunk, pos, x, deckY - sag + 1, z, rail, minY, maxY);
                }
                // rebar hanging from a broken end
                if (broken && Math.abs(w) <= 3.0 && k > 0.93 && k <= 1.0 && (Landforms.hash2(v.seed(), x, z) & 3) == 0) {
                    fill(chunk, pos, x, z, deckY - sag - 3, deckY - sag - 2, rebar, minY, maxY);
                }
                // rubble where the deck has come down
                if (broken && !deckHere && Math.abs(w) <= 5.0) {
                    int h = Landforms.hash2(v.seed(), x * 3, z);
                    if (h % 3 == 0) set(chunk, pos, x, ground + 1, z, (h >> 4 & 7) == 0 ? rebar : rubble, minY, maxY);
                    if (h % 9 == 0) set(chunk, pos, x, ground + 2, z, rubble, minY, maxY);
                }
                // the piers
                for (int i = 0; i <= spans; i++) {
                    double s = start + i * spacing;
                    if (Math.abs(u - s) > 1.2) continue;
                    for (int side = -1; side <= 1; side += 2) {
                        if (Math.abs(w - side * 2.0) <= 1.2 && ground < deckY - 2) {
                            fill(chunk, pos, x, z, ground + 1, deckY - 2, under, minY, maxY);
                        }
                    }
                    if (Math.abs(w) <= 3.5) set(chunk, pos, x, deckY - 2, z, under, minY, maxY);
                }
            }
        }
    }

    // ---- sealed B: basalt column field ----

    private static void columns(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double cell = 4.4;
        BlockState polished = Blocks.POLISHED_BASALT.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r >= v.reach()) continue;

                // the nearest and second-nearest column centre, on a jittered grid, so each column is a many-sided prism
                int cellX = (int) Math.floor(x / cell);
                int cellZ = (int) Math.floor(z / cell);
                double best = Double.MAX_VALUE;
                double second = Double.MAX_VALUE;
                int bestId = 0;
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        int h = Landforms.hash2(v.seed(), cellX + ox, cellZ + oz);
                        double px = (cellX + ox + (h & 0xFF) / 255.0) * cell;
                        double pz = (cellZ + oz + ((h >> 8) & 0xFF) / 255.0) * cell;
                        double d = Math.hypot(x + 0.5 - px, z + 0.5 - pz);
                        if (d < best) {
                            second = best;
                            best = d;
                            bestId = h;
                        } else if (d < second) {
                            second = d;
                        }
                    }
                }
                double env = r < v.craterRadius() ? Math.pow(1.0 - (r / v.craterRadius()) * (r / v.craterRadius()), 0.7) :
                        0.0;
                double height = v.height() * env * (0.45 + 0.55 * ((bestId >>> 16 & 0xFF) / 255.0));
                int rise = (int) Math.round(height) - (second - best < 0.55 ? 2 : 0);
                if (rise <= 0) continue;

                int ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                fill(chunk, pos, x, z, ground + 1, ground + rise, BASALT, minY, maxY);
                int roll = Landforms.hash2(v.seed(), x, z) & 15;
                set(chunk, pos, x, ground + rise, z, roll == 0 ? magma : polished, minY, maxY);
            }
        }
    }

    // ---- sealed B: geyser vents ----

    private static void geysers(Landforms.Instance v, ChunkAccess chunk, int minX, int minZ, int minY, int maxY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState[] sinter = { Blocks.CALCITE.defaultBlockState(), Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                Blocks.WHITE_TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState() };
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int ground = Integer.MIN_VALUE;
                for (Landforms.Pillar p : v.pillars()) {
                    double d = Math.hypot(x - p.x(), z - p.z());
                    if (d > p.radius()) continue;
                    if (ground == Integer.MIN_VALUE) ground = Landforms.surfaceGround(chunk, x, z, minY, maxY, pos);
                    int rise = (int) Math.round(4.0 * Math.pow(1.0 - d / p.radius(), 1.2));
                    int top = ground + rise;
                    fill(chunk, pos, x, z, ground + 1, top, sinter[0], minY, maxY);
                    // terraces of sinter in rings of different colours
                    set(chunk, pos, x, top, z, sinter[(int) (d / 1.6) % sinter.length], minY, maxY);

                    if (d <= 1.6) {
                        // the vent itself: boiling water over a bed of soul sand, so it bubbles
                        fill(chunk, pos, x, z, ground - 4, top - 1, WATER, minY, maxY);
                        set(chunk, pos, x, ground - 5, z, Blocks.SOUL_SAND.defaultBlockState(), minY, maxY);
                        fill(chunk, pos, x, z, top, top, AIR, minY, maxY);
                    }
                }
            }
        }
    }
}
