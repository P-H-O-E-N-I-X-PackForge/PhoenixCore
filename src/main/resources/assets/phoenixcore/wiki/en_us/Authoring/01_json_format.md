# Research Tree JSON Format

Datapack path: `data/<namespace>/conflux/research/<file>.json`. It is a normal datapack reload.

```
{
  "title": "My Tree",
  "description": "Markdown **works** here.",
  "group": "Discipline",
  "discipline": "mypack_discipline",
  "switch_cost": { "material": 500 },
  "nodes": [
    {
      "id": "mypack:first_node",
      "title": "First Node",
      "lore": "Markdown lore shown in the node viewer.",
      "icon": "minecraft:diamond",
      "position": [0, 0],
      "cost": { "material": 100 },
      "commitment": true,
      "unlocks": [
        { "type": "flag", "value": "mypack_started" },
        { "type": "item", "value": "minecraft:diamond*4" }
      ]
    },
    {
      "id": "mypack:second_node",
      "prerequisites": ["mypack:first_node"],
      "position": [1, 1]
    }
  ]
}
```

- **Data types** for `cost` and `switch_cost`: `material`, `biological`, `energetic`, `computational`, `arcane`.
- **discipline** marks the tree as a discipline's main tree. Omit it for utility trees, which never trigger a commitment.
- **requires_discipline** hides the tree from everyone who did not choose that discipline.
- **hidden: true** keeps a node invisible until its prerequisites are met; **hint** is shown for a node you can see but not yet read.
- **exclusion_group** locks out every other node sharing the group once one is researched.
