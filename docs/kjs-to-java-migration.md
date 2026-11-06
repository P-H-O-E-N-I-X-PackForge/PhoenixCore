# KubeJS to Java: Phoenix Forge Technologies

The scripts in `Phoenix-Forge-Technologies/kubejs` are being moved into PhoenixCore as Java, because they all have to be
reworked for 1.21.1 anyway. Everything lives in PhoenixCore (no separate pack mod). This file is the map: what each script
became, what is left, and how ids changed.

## Ids change namespace

KubeJS registered things under `kubejs:` (items, fluids, blocks) and, for GregTech materials, under `gtceu:`. In Java they
belong to PhoenixCore, so:

| was | is |
|---|---|
| `kubejs:<id>` (items, blocks, fluids) | `phoenixcore:<id>` |
| `gtceu:<custom material>_<form>` (for example `gtceu:stabilized_antimatter_plate`) | `phoenixcore:<custom material>_<form>` |
| fluids that were plain KubeJS fluids (`kubejs:chaos`) | fluid-only GregTech materials, so `phoenixcore:chaos` |

Recipes and other scripts that mention the old ids have to be ported with the new ones; the ports below say where that is
done.

## Startup scripts

| script | now | status |
|---|---|---|
| `registry/phoenixware_mats_registry.js` (items) | `common/data/item/PftItems.java`; textures under `textures/item/{componets,veins,phoenixware}` | done |
| `registry/phoenixware_mats_registry.js` (quantum binder dusts) | `common/data/materials/PftMaterials.java` | done |
| `registry/divine_mats.js`, `phoenix_enriched_neutronium.js` | `PftMaterials.java`, elements in `PhoenixElements`, icon sets in `PhoenixMaterialSet` | done |
| `registry/divine_fluids.js`, `phoenixware_fluids_registry.js` | `PftMaterials.java` (plasmas, and the plain fluids as fluid-only materials; the thin/thick texture choice is gone, GregTech's own fluid look is used) | done |
| `registry/phoenixware_materials.js` (item modification) | `common/data/item/PftItemTweaks.java` | done |
| `registry/phoenixware_blocks_registry.js` | `common/block/PhoenixBlocks.java` (21 casings and the dust block were missing; the rest, and the heat coil, already existed) | done |
| `components/hsla-steel.js` | `PftMaterials.modify()` | done |
| `registry/phoenixware_circuits_registry.js` | `common/data/item/PftCircuits.java` (the commented-out lines are not ported) | done |
| `registry/multiblocks/*.js`, `machinery/*.js`, `recipes/*.js` (recipe types and machines) | `PftRecipeTypes`, `PftMachines` | done |
| `active_transformer.js` | `PftMachines.ActiveTransformerOverride` and the new `conductive_honey_coil` block in `PhoenixBlocks` | done |
| `recipereplacement.js` (crafting components) | `PftCraftingComponents` | done |
| `dimension_marker.js` (Ad Astra dimension markers) | | not ported: the plan is to gate IV by Continuum, not Ad Astra |
| `icons.js` (`pft_icon`; renaming mods) | `pft_icon` in `PftItems` | the mod renames are not possible from Java |
| `deprecations.js`, `deprecation_pipeline.js` | | empty / unused, nothing to port |
| `material_testing/material.js` | | a test material, not ported |

## Server scripts

| script | now |
|---|---|
| `server_scripts/gregtech/*.js` | `common/data/recipe/pft/Pft*Recipes.java`, run by `PftGtRecipes` from the GregTech addon |
| the other recipe scripts (AE2, ExtendedAE, architects palette, feathers, steel and bronze, CC: Tweaked and Advanced Peripherals, Functional Storage, Integrated Dynamics, Sophisticated Storage and Backpacks, xtones, building gadgets, torchmaster, hang glider, elevator, hnn, misc, vanilla, smelting) | one `Pft<Name>Recipes.java` each in the same folder, run by `PftGtRecipes`; the loops in the AE2 scripts are in `PftAe2LoopRecipes` |
| `event.remove` in every script, `recipe_removal.js`, `appliedenergistics2/removals.js`, the removals in `recipe_replacement.js` | `PftRemovals` (data) read by `api/RecipeBlacklist` while recipes load; it now also removes by recipe type and by pattern, and reads the 1.21 `result.id` |
| `event.replaceInput` (certus quartz, integrated tunnels hoppers) | `PftRemovals.apply()` adds rules to `api/RecipeReplacements`, applied to the recipe JSON as it loads |
| `ServerEvents.tags` (`tags/*.js`, plus the ones in `adastra.js` and `steel_and_bronze.js`) | datapack tag files under `src/main/resources/data/{c,ae2,gtceu,ad_astra}/tags`, every entry optional; the removals in them are `remove` entries, which only work if this pack loads after the tag's own |
| `event.custom` (sophisticated backpack and storage upgrades) | datapack recipes `data/phoenixcore/recipe/pft_*`, in the 1.21 format with `neoforge:conditions` |
| `event.smelting`, `event.stonecutting` | `PftCooking` |

Recipes are written with `PftRecipe` (GregTech machine recipes), `PftCrafting` (crafting table) and `PftCooking`
(furnace types, stonecutting), which take items and fluids as text the way the scripts did, so a script moves over line
by line. Ids move as described above, and a recipe that mentions something that does not exist is skipped and logged
(`[PFT] skipped recipe ...`), with a summary at the end of `PftGtRecipes.init`. **Look at that log after the first run**:
it is the list of what still needs an id fixed. Mods that are not in the pack just cause their recipes to be skipped.

Recipes of `phoenixcore` are exempt from removals by result, input or pattern (only an exact id removes them), because
they are the replacements for what the scripts removed.

Not carried over:

* from `server_scripts/gregtech`: the manual scanner recipes in `iv.js` (GregTech makes them from the research); the rock
  breaker in `earth_sieve.js` (needs adjacent-fluid conditions); the bucket returned by the water source crafting recipe;
  the kept wooden form of the coke clay recipe; the commented-out fusion scripts.
* `adastra.js`: the rockets, the NASA workbench and the replacements. Ad Astra is not part of the plan (IV is gated by
  Continuum); only its two fuel tags and armor tags came over, and all of it is optional.
* `recipe_replacement.js` / `adastra.js` tier replacements in GregTech recipes (steel to aurum steel and so on): those
  recipes are built in code, which JSON replacement cannot reach; `PftCraftingComponents` covers the crafting components.
* `portable_cells.js`: the portable cell is made without its preset charge (the component that holds it is not known).
* `ore_veins.js`: dropped on purpose. Only dormant ember and permafrost were kept, as ores of the Conflux dimension's
  `ProvinceVeinTemplates` (ember in the crimson and ashlands regions, permafrost in the peaks); the rest is not wanted.

## Client scripts

| script | now |
|---|---|
| `tooltips.js` | `client/tooltips/PftScriptTooltips` with the text in `assets/phoenixcore/pft_tooltips.json` (materials by name and the forms they apply to, then single items; `addAdvanced` lines are appended instead of inserted at a position) |
| `hide.js`, `jei_hidden_categories.js` | `PhoenixEmiPlugin.hideFromScripts` (EMI; JEI is not used for this) |

## NBT recipes

Done as 1.21 data components: the fire resistance potions and the silk touch book in
`PftVanillaRecipes.componentRecipes`, the infinity cells as ExtendedAE's own `infinity_water_cell` and
`infinity_cobblestone_cell` items. ExtendedAE's mod id is `extendedae` in 1.21 (it was `expatternprovider`), and the ids in the Java were changed to
match; recipe ids inside the mod changed too, so check the skipped log for ExtendedAE entries.
