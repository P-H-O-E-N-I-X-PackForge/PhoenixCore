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

## How a team lifts a restriction

Research the node that grants the flag, usually in that discipline's *Dispensations* tree, or earn it from a Chronicles quest whose reward grants the flag.

## Reading the condition

A restricted recipe shows its condition in the recipe viewer. The check uses the nearest player within 24 blocks of the machine.
