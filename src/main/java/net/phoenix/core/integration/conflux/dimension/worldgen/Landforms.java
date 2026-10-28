package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

public final class Landforms {

    private static final int CELL = 192;

    private static final int SPAWN_CLEAR = 96;

    enum Kind {
        VOLCANO,
        HOODOOS,
        KARST,

        PLATEAU,

        SEA_STACKS,
        CENOTE,
        FUNGUS,
        RIBS,
        SKY_ISLAND,
        GEODE,
        IMPACT,
        PIT_MINE,
        TOWERS,
        OVERPASS,
        COLUMNS,
        GEYSER
    }

    record Palette(BlockState[] bands, int bandHeight, BlockState cap, BlockState capAlt) {}

    record Pillar(int x, int z, int radius, int top) {}

    static final int ACTIVE = 0;
    static final int DORMANT = 1;
    static final int EXTINCT = 2;

    record Instance(Kind kind, int cx, int cz, int reach, long seed, int baseY, int height, int craterRadius,
                    int craterDepth, double[] rivuletAngles, Pillar[] pillars, Palette palette, int state,
                    int variant) {}

    private final String disciplineId;
    private final long seed;
    private final ConcurrentHashMap<Long, Optional<Instance>> cache = new ConcurrentHashMap<>();

    public Landforms(String disciplineId, long seed) {
        this.disciplineId = disciplineId;
        this.seed = seed;
    }

    public boolean enabled() {
        return switch (disciplineId) {
            case "phoenix", "sculk", "void", "sealed_a", "sealed_b" -> true;
            default -> false;
        };
    }

