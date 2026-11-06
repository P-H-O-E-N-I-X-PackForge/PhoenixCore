# Recipe Restrictions

Files in `data/<ns>/conflux/gated_recipes/` attach a research condition to GTCEu recipes. A rule matches recipes by id, by recipe type and minimum tier, or by output item/tag.

```
{
  "flag": "px_contained_reactions",
  "only_disciplines": ["phoenix"],
  "recipe_types": [ { "type": "gtceu:chemical_reactor", "min_tier": "hv" } ]
}
```

- **flag** - the research flag that opens the recipe. Optional: with no flag the recipe is simply closed to the scoped disciplines.
- **only_disciplines** - the restriction applies to these disciplines only. Everyone else is unaffected.
- **except_disciplines** - the restriction applies to everyone except these.

With neither list, the rule applies to every team, exactly like the original gates.

## Crafting table recipes

A rule only reaches machine recipes unless it says `"crafting": true`. With it, the rule also covers crafting-table recipes that it names by `recipe_ids`, or by what they make (`output_items`, `output_tags`). The same scope and flag apply: a player whose discipline is in scope cannot craft the recipe until they have the flag, and a rule with no flag closes it for good. The crafting grid then shows no result.

```
{
  "only_disciplines": ["phoenix"],
  "crafting": true,
  "recipe_ids": ["mypack:gear_sculk_version"]
}
```

Crafting-table recipes are the only non-machine recipes a rule reaches. Furnaces, stonecutters and the like are not covered.

## A different recipe for each discipline

Write one recipe per discipline for the same result, and close each to everyone else. For example, `gear_phoenix` with `"except_disciplines": ["phoenix"]` and `gear_other` with `"only_disciplines": ["phoenix"]`, both with no flag, leaves exactly one of the two craftable by any team. Recipes that a rule closes for good are hidden from the recipe viewer (see below), so each team sees only its own version.

## What the recipe viewer shows

- A recipe a rule closes for good to your discipline (no flag, so nothing can lift it) is hidden in EMI.
- A recipe that only waits for research stays visible, with the research it needs, because that is something to work toward.
- The viewer rebuilds itself when your discipline changes, so a team that switches paths sees the recipes of the new one.

## How a team lifts a restriction

Research the node that grants the flag, usually in that discipline's *Dispensations* tree, or earn it from a Chronicles quest whose reward grants the flag.

## Reading the condition

A restricted recipe shows its condition in the recipe viewer. The check uses the nearest player within 24 blocks of the machine.
