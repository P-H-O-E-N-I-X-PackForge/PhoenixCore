# TFG worldgen catalog (for the Conflux dimensions)

What the TerraFirmaGreg Core-Modern fork (`../Core-Modern`) has that we could pick from for the Conflux dimensions.
This is a list of what exists, not a plan. Nothing here has been copied into PhoenixCore.

## Licence, before copying anything

- `data/tfg/**` and `su.terrafirmagreg.core.*` are TFG's own work. Core-Modern is LGPL-3.0, so reuse is fine with the
  licence text and attribution kept.
- `data/tfc/**` (structures, recipes, advancements) and the whole `net.dries007.tfc.*` source tree in that repo are
  **TerraFirmaCraft's**, vendored into the fork. They carry TFC's licence, not TFG's. Check it before copying any of it.

## What it is made of

The worldgen is a data pack on top of TerraFirmaCraft. Almost everything refers to `tfc:` features, carvers, blocks and
density functions, so it does not run on its own in our dimensions. The reusable part is the **content list and the
parameters**, not the files as they stand.

## Biomes (109, `data/tfg/worldgen/biome/earth`)

Terrain families (each is a set of variants such as plains, hills, plateau, canyons, lake):

| Family | Biomes |
| --- | --- |
| Karst | tower_karst_{plains, hills, highlands, canyons, bay, lake}, shilin_{plains, hills, highlands, plateau, canyons}, doline_{plains, hills, highlands, plateau, canyons, rolling_hills}, extreme_doline_{mountains, plateau}, cenote_{plains, hills, highlands, plateau, canyons, rolling_hills}, burren_{plains, plateau, badlands, badlands_tall, roche_moutonee} |
| Canyons and badlands | badlands, buttes, canyons, low_canyons, stair_step_canyons, whorled_canyons, mesas, hoodoos |
| Volcanic | active, dormant, extinct, ancient, old, sunken, glaciated and ice-sheet shield volcanoes, shield_volcano_shore, volcanic_mountains, volcanic_oceanic_mountains and their lakes |
| Glacial and ice | ice_sheet (+ edge, mountains, oceanic, shore, shield volcano, tuyas), glaciated_mountains, glacially_carved_mountains (+ oceanic), subglacial_lake, meltwater_lake, tuyas, drumlins, knob_and_kettle, patterned_ground, inverted_patterned_ground |
| Arid and coastal | dune_sea, grassy_dunes, coastal_dunes, salt_flats, salt_marsh, mud_flats, tidal_flats, guano_island, sea_stacks, setback_cliffs, rocky_shores, embayments |
| Oceans and water | ocean, deep_ocean, deep_ocean_trench, ocean_reef, river, lake, mountain_lake, old_mountain_lake, plateau_lake |
| Standard land | plains, hills, rolling_hills, lowlands, highlands, plateau, plateau_wide, rocky_plateau, mountains, old_mountains, oceanic_mountains, terrace_lower, terrace_upper, stone_circles |

The biome tag files under `data/tfg/tags/worldgen/biome/earth` sort these by climate and shape: cold, dry, hill,
mountain, karst, volcanic, sandstone, swamp, salt flats, ice sheet, lake, river, ocean.

## Nether-style biome tags (`data/tfg/tags/worldgen/biome/nether`)

Only the tags are in Core-Modern, not the biome files. They name a set of hell biomes that could suit Sculk or Void:
ash_forest, basalt_deltas, decaying_caverns, geothermal_springs, lava_floes, lush_hollow, muggy_bog, salt_caves,
tar_pools, thorny_brambles, webbed_lair, plus resource tags has_garnet_sands and has_oilsands.

## Large and terrain features (`configured_feature/earth` and `placed_feature/earth`)

- **Fissures and tuyas:** tuya_fissure, tuya_caldera, tuya_rivulet, random_tuya_fissure, diamond_tuya_fissure,
  topaz_tuya_fissure.
- **Volcanic:** pumice_shield_volcano (+ patch), pumice_patch, lava_surface_spring.
- **Rock formations:** sea_stacks (+ patch), cave_spike, ice_cave.
- **Water:** water_surface_spring, extra_water_surface_spring, glacial_spring, glacial_oceanic_spring,
  ice_sheet_spring, flood_fill_lake.
- **Ground cover:** alfisol_disc, podzol_disc, duff_disc_{normal, edge, old_growth}, surface_grasses, forest.
- **Odd extras:** feather and feather_patch, extra_island_guano (+ patch), salt_lick_patch.

## Plants (61 configured, 66 placed, `earth/plant`)

anemone (green, large orange, large purple, purple), azalea, barnacles, bear_grass, bird_nest_fern, buttercup,
cornflower, cycad, dry_grass, edelweiss, elegant_sunburst_lichen, fan_palm, flame_vine, kinnikinnick, moss_campion,
mountain_hullwort, mussels, palash, penwortel, prickly_pear (+ purple), qantu, ramirezella, ramunda, red_oat_grass,
shawiash, silken_pincushion_cactus, silver_bromeliad, starfish, tank_bromeliad, yellow_saxifrage. Most have a `_patch`
variant.

## Tree structures (`data/tfc/structures`, 539 `.nbt` files, TFC's)

acacia, ash, aspen, birch, blackwood, chestnut, douglas_fir, hickory, kapok, mangrove, maple, oak, palm, pine,
rosewood, sequoia, spruce, sycamore, white_cedar, willow. Most also have `_dead` and `_large` variants. They use TFC wood
blocks, so they would need their blocks remapped for a Conflux dimension.

## Java worth reading (`net.dries007.tfc.world`, TFC's)

Not TFG's, but it is where the terrain comes from: `TFCChunkGenerator`, the biome `layer` and `region` code,
`BiomeNoiseSampler`, `ChunkNoiseFiller`, `river`, `carver` (cave, canyon, karst caves) and the `feature` classes (fissure,
boulders, hot springs, ice cave, cave spikes, flood-fill lake, loose rock). TFG's own Java in
`su.terrafirmagreg.core` has no worldgen of its own.

## What the Conflux profile can take

`WorldgenProfile` already has biome definitions, decoration (trees, flowers, shrubs, special decorations), cave, liquid
and structure profiles. So the cheap route is to take the **names and shape parameters** (karst, shilin, tuya, sea
stacks, shield volcano, dune sea, salt flats, hoodoos, stone circles) as biome and decoration presets, and write the
placement ourselves.

## Not in Core-Modern

The planets and space dimensions (moon, mars and so on) are not in this repo. They live in the TFG modpack, which is not
in this workspace.
