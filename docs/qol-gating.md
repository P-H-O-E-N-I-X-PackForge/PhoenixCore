# QoL gating table (Phase 2: "decide where X fits")

A working sheet for deciding what is gated behind Conflux research, by which discipline, and how late. The **Decision**
column is yours to fill in (tier / discipline / research node / "free from the start" / "removed").

Sources: the PhoenixCore source, the other Phoenix suite repos, and the mods in the `PFT 1.21.1` instance folder (100 jars
when this was written). Anything marked **(?)** is a mod I could not describe with confidence from its name alone.
Descriptions of PhoenixCore items and of the other Phoenix mods are written from their item names, package names and
READMEs, not from playing them, so correct any that are off.

## How a gate can be enforced

| Code | Mechanism | Needs new code? |
| --- | --- | --- |
| **R** | Recipe gate: the recipe only exists / only works once a research flag or recipe tag is unlocked (`WorldResearchData.hasRecipeTag`, KubeJS recipes) | No |
| **F** | Flag gate: a feature checks a research flag before it works (Conflux nodes can unlock `flag:` and `multiblock:` entries) | Small: one check per feature |
| **M** | Multiblock gate: the structure will not form until its `multiblock:` unlock is researched | No (Conflux already supports it) |
| **S** | Shop gate: the shop entry is hidden until a node is researched | Yes: the shop has no research hook yet |
| **C** | Config gate: a server/pack config turns the feature off, or limits it (tier, range, slots) | No, but it is all-or-nothing per world, not per team |
| **H** | Hide only: remove it from recipe viewers / creative tab (KubeJS), the item still exists | No |
| **G** | Gate API: the mod asks a registered per-team gate before the feature works (exists in Solaris today) | Only where marked |
| **X** | Cannot be gated sensibly: library, performance mod or client-only | n/a |

Gates in Conflux are per **team** (research is shared across a team), not per player.

---

## 1. PhoenixCore

| Feature | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| Cinder Atlas (+ Cinder Core, loadout expansion, range extenders 1-3) | Places a prepared build from an AE2 network; wireless sourcing, requester, loadouts | R, F, C (range/slots) | |
| Cinder Forge / Requester (AE2 hatch + ME input bus/hatch) | Machine side of Cinder, ME tag input | R, M | |
| Wireless Vault Terminal | Remote access to GregVaults storage | R, F | |
| GregPacks / GregVaults (backpacks, vaults, stack upgrades) | Portable and placed storage with upgrades | R, C (slot counts) | |
| Omni Pack curio | Curio that carries pack functionality | R, F | |
| Matter manipulator (Phoenix Manipulator) | Bulk place / copy blocks | R, F, C | |
| Soul Lens | Shows soul density of the land around you | R, F | |
| Chameleon Spray Can | Recolours blocks/machines (part appearance) | R, F | |
| Phoenix Wings (Wings of Fire) | Creative-style flight / glide | R, F | |
| Drone (wand, controller, groups) | Commanded drones for tasks | R, M, F | |
| Quarry Bee / Lumber Bee / honey line | Automated mining and wood with bees | R, M | |
| Drilling kits and drill heads (aluminfrost, aurum steel, space grade, ...) | Higher-tier mining tools | R | |
| Modules (energy, fluid, item, jetpack, magnet, crafting, processing, feeding, maintenance, earthbound/space miner) and the tiered module bases | Upgradable multi-purpose tool/suit modules | R (per module tier) | |
| Jetpack modules 1-2 | Flight through a module | R, F | |
| Magnet modules 1-2 | Item pickup range | R | |
| Maintenance module | Fixes GregTech maintenance remotely | R, F | |
| Research Terminal | The Conflux terminal itself | M, R | |
| Shop (entries, rewards, `/phoenixshop`) | Buy things with currency; rewards | S (needs code), C | |
| Cinema screens (3x3, 9x9 arrays) | Info and decoration screens | R, H | |
| Growth (seeds, shells, tendrils, control hatch) | Growing structures and materials | R, M | |
| Astral Ensorcellation (codex, wand, survey, ritual pedestal, thread cell, skein) | Magic crafting add-on (planned around ZPM) | R, M, F | |
| Vocal Resonance (resonant jukebox, speakers, sound hatch, console) | Sound-based machines and puzzles | R, M | |
| Ars Nouveau integration (Source machines, Soul balance) | Source reactors, imbuer, soul growth | R, M | |
| Fission items (fuel rods, pellets) and component blocks | Reactor fuel and parts | R | |
| Cleanroom / HPCA parts, sterile cleanroom | Machine environment multiblocks | R, M | |
| Creative energy multiblock | Testing/creative power | C (keep out of survival) | |
| Conflux wiki / HUD button | In-game docs | X | |

