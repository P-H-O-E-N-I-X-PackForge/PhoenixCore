package net.phoenix.core.integration.conflux.dimension.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.integration.conflux.dimension.physics.PhysicsHook;
import net.phoenix.core.integration.conflux.dimension.physics.PhysicsImpulse;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.S2CPhysicsImpulsePacket;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

/**
 * What the landforms do to whoever stands in them. Every landform that has a physical character carries it with it, worked
 * out from the same seed that built it, so the physics always sits exactly where the terrain is:
 *
 * <ul>
 * <li>Active volcanoes, the cooling towers and the canyon ribs breathe updrafts.
 * <li>Floating islands, geodes, impact craters and cenotes weaken gravity (and so make the fall safe).
 * <li>Fungus caps bounce, geysers erupt on a cycle, polished basalt is slick.
 * <li>The pit-mine haul road and the overpass decks are conveyors.
 * </ul>
 *
 * Only landforms near a player are looked at, and each is handled once a tick however many players are close. Mobs and
 * items are pushed on the server; a player is sent the nudge and applies it to their own motion.
 */
public final class LandformPhysics {

    private static final int RANGE_MARGIN = 40;

    private LandformPhysics() {}

    public static void tick(ServerLevel level, Landforms landforms, int seaLevel, IntBinaryOperator ground) {
        if (level.players().isEmpty()) return;
        long time = level.getGameTime();
        Set<Landforms.Instance> done = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            for (Landforms.Instance inst : landforms.near(player.getBlockX(), player.getBlockZ(), RANGE_MARGIN,
                    seaLevel, ground)) {
                if (done.add(inst)) apply(level, inst, time);
            }
        }
    }

    private static void apply(ServerLevel level, Landforms.Instance v, long time) {
        switch (v.kind()) {
            case VOLCANO -> volcano(level, v, time);
            case SKY_ISLAND -> skyIslands(level, v);
            case GEODE -> geode(level, v, time);
            case IMPACT -> well(level, v, v.craterRadius(), v.baseY() - v.craterDepth() - 2, v.baseY() + 14);
            case CENOTE -> well(level, v, v.craterRadius() * 0.95, v.baseY() - v.craterDepth(), v.baseY() + 12);
            case FUNGUS -> fungus(level, v);
            case RIBS -> {
                if (v.variant() == 1) canyon(level, v);
            }
            case PIT_MINE -> pitMine(level, v);
            case TOWERS -> towers(level, v);
            case OVERPASS -> overpass(level, v);
            case COLUMNS -> columns(level, v);
            case GEYSER -> geysers(level, v, time);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- shared

    private static boolean affected(Entity e) {
        if (e.isSpectator() || e.isPassenger() || PhysicsHook.exempt(e)) return false;
        if (e instanceof Player p && p.getAbilities().flying) return false;
        return e instanceof LivingEntity || e instanceof ItemEntity;
    }

    private static void each(ServerLevel level, AABB box, Predicate<Entity> test, java.util.function.Consumer<Entity> action) {
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, LandformPhysics::affected)) {
            if (test.test(e)) action.accept(e);
        }
    }

    private static void send(Entity e, PhysicsImpulse impulse) {
        if (e instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new S2CPhysicsImpulsePacket(impulse));
            if (impulse.resetFall()) player.fallDistance = 0.0f;
        } else {
            impulse.apply(e);
            e.hurtMarked = true;
        }
        if (impulse.resetFall() && e instanceof LivingEntity living) {
            // leaving an updraft or a weak-gravity pocket high up must not end in a fall
            slowFall(living);
        }
    }

    private static void slowFall(LivingEntity living) {
        MobEffectInstance current = living.getEffect(MobEffects.SLOW_FALLING);
        if (current == null || current.getDuration() < 120) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, false));
        }
    }

    /** Weak gravity: Slow Falling for the living (which also makes the way down safe), a lighter pull for items. */
    private static void floaty(Entity e) {
        e.fallDistance = 0.0f;
        if (e instanceof LivingEntity living) {
            slowFall(living);
        } else {
            var m = e.getDeltaMovement();
            e.setDeltaMovement(m.x, m.y + 0.03, m.z);
            e.hurtMarked = true;
        }
    }

    private static boolean standingOn(ServerLevel level, Entity e, Block... blocks) {
        if (!e.onGround()) return false;
        Block below = level.getBlockState(BlockPos.containing(e.getX(), e.getY() - 0.2, e.getZ())).getBlock();
        for (Block block : blocks) {
            if (below == block) return true;
        }
        return false;
    }

    private static double horizontal(Entity e, double cx, double cz) {
        return Math.hypot(e.getX() - cx, e.getZ() - cz);
    }

    private static AABB box(double cx, double cz, double reach, double y0, double y1) {
        return new AABB(cx - reach, y0, cz - reach, cx + reach, y1, cz + reach);
    }

    // ---------------------------------------------------------------- phoenix: thermals over an active volcano

    private static void volcano(ServerLevel level, Landforms.Instance v, long time) {
        if (v.state() != Landforms.ACTIVE || v.variant() == Landforms.DOME) return;
        double cx = v.cx();
        double cz = v.cz();
        AABB area = box(cx, cz, v.reach(), v.baseY() - 8, v.baseY() + v.height() + 52);

        if (time % 4 == 0) {
            double spread = v.variant() == Landforms.CALDERA ? v.reach() * 0.25 : Math.max(2.0, v.craterRadius() * 0.4);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, v.baseY() + v.height() + 3, cz, 2, spread, 1.0,
                    spread, 0.02);
        }

        Predicate<Entity> inside;
        if (v.variant() == Landforms.FISSURE) {
            double theta = v.rivuletAngles()[0];
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            double half = (v.reach() - 6.0) * 0.85;
            inside = e -> {
                double dx = e.getX() - cx;
                double dz = e.getZ() - cz;
                return Math.abs(-dx * sin + dz * cos) < 4.0 && Math.abs(dx * cos + dz * sin) < half;
            };
        } else if (v.variant() == Landforms.CALDERA) {
            double radius = v.reach() * 0.35;
            inside = e -> horizontal(e, cx, cz) < radius;
        } else {
            double radius = v.craterRadius() * 1.3;
            inside = e -> horizontal(e, cx, cz) < radius;
        }
        each(level, area, inside, e -> send(e, PhysicsImpulse.lift(0.06, 0.38)));
    }

    // ---------------------------------------------------------------- void: floating islands, geodes, craters

    private static void skyIslands(ServerLevel level, Landforms.Instance v) {
        for (Landforms.Pillar p : v.pillars()) {
            double reach = p.radius() + 5.0;
            AABB area = box(p.x(), p.z(), reach, p.top() - p.radius() - 9, p.top() + 30);
            each(level, area, e -> !e.onGround() && horizontal(e, p.x(), p.z()) < reach,
                    LandformPhysics::floaty);
        }
    }

    private static void geode(ServerLevel level, Landforms.Instance v, long time) {
        double radius = v.height();
        double inner = radius - 4.0;
        double centreY = v.baseY() + Math.round(radius * 0.35);
        if (time % 120 == (v.seed() & 63)) {
            level.playSound(null, v.cx(), centreY, v.cz(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 2.0f,
                    0.8f);
        }
        each(level, box(v.cx(), v.cz(), radius, v.baseY() - 1, centreY + radius), e -> {
            if (e.onGround() || e.getY() < v.baseY() - 1) return false;
            double dx = e.getX() - v.cx();
            double dy = e.getY() + e.getBbHeight() * 0.5 - centreY;
            double dz = e.getZ() - v.cz();
            return Math.sqrt(dx * dx + dy * dy + dz * dz) < inner;
        }, LandformPhysics::floaty);
    }

    /** A pocket of weak gravity: an impact crater, or the shaft of a cenote (a slow and safe way down). */
    private static void well(ServerLevel level, Landforms.Instance v, double radius, double y0, double y1) {
        each(level, box(v.cx(), v.cz(), radius, y0, y1), e -> !e.onGround() && horizontal(e, v.cx(), v.cz()) < radius,
                LandformPhysics::floaty);
    }

    // ---------------------------------------------------------------- sculk: bouncy fungus caps, canyon thermals

    private static void fungus(ServerLevel level, Landforms.Instance v) {
        for (Landforms.Pillar p : v.pillars()) {
            double capRadius = p.radius() * 4 + 2;
            AABB area = box(p.x(), p.z(), capRadius, p.top() - 6, p.top() + capRadius * 0.5 + 4);
            each(level, area, e -> {
                if (!e.onGround()) return false;
                double d = horizontal(e, p.x(), p.z());
                if (d > capRadius) return false;
                double f = d / capRadius;
                double surface = p.top() + Math.round(capRadius * 0.5 * Math.sqrt(Math.max(0.0, 1.0 - f * f))) + 1;
                return Math.abs(e.getY() - surface) < 1.3;
            }, e -> {
                send(e, PhysicsImpulse.bounce(0.95));
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS,
                        0.8f, 1.2f);
            });
        }
    }

    private static void canyon(ServerLevel level, Landforms.Instance v) {
        double theta = v.rivuletAngles()[0];
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double canyonHalf = (v.craterRadius() * v.craterDepth() + 34) * 0.5;
        each(level, box(v.cx(), v.cz(), v.reach(), v.baseY() - 14, v.baseY() + 24), e -> {
            double dx = e.getX() - v.cx();
            double dz = e.getZ() - v.cz();
            double u = dx * cos + dz * sin;
            double w = -dx * sin + dz * cos;
            double half = (5.5 + 1.5 * Math.sin(u * 0.21 + (v.seed() & 63))) *
                    Landforms.smooth((canyonHalf - Math.abs(u)) / 10.0);
            return half > 1.0 && Math.abs(w) < half;
        }, e -> send(e, PhysicsImpulse.lift(0.05, 0.3)));
    }

    // ---------------------------------------------------------------- sealed A: haul road, steam lifts, walkways

    private static void pitMine(ServerLevel level, Landforms.Instance v) {
        AABB area = box(v.cx(), v.cz(), v.reach(), v.baseY() - v.craterDepth() - 3, v.baseY() + 3);
        each(level, area, e -> {
            double d = horizontal(e, v.cx(), v.cz());
            return d < v.craterRadius() && d > 4.0 && standingOn(level, e, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_SLAB,
                    Blocks.YELLOW_CONCRETE, Blocks.WHITE_CONCRETE);
        }, e -> {
            // along the road toward the floor: the way the road winds is the direction of increasing angle
            double angle = Math.atan2(e.getZ() - v.cz(), e.getX() - v.cx());
            send(e, PhysicsImpulse.push(-Math.sin(angle) * 0.05, Math.cos(angle) * 0.05, 0.28));
        });
    }

    private static void towers(ServerLevel level, Landforms.Instance v) {
        int base = v.baseY();
        for (Landforms.Pillar p : v.pillars()) {
            boolean stack = p.radius() <= 4;
            AABB area = box(p.x(), p.z(), p.radius() + 2, base, p.top() + 8);
            each(level, area, e -> {
                if (e.getY() > p.top() + 6) return false;
                double r = SignatureLandforms.towerRadius(p, base, (int) Math.min(p.top(), Math.max(base, e.getY())));
                return horizontal(e, p.x(), p.z()) < r - (stack ? 1.1 : 1.5);
            }, e -> send(e, stack ? PhysicsImpulse.lift(0.09, 0.7) : PhysicsImpulse.lift(0.07, 0.5)));
        }
    }

    private static void overpass(ServerLevel level, Landforms.Instance v) {
        double theta = v.rivuletAngles()[0];
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double half = v.craterRadius() * v.craterDepth() / 2.0 + 4.0;
        double sign = (v.seed() & 1) == 0 ? 1.0 : -1.0;
        AABB area = box(v.cx(), v.cz(), v.reach(), v.baseY(), v.baseY() + v.height() + 4);
        each(level, area, e -> {
            double dx = e.getX() - v.cx();
            double dz = e.getZ() - v.cz();
            return Math.abs(-dx * sin + dz * cos) <= 4.5 && Math.abs(dx * cos + dz * sin) <= half &&
                    standingOn(level, e, Blocks.GRAY_CONCRETE, Blocks.YELLOW_CONCRETE);
        }, e -> send(e, PhysicsImpulse.push(cos * sign * 0.045, sin * sign * 0.045, 0.27)));
    }

    // ---------------------------------------------------------------- sealed B: slick basalt, geysers

    private static void columns(ServerLevel level, Landforms.Instance v) {
        AABB area = box(v.cx(), v.cz(), v.reach(), v.baseY() - 2, v.baseY() + v.height() + 6);
        each(level, area, e -> standingOn(level, e, Blocks.POLISHED_BASALT), e -> send(e, PhysicsImpulse.slide(1.3)));
    }

    private static final int GEYSER_PERIOD = 200;
    private static final int GEYSER_ERUPTION = 40;

    private static void geysers(ServerLevel level, Landforms.Instance v, long time) {
        for (Landforms.Pillar p : v.pillars()) {
            ChunkAccess chunk = level.getChunkSource().getChunkNow(p.x() >> 4, p.z() >> 4);
            if (chunk == null) continue;
            int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.x() & 15, p.z() & 15);
            long phase = Math.floorMod(time + Landforms.mix(v.seed(), p.x(), p.z()), (long) GEYSER_PERIOD);

            if (phase >= GEYSER_PERIOD - 30 && phase % 4 == 0) {
                // it gurgles before it blows
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, p.x() + 0.5, top + 0.2, p.z() + 0.5, 4, 0.4, 0.3,
                        0.4, 0.1);
            }
            if (phase >= GEYSER_ERUPTION) continue;
            if (phase == 0) {
                level.playSound(null, p.x(), top, p.z(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.AMBIENT,
                        2.0f, 0.6f);
            }
            level.sendParticles(ParticleTypes.CLOUD, p.x() + 0.5, top + 1.0, p.z() + 0.5, 6, 0.3, 2.0, 0.3, 0.2);
            AABB column = new AABB(p.x() - 1.4, top - 4, p.z() - 1.4, p.x() + 2.4, top + 28, p.z() + 2.4);
            each(level, column, e -> true, e -> send(e, PhysicsImpulse.lift(0.16, 0.75)));
        }
    }
}
