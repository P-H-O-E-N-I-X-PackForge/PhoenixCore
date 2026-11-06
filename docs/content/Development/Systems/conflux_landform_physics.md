# Conflux landform physics

Each Conflux landform carries its own physics. The effect is worked out from the same seed that built the landform, so it
always sits exactly where the terrain is. There are no longer any zones or platforms at fixed coordinates.

Code: `integration/conflux/dimension/worldgen/LandformPhysics.java` (the effects), `Landforms.near(...)` (which landforms
are near a point), `DisciplineChunkGenerator.tickLandformPhysics` (the per-level tick, called from `PhysicsHook`).

## How it runs

- Once a tick, for each Conflux level that has players: look up the landforms near each player (a 3x3 cell lookup in the
  landform cache, creating one from the seed if it is not cached yet, so it works on chunks generated before a restart).
- Each landform is handled once a tick however many players are near it.
- Mobs and items are pushed on the server. A player's motion lives on their client, so a player is sent a
  `S2CPhysicsImpulsePacket` and applies the impulse to their own motion.
- Anything that lifts, or weakens gravity, also gives Slow Falling (long enough to land from the top of a floating island)
  and clears the fall distance, so the way down is never fatal.
- Entities tagged `phoenixcore:ignores_conflux_physics` are skipped, as before.

## What each landform does

| Dimension | Landform | Effect |
|---|---|---|
| Phoenix | Active volcano (shield, strato, cinder) | Updraft over the crater, smoke plume |
| Phoenix | Active caldera | Updraft over the lake |
| Phoenix | Active fissure | Updraft along the lava trench |
| Sculk | Cenote | Weak gravity down the shaft: a slow, safe descent into the pool |
| Sculk | Giant fungus | Caps bounce you |
| Sculk | Rib arches | None (decor) |
| Void | Floating island | Weak gravity around and below the island |
| Void | Geode | Near weightless inside; amethyst chime |
| Void | Impact crater | Weak gravity in the bowl |
| Sealed A | Pit mine | The haul road is a conveyor, carrying you down toward the floor |
| Sealed A | Cooling tower and smokestack | Steam updraft up the inside (stacks are stronger) |
| Sealed A | Overpass | The deck is a walkway; the direction is set by the seed |
| Sealed B | Basalt columns | Polished basalt is slick |
| Sealed B | Geyser | Erupts every 10 s for 2 s, launching anything over the vent; bubbles before it blows |
| Sealed B | Rib canyon | Rising heat over the canyon |

## Tuning

All numbers are in `LandformPhysics`. Lift is `PhysicsImpulse.lift(accel, cap)`: `accel` is the rise on top of cancelling
gravity (so any value above 0 climbs) and `cap` is the top upward speed in blocks per tick.

Known limits: a player is nudged by a packet, so effects lag by one round trip; and an updraft can carry an entity out of
the top of its column, where it simply drifts down under Slow Falling.