    public void apply(ChunkAccess chunk, int minY, int maxY, int seaLevel, IntBinaryOperator ground) {
        if (!enabled()) return;
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int cellMinX = Math.floorDiv(minX, CELL) - 1;
        int cellMaxX = Math.floorDiv(minX + 15, CELL) + 1;
        int cellMinZ = Math.floorDiv(minZ, CELL) - 1;
        int cellMaxZ = Math.floorDiv(minZ + 15, CELL) + 1;

        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                int cx = cellX;
                int cz = cellZ;
                Optional<Instance> found = cache.computeIfAbsent(key(cx, cz),
                        k -> Optional.ofNullable(create(cx, cz, seaLevel, ground)));
                if (found.isEmpty()) continue;
                Instance inst = found.get();
                if (inst.cx() + inst.reach() < minX || inst.cx() - inst.reach() > minX + 15 ||
                        inst.cz() + inst.reach() < minZ || inst.cz() - inst.reach() > minZ + 15) {
                    continue;
                }
                switch (inst.kind()) {
                    case VOLCANO -> volcano(chunk, inst, minX, minZ, minY, maxY);
                    case HOODOOS, KARST, SEA_STACKS -> pillars(chunk, inst, minX, minZ, minY, maxY);
                    case PLATEAU -> plateau(chunk, inst, minX, minZ, minY, maxY);
                    default -> SignatureLandforms.apply(inst, chunk, minX, minZ, minY, maxY);
                }
            }
        }
    }

    private static long key(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    private static final int VOLCANO_GAP = 260;

    private final java.util.concurrent.ConcurrentHashMap<Long, Boolean> volcanoSurvival = new java.util.concurrent.ConcurrentHashMap<>();

    private int[] volcanoCentre(int cellX, int cellZ) {
        RandomSource rnd = RandomSource.create(mix(seed, cellX, cellZ));
        if (pick(rnd.nextFloat()) != Kind.VOLCANO) return null;
        int cx = cellX * CELL + 40 + rnd.nextInt(CELL - 80);
        int cz = cellZ * CELL + 40 + rnd.nextInt(CELL - 80);
        if (Math.abs(cx) < SPAWN_CLEAR && Math.abs(cz) < SPAWN_CLEAR) return null;
        return new int[] { cx, cz };
    }

    private boolean volcanoSurvives(int cellX, int cellZ) {
        long k = key(cellX, cellZ);
        Boolean known = volcanoSurvival.get(k);
        if (known != null) return known;
        int[] me = volcanoCentre(cellX, cellZ);
        boolean ok = me != null;
        for (int dx = -2; ok && dx <= 2; dx++) {
            for (int dz = -2; ok && dz <= 2; dz++) {
                int nx = cellX + dx, nz = cellZ + dz;
                if (nx > cellX || (nx == cellX && nz >= cellZ)) continue;
                int[] other = volcanoCentre(nx, nz);
                if (other == null || !volcanoSurvives(nx, nz)) continue;
                if (Math.hypot(me[0] - other[0], me[1] - other[1]) < VOLCANO_GAP) ok = false;
            }
        }
        volcanoSurvival.put(k, ok);
        return ok;
    }

    private Instance create(int cellX, int cellZ, int seaLevel, IntBinaryOperator ground) {
        RandomSource rnd = RandomSource.create(mix(seed, cellX, cellZ));
        float roll = rnd.nextFloat();
        Kind kind = pick(roll);
        if (kind == null) return null;
        if (kind == Kind.VOLCANO && !volcanoSurvives(cellX, cellZ)) return null;

        int cx = cellX * CELL + 40 + rnd.nextInt(CELL - 80);
        int cz = cellZ * CELL + 40 + rnd.nextInt(CELL - 80);
        if (Math.abs(cx) < SPAWN_CLEAR && Math.abs(cz) < SPAWN_CLEAR) return null;
        long instSeed = rnd.nextLong();

        int centreGround = ground.applyAsInt(cx, cz);
        boolean wet = centreGround < seaLevel - 2;
        if (wet && kind != Kind.VOLCANO && kind != Kind.SKY_ISLAND) kind = Kind.SEA_STACKS;

        if (SignatureLandforms.handles(kind)) {
            return SignatureLandforms.create(kind, disciplineId, cx, cz, rnd, instSeed, seaLevel, ground);
        }

        if (kind == Kind.PLATEAU) {
            int radius = 18 + rnd.nextInt(15);
            int base = (centreGround * 2 + ground.applyAsInt(cx + radius, cz) + ground.applyAsInt(cx - radius, cz) +
                    ground.applyAsInt(cx, cz + radius) + ground.applyAsInt(cx, cz - radius)) / 6;
            int height = 14 + rnd.nextInt(17);

            int pool = rnd.nextInt(3) == 0 ? 0 : 4 + rnd.nextInt(3);
            double[] fall = disciplineId.equals("phoenix") || disciplineId.equals("sealed_b") ?
                    new double[] { rnd.nextDouble() * Math.PI * 2.0 } : null;
            return new Instance(Kind.PLATEAU, cx, cz, radius + 6, instSeed, base, height, pool, 0, fall, null,
                    palette(Kind.HOODOOS), 0, 0);
        }

        if (kind == Kind.SEA_STACKS) {
            int count = 3 + rnd.nextInt(4);
            Pillar[] stacks = new Pillar[count];
            int reach = 0;
            for (int i = 0; i < count; i++) {
                int px = cx + rnd.nextInt(45) - 22;
                int pz = cz + rnd.nextInt(45) - 22;
                int radius = 2 + rnd.nextInt(3);
                stacks[i] = new Pillar(px, pz, radius, seaLevel + 4 + rnd.nextInt(17));
                reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + radius * 1.6 + 2));
            }
            return new Instance(Kind.SEA_STACKS, cx, cz, reach, instSeed, 0, 0, 0, 0, null, stacks,
                    palette(Kind.KARST), 0, 0);
        }

        if (kind == Kind.VOLCANO) {
            float aliveRoll = rnd.nextFloat();
            int state = disciplineId.equals("phoenix") ?
                    (aliveRoll < 0.60f ? ACTIVE : aliveRoll < 0.85f ? DORMANT : EXTINCT) :
                    (aliveRoll < 0.15f ? ACTIVE : aliveRoll < 0.65f ? DORMANT : EXTINCT);
            int variant = pickVolcano(rnd);

            int radius;
            int height;
            int craterR = 0;
            int craterDepth = 0;
            switch (variant) {
                case SHIELD -> {
                    radius = 90 + rnd.nextInt(20);
                    height = 22 + rnd.nextInt(10);
                    craterR = 6 + rnd.nextInt(4);
                    craterDepth = 4 + rnd.nextInt(3);
                }
                case STRATO -> {
                    radius = 62 + rnd.nextInt(14);
                    height = 62 + rnd.nextInt(24);
                    craterR = 8 + rnd.nextInt(4);
                    craterDepth = 11 + rnd.nextInt(5);
                }
                case CINDER -> {
                    radius = 30 + rnd.nextInt(8);
                    height = 20 + rnd.nextInt(8);
                    craterR = 9 + rnd.nextInt(3);
                    craterDepth = 8 + rnd.nextInt(3);
                }
                case DOME -> {
                    radius = 24 + rnd.nextInt(10);
                    height = 22 + rnd.nextInt(10);
                }
                case CALDERA -> {
                    radius = 100 + rnd.nextInt(15);
                    height = 22 + rnd.nextInt(10);
                }
                default -> {
                    radius = 66 + rnd.nextInt(12);
                    height = 12 + rnd.nextInt(6);
                }
            }

            int base = (ground.applyAsInt(cx, cz) * 2 + ground.applyAsInt(cx + radius / 2, cz) +
                    ground.applyAsInt(cx - radius / 2, cz) + ground.applyAsInt(cx, cz + radius / 2) +
                    ground.applyAsInt(cx, cz - radius / 2)) / 6;
            if (base < seaLevel - 4) return null;

            int count = variant == SHIELD ? 5 + rnd.nextInt(4) : variant == STRATO ? 3 + rnd.nextInt(3) :
                    variant == CINDER ? 2 + rnd.nextInt(2) : 0;
            if (state != ACTIVE && variant != FISSURE) count = 0;
            if (variant == FISSURE) count = 1;
            double[] angles = new double[count];
            for (int i = 0; i < count; i++) angles[i] = rnd.nextDouble() * Math.PI * 2.0;
            return new Instance(Kind.VOLCANO, cx, cz, radius, instSeed, base, height, craterR, craterDepth, angles,
                    null, null, state, variant);
        }

        boolean karst = kind == Kind.KARST;
        int count = karst ? 3 + rnd.nextInt(4) : 4 + rnd.nextInt(4);
        int spread = karst ? 26 : 16;
        Pillar[] pillars = new Pillar[count];
        int reach = 0;
        for (int i = 0; i < count; i++) {
            int px = cx + rnd.nextInt(spread * 2 + 1) - spread;
            int pz = cz + rnd.nextInt(spread * 2 + 1) - spread;
            int radius = karst ? 5 + rnd.nextInt(5) : 2 + rnd.nextInt(3);
            int height = karst ? 25 + rnd.nextInt(36) : 10 + rnd.nextInt(21);
            int top = ground.applyAsInt(px, pz) + height;
            pillars[i] = new Pillar(px, pz, radius, top);
            reach = Math.max(reach, (int) Math.ceil(Math.hypot(px - cx, pz - cz) + radius * 1.6 + 2));
        }
        return new Instance(kind, cx, cz, reach, instSeed, 0, 0, 0, 0, null, pillars, palette(kind), 0, 0);
    }

    private int pickVolcano(RandomSource rnd) {
        float r = rnd.nextFloat();
        return switch (disciplineId) {
            case "phoenix" -> r < 0.22f ? SHIELD : r < 0.46f ? STRATO : r < 0.66f ? CINDER : r < 0.78f ? DOME :
                    r < 0.88f ? CALDERA : FISSURE;
            case "sealed_b" -> r < 0.15f ? STRATO : r < 0.45f ? CINDER : r < 0.60f ? DOME : FISSURE;
            default -> rnd.nextInt(6);
        };
    }

    private Kind pick(float roll) {
        return switch (disciplineId) {
            case "phoenix" -> roll < 0.20f ? Kind.VOLCANO : roll < 0.38f ? Kind.HOODOOS :
                    roll < 0.60f ? Kind.PLATEAU : null;
            case "sculk" -> roll < 0.15f ? Kind.KARST : roll < 0.20f ? Kind.HOODOOS :
                    roll < 0.25f ? Kind.PLATEAU : roll < 0.37f ? Kind.CENOTE : roll < 0.45f ? Kind.FUNGUS :
                            roll < 0.50f ? Kind.RIBS : null;
            case "void" -> roll < 0.10f ? Kind.HOODOOS : roll < 0.15f ? Kind.PLATEAU :
                    roll < 0.30f ? Kind.SKY_ISLAND : roll < 0.40f ? Kind.GEODE : roll < 0.45f ? Kind.IMPACT : null;
            case "sealed_a" -> roll < 0.08f ? Kind.HOODOOS : roll < 0.15f ? Kind.KARST : roll < 0.20f ? Kind.PLATEAU :
                    roll < 0.35f ? Kind.PIT_MINE : roll < 0.47f ? Kind.TOWERS : roll < 0.55f ? Kind.OVERPASS : null;
            case "sealed_b" -> roll < 0.05f ? Kind.KARST : roll < 0.15f ? Kind.VOLCANO : roll < 0.20f ? Kind.PLATEAU :
                    roll < 0.32f ? Kind.COLUMNS : roll < 0.40f ? Kind.GEYSER : roll < 0.45f ? Kind.RIBS : null;
            default -> null;
        };
    }

    private Palette palette(Kind kind) {
        return switch (disciplineId) {
            case "phoenix" -> new Palette(new BlockState[] { Blocks.BASALT.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState() }, 3, Blocks.BLACKSTONE.defaultBlockState(),
                    Blocks.MAGMA_BLOCK.defaultBlockState());
            case "sculk" -> new Palette(new BlockState[] { Blocks.DEEPSLATE.defaultBlockState(),
                    Blocks.TUFF.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState() }, 4,
                    Blocks.SCULK.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState());
            case "void" -> new Palette(new BlockState[] { Blocks.END_STONE.defaultBlockState(),
                    Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState() }, 3,
                    Blocks.OBSIDIAN.defaultBlockState(), Blocks.END_STONE.defaultBlockState());
            case "sealed_a" -> kind == Kind.KARST ?
                    new Palette(
                            new BlockState[] { Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(),
                                    Blocks.STONE.defaultBlockState(), Blocks.CALCITE.defaultBlockState() },
                            4,
                            Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState()) :
                    new Palette(new BlockState[] { Blocks.RED_TERRACOTTA.defaultBlockState(),
                            Blocks.ORANGE_TERRACOTTA.defaultBlockState(), Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
                            Blocks.TERRACOTTA.defaultBlockState(), Blocks.WHITE_TERRACOTTA.defaultBlockState() }, 2,
                            Blocks.TERRACOTTA.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState());
            default -> new Palette(new BlockState[] { Blocks.STONE.defaultBlockState(),
                    Blocks.ANDESITE.defaultBlockState(), Blocks.STONE.defaultBlockState() }, 4,
                    Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState());
        };
    }

    static final int SHIELD = 0;
    static final int STRATO = 1;
    static final int CINDER = 2;
    static final int DOME = 3;
    static final int CALDERA = 4;
    static final int FISSURE = 5;

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double crateredCone(Instance v, double r, double exponent, double lipHeight) {
        double radius = v.reach();
        double t = r / radius;
        if (t >= 1.0) return 0.0;
        double rc = v.craterRadius();
        if (r < rc) {
            double rim = v.height() * Math.pow(1.0 - rc / radius, exponent) + lipHeight;
            double q = r / rc;
            return rim - v.craterDepth() * (1.0 - q * q);
        }
        double bump = Math.max(0.0, 1.0 - (r - rc) / 4.0) * lipHeight;
        return v.height() * Math.pow(1.0 - t, exponent) + bump;
    }

    private static double edgeT(Instance v, double dx, double dz) {
        if (v.variant() == FISSURE) {
            double theta = v.rivuletAngles()[0];
            double u = dx * Math.cos(theta) + dz * Math.sin(theta);
            double w = -dx * Math.sin(theta) + dz * Math.cos(theta);
            double l = v.reach() - 6.0;
            return Math.sqrt((u / l) * (u / l) + (w / FISSURE_WIDTH) * (w / FISSURE_WIDTH));
        }
        return Math.sqrt(dx * dx + dz * dz) / v.reach();
    }

    private static final double FISSURE_WIDTH = 15.0;

    private static double relief(Instance v, double dx, double dz) {
        double radius = v.reach();
        double r = Math.sqrt(dx * dx + dz * dz);
        double height = v.height();
        switch (v.variant()) {
            case SHIELD:
                return crateredCone(v, r, 1.15, 1.5);
            case STRATO: {
                double h = crateredCone(v, r, 1.6, 3.0);
                double t = r / radius;

                double gully = Math.abs(Math.sin(Math.atan2(dz, dx) * 9.0 + (v.seed() & 255) * 0.1));
                double zone = smooth((t - 0.12) / 0.2) * (1.0 - smooth((t - 0.7) / 0.3));
                return r < v.craterRadius() ? h : h * (1.0 - 0.10 * gully * zone);
            }
            case CINDER:
                return crateredCone(v, r, 1.0, 3.0);
            case DOME: {
                double t = r / radius;
                return t >= 1.0 ? 0.0 : height * Math.sqrt(1.0 - t * t);
            }
            case CALDERA: {
                double r0 = radius * 0.5;
                double w = r < r0 ? radius * 0.13 : radius * 0.15;
                double ring = height * Math.exp(-Math.pow((r - r0) / w, 2.0));
                if (r >= r0) return ring;
                double floor = -4.0 + (r < 16.0 ? 10.0 * (1.0 - (r / 16.0) * (r / 16.0)) : 0.0);
                return ring + floor * (1.0 - ring / height);
            }
            default: {
                double theta = v.rivuletAngles()[0];
                double u = dx * Math.cos(theta) + dz * Math.sin(theta);
                double w = -dx * Math.sin(theta) + dz * Math.cos(theta);
                double length = radius - 6.0;
                double d2 = (u / length) * (u / length) + (w / FISSURE_WIDTH) * (w / FISSURE_WIDTH);
                if (d2 >= 1.0) return 0.0;
                double h = height * Math.pow(1.0 - Math.sqrt(d2), 1.2);
                if (Math.abs(w) < 2.3 && Math.abs(u) < length * 0.85) h = Math.min(h, height * 0.30);

                for (int k = -1; k <= 1; k++) {
                    double du = u - k * length * 0.5;
                    double dd = Math.sqrt(du * du + w * w);
                    h += 9.0 * Math.max(0.0, 1.0 - dd / 6.5);
                }
                return h;
            }
        }
    }

    private static double poolLevel(Instance v) {
        double radius = v.reach();
        double rc = v.craterRadius();
        return switch (v.variant()) {
            case SHIELD -> v.height() * Math.pow(1.0 - rc / radius, 1.15) + 1.5 - v.craterDepth() + 3.0;
            case STRATO -> v.height() * Math.pow(1.0 - rc / radius, 1.6) + 3.0 - v.craterDepth() + 4.0;
            case CINDER -> v.height() * (1.0 - rc / radius) + 3.0 - v.craterDepth() + 4.0;
            case CALDERA -> -1.0;
            case FISSURE -> v.height() * 0.45;
            default -> Double.NaN;
        };
    }

    private static boolean poolCell(Instance v, double dx, double dz, double h, double poolLevel) {
        if (Double.isNaN(poolLevel)) return false;
        switch (v.variant()) {
            case SHIELD:
            case STRATO:
            case CINDER:
                return Math.sqrt(dx * dx + dz * dz) < v.craterRadius();
            case CALDERA:
                return h < poolLevel;
            default: {
                double theta = v.rivuletAngles()[0];
                double u = dx * Math.cos(theta) + dz * Math.sin(theta);
                double w = -dx * Math.sin(theta) + dz * Math.cos(theta);
                return Math.abs(w) < 2.6 && Math.abs(u) < (v.reach() - 6.0) * 0.85 && h < poolLevel;
            }
        }
    }

    static int surfaceGround(ChunkAccess chunk, int x, int z, int minY, int maxY,
                             net.minecraft.core.BlockPos.MutableBlockPos pos) {
        for (int y = maxY - 1; y >= minY; y--) {
            BlockState state = chunk.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && state.getFluidState().isEmpty()) return y;
        }
        return minY;
    }

    static int hash2(long seed, int x, int z) {
        return (int) (mix(seed, x, z) >>> 33);
    }

    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState TUFF = Blocks.TUFF.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();

    private static BlockState volcanoSurface(Instance v, double dx, double dz, double h, int rough) {
        boolean hot = v.state() == ACTIVE;
        boolean extinct = v.state() == EXTINCT;
        double f = Math.max(0.0, h / v.height());
        int roll = rough & 15;
        double r = Math.sqrt(dx * dx + dz * dz);

        switch (v.variant()) {
            case SHIELD: {
                if (r < v.craterRadius() * 0.85) return extinct ? COARSE : BASALT;
                BlockState s = roll < 6 ? SMOOTH_BASALT : roll < 8 ? BLACKSTONE : BASALT;
                if (hot && (rough & 31) == 0) s = MAGMA;
                if (!hot && f < 0.4 && roll >= 11) s = roll >= 14 ? MOSS : COARSE;
                return s;
            }
            case STRATO: {
                if (r < v.craterRadius() * 0.85) return extinct ? COARSE : BASALT;

                int band = Math.floorMod((int) (h / 3.0) + ((rough & 7) == 0 ? 1 : 0), 4);
                BlockState s = switch (band) {
                    case 0 -> TUFF;
                    case 1 -> BASALT;
                    case 2 -> BLACKSTONE;
                    default -> SMOOTH_BASALT;
                };
                if (hot && f > 0.8 && (rough & 7) == 0) s = MAGMA;
                if (!hot && f < 0.3 && roll >= 10) s = roll >= 13 ? MOSS : COARSE;
                return s;
            }
            case CINDER: {
                if (r < v.craterRadius() * 0.8) return hot && roll < 3 ? MAGMA : BASALT;
                BlockState s = roll < 6 ? GRAVEL : roll < 11 ? BLACKSTONE : roll < 14 ? BASALT : TUFF;
                if (extinct && f < 0.4 && roll == 15) s = MOSS;
                return s;
            }
            case DOME: {
                BlockState s = roll < 6 ? OBSIDIAN : roll < 12 ? BLACKSTONE : BASALT;

                if (hot && (rough & 15) == 0 || hot && f > 0.9 && (rough & 3) == 0) s = MAGMA;
                if (extinct && f < 0.3 && roll == 15) s = MOSS;
                return s;
            }
            case CALDERA: {
                double r0 = v.reach() * 0.5;
                if (h > v.height() * 0.6) return roll < 8 ? BLACKSTONE : BASALT;
                if (r > r0) return roll < 5 ? TUFF : roll < 10 ? BASALT : roll < 13 ? BLACKSTONE : COARSE;
                if (h < 2.0) return roll < 8 ? GRAVEL : roll < 13 ? BLACKSTONE : BASALT;
                return roll < 8 ? OBSIDIAN : roll < 13 ? BLACKSTONE : (hot && roll == 15 ? MAGMA : BASALT);
            }
            default: {
                double theta = v.rivuletAngles()[0];
                double w = -dx * Math.sin(theta) + dz * Math.cos(theta);
                if (Math.abs(w) < 2.6) return hot ? MAGMA : BASALT;
                BlockState s = roll < 5 ? BLACKSTONE : roll < 11 ? BASALT : roll < 13 ? SMOOTH_BASALT : TUFF;
                if (hot && f > 0.5 && roll == 15) s = MAGMA;
                if (extinct && f < 0.25 && roll >= 13) s = MOSS;
                return s;
            }
        }
    }

    private void volcano(ChunkAccess chunk, Instance v, int minX, int minZ, int minY, int maxY) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        double poolLevel = poolLevel(v);
        int lakeY = Double.isNaN(poolLevel) ? Integer.MIN_VALUE : v.baseY() + (int) Math.round(poolLevel);
        boolean channels = v.variant() == SHIELD || v.variant() == STRATO || v.variant() == CINDER;
        double channelHalf = v.variant() == SHIELD ? 3.0 : v.variant() == STRATO ? 1.6 : 1.8;

        for (int x = minX; x < minX + 16; x++) {
            double dx = x - v.cx();
            for (int z = minZ; z < minZ + 16; z++) {
                double dz = z - v.cz();
                double t = edgeT(v, dx, dz);
                if (t >= 1.0) continue;

                int ground = surfaceGround(chunk, x, z, minY, maxY, pos);
                double w = smooth((1.0 - t) / 0.35);
                double h = relief(v, dx, dz);
                double target = v.baseY() + h;
                int top = (int) Math.round(ground + (target - ground) * w);
                top = Math.max(minY + 1, Math.min(maxY - 2, top));

                int rough = hash2(v.seed(), x, z);
                BlockState surface = volcanoSurface(v, dx, dz, h, rough);

                double r = Math.sqrt(dx * dx + dz * dz);
                if (channels && v.state() == ACTIVE && r > v.craterRadius() + 2 && h > 3) {
                    double angle = Math.atan2(dz, dx);
                    double half = channelHalf / Math.max(r, 1.0);
                    for (double a : v.rivuletAngles()) {
                        double diff = Math.abs(Math.atan2(Math.sin(angle - a), Math.cos(angle - a)));
                        if (diff < half) {
                            surface = MAGMA;
                            break;
                        }
                    }
                }

                if (top > ground) {
                    for (int y = ground + 1; y < top; y++) chunk.setBlockState(pos.set(x, y, z), BASALT, false);
                } else if (top < ground) {
                    for (int y = top + 1; y <= ground; y++) chunk.setBlockState(pos.set(x, y, z), air, false);
                }
                chunk.setBlockState(pos.set(x, top, z), surface, false);
                for (int d = 1; d <= 2; d++) {
                    if (top - d > minY) chunk.setBlockState(pos.set(x, top - d, z), BASALT, false);
                }

                if (v.state() != EXTINCT && top < lakeY && poolCell(v, dx, dz, h, poolLevel)) {
                    BlockState fluid = v.state() == ACTIVE ? lava : water;
                    for (int y = top + 1; y <= lakeY; y++) chunk.setBlockState(pos.set(x, y, z), fluid, false);
                }
            }
        }
    }

    java.util.List<Instance> near(int x, int z, int margin, int seaLevel, IntBinaryOperator ground) {
        java.util.List<Instance> out = new java.util.ArrayList<>(4);
        if (!enabled()) return out;
        int cellX = Math.floorDiv(x, CELL);
        int cellZ = Math.floorDiv(z, CELL);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                Optional<Instance> found = cache.computeIfAbsent(key(cx, cz),
                        k -> Optional.ofNullable(create(cx, cz, seaLevel, ground)));
                if (found.isPresent() && Math.hypot(x - found.get().cx(), z - found.get().cz()) <
                        found.get().reach() + margin) {
                    out.add(found.get());
                }
            }
        }
        return out;
    }

    public record Sighting(String key, String label, String color, String icon, int x, int z, int baseY,
                           int reach, java.util.List<Zone> zones) {}

    public enum ZoneType {
        UPDRAFT,
        LOW_GRAVITY,
        CONVEYOR,
        BOUNCE,
        SLICK,

        GEYSER
    }

    public enum ZoneShape {
        CIRCLE,

        RECT
    }

    public record Zone(ZoneType type, ZoneShape shape, double x, double z, double a, double b, double theta,
                       long phase) {

        public static Zone circle(ZoneType type, double x, double z, double radius) {
            return new Zone(type, ZoneShape.CIRCLE, x, z, radius, radius, 0.0, 0L);
        }

        public static Zone rect(ZoneType type, double x, double z, double halfLength, double halfWidth,
                                double theta) {
            return new Zone(type, ZoneShape.RECT, x, z, halfLength, halfWidth, theta, 0L);
        }
    }

    private java.util.List<Zone> zonesOf(Instance inst) {
        java.util.List<Zone> zones = new java.util.ArrayList<>();
        double cx = inst.cx();
        double cz = inst.cz();
        switch (inst.kind()) {
            case VOLCANO -> {
                if (inst.state() == ACTIVE && inst.variant() != DOME) {
                    if (inst.variant() == FISSURE) {
                        zones.add(Zone.rect(ZoneType.UPDRAFT, cx, cz, (inst.reach() - 6.0) * 0.85, 4.0,
                                inst.rivuletAngles()[0]));
                    } else if (inst.variant() == CALDERA) {
                        zones.add(Zone.circle(ZoneType.UPDRAFT, cx, cz, inst.reach() * 0.35));
                    } else {
                        zones.add(Zone.circle(ZoneType.UPDRAFT, cx, cz, inst.craterRadius() * 1.3));
                    }
                }
            }
            case SKY_ISLAND -> {
                for (Pillar p : inst.pillars()) {
                    zones.add(Zone.circle(ZoneType.LOW_GRAVITY, p.x(), p.z(), p.radius() + 5.0));
                }
            }
            case GEODE -> zones.add(Zone.circle(ZoneType.LOW_GRAVITY, cx, cz, inst.height() - 4.0));
            case IMPACT -> zones.add(Zone.circle(ZoneType.LOW_GRAVITY, cx, cz, inst.craterRadius()));
            case CENOTE -> zones.add(Zone.circle(ZoneType.LOW_GRAVITY, cx, cz, inst.craterRadius() * 0.95));
            case FUNGUS -> {
                for (Pillar p : inst.pillars()) {
                    zones.add(Zone.circle(ZoneType.BOUNCE, p.x(), p.z(), p.radius() * 4.0 + 2.0));
                }
            }
            case RIBS -> {
                if (inst.variant() == 1) {
                    double half = (inst.craterRadius() * inst.craterDepth() + 34) * 0.5;
                    zones.add(Zone.rect(ZoneType.UPDRAFT, cx, cz, half, 6.5, inst.rivuletAngles()[0]));
                }
            }
            case PIT_MINE -> zones.add(Zone.circle(ZoneType.CONVEYOR, cx, cz, inst.craterRadius()));
            case TOWERS -> {
                for (Pillar p : inst.pillars()) {
                    zones.add(Zone.circle(ZoneType.UPDRAFT, p.x(), p.z(), Math.max(2.0, p.radius() - 1.0)));
                }
            }
            case OVERPASS -> zones.add(Zone.rect(ZoneType.CONVEYOR, cx, cz,
                    inst.craterRadius() * inst.craterDepth() / 2.0 + 4.0, 4.5, inst.rivuletAngles()[0]));
            case COLUMNS -> zones.add(Zone.circle(ZoneType.SLICK, cx, cz, inst.reach()));
            case GEYSER -> {
                for (Pillar p : inst.pillars()) {
                    zones.add(new Zone(ZoneType.GEYSER, ZoneShape.CIRCLE, p.x(), p.z(), 2.4, 2.4, 0.0,
                            mix(inst.seed(), p.x(), p.z())));
                }
            }
            default -> {}
        }
        return zones;
    }

    public java.util.List<Sighting> sightingsNear(int x, int z, int margin, int seaLevel,
                                                  IntBinaryOperator ground) {
        java.util.List<Sighting> out = new java.util.ArrayList<>(4);
        for (Instance inst : near(x, z, margin, seaLevel, ground)) out.add(describe(inst));
        return out;
    }

    private Sighting describe(Instance inst) {
        String label;
        String color;
        String icon;
        switch (inst.kind()) {
            case VOLCANO -> {
                label = switch (inst.state()) {
                    case ACTIVE -> "Active Volcano";
                    case DORMANT -> "Dormant Volcano";
                    default -> "Extinct Volcano";
                };
                color = "FF5A30";
                icon = "DANGER";
            }
            case HOODOOS -> {
                label = "Hoodoos";
                color = "C8A070";
                icon = "CIRCLE";
            }
            case KARST -> {
                label = "Karst Towers";
                color = "B0C8A0";
                icon = "CIRCLE";
            }
            case PLATEAU -> {
                label = "Plateau";
                color = "C8B090";
                icon = "CIRCLE";
            }
            case SEA_STACKS -> {
                label = "Sea Stacks";
                color = "70B8D8";
                icon = "CIRCLE";
            }
            case CENOTE -> {
                label = "Cenote";
                color = "30C8C8";
                icon = "RESOURCE";
            }
            case FUNGUS -> {
                label = "Giant Fungus";
                color = "8AE08A";
                icon = "STAR";
            }
            case RIBS -> {
                label = "Rib Arches";
                color = "E0E0D0";
                icon = "CIRCLE";
            }
            case SKY_ISLAND -> {
                label = "Floating Island";
                color = "B890FF";
                icon = "STAR";
            }
            case GEODE -> {
                label = "Geode";
                color = "E070FF";
                icon = "STAR";
            }
            case IMPACT -> {
                label = "Impact Crater";
                color = "FFA040";
                icon = "DANGER";
            }
            case PIT_MINE -> {
                label = "Pit Mine";
                color = "C0A060";
                icon = "RESOURCE";
            }
            case TOWERS -> {
                label = "Cooling Towers";
                color = "C0C0C0";
                icon = "CIRCLE";
            }
            case OVERPASS -> {
                label = "Overpass";
                color = "A0A0A0";
                icon = "CIRCLE";
            }
            case COLUMNS -> {
                label = "Basalt Columns";
                color = "909098";
                icon = "CIRCLE";
            }
            case GEYSER -> {
                label = "Geyser";
                color = "60E0FF";
                icon = "DANGER";
            }
            default -> {
                label = "Landform";
                color = "FFFFFF";
                icon = "DOT";
            }
        }
        String key = disciplineId + ":" + inst.kind().name().toLowerCase(java.util.Locale.ROOT) + ":" + inst.cx() +
                ":" + inst.cz();
        return new Sighting(key, label, color, icon, inst.cx(), inst.cz(), inst.baseY(), inst.reach(),
                zonesOf(inst));
    }

    public boolean occupies(int x, int z, int margin, int seaLevel, IntBinaryOperator ground) {
        if (!enabled()) return false;
        int cellX = Math.floorDiv(x, CELL);
        int cellZ = Math.floorDiv(z, CELL);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                Optional<Instance> found = cache.computeIfAbsent(key(cx, cz),
                        k -> Optional.ofNullable(create(cx, cz, seaLevel, ground)));
                if (found.isEmpty()) continue;
                Instance inst = found.get();
                if (Math.hypot(x - inst.cx(), z - inst.cz()) < inst.reach() + margin) return true;
            }
        }
        return false;
    }

    private void plateau(ChunkAccess chunk, Instance inst, int minX, int minZ, int minY, int maxY) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        Palette palette = inst.palette();
        double radius = inst.reach() - 6;
        int top = inst.baseY() + inst.height();
        BlockState liquid = disciplineId.equals("phoenix") || disciplineId.equals("sealed_b") ?
                Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double d = Math.hypot(x - inst.cx(), z - inst.cz());

                double edge = radius + ((hash2(inst.seed(), x, z) & 7) - 3.5) * 0.5;
                if (d > edge + 6) continue;

                int columnTop = d <= edge ? top : top - (int) ((d - edge) * (inst.height() / 6.0));
                int ground = surfaceGround(chunk, x, z, minY, maxY, pos);
                if (columnTop <= ground) continue;
                int to = Math.min(maxY - 2, columnTop);
                for (int y = Math.max(minY + 1, ground + 1); y <= to; y++) {
                    BlockState state;
                    if (y == to) {
                        state = (hash2(inst.seed(), x + y, z) & 3) == 0 ? palette.capAlt() : palette.cap();
                    } else {
                        int band = Math.floorMod((y + (int) (inst.seed() & 31)) / palette.bandHeight(),
                                palette.bands().length);
                        state = palette.bands()[band];
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }

                if (inst.rivuletAngles() != null && inst.craterRadius() > 0 && columnTop > ground) {
                    double a = inst.rivuletAngles()[0];
                    double along = (x - inst.cx()) * Math.cos(a) + (z - inst.cz()) * Math.sin(a);
                    double across = Math.abs(-(x - inst.cx()) * Math.sin(a) + (z - inst.cz()) * Math.cos(a));
                    if (across <= 1.2 && along >= inst.craterRadius() && along <= edge + 6) {
                        chunk.setBlockState(pos.set(x, columnTop, z), Blocks.LAVA.defaultBlockState(), false);
                    }
                }
                if (inst.craterRadius() > 0 && d <= inst.craterRadius() && columnTop == top) {
                    chunk.setBlockState(pos.set(x, top, z), liquid, false);
                    chunk.setBlockState(pos.set(x, top - 1, z), palette.bands()[0], false);
                }
            }
        }
    }

    private void pillars(ChunkAccess chunk, Instance inst, int minX, int minZ, int minY, int maxY) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        boolean hoodoo = inst.kind() == Kind.HOODOOS;
        Palette palette = inst.palette();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                Pillar best = null;
                int bestTop = Integer.MIN_VALUE;
                boolean capOnly = false;
                double bestD = 0;
                for (Pillar p : inst.pillars()) {
                    double d = Math.hypot(x - p.x(), z - p.z());

                    double edge = p.radius() + ((hash2(inst.seed(), x, z) & 3) - 1.5) * 0.35;
                    boolean inShaft = d <= edge;
                    boolean inCap = hoodoo && d <= edge * 1.5;
                    if (!inShaft && !inCap) continue;
                    int taper = hoodoo ? 5 : 2;
                    int top = p.top() - (int) (taper * (d / Math.max(1.0, edge)) * (d / Math.max(1.0, edge)));
                    if (top > bestTop) {
                        best = p;
                        bestTop = top;
                        capOnly = !inShaft;
                        bestD = d;
                    }
                }
                if (best == null) continue;

                int ground = surfaceGround(chunk, x, z, minY, maxY, pos);
                int from = capOnly ? bestTop - 2 : ground + 1;
                if (bestTop <= ground && !capOnly) continue;
                int to = Math.min(maxY - 2, bestTop);
                for (int y = Math.max(minY + 1, from); y <= to; y++) {
                    BlockState state;
                    if (y == to) {
                        state = (hash2(inst.seed(), x + y, z) & 3) == 0 ? palette.capAlt() : palette.cap();
                    } else {
                        int band = Math.floorMod((y + (int) (inst.seed() & 31)) / palette.bandHeight(),
                                palette.bands().length);
                        state = palette.bands()[band];
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }
            }
        }
    }
}
