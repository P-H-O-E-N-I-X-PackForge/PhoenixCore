# Signature Mechanics

Each discipline has a **signature meter**: a number that belongs to the team, with a ceiling, named levels, and a rate it moves by itself. What makes a path feel like itself is what feeds that meter, and what its levels open.

## Defining a meter

One file per meter in `data/<ns>/conflux/signatures/`. The file name is the meter's id.

```
{
  "discipline": "phoenix",
  "title": "Ember",
  "description": "Nothing here is kept for free.",
  "color": "FF7A2E",
  "icon": "minecraft:blaze_powder",
  "max": 1000,
  "start": 100,
  "gain_per_minute": 0,
  "decay_per_minute": 3,
  "levels": [
    { "at": 0, "name": "Cold" },
    { "at": 100, "name": "Kindled" },
    { "at": 350, "name": "Blazing" }
  ]
}
```

- **gain_per_minute / decay_per_minute** - the meter moves by the difference, counted from the game time, so a team that was away for an hour has had that hour (at most a day's worth). Ember decays, Patience gathers, Loop stays where it is until something moves it.
- **levels** - named steps by value. Level 0 is the first.

The three that ship are `ember` (Phoenix), `loop` (Sculk) and `patience` (Void). The meter is shown as a thin bar under the discipline's name in the research terminal sidebar.

## What a level opens

Every level a team has reached is a flag, `sig:<id>:<level>`: `sig:ember:2` means Ember is at level 2 or above. It works anywhere a flag does:

```
{ "flag": "sig:ember:2", "only_disciplines": ["phoenix"], "crafting": true, "recipe_ids": [ ... ] }
```

Unlike an unlock flag, it is lost again if the meter falls back below the level.

## Research that needs a level

A research node can ask for flags, and a signature level is one:

```
{ "id": "phoenixcore:px_something", "requires_flags": ["sig:ember:2"], "cost": { ... } }
```

The node cannot be researched until the team holds every flag listed, and the node page shows each one with a tick when it is met. It is checked when the research is attempted, so a meter that falls afterwards does not take back what was researched; it only keeps the node closed until the level is reached again. The list works with any flag, not just signature levels, such as the flag another node or a Chronicles quest grants.

## Making a meter move (Java)

Event-driven things call the API from wherever they happen:

```
Signatures.add(level, teamId, "ember", 25);   // a pyre was fed
Signatures.set(level, teamId, "loop", 0);     // a loop was broken
Signatures.level(level, teamId, "ember");     // the level it is at
```

Things that run all the time register a mechanic, called once a second for every team on that discipline:

```
SignatureMechanics.register("loop", (level, team, definition) -> { ... });
```

## Testing

`/conflux dev signature show` lists your team's meters. `/conflux dev signature <id> add <n>` and `... set <n>` move one.