## 2. The other Phoenix suite mods

| Mod | Feature | What it does | Gate via | Decision |
| --- | --- | --- | --- | --- |
| Solaris | Fullscreen map | World map with colour/lighting, shapes, planning | **G** (`SolarisGatesAPI`; Core's `SolarisGatesBridge` already registers `conflux:discipline`, `conflux:discipline/<id>`, `conflux:core/<id>` and `conflux:node/<id>` gates and binds minimap/world/fullscreen map per Conflux dimension) | |
| Solaris | Minimap HUD | Corner map, waypoints, coordinates, biome, time | **G** (same gate API, already bound per Conflux dimension) | |
| Solaris | Waypoints (+ beams, compass, death markers, structure waypoints) | Saved locations, tracking, auto structure markers | **G** (feature states bindable to a gate; not bound by default) | |
| Solaris | Player positions / mobs on map | Shows other players and mobs | C (server `playerVisibility`) | |
| Solaris | Shared exploration / shared veins | Team shares explored chunks and ore veins | C (server config) | |
| Solaris | GT ore vein overlay | Shows GregTech vein locations once found | C | |
| Solaris | Progression gates (`progressionGates`) | Already ties map features to advancements | built in | |
| Solaris | Underground / cave view, globe view, show mobs, rail network, claims overlay | Extra map layers | **G** (underground, globe and mobs are already bound to `conflux:core/<discipline>`) | |
| Phoenix Chronicles | Questbook | Quests, rewards, chapters, categories with `enable_if` flags | built in (flag expressions) | |
| Phoenix Continuum | Star map, missions, personal dimensions, swarms | Space progression and power | R (rockets, pods, pads), M | |
| Phoenix Domains | Chunk claims, claim protection, chunk loading | Team land claims, forced loading | C (claim limits, chunkloading) | |
| Phoenix Guilds | Teams | Team membership and flags (used by Solaris/Domains/Chronicles) | X (foundation) | |
| Phoenix Excavate | Vein mining / placing | Break or place connected blocks at once | C, F | |
| Phoenix Archive | Lore terminal | Lore entries unlocked by progression | built in | |
| Phantasia | Multiblock / structure guide (**?** from the roadmap: it handles substructures) | Shows and guides building structures | F, H | |
| Phoenix Wiki | In-game wiki | Docs with themes | X | |
| Phoenix Tesla Network | Tesla towers, wireless power and data, tesla hatches, binder | Capstone wireless energy network | R, M | |
| Phoenix Fission (`PhoenixFission`) | Fission reactor multiblocks | Late-game power | R, M | |
| Phoenix Chromatic Codes | Colour and animated text codes | Cosmetic text | X | |
| Phoenix Essentials | (not in the 1.21.1 modlist; unsure) | | | |

## 3. Third-party mods in `PFT 1.21.1`

### Storage, inventory and crafting

| Mod | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| Applied Energistics 2, ExtendedAE, ExpandedAE, MEGA Cells | Digital storage and autocrafting | R | |
| AE2 Wireless Terminals (`ae2wtlib`) | Use AE2 terminals from anywhere | R, F | |
| ME Requester (`merequester`), `ae2ct` (AE2 crafting terminals) (**?** for ae2ct), Better P2P | Request crafts from ME; P2P tunnel tweaks | R | |
| Functional Storage | Drawers, storage controller | R | |
| Ender Chests | Remote shared chests | R, F | |
| Crafting on a Stick, Crafting Station (JEI), Crafting Tweaks | Portable / easier crafting tables | R | |
| Tool Belt | Extra tool slots | R | |
| Item Collectors | Auto-pickup blocks | R | |
| Trash Cans | Item/fluid/energy voiding | R | |
| Inventory Essentials, Inventory Tweaks, Mouse Tweaks | Inventory sorting, bulk move | X (client UI) | |
| Gravestone | Keeps items on death | C | |
| Curios | Extra equipment slots (library for many items) | X | |
| Iron Furnaces | Faster furnace tiers | R | |
| Utilitarian | Utility blocks (placers, breakers, redstone tools) | R | |
| Fast Suite, Polymorph | Faster crafting, recipe choice | X | |

### Movement and travel

| Mod | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| Waystones | Teleport network | R, C (cost/cooldown) | |
| Travel Anchors | Short-range teleport anchors | R, C | |
| Angel Ring + Flight API | Creative-style flight item | R | |
| Elevator ID | Elevators | R | |
| Chunky | World pre-generation (admin) | X | |

### Building

| Mod | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| Construction Wands Revived | Place many blocks / build tools | R, C (limits) | |
| Framed Blocks, Rechiseled, Architect's Palette, XTones Reworked, Fusion (connected textures), Antiblocks Rechiseled | Decorative blocks and connected looks | H | |
| Clumps | Merges XP orbs | X | |

### Information and interface

| Mod | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| EMI, TooManyRecipeViewers, GuideME, Jade | Recipes, in-game guides, block info | X (EMI/Jade can hide entries; see H) | |
| AppleSkin | Food values | X | |
| Nature's Compass | Biome finder | R, F | |
| Better Advanced Tooltips, Toast Control, Searchables, Controlling, Better Tags | Tooltips, toasts, search, keybinds | X | |

### Content and world

| Mod | What it does | Gate via | Decision |
| --- | --- | --- | --- |
| GregTech (GTCEu) | The tech tree | R, M | |
| Ars Nouveau | Source magic | R, M | |
| Trenzalore | Extra dimension and content | R, C | |
| Torchmaster | Mob-spawn control torches | R | |
| Easy Anvils, Fast Leaf Decay | Anvil and leaf tweaks | C | |
| PeanutCraft (**?**), Glodium (**?**), Polyeng (**?**), Shetiphian Core (library), HaydenAPI (library) | Not described from the name alone | | |

### Not gateable (libraries and performance)

Sodium, ModernFix, FerriteCore, Smooth Boot, FpsReducer, Alternate Current, spark, SimpleBackups, NoChatReports, Placebo, Balm,
Bookshelf, Configuration, Puzzles Lib, LibX, Titanium, Geckolib, Rhino, KubeJS, Kotlin for Forge, LDLib 2, Prickle, Model Fix,
Better Lib, SuperMartijn642's Core Lib and Config Lib, `world_first_join_message`, `yeetusexperimentus` (**?**), `attributefix`.

---

## Things the modlist does not currently contain

`PhoenixCore`, `Phoenix Guilds`, `Phoenix Excavate`, `Phoenix Archive` and `Phoenix Essentials` were not in the instance folder
when this was written (the instance may be in the middle of being rebuilt). Their rows above come from the repos, not from the
pack.

## Gaps in the gating tools

Gating in Conflux **is** per team: a gate asks "has this player's team unlocked X" (`ConfluxApi.hasFlag`, `isUnlocked`,
`discipline`). The gaps are features with **no check at all**, so nothing asks that question:

| Where | Gap |
| --- | --- |
| Solaris | Has the gate API and the Conflux bridge (done); only the map features are bound by default |
| Phoenix Domains, Phoenix Excavate | Have feature states (on/off, per dimension) but no gate API, so they cannot ask the team |
| Continuum, Guilds, Phantasia, Tesla Network, Fission | No feature switch and no gate hook |
| PhoenixCore items (wings, Cinder, manipulator, wands, spray can, drone, modules) | Use the recipe gate only; using the item once you have it is not checked |
| Shop | No research hook on entries |
| Third-party mods (Waystones, Travel Anchors, Construction Wands, AE2 wireless, Angel Ring, Elevator ID, Nature's Compass, Utilitarian, Ender Chests, Tool Belt, ...) | Nothing on their side; they can only be gated by recipe, hiding or a world-wide config |

The fix that covers all of these is shared code rather than per-mod patches:

1. **A shared gate API** in the library every suite mod already depends on (PhoenixWiki): `registerGate(id, predicate)`,
   `bind(feature, scope, gate)`, `allowed(player, feature)`. This is Solaris' `SolarisGatesAPI` lifted out so Domains, Excavate,
   Continuum and the others use the same thing. PhoenixCore keeps registering the Conflux gates.
2. **A generic use gate in PhoenixCore** for items, blocks and screens by registry id (a data file: `item minecraft:x needs
   conflux:node/...`), enforced by NeoForge events (use item, use block, open container, place block, break block). That
   makes third-party features per-team without changing those mods.
3. **A research hook on shop entries** (S).

### Status of the three fixes

1. **Shared gate API: done in PhoenixWiki (`net.phoenixvine.wiki.api.gate.SuiteGates`, 1.20.1 and 1.21.1).** Named player gates,
   namespace resolvers, `a | b & !c` expressions, and `bind` / `allowed` for features. An unregistered gate is closed.
   PhoenixCore registers the `conflux:` gates (`ConfluxGates`): `discipline`, `discipline/<id>`, `core/<id>`, `node/<id>`,
   `flag/<flag>`. Solaris keeps its own `SolarisGatesAPI` and the Conflux bridge, and does not need to change; Domains,
   Excavate, Continuum and the rest can now call `SuiteGates` (still to wire).
2. **Generic use gate: done in PhoenixCore (`UseGates`).** Data pack files in `data/<ns>/phoenix/use_gates/*.json` give a
   `gate` expression, `items` / `blocks` (ids or `#tags`), `actions` (`use`, `interact`, `break`) and a `message`. Enforced
   on the server for right-click item, right-click block and block break. Not yet covered: placing blocks, opening
   containers by menu, and crafting (crafting is still the recipe gate).
3. **Shop research hook: done in PhoenixCore.** `ShopEntry.requires` (a gate expression, saved with the entry), entries a
   player does not satisfy are left out of their sync and refused at purchase; operators see and can buy everything so they can
   edit. Set with `/phoenixshop require "<name>" <expression>`; `/phoenixshop require "<name>"` clears it. Editing an entry in
   the shop screen keeps its gate.

### Gate features by mod (all through `SuiteGates`, wiki 0.6.0)

Bind any of these with `SuiteGates.bind(feature, scope, expression...)`. A feature nothing is bound to is allowed. Scope is a
dimension id where the mod has per-dimension features (Domains, Excavate, Essentials), otherwise `*`.

| Mod | Class | Features |
|---|---|---|
| Domains | `DomainGates` | `domains:claiming`, `domains:chunkloading`, `domains:claim_flags` |
| Excavate | `ExcavateGates` | `excavate:<id>` for `vein_mining`, `vein_placing`, `vein_harvesting`, `vein_tool_use`, `vein_replacing`, `vein_planting` and any id an action registers |
| Continuum | `ContinuumGates` | `continuum:map`, `missions`, `swarms`, `travel`, `anomalies` |
| Chronicles | `ChroniclesGates` | `chronicles:claim_rewards`, `submit_tasks`, `emergency_items`, `auto_claim`, and `chronicles:quest/<quest id>` for one quest |
| Essentials | `EssentialsGates` | `essentials:teleport`, `kits`, `social` |
| Tesla | `TeslaGates` | `tesla:discharge`, `lore_scan`, `suite_menu`, `tesla_mode` (1.20.1 has discharge and tesla_mode) |
| Guilds | `GuildGates` | `guilds:create`, `home`, `alliances`, `chat`, `wiki` (managing a guild you are in is never gated) |
| Solaris | (own API) | Any `SuiteGates` gate id also works as a Solaris gate |
| Phantasia | none | Wand and manual are items: use `phoenix/use_gates` |

Not done yet: passive Tesla module effects (they tick
inside GregTech's module code, so only the key-triggered abilities are gated), and Core's own items beyond `use_gates`.
