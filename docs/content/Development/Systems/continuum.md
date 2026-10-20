# Continuum - Design Doc

**Status:** milestones 1-3 built; see section 11. Lives in PhoenixCore at `net.phoenix.core.integration.continuum` (branch `8.0`).
Items marked **Proposed** are my recommendation and can change; items marked **Decided** were settled with the project owner.

---

## 1. Premise

Most space mods put planets in dimensions: build a rocket, press a button, load a new world. Continuum doesn't.
Space is a **UI**. Planets, moons and stars are rendered objects (VBO meshes driven by shaders), and a rocket journey
is something you *watch and steer through those renders*, not a loading screen into another world.

Pillars:

1. **Space is a place you look at.** The planet you pick is a real, lit, rotating, atmospheric sphere, not an icon.
2. **The journey is the content.** Ascent, transit and arrival are rendered sequences you take part in.
3. **No dimension per planet.** Adding a planet means adding data and shader parameters, not a worldgen pipeline.
4. **Gated by progression.** Bodies unlock through Conflux research and GT tier, like the rest of PFT 2.0.
5. **Matches the Phoenix look.** Same UI theming as the rest of the phoenixsuite (Phase 8 consistency pass).

## 2. What "no dimensions" means for gameplay (Decided: A, then B)

If planets aren't worlds, something else has to give the trip a purpose. Options:

| Option | What you do at a planet | Cost |
|---|---|---|
| **A. Missions** | Launch an expedition; it runs on a timer; results (items, research, discoveries) are delivered back to a landing/recovery block. | Lowest. Pure UI + server state. |
| **B. Remote operations** | Deploy probes/drones/outposts at a body and manage them from the UI (extraction rates, power, logistics). | Medium. Ties into GT/AE2. |
| **C. Instanced landing zones** | Physically land in a small pooled dimension chosen by the planet's data. | Highest, and reintroduces dimensions through the back door. |

**Decided:** build **A** (missions) first, then **B** (remote operations) on top. **C** stays a possible later layer, so the
render/UI work never depends on it.

## 3. Player flow

1. **Launch pad multiblock** (GT-style, tier-gated) takes a rocket assembly + fuel (GT fluids, AE2-fed like other PhoenixCore machines).
2. **Mission planner UI** opens from the pad: destination, payload, crew/probe, fuel budget.
3. **Ascent sequence:** a full-screen render of the planet you launched from falling away - atmosphere layers, curvature, stars appearing.
4. **Orbit / star map:** a navigable solar system. Bodies are real meshes; orbits are drawn; you pick a target and a transfer.
5. **Transit:** timeline UI with events (fly-bys, hazards, decisions). Render shows the target growing in view.
6. **Arrival:** the destination at full detail; mission resolves per section 2.
7. **Return:** results delivered to the pad/recovery block; the rocket is consumed or recovered depending on tier.

## 4. Rendering architecture

### 4.1 Mesh
- One **icosphere** per LOD (e.g. 3 levels), built once and uploaded to a `VertexBuffer` (VBO). Planets are the *same* mesh, scaled and parameterised by shader uniforms - no per-planet geometry.
- Separate small meshes for: ring discs (annulus), orbit lines, a billboard for far stars.

### 4.2 Shaders
Registered as normal `ShaderInstance`s (vanilla core-shader JSON + `.vsh`/`.fsh`), so they are unaffected by shader packs - the screens are GUI, not world geometry.

| Shader | Job |
|---|---|
| `continuum_planet` | Procedural surface from a seed: noise height/biome palette, ocean mask + specular, day/night terminator, optional city lights. |
| `continuum_atmosphere` | Rim scattering (Fresnel-style), sun-direction tint, separate pass so it can be additive. |
| `continuum_clouds` | Second shell, scrolling noise, soft shadowing. |
| `continuum_ring` | Banded ring colour + transparency, shadow cast on the planet. |
| `continuum_star` | Reuse/extend the existing `phoenix_sunflare`. |
| Background | Reuse `phoenix_nebula`, `void_galaxy`, `cinema_void_galaxy`. |

Parameters come from data (section 5), passed as uniforms: palette, noise seed/scale, ocean level, atmosphere colour/density, cloud cover, axial tilt, spin rate.

### 4.3 Compositing in a screen
Planet views are drawn into an offscreen `RenderTarget` and blitted into the GUI, which gives clean depth, bloom-style passes and easy cropping into UI panels. `CinemaRenderTarget` already does render-to-texture for the cinema screens and is the model to copy.

### 4.4 Budget and options
- Draw count per frame: ~1 sphere per visible body + atmosphere + clouds; far bodies drop to the lowest LOD and skip clouds.
- Config: `quality` (low/med/high: LOD, cloud pass, noise octaves), `animateBodies`, `maxFps` for the planner screen.
- Must run when Embeddium/Oculus are present (GUI path, own shaders) - **to verify early**, not assume.

### 4.5 Existing code to reuse (verified in this repo)
- `client/renderer/cinema/cutscene/background/CutsceneShaders` - compiles fragment shaders at runtime, Shadertoy-style, hot reload. Ideal for iterating on planet shaders without restarts.
- `client/renderer/cinema/CinemaRenderTarget` - render-to-texture pattern.
- `client/worldfx` (`WorldFXManager`, `PhoenixSkyLayer`, `BlackHoleSkyLayer`, `NebulaSkyLayer`) - sky effect structure and shader plumbing.
- `integration/conflux/dimension/sky/PlanetOrbit` - an existing planet/orbit model for Conflux skies; reconcile rather than duplicate.
- Existing shaders: `phoenix_nebula`, `phoenix_sunflare`, `void_galaxy`, `black_hole`, `phoenix_atmosphere_grade`.
- `client/render/structure/StructureRenderer` - existing `VertexBuffer` usage.

## 5. Data model

Bodies are **data-driven** (datapack JSON; KubeJS-friendly like the rest of PFT):

