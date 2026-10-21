# Conflux and the Solaris map

Solaris is the map mod. PhoenixCore links Conflux to it when Solaris is installed; nothing happens otherwise. Solaris is
a compile-only dependency (`libs/solaris-neoforge-*.jar`), so only the `*Bridge` classes in
`integration/solaris` name Solaris types, and they are only reached after `SolarisSupport.isActive()`.

Config: `confluxSolaris` in the PhoenixCore config (`enabled`, `gateMapFeatures`, `landformMarkers`, `discoverMargin`,
`disciplineSkin`).

## Research gates

`SolarisGatesBridge` registers gate ids with `SolarisGatesAPI`, on server start and again after a data reload:

| Gate id | True when |
|---|---|
| `conflux:discipline` | the player has chosen a discipline |
| `conflux:discipline/<id>` | the player has chosen that discipline (`phoenix`, `sculk`, `void`, `sealed_a`, `sealed_b`) |
| `conflux:core/<id>` | the team has unlocked the commitment node of that discipline's tree |
| `conflux:node/<node id>` | the team has unlocked that research node |

Solaris re-evaluates a team's gates whenever a node is researched or a team changes. Bind features with Solaris's own
`progressionGates` server config, for example `minimap@phoenixcore:conflux/void=conflux:core/void`.

With `gateMapFeatures` on, these defaults are bound inside each Conflux dimension (`phoenixcore:conflux/<id>`): the
minimap, world map and fullscreen map need `conflux:discipline`; the underground map, globe view and mob display need
`conflux:core/<id>`. Off by default so it never changes an existing world.

## Landform markers

`Landforms.Sighting` is a read-only description of a landform (stable key, label, colour, icon, position), exposed through
`DisciplineChunkGenerator.landformSightingsNear`. While a player is in a Conflux dimension, `SolarisLandmarkBridge` checks
every 2 seconds for landforms within `discoverMargin` blocks of their edge and records them in `LandformDiscoveryData`
(saved with the overworld, per research team). Every 5 seconds the team's discovered landforms are synced to each member
as Solaris markers in a "Landforms" category; only changes are sent.

Not done yet: the geyser eruption countdown and updraft-zone overlays from `LandformPhysics`.

## Discipline skin (client)

`SolarisSkinBridge` sets Solaris's accent colour (`SolarisThemeUtils.setAccentOverride`) from `ConfluxTheme.accent`: the
dimension's own discipline inside a Conflux dimension, otherwise the one the player chose. Each Conflux dimension also gets
its own fog of war through `SolarisAPI.setUnexploredStyle`: Phoenix, Void (starfield), Sculk (cloud), Sealed (plain fog).
