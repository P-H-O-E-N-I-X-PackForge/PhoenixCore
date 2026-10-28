# Personal dimension examples (Continuum + PhoenixCore, 1.20.1)

A worked example of Continuum's personal dimension (pdim) worldgen API, using ten of PhoenixCore's own planets and moons.
Every feature of the API is used at least once, so you can copy the part you want.

## Install

Copy these into your pack (or a test instance), keeping the folders:

| This folder | Goes to |
| --- | --- |
| `kubejs/startup_scripts/phoenixcore_pdim_worlds.js` | `kubejs/startup_scripts/` |
| `kubejs/server_scripts/phoenixcore_pdim_events.js` | `kubejs/server_scripts/` |
| `kubejs/data/phoenixcore_examples/` | `kubejs/data/` (the breathing gear item tag; on 1.20.1 item tags live in `tags/items/`) |
| `config/phoenix_continuum/pdim_worldgen/*.json` | `config/phoenix_continuum/pdim_worldgen/` (the same API as files, for packs without KubeJS) |

You need the 1.20.1 build of Continuum (0.2.5 or newer), PhoenixCore (this branch) and KubeJS 6. Install **Infiniverse 1.0.0.5** (Forge,
Modrinth) as well if you want worlds made while the game runs with no restart, and for the rules at the bottom of the script (they are
worked out when a dimension is made).
Without Infiniverse a changed spec needs a restart, and only the named worlds (not the rules) apply.

Use a fresh world: chunks a world has already generated keep their old terrain.

## The worlds

| World | Body | What it shows |
| --- | --- | --- |
| **Anvil** | home system, rocky and industrial | A shared colony instead of plots: `.plots(false)`, `.spawnAt(...)`, GregTech's overworld ores with `.gtOres(...)`, `.weather('clear')`, and a house put down once for the whole dimension with `.plotStructure(...)`. |
| **Halo** | home system, airless moon | Vacuum rules: `.requireOxygen(tag)` (damage without a helmet from the tag), `.mobs(false)`, protected plots with `.plotRadius(...)` and `.protectPlots(true)`, a flat world with layers. |
| **Rime** | home system, ice world | Weather of its own that comes and goes: `.weatherCycle(...)`, on a vanilla noise world in an ice biome (so the rain is snow). |
| **Meridian** | home system, temperate ocean world | `.auto()`: the world is worked out from the body's own data, so there is nothing else to write. |
| **Ember** | Aurelian, volcanic crust | The nether's terrain with a biome source of your own (an object), the nether's ores, and `.fallDamage(true)`. |
| **Cinder** | Reliquary, scorched | A flat basalt world under a permanent `.weather('storm')`, `.mobs(false)`, and a house at every player's plot. |
| **Calliope** | Carousel, pink gas giant | No ground: `.voidWorld()` (the obsidian pad), a storm and no air. |
| **Marrow** | Thalassa, dark ocean world | `.generator({...})` with another mod's chunk generator and `.dimensionType(...)`: a Conflux discipline world, with its structures and ores, on a Continuum planet. |
| **Lantern** | Vesper, glowing jungle | A raw biome and permanent rain. |
| **Pelagia** | Thalassa, tidal sea | The file version of the same API (`pelagia_tidal_sea.json`). |

The three rules at the end of the startup script show `event.type(...)`, `event.system(...)` and `event.where(...)`: every other
moon becomes bare rock with no air, the rest of the home system works itself out, and any giant becomes a platform in a storm.
A world you name always wins over a rule. `home_system_defaults.json` is the file form of a rule (`match`), using `auto` and a
weather cycle.

The server script is the events: `ContinuumEvents.pdimEnter` (with `event.created`, true the first time) gives a welcome and a
starter kit, and `pdimLeave` logs.

## Try it

1. Start the game. In a world, run `/continuumadmin pdimworldgen`: it lists every body, what it will be made of (void or the
   generator), where players arrive, and whether the dimension is loaded.
2. Survey a body (the example bodies are in the home system, which starts surveyed: Anvil at least), then make its personal
   dimension from the map screen and step in.
3. Change something in the script and restart. With Infiniverse, `/continuumadmin pdimworldgen reset <body>` drops a loaded
   dimension so the next visit makes it again from the current spec (what was built stays).

## Things to know

- **1.20.1 differences:** the scripts are the same as the 1.21.1 ones. Weather is applied after the game's weather step instead of replacing it
  (see Continuum's `PACKDEV.md`, "On 1.20.1"), and `.gtOres(...)` works with GregTech 7.5.3 (with GregTech 8 it is skipped, and the log says so).

- The breathing gear tag (`#phoenixcore_examples:breathing_gear`) holds a few vanilla helmets. Add your own items to the tag
  file to change what counts as a suit.
- `plotRadius` and `protectPlots` only matter where there are plots; on Anvil (no plots) they would do nothing.
- Weather is per dimension: Rime's cycle and Cinder's storm do not touch the overworld, and `/weather` does not touch them.
- Full documentation: the "Personal dimension worldgen" sections of Continuum's `PACKDEV.md`.
