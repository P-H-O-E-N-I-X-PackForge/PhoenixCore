# Wings of Fire compatibility

The Wings of Fire mod (phoenixes) is All Rights Reserved, so nothing of it is copied or patched. PFT only reads its public entity data and uses vanilla taming and tag APIs, all keyed off the mod's own `wings_of_fire:phoenix` entity tag, so every part is inert if the mod is absent. Code: `integration/wingsoffire`. Config: `wingsOfFire` in the Phoenix config.

- **Phoenixes stay** (`persistentMounts`): the mod discards a phoenix as soon as it has been ridden and has no rider. Its "has been ridden" flag is cleared whenever the phoenix has no rider, so that branch never runs.
- **Stay command** (`stayCommand`): sneak + right-click your own phoenix with an empty hand to order it to stay where it is (also in mid-air; its spot is remembered and held exactly). Do it again to let it follow you. Riding it clears the order.
- **Conflux physics** (`data/phoenixcore/tags/entity_types/ignores_conflux_physics.json`): the Conflux gravity zones and conveyors used to act on every entity, which squashed a phoenix's own vertical velocity (a zone at 0 stopped it climbing) and shoved it. Tagged entities and their riders are now exempt; add other flying mounts to the tag.
- **Conflux spawns**: the mod gives phoenixes no natural spawning anywhere and the Conflux biomes list no animals, so they are added by the general Conflux spawn lists (see [Conflux mob spawns](#conflux-mob-spawns)): fire and red in Phoenix, dark in Void, nature in Sculk, gilded in Sealed A, wind in Sealed B.


## Conflux mob spawns

What lives in each Conflux dimension is data: `data/<namespace>/conflux/spawns/*.json`. Files for the same discipline from different namespaces add together, so other mods and packs can add their own mobs.

```json
{
  "discipline": "sealed_a",
  "spawns": [
    { "entity": "wings_of_fire:gilded_phoenix", "weight": 3, "cap": 1, "placement": "air" },
    { "entity": "phoenixcore:teto_slime", "weight": 4, "cap": 4, "placement": "ground" }
  ]
}
```

- `weight` picks among the discipline's mobs that are not yet at their cap.
- `cap` is the most of that kind within `countRadius` of one player.
- `placement` is `ground` (on the surface) or `air`.
- Entities from mods that are not installed are skipped, so lists can name any mod's mobs.
- Config (`confluxSpawns`): `enabled`, `intervalTicks`, `chance`, `countRadius`.

## Teto slime

`phoenixcore:teto_slime` is a harmless slime (jumps, grows and splits, but never targets or hurts anything; spawns small 55%, medium 35%, large 10%) with a Kasane Teto model built from `kasana_teto3.bbmodel`. It spawns in every discipline's Conflux dimension. It drops slime balls and sometimes bread, and has a spawn egg. The texture at `assets/phoenixcore/textures/entity/teto_slime.png` is the one embedded in the model file; replace it freely, the UV layout is the model's 128 x 128 box UV.
