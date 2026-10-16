# Unlock Types

Each entry in a node's `unlocks` list is `{ "type": "...", "value": "..." }`. Recognised types:

- **flag** - `value` is any string id, added to the team's flag set. Check it with `ConfluxApi.hasFlag`, or gate a GTCEu recipe with it (see Recipe Restrictions).
- **multiblock** - `value` is a machine registry id. The team may form that multiblock once it is unlocked; checked by the multiblock gate before a gated machine forms.
- **recipe_tag** - `value` is a tag string, checked with `hasRecipeTag(team, tag, ...)`.
- **world_stage** - `value` is a stage name. Advances the discipline dimension's world stage for the team.
- **item** - `value` is `namespace:item` or `namespace:item*count`. Handed to the player who researches the node, dropped at their feet if their inventory is full.
- **archive** - `value` is a Phoenix Archive entry id. Shown in the node viewer with a "Read lore" button once Archive provides an opener.

---

Unknown unlock types are ignored rather than rejected, so a typo never breaks the whole tree's load.

## Rewards

Item rewards are shown as item icons in the node viewer before you research the node, and marked as claimed afterwards. A node researched through **Research All** pays its rewards like any other.
