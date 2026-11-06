# Conflux starting chests

A team arrives in a Conflux dimension on the ground, next to a chest of starting items. There is no platform: the spot
is the nearest dry, fairly level ground to the middle of the dimension, remembered for everyone who comes later.

## What is in the chest

One loot table per dimension, in the mod's data:

```
src/main/resources/data/phoenixcore/loot_table/chests/conflux/start/phoenix.json
src/main/resources/data/phoenixcore/loot_table/chests/conflux/start/sculk.json
src/main/resources/data/phoenixcore/loot_table/chests/conflux/start/void.json
src/main/resources/data/phoenixcore/loot_table/chests/conflux/start/sealed_a.json
src/main/resources/data/phoenixcore/loot_table/chests/conflux/start/sealed_b.json
```

(`data/phoenixcore/loot_table/chests/conflux/start/<dimension>.json` in a datapack or resource pack overrides them.)

They are ordinary chest loot tables. Every item is its own pool with `rolls: 1`, so each is always given; to make an item
chancy, give its pool a `rolls` range or add a `minecraft:random_chance` condition. For example, to add a stack of
glass to the Sculk chest, add a pool:

```json
{ "rolls": 1, "entries": [ { "type": "minecraft:item", "name": "minecraft:glass",
    "functions": [ { "function": "minecraft:set_count", "count": 16 } ] } ] }
```

## Existing worlds

The chest is made the first time someone enters the dimension. A world that already made the old platform keeps it, and
gets a chest on the ground the next time someone enters (the old chests had nothing in them). A chest that is broken or
missing is put back.