```json
{
  "id": "continuum:cinder",
  "system": "continuum:sol",
  "parent": "continuum:sol_star",
  "type": "planet",
  "orbit": { "radius_au": 1.4, "period_days": 90, "phase": 0.2 },
  "body": { "radius": 0.8, "axial_tilt": 12, "spin_hours": 20 },
  "surface": { "seed": 4417, "palette": ["#6b3a1f", "#a8643a", "#d9a066"], "ocean_level": 0.0 },
  "atmosphere": { "color": "#ffb36b", "density": 0.6 },
  "clouds": { "cover": 0.2 },
  "gate": { "research": "conflux:...", "min_tier": "ev" },
  "missions": ["continuum:survey", "continuum:mine_ore_field"]
}
```

Server holds *state* (discovered bodies, active missions, outposts); the client holds *rendering* and reads the same definitions.

## 6. Systems and integrations

- **Conflux:** research gates which bodies/routes appear; discipline themes can tint a body or unlock unique missions.
- **GT:** pad multiblock, rocket tiers mapped to voltage tiers, fuels as GT fluids/recipes.
- **AE2:** fuel and payload sourced from the network like Cinder Atlas.
- **Phoenix Archives / Chronicles:** discovered bodies write lore entries.
- **Tesla Network:** optional - ground stations powered through the network.

## 7. Package layout

```
integration/continuum/
  api/        body/mission definitions, events
  common/     server state, missions, pad multiblock, packets
  client/
    render/   meshes (icosphere, rings), shader holders, render targets
    screen/   star map, mission planner, ascent/transit sequences
  data/       codecs + datapack loading
```
Shaders under `assets/phoenixcore/shaders/core/continuum_*`.

## 8. Decisions and what they imply (settled with the project owner)

| Decision | Consequence |
|---|---|
| **Missions, then remote ops** | Missions are server state with a start time and a duration. Remote ops (probes/outposts) reuse that state and add a running rate. |
| **Real-time travel** | A mission finishes after real wall-clock time, including while the player is offline. Store start/end as timestamps (not ticks), settle on login/chunk load, and show a countdown that doesn't depend on the server's tick rate. Needs a per-mission option to speed up with upgrades, and a sane cap so a week-long trip can't be started by accident. |
| **Galaxy scale** | Bodies belong to **systems**, systems sit on a **galaxy map**. The UI needs three zoom levels (galaxy -> system -> body) with a continuous camera between them, and far systems drawn as cheap billboards/points until you zoom in. The data model gains a `system` layer above `parent`. |
| **Rockets wear out** | A rocket has durability that drops per mission (more for harsh destinations and rough ascents). Damage can be repaired, and upgrades extend its life or reduce wear. Rockets are a persistent item/entity-free record (data on the pad or in the item), not a one-shot consumable. |
| **Failure and the interlock** | Wear raises the chance a mission fails. By default a failed mission **loses the payload**. A *Hull Interlock* upgrade changes that: a rocket past its wear threshold simply **refuses to launch**, and the payload stays loaded and safe. Repairing the rocket clears it. |
| **Missions are per team** | A mission belongs to a team, falling back to the player when there isn't one, exactly like the Tesla Network (`TeamUtils.getTeamIdOrPlayerFallback`, which already handles Phoenix Guilds and FTB Teams). Anyone on the team can see and collect it. |
| **Mission length by distance** | Near bodies take minutes, far ones longer, the farthest around **2 hours**. Upgrades reduce trip time (a cap on the total reduction keeps the longest trip from collapsing to nothing). |
| **Undiscovered systems are unknown points** | The galaxy map shows unexplored systems as unlabelled points. Research reveals them in stages (see below). |
| **~12 bodies to start** | Small enough to hand-tune shader parameters per body, so procedural generation is a stretch goal, not a requirement. Plan them across at least 2 systems to exercise the galaxy map. |

## 9. Discovery and reveal

Each system and body has a discovery state, stored per team:

1. **Unknown** - a faint point on the galaxy map, no name, no mission targets.
2. **Detected** - research has resolved it enough to show a name, rough type and distance. Missions can be planned to it, with a higher failure chance.
3. **Surveyed** - fully revealed: full-detail render, surface/atmosphere parameters, all mission types.

The galaxy view therefore tells a story as research advances: points sharpen into systems, systems into worlds.
The state is gated by Conflux research (section 6), so the stages map onto the existing research trees.

## 10. Proposed starting bodies (12)

Names are placeholders. The systems echo the Conflux factions so the map and the lore point at each other
(V.O.I.D, P.H.O.E.N.I.X, S.C.U.L.K; Conflux's sky renderers already exist for Phoenix, Void, Sculk and the two Sealed dimensions).

| # | Body | System | Type | Look (shader focus) | Trip | Gate |
|---|---|---|---|---|---|---|
| 1 | **Anvil** | Home | rocky, industrial haze | warm greys, orange atmosphere | ~5 min | start |
| 2 | **Halo** | Home | moon of Anvil | pitted grey, no atmosphere, strong terminator | ~8 min | start |
| 3 | **Tempest** | Home | gas giant + rings | banded storms, ring shadow on the planet | ~20 min | early research |
| 4 | **Rime** | Home | ice world | white-blue, cracked ice, thin atmosphere | ~30 min | early research |
| 5 | **Ember** | Phoenix | volcanic world | lava veins on a dark crust, city-light style glow | ~45 min | mid research |
| 6 | **Glasswind** | Phoenix | glassy desert | specular dunes, orange-gold atmosphere | ~55 min | mid research |
| 7 | **Kindle** | Phoenix | the system's star-side world | bright, scorched, strong sunflare | ~65 min | mid research |
| 8 | **Umbra** | Void | rogue rock near a black hole | gravitational lensing, accretion glow (reuse `black_hole`) | ~80 min | late research |
| 9 | **Hollow** | Void | shattered moon | broken geometry, deep violet | ~95 min | late research |
| 10 | **Event** | Void | the black hole itself (viewing target) | lensing, no landing; missions are observation only | ~105 min | late research |
| 11 | **Spore** | Sculk | bioluminescent world | teal glow in the nightside, pulsing noise | ~110 min | endgame |
| 12 | **Marrow** | Sculk | deep-sea ocean world | dark ocean, pulsing biolum, heavy clouds | ~120 min | endgame |

The two **Sealed** Conflux dimensions are left for later systems, so the galaxy map has room to grow past 12
and the unknown-point mechanic has something real to reveal. Trip times are before upgrades.

## 11. Milestones

1. **Planet renderer prototype** - *done.* Icosphere VBO, `continuum_planet` / `continuum_atmosphere` shaders, a full-screen test bench (`/continuum test`).
2. **Star map** - *done.* Definitions as JSON, a galaxy -> system -> body map with orbit rings, picking, zoom transitions and discovery-stage visuals (`/continuum map`).
3. **Pad + missions** - *done (first pass).* See "Milestone 3 as built" below.
4. **Arrival and results** - *done.* Extraction runs, upgrade modules, repair kits, outposts with power upkeep, the GT Launch Complex and the crafting recipes are in. The Conflux research nodes are milestone 5.
5. **Integration + polish** - *research gates done.* Conflux research nodes behind the stage gates are in; lore hooks, quality settings and a UI theming pass remain.

### Milestone 3 as built

- **Data** moved to datapacks: `data/<ns>/continuum/systems|bodies/*.json`, loaded on the server and synced to clients as raw JSON.
- **Team state** (`ContinuumTeamData`, a `SavedData`): discovery stages and missions per team, using the same `TeamUtils` team resolution as the Tesla Network (Phoenix Guilds, FTB Teams, or the solo player).
- **Discovery** is real now: a team starts at each definition's `initial_stage`; a landed mission surveys its target, resolves its moons, and picks up the nearest still-unknown body in that system as a signal. Research hooks exist (`gate.detected_research` / `gate.surveyed_research`) but no Conflux nodes use them yet.
- **Rocket** (`continuum_rocket`): one persistent item. Wear and upgrades live in its NBT (`RocketStats`). Upgrades: `overdrive` (-10% trip time per level, capped by config), `reinforced_hull` (-25% wear per level), `hull_interlock` (a rocket past the wear threshold refuses to launch instead of risking the mission). The failure chance grows with wear and is higher for a body that is only detected.
- **Missions** (`Mission`, `ContinuumMissions`): wall-clock timing, outcome rolled at launch and hidden until landing, rocket carried inside the mission and returned on collection. A failed mission adds extra wear.
- **Launch Pad** (`continuum_launch_pad`): a plain block for now; right-clicking opens the map with launching enabled. The map opened with `/continuum map` is view-only. The GT multiblock pad is milestone 4.
- **Screens**: the map gains a planner (trip time, wear, failure risk, launch button) and a missions list; launching plays a cinematic ascent, then a transit view (destination approaching, countdown, waypoints) with a collect button.
- **Config** (`continuum` in the Phoenix config): `tripTimeMultiplier`, `maxTripReduction`, `interlockWearThreshold`, `wearMultiplier`, `maxMissionsPerTeam`, `launchRangeBlocks`.
- **Operator tools** (`/continuumadmin`, permission level 2): `rocket give|repair|wear|upgrade`, `discover <id> <stage>`, `reset`, `missions finish|clear`.

### Milestone 4, first slice: payload and resources

- **Two mission types** (`Mission.Type`): *Survey* resolves a detected body; *Extract* carries extraction probes to a **surveyed** body and brings resources home.
- **Payload**: Extraction Probes (`continuum_probe`) are taken from the player's inventory at launch. A failed extraction loses them; a rocket with a Hull Interlock refuses to launch before anything is taken, so the payload stays safe.
- **Yields** are data: each body's JSON has a `yields` list (`item`, `min`, `max`, `chance`, rolled per probe). They are rolled once at launch and stored in the mission, so reloading cannot re-roll them, and handed over when the rocket is collected. The shipped values are placeholders for the pack to retune.
- **Upgrades are items now**: Overdrive Module (up to level 5), Reinforced Hull Plating (3), Hull Interlock Module (1) - use one with a rocket in the other hand. The Rocket Repair Kit removes wear the same way.
- **Planner**: a modal over the map (survey or extract, probe count, trip time, wear, failure risk, expected yield).
- **Config**: `maxProbesPerMission`, `repairKitWear`.

### Milestone 4, second slice: outposts (option B)

- **An outpost** (`Outpost`) is a team's installation of probes on one surveyed body. Each production **cycle** (config `outpostCycleMinutes`, default 10) every probe rolls the body's `yields` table once. Finished cycles stockpile up to `outpostMaxStoredCycles` (default 144), then the outpost idles until it is hauled.
- **Nothing ticks.** `Outpost.settle` works out how many cycles finished from wall-clock time, so outposts keep producing while the server is off.
- **Two new mission types**: *Deploy* carries probes out (up to `maxProbesPerOutpost`, default 8, per outpost); *Haul* flies out, claims the whole stockpile at launch and brings it home. A failed Deploy loses the probes; a failed Haul loses the haul. The Hull Interlock refuses both before anything is taken.
- **Map**: bodies with an outpost carry a teal marker and an `[outpost]` tag; the body panel shows probes and cycles ready; the planner has Deploy and Haul modes with expected output.
- **Tuning note**: a full outpost (8 probes, 144 cycles) hauls over a thousand rolls of the yield table in one trip. The shipped yields are placeholders, so retune the yields, the cycle length or the cap together.
- **Operator tools**: `/continuumadmin outpost grant <body> <probes>`, `outpost fill` (bank a full stockpile at every outpost), `outpost clear`.

Not yet: no one can attack or lose an outpost.

**Power and upkeep.** Running an outpost costs EU, paid from the owning team's **Tesla Network** pool (`OutpostPower`): every probe costs `outpostUpkeepEUPerProbe` (default 500,000, about 42 EU/t per probe over a 10 minute cycle) for each finished cycle. A cycle the pool cannot pay for is **lost**, not deferred, so an unpowered outpost idles instead of storing a free backlog; the outpost remembers how many cycles were lost since the last haul. A team with no Tesla Network at all cannot pay. Upkeep is settled lazily like production, plus once a minute on the server so the power drains steadily. The pool is only ever read, never created. Config: `outpostRequiresPower` (turn upkeep off) and `outpostUpkeepEUPerProbe`. The map shows `[outpost - no power]` with a red marker, and the body panel and planner show the upkeep, the power state and lost cycles.

### Milestone 4, third slice: the Launch Complex and recipes

- **Continuum Launch Complex** (`continuum_launch_complex`, a GT multiblock): a 7x7 steel slab with a corner tower at each corner, a ring beam on top and an open gantry in the middle with a 1x1 shaft that must stay clear. The controller sits at the front centre of the slab, flanked by the two spots that take **energy input hatches** (1-2). Using the formed controller opens the map with launching enabled; sneaking falls through to GT's normal behaviour.
- **Tiered by its hatches.** The pad's tier is the best energy input hatch it has. Each body's `gate.min_tier` maps to a minimum pad tier through config (defaults: start MV, early HV, mid EV, late IV, endgame LuV), and the body panel shows the requirement.
- **Launching costs power.** A launch draws `voltage of the pad tier x launchEnergyAmpTicks` EU (default 1200 amp-ticks, so one minute at one amp) from the hatches' buffers; the server refuses if there is not enough, and nothing is taken from the player until every check has passed.
- **The plain block** is now the *Test Launch Pad*. With `requireMultiblockPad` on (the default) it is refused; turn it off for creative or testing.
- **Recipes** (all GT assembler): probes and repair kits MV; the rocket and hull plating HV; the overdrive and interlock modules and the Launch Complex EV. They live in `ContinuumRecipes` and are meant to be retuned by hand.

Deliberately not in milestone 3: cargo and rewards (so "failure loses the payload" currently means a damaged rocket and no survey), crafting recipes for the rocket and pad, and Conflux research nodes.

## 12. Risks

- **Looking good in a GUI at a reasonable cost** - mitigate with LODs, render targets and the quality setting; profile with the existing `ShaderProfiler`.
- **Shader compatibility** - vanilla core-shader path is the safe one; test with Embeddium and Oculus early.
- **Scope creep into dimensions** - keep option C out of milestones 1-4.
- **Two planet models** (`PlanetOrbit` vs Continuum bodies) - decide whether Conflux skies migrate to the new data later.

## 13. Remaining open questions

1. **The 12 bodies** (section 10) are a proposal. Swap, rename or reorder freely; the only structural constraint is at least 2 systems.
2. **Numbers:** exact wear per mission, failure chance curve, how much upgrades can cut trip time (suggest a cap around 50%), and whether the Hull Interlock threshold is fixed or tunable.
3. **Reveal costs:** which research steps move a body from Unknown -> Detected -> Surveyed, and whether Detected bodies should be reachable at all.
4. **Team handover:** if a player leaves a team mid-mission, who keeps the mission? (Suggest: it stays with the team it was launched under.)

## Research gates (milestone 5)

The **Deep Space** tree (`data/phoenixcore/conflux/research/continuum.json`, 23 nodes) drives discovery. Bodies and systems carry `gate.detected_research` / `gate.surveyed_research` node ids (systems read the same fields as bodies); `ContinuumTeamData.effectiveStage` checks them against `WorldResearchData`.

- **Engineering branch:** Orbital Mechanics -> Rocketry -> Hull Engineering / Advanced Propulsion / Launch Complex Design. Each sets a flag that gates the matching assembler recipes (`data/phoenixcore/conflux/gated_recipes/continuum_*.json`, matched by recipe id and output item).
- **Detection branch:** Deep Survey -> Cold Signals (Rime), Aurelian Signals (Ember, Glasswind, Kindle), Tenebrae Lensing (Umbra, Hollow, Event), Thalassa Bloom Spectra (Spore, Marrow) mark bodies Detected; one `Survey: <body>` node each makes it Surveyed.
- **Sealed systems:** two hidden signal/decode node pairs gate the sealed systems.
- Unlocking any node pushes a fresh Continuum snapshot to the team so the map updates at once. All costs are placeholders.

System display names are deliberately not the Conflux discipline names, so each discipline stays separate from space: Phoenix system = **Aurelian**, Void = **Tenebrae**, Sculk = **Thalassa**, Sealed A = **Orrery**, Sealed B = **Kaleid**. The ids match the names (`phoenixcore:aurelian`, `tenebrae`, `thalassa`, `orrery`, `kaleid`), and the research nodes and flags follow (`space_aurelian_signals`, `space_tenebrae_lensing`, `space_thalassa_bloom`, `space_orrery_*`, `space_kaleid_*`).

## Black holes, quasars and the extra systems

- **Black holes** (`black_hole` systems and bodies) are drawn by `continuum_blackhole`: a camera-facing billboard that does the horizon, a photon ring, an accretion disk in a tilted plane (intersected per pixel, so the far half hides behind the hole), and a lensed copy of the disk's far side arching over and under the horizon. It is not true background lensing; the backdrop is not bent.
- **Quasar** (`star_kind: quasar`): the same shader with a hotter blue-white disk, a larger billboard and jets along the spin axis. **Pharos** is the first, far out at the edge of the galaxy, with Beacon (the core, observation only), Mote, Halogen and Parallax. Research: *Pharos Signal* then *Survey: Pharos*, behind Tenebrae Lensing.
- Every system now has at least two worlds besides its star or hole: the sealed systems gained **Gearwork / Sprocket** (Orrery) and **Fracture / Flicker** (Kaleid).

## Sound and the landing scene

- **Sound** (`ContinuumSounds`): one place for every Continuum sound, so swapping in custom audio later touches only that class. Engine rumble on ascent (Conflux's volcanic rumble loop, following the burn) with a firework whoosh at lift-off; a void-drone hum and waypoint pings in transit; a soft click on map and planner buttons; a rising amethyst/bell arpeggio on arrival; three low flat pulses as the warning (opening the planner with a rocket past the interlock wear threshold, a failed landing, or a mission that fails while you are elsewhere). One-shots reuse vanilla sounds at shifted pitches; no new audio files.
- **Landing** (`ContinuumArrivalScreen`): when a mission you are watching lands, the transit view rolls into a nine second descent - the destination swells, a re-entry glow builds (red with shake on a failure, weaker on thin atmospheres, none for black holes) - and ends on a results card with Collect and Back buttons. Esc skips to the card. A finished mission can replay it from the transit view. Missions that land while you are on another screen play the tone and show an action-bar message.

## Rocket trails and the Archive

- **Trails:** a running mission is drawn on the map as a line from home to its destination (system to system on the galaxy map; planet to planet, or in from the edge, inside a system), solid behind the rocket, dashed ahead, with a small arrow, flame and percentage at the rocket. Progress is the mission's outbound progress.
- **Archive (lore):** every body has two short entries in its JSON (`"lore": {"detected": ..., "surveyed": ...}`), written when the team first reaches that stage. The body view shows the entries top left; the **Archive** button on the map opens a list of everything uncovered, grouped by system. A new entry plays a soft chime and an action-bar message ("Archive updated: Rime (detected)"). The first snapshot after login does not announce anything.

## Map feel and visual settings

- **Transitions:** zooming between galaxy, system and body now dives toward what was clicked (the system's star, or the exact body inside a system), over 0.42 s out and 0.6 s in, with eased fades so there is no black flash, then settles into the new level from further out.
- **Orbit rings** fade with depth: the near side of a ring stays bright and the far side dims relative to how far out the camera is.
- **Quality** (`continuum.renderQuality`, LOW / MEDIUM / HIGH, client-side): planet mesh subdivision, noise layers and the backdrop's noise layers. Q on the map overrides it for the session. The ascent, transit and landing scenes follow the same setting.
- **Planet style** (`continuum.planetStyle`, SPHERE / CUBE): CUBE draws worlds as blocky cubes whose surface is square cells (8 / 12 / 16 per face at LOW / MEDIUM / HIGH; moons are always chunkier), flat-lit, with a faint grid and per-cell shade variation. Stars and black holes stay round, and cube worlds go without the atmosphere shell. C on the map flips it for the session.

## Hazards: mission events and broken outposts

- **Mission events** (`missionEventChance`, default 0.3): a mission may get one random event, rolled at launch and revealed when it lands (landing card and chat). Bad: *solar flare* (+12% rocket wear), *micrometeoroids* (one probe lost on extraction and deployment runs). Good: *tailwind* (15% shorter trip), *derelict cache* (extra resources on extraction and haul runs), *signal echo* (a survey reveals one more nearby signal). A failed mission only keeps the solar flare.
- **Outpost incidents** (`outpostIncidentChance`, default 0.05 per finished cycle): an outpost can break down. A broken outpost produces nothing and costs no upkeep, but keeps what it already stockpiled (a haul still works). The team gets a chat message, a warning tone and a red marker.
- **Repair runs:** a new mission type, planned from the body view when the outpost is damaged. It consumes `outpostRepairKits` repair kits (default 1) from the launching player, flies like any mission, and on landing the outpost produces again from that moment. A failed run loses the kits and the outpost stays broken.
- Admin: `/continuumadmin outpost damage` and `/continuumadmin outpost repair` break and fix every outpost of the team, for testing.

## Deep surveys and deposit richness

- Every resource on a body has a **richness** for each team (poor x0.6, average x1.0, rich x1.45, jackpot x2.1; fixed per team, body and resource). It scales every extraction and haul result, but is **unknown** until surveys map it.
- A first survey (Detected -> Surveyed) maps the body's first deposit. A surveyed body can be **deep surveyed** again and again, each mapping one more deposit, until all are known (the map panel shows "Deposits mapped n of m"; the planner tags each resource as unmapped / poor / average / rich / jackpot and scales its expected range).
- A successful survey has a chance (`surveyAnomalyChance`, 0.25) of an **anomaly**: a surface cache (resources come home with the rocket), a vein signature (maps another deposit), or rare ancient ruins (a large cache). Anomalies show on the landing card.
- Mission types the player cannot fly are greyed out in the planner, and hovering one says why.
- Cube mode now also covers stars (a blocky sun); a black hole's horizon stays round because its disk and lensing assume a circle.

## Launching from the item input buses

A formed Launch Complex takes its **rocket, extraction probes and repair kits from its item input buses** (up to 4, placed where the casing is, alongside the energy hatches) instead of the player's inventory. The map asks the server what the pad has loaded about once a second and the planner shows "in the item input buses". The plain testing pad (when the multiblock is not required) still uses the player's inventory. Landed rockets and hauls are still collected into the player's inventory.

## Central bodies

Every system now has a page for the object at its heart. The black-hole and quasar systems already had one (Event, Beacon); the five ordinary systems gained a **star**: **Hearth** (home), **Aurelion** (Aurelian), **Nereus** (Thalassa), **Escapement** (Orrery) and **Prism** (Kaleid). A star is a normal body of `type: "star"` with orbit radius 0, so it has a name, a Detected and a Surveyed lore entry (shown in the body view and the Archive), a research gate, a trip time and a pad tier like any other body, and can be selected from the system view or flown to for a survey. Stars have no resources, so they cannot be extracted from or built on. They are drawn in the body view, the transit view and the landing view as the system's star (a cube in cube mode).

## Shielding: working at stars and black holes

Stars and black holes (including a quasar's core) can now be extracted from, built on, hauled from and repaired, but only by a rocket with the matching **shielding module** fitted. A survey (observing from range) needs none.

- **Stellar Shielding Module** (`continuum_module_stellar_shield`): for stars. IV assembler recipe, gated by the *Stellar Shielding* research node (after Hull Engineering).
- **Singularity Shielding Module** (`continuum_module_singularity_shield`): for black holes and quasar cores. LuV assembler recipe, gated by *Singularity Shielding* (after Stellar Shielding and Tenebrae Lensing).
- Fit a module by using it with the rocket in the other hand, like the other modules. A rocket without it is refused at launch with a message naming the module, and the planner shows the requirement and keeps Launch disabled.
- Stars now have resources (Hearth: blaze powder, glowstone dust, magma cream; Aurelion: blaze rods, magma cream, netherite scrap; Nereus: prismarine, sea lanterns, glow ink; Escapement: redstone, gold, copper; Prism: amethyst, quartz, diamond), and the black holes gained crying obsidian. Deep surveys map their deposits like any other body.
- Recipe gates are now also applied once the server has started; before this, a fresh world start could leave every gated recipe ungated until the first reload.

## Home Conflux world

The home system gains a world that follows the team's Conflux discipline. Each discipline has its own body definition (`conflux_phoenix`, `conflux_void`, `conflux_sculk`, `conflux_sealed_a`, `conflux_sealed_b`, and `conflux_none` before a discipline is chosen), tagged with `"discipline"` in its JSON. A team only has the one matching its discipline on its map: **Pyrelight** (Phoenix), **Nullhaven** (Void), **Hollowgrove** (Sculk), **Ironwake** (Sealed A), **Shiftward** (Sealed B) or **The Conflux**. The others are hidden for that team (the server reports them as hidden and the map skips them), and choosing or abandoning a discipline refreshes the map at once. Each has its own palette, lore, and a few resources.

## Personal dimensions

Every Continuum body has a void dimension (`phoenixcore:pdim/<body>`, one datapack file each, all using the `phoenixcore:pdim` dimension type and a flat generator with no layers in the_void biome). A player's **personal dimension** on a body is their own 7 x 7 obsidian platform (crying obsidian corners) at a plot of its own inside that shared dimension, 1024 blocks from the next, so there is no per-player world to create or load.

- **Sky:** the dimension type's effects (`phoenixcore:pdim`) render a nebula and star field tinted by the body and system, with the body itself hanging in the sky: a planet or moon, a star, or a black hole with its disk.
- **Gravity:** picked when it is made (presets from 0.16 to 2.5, defaulting to the body's natural gravity) and fixed afterwards. The client scales gravity the way vanilla does, so jumping and falling feel it.
- **Void loop:** falling below the bottom teleports you back to the top, keeping your column; fall damage is off in these dimensions.
- **Entering:** the **Personal dimension** button on a surveyed body's panel on the map, or `/pdim create|enter <body> [gravity]`, `/pdim leave`, `/pdim list`, `/pdim delete <body>`. Entering remembers where you came from; leaving takes you back (or to the overworld spawn).

## Black hole performance

The accretion disk is drawn on a camera-facing billboard that covers most of the screen up close, and every pixel of it shaded the whole disk; the quasar was cheaper only because most of its (larger) billboard lies outside the disk. The disk is now drawn into a smaller target (0.35 / 0.5 / 0.7 of the window at LOW / MEDIUM / HIGH quality, bilinear-scaled and composited back, premultiplied) with 2 / 3 / 4 noise layers, and the horizon sphere no longer runs the star shader at all (it is plain black).

## Personal dimension costs

A trip to a personal dimension costs items from the player's inventory (creative players are exempt, leaving is free). The default is the same for every body: `pdimCostItem` x `pdimCostCount` in the config (2 ender pearls; count 0 makes them free). `pdimChargeMode` is `EVERY_VISIT` (each trip there) or `FIRST_VISIT` (only when the dimension is made). A body overrides it in its own definition with `"pdim_cost": {"item": "minecraft:diamond", "count": 3}` (count 0 for a free one). The server sends every body's cost to the client, so the panel shows what a trip costs and disables the button when the player cannot pay.

## Home worlds are real dimensions

A discipline's home world on the map (the body named `conflux_<discipline>`) is the real Conflux dimension of that discipline. Its page has a **Travel there** button next to the personal dimension one: it takes the player to the dimension's starting area (respawn is left alone) and remembers where they left from, and while they are in a Conflux dimension the button reads **Return** and takes them back. Only the team's own discipline can travel, and only once the world is surveyed (home worlds start surveyed). Server side this is `ConfluxTravel`; the return point is stored in the player's persistent data.

## Hauls come home to the output buses

A mission launched from a formed Launch Complex remembers its pad. When the team collects it, the returned rocket goes into the complex's item input buses (ready to launch again), or the output buses if the inputs are full, and the resources go into the item output buses. Anything that does not fit, or everything if the pad is gone, unbuilt or unloaded, goes to the player's inventory instead. Missions launched from the plain testing pad still deliver to the player.

## Distress signals and rescue runs

A failed extraction, outpost deployment or haul can strand the rocket instead of bringing it home (`distressChance`, default 40%). A stranded mission shows as **STRANDED** in the mission list with a countdown, cannot be collected, and sends a gold chat message. The team has `distressWindowMinutes` (default 180, real time) to send a **rescue run** to that body: pick *Rescue stranded rocket* in the planner, which flies a second rocket out carrying `rescueKits` repair kits (default 2). The launch is refused if that rocket could not arrive before the signal fades.

- A rescue that lands brings the stranded rocket home: it becomes collectable like any landed mission (with the wear it already took).
- A failed rescue loses its kits and the rescue rocket takes the usual failure wear; the stranded rocket keeps waiting and can be rescued again while the signal lasts.
- If the signal fades before a rescue lands, the stranded rocket (and its mission) is lost.
- Only one rescue can be on its way to a given stranded rocket at a time. Surveys never strand, and a rescue run cannot itself strand.

Mission state: `Mission.distressUntil` (wall-clock ms, 0 when not stranded) and `Mission.rescuing`; both are saved, and the snapshot carries them to the client.

## New bodies

Meridian (home), Pelagia (Thalassa) and Steamreach (Aurelian) are ocean worlds; Carbonado (Kaleid) is a diamond planet. Each has its own survey research node (`space_survey_<body>`) and personal-dimension file.

## Orbital stations

A surveyed body can have an orbital station, built by a **Station construction** mission that carries `stationProbesPerLevel` probes (default 4, used up). It can be upgraded to level 3, one mission per level. For missions to that body each level gives:

- trips 10% faster and rocket wear 15% lower,
- outpost hauls 15% richer,
- at level 3, a failed run can no longer strand the rocket (see distress signals).

Every station, whatever its level, also adds one slot to the team's mission limit (`maxMissionsPerTeam` + stations). The effects are in `Stations`; levels are saved per team in `ContinuumTeamData` and sent in the snapshot, and the planner and body page show them.

## Fleet screen

The **Fleet** button on the map (next to Archive) lists every mission the team has out, stranded ones first, then landed, then in flight: progress, countdown or distress timer, and rocket wear, with a slot counter and the team's stations alongside. Click a row to watch a mission or collect it, and use **Collect all** to bring every landed (non-stranded) rocket home in one go (`C2SCollectAllPacket`, `ContinuumMissions.collectAll`).

## Launching several rockets at once

Extraction, outpost deployment and rescue runs have a **Rockets** stepper in the planner. Each rocket is its own mission, with its own rocket, probes and launch energy; they go one after another and stop at the first that cannot (no more rockets, probes or energy, or no free mission slot), and the chat says how many left. The most per launch is `maxRocketsPerLaunch` (default 8); the stepper is also limited by what the pad has loaded and the free mission slots, and a rescue launch sends one rocket per stranded rocket waiting there. Probes already on their way to an outpost count against its capacity. Surveys, hauls, repairs and station runs always send one.

## Disciplines in Continuum

A discipline now changes how missions go at certain places. The data is `data/<namespace>/continuum/disciplines/<discipline>.json`:

```json
{ "discipline": "phoenix",
  "affinities": [
    { "system": "phoenixcore:aurelian", "trip": 0.9, "wear": 0.8, "yield": 1.25, "label": "Phoenix are at home in the fires of Aurelian." },
    { "type": "star", "wear": 0.85, "label": "Phoenix shrug off stellar heat." } ] }
```

An entry applies when all its selectors match (`system`, `type`: planet, moon, star or black_hole, or one `body`); `trip` and `wear` multiply trip time and rocket wear, `yield` what extraction and hauls bring back. Matching entries multiply, and combine with station bonuses. The defaults are Phoenix at Aurelian and stars, Void at black holes and Tenebrae, Sculk at Thalassa and moons, Sealed A at the Orrery, and Sealed B at the Kaleid. The planner shows the team's affinity at a body with its effects. Server side this is `Affinities`; the snapshot carries only the factors that do something.

## Launch and landing variety

`FlightProfiles` picks how a launch and a landing look and sound from the destination.

- **Ascent** (leaving the home world): planets are the standard 7 second climb; gas giants burn longer and end in a slingshot burn; moons are shorter with a cool plume and a flatter climb; stars tint the view warm and deploy a sun-shield; black holes get a long climb with a cold violet plume, a darkening vignette and a shield check. Every ascent has a stage-separation clunk and flash at the halfway mark.
- **Landing**: an ordinary rocky world keeps the re-entry heat and a thud; a body with almost no air (or a moon) has no heat, a long blue-white powered descent and a dust cloud; an ocean world ends in a splashdown with spray and a splash; an ice world throws up glittering snow; a gas giant is aerobraking through a hot blue haze with no ground; a star pulses warm; a black hole closes in dark. Each has its own touchdown sound, and the readout and result card titles change to match.

## Layered ambience

The map plays a quiet space bed (`ContinuumAmbience`) and fades more layers in with the view: a star's low roar, a black hole's hum, wind over a world with air (louder with density, strongest on gas giants), a deep drone over an ocean, a bright hum over a glowing or crystalline surface, and a thin wind on moons. The layers are Conflux's existing ambience loops, so there are no new audio files, and they crossfade as the camera moves; the whole mix ducks while the planner is open. `ambienceVolume` (0 to 1, default 0.6) scales it, and 0 turns it off.

## Test commands for missions

All under `/continuumadmin missions` (operators), acting on your own team, needing no pad, rockets, probes or research:

- `send <body> <type> <seconds> [success|fail] [count]` sends that many fresh rockets that land in exactly that many seconds. Types: survey, extract, deploy, haul, repair, rescue, station. Extraction and haul rewards, discovery, outposts, stations and distress are all the real landing logic.
- `strand <body> <seconds>` puts a stranded rocket at a body right now, with a distress signal that fades in that many seconds.
- `time <seconds>` makes every running mission land that many seconds from now.
- `finish` and `clear` as before.

## More systems

Four more systems, each gated by a hidden pair of research nodes (`space_<system>_signal` and `_decode`, shared by all of the system's bodies like Kaleid):

- **Vesper** (mid): a red dwarf, Evenfall, with Terminus (tidally locked, fire on one face and ice on the other), Lantern (a glowing jungle world), Ashfall (black volcanic, under ash) and the moon Twilight.
- **Carousel** (late): a fast-spinning white star, Spindle, two gas giants (pink Calliope and gold Merry), the striped world Fairground, and three moons: Tinsel and Brass around Calliope, Pennant around Merry.
- **Reliquary** (endgame): a white dwarf, Pall, with the ruin-covered Sepulchre, the scorched Cinder and the moon Epitaph.
- **Trove** (late): a gold star, Aurum, with the gold plains of Glint, the dense Hoard, and the moons Pennyweight (copper, around Glint) and Ingot (iron, around Hoard).

Each body has its own yields, lore and personal-dimension file, and the disciplines have affinities in some of them (Phoenix at Trove, Void at Reliquary, Sculk at Vesper, Sealed A at Carousel, Sealed B at Reliquary).

## Special body shaders and rings

A body's JSON can ask for a special surface shader with `"style"`, and for rings with `"rings"`:

```json
{ "style": "storm",
  "rings": { "inner": 1.5, "outer": 2.6, "color": "#f6c8e8", "color2": "#a0688c", "tilt": 12, "opacity": 0.85 } }
```

Styles (all in `continuum_planet.fsh`, selected by the `Style` uniform; a plain body is style 0):

- `crystal`: faceted shards (cellular noise) with a flat normal each, a prismatic colour shift, bright edges and glitter that twinkles. Carbonado.
- `veins`: glowing seams that flow and pulse, visible on the night side (the emissive colour and amount set their colour and strength). Ashfall, Cinder, Ember, Lantern, Terminus.
- `storm`: for gas giants, the bands wound into a spiral round a great storm with a dark eye. Calliope, Merry, Halogen.
- `chrome`: a mirror that reflects a starfield, a lit horizon and a hard glint, tinted by the palette. Glint, Tinsel, Brass.
- `shatter`: slabs of the surface slip sideways, flip colour and flicker in and out, with glowing seams. Fracture, Flicker.
- `aurora`: curtains of light over both poles (the emissive colour, or green). Meridian, Rime.

Rings are a flat banded disk in the body's equatorial plane (`continuum_ring`), with a gap, soft edges, the planet's shadow falling across them and the planet hiding the half behind it. `inner` and `outer` are in body radii, `tilt` tips the ring plane, `color` and `color2` are what the bands alternate between. Cube mode draws crystal, chrome and shatter as plain blocks and has no rings; detected-but-unsurveyed bodies are always plain.

## Nebulas

Nebulas are atmosphere only: soft coloured clouds behind the galaxy map's systems, with a faint name. They are read from resource packs (`assets/<namespace>/continuum/nebulas/*.json`, client only, never synced):

```json
{ "name": "The Ember Veil", "center": [6, 0.5, -4], "radius": 5.5,
  "color1": "#ff7a30", "color2": "#b01850", "density": 0.85, "seed": 3, "puffs": 5 }
```

`center` and `radius` are in galaxy coordinates (the same space as a system's `galaxy` position). Each nebula is `puffs` camera-facing cloud billboards scattered through its volume, each with its own size, turn and cloud pattern (`continuum_nebula`: a domain-warped fbm with brighter filaments and hot cores). They are drawn into a half-resolution target and laid over the pass, so they are cheap and soft; detail follows the quality setting. The five defaults sit behind the existing systems: the Ember Veil, the Hollow, the Verdant Drift, the Prism Cloud and the Pale Mist.

A system whose galaxy position lies inside a nebula (within 1.1 radii of its centre) also sits in a faint wash of that nebula's colours: while you look at the system or one of its bodies, a few huge dim puffs are drawn far behind the scene and shift slightly as the camera turns. The deeper inside the nebula, the stronger the wash (`NebulaData.around`, `SceneRenderer.drawNebulaWash`).

## Preview commands

`/continuumpreview` (client-side: no operator, no pad, nothing sent to the server) plays the launch and landing scenes for any body:

- `/continuumpreview ascent <body>`
- `/continuumpreview arrival <body> [success|fail]`

## Launch energy

`launchEnergyAmpTicks` now defaults to 10 (half a second of the pad's voltage). A launch also never asks for more than 90% of what the pad's energy hatches can store (a hatch holds 16 times its voltage), so an old config value cannot make a launch impossible.

## Rescuing from the map

A stranded rocket shows a pulsing red beacon on its body in the system and body views. Clicking its row in the Missions panel or the Fleet screen, or **Plan rescue** on its mission view, goes straight to that body with the planner open on the rescue run (it needs the map opened from a Launch Complex). When a rescue lands it recovers the stranded rocket the run was sent to, or, for a run sent without the pad (`/continuumadmin missions send ... rescue`), the one closest to fading.

## Suite theme

Every Continuum screen and EMI page takes its colours from the Phoenix suite theme (`PhoenixTheme`, from Phoenix Wiki): accent for frames, panel and border for panels, header/text/dim for text, done and active for good and warning. `MapUi.refreshTheme()` re-reads it each frame, so changing the theme restyles the map live. The map's **Theme** button opens the suite theme editor. Buttons and panel fills are derived from the theme's panel, background and accent colours; the red "bad" colour is fixed.

## Rocket hulls and refit

Rockets have a hull tier, Mk I to Mk IV (`RocketStats.tier`, stored on the item; every rocket starts at Mk I and its name shows the tier). Each tier above Mk I gives 5% faster trips, 10% less wear and 12% less failure risk, raises how many levels of each module the hull can carry (overdrive Mk+1 up to 5, reinforced hull up to the tier, the stellar shield from Mk II, the singularity shield from Mk III), and, with `rocketTierGating` on (default), is required to work at bodies of higher tier (mid needs Mk II, late Mk III, endgame Mk IV; surveys are exempt).

Hull Refit Kits Mk II, III and IV are assembler recipes (HV, EV and IV). **Sneak + use a rocket** opens the refit screen, which raises the hull with the kit, fits modules the hull has room for, and repairs, all from the player's inventory (`RocketRefit`, one packet, the server decides). `/continuumadmin rocket tier <1-4>` sets the held rocket's hull. The three kit textures are copies of the hull module's: replace them.

## Mission history

Every launch, landing, failure, stranding, rescue, collection, station and comet is written to the team's history (the newest 120, saved in `ContinuumTeamData`). The **History** button on the Fleet screen, or **H** on the map, opens it: newest first, filtered to all, landings or problems, scrollable, with hover for the full line. The server sends it on request (`C2SRequestHistoryPacket`).

## Comets

A body with a `transient` block is a comet that is only there while the server has a window open for it:

```json
{ "transient": { "duration_minutes": 360, "cooldown_minutes": 600, "chance": 0.5, "inner_au": 0.35, "outer_au": 5.0 } }
```

`ContinuumTransients` checks once a minute: a comet off cooldown opens a window by chance (`chance` is per ten minutes), announces it to everyone and writes it to each online team's history, and announces when it leaves. While it is there it falls from `outer_au` to `inner_au` across the map with a tail streaming away from the star, and it counts as surveyed for any team that has found its system. Only extraction runs can go to a comet, and only if the rocket would arrive before it leaves; it has no personal dimension. Defaults: Snowfall (Home), Cinderfall (Aurelian), Glasswake (Kaleid), Longshadow (Tenebrae). `/continuumadmin comet spawn <body> [minutes]` and `clear <body>` open and close a window for testing.

## EMI pages

`ContinuumEmi` adds two EMI categories. **Continuum Bodies** has a page per body with yields: what extraction brings home (the min-max and chance on each item), the body's tier, flight time and the hull it needs, and whether it is a passing comet. **Rocket Refit** has a page per hull tier (rocket + kit to the next hull) and per module (the most levels each hull can carry). The pages are rebuilt when the Continuum data arrives on the client, since it syncs after EMI has first built its recipes, and are drawn in the suite theme.
