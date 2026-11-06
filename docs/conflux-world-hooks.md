# Conflux world hooks

World hooks let quests, archive entries and research react to things that happen in the Conflux dimensions, with no
code. Every hook is an event; a **rule** says "when this event happens, and these conditions hold, do these actions".

Rules are datapack JSON: `data/<namespace>/conflux_hooks/<name>.json`, reloaded with `/reload`.

## Events

Every event carries the player, where they are, and some data. Everything is also posted to the NeoForge bus as
`ConfluxWorldEvent` (for addons), and fired to Chronicles as the trigger `phoenixcore:conflux/world/<type>` with the
data as its tag.

| type | when | data |
|---|---|---|
| `discipline_entered` | a player arrives in a Conflux dimension | `discipline` |
| `discipline_left` | a player leaves one | `discipline` |
| `region_entered` | the player walks into another terrain region (a biome of the dimension) | `region` (the biome id, such as `sculk_marsh`), `discipline` |
| `void_edge` | the player is at the edge of a Void chasm (at most every second; give rules a cooldown) | `margin` (0 at the edge) |
| `familiarity_up` | the Sculk now knows a chunk better (levels 1 to 5) | `level`, `chunk_x`, `chunk_z`, `known_chunks` (chunks known at level 1 or more) |
| `landform_discovered` | the player's team finds a landform | `landform`, `label`, `x`, `z`, `icon` |

You can fire your own from a rule with the `event` action, or from code with `ConfluxWorld.fire(player, type, data)`.
`/conflux world fire <type>` fires one as yourself, for testing.

## A rule

```json
{
  "enabled": true,
  "on": ["region_entered", "familiarity_up"],
  "conditions": { "discipline": "sculk", "min_level": "3" },
  "once": "team",
  "cooldown_ticks": 0,
  "actions": [ { "type": "message", "value": "{player} reached level {level} at {x}, {z}." } ]
}
```

- `on`: one type, a list, or `"*"` for all.
- `enabled`: false switches a rule off (the shipped example is off).
- `once`: `never` (default, every time), `player` or `team` (only the first time for that player or team).
- `cooldown_ticks`: the least ticks between two runs for the same player.

### Conditions

All must hold.

- `discipline`: `phoenix`, `sculk`, `void`, `sealed_a`, `sealed_b`.
- `dimension`: the full dimension id.
- `y_min`, `y_max`: the player's height.
- `min_<key>`, `max_<key>`: the event's number `<key>` at least or at most this (`min_level`, `max_margin`...).
- any other key: the event's `<key>` must equal this text (`region`, `landform`...).

### Actions

Texts can use `{player}`, `{x}`, `{y}`, `{z}` and every key the event carries, such as `{level}` or `{region}`.

| type | value | what it does |
|---|---|---|
| `flag` | the flag | grants a Conflux research flag to the team |
| `unlock` | a research node id | unlocks the node |
| `archive` | the entry | unlocks an archive entry |
| `chronicles` | a trigger id | fires a Chronicles trigger (the event's data, plus the action's `data`) |
| `command` | a command | runs it; `"as": "server"` for full permission, otherwise as the player at permission 2 |
| `message` | text | tells the player |
| `event` | a hook type | fires another hook with the same data (chains stop after three) |

## The Sculk and where you have been

The Sculk Conflux learns the chunks players spend time in (and around). The more it knows a place, the more of it the
sculk has grown over (natural ground only, plain blocks only). Levels 0 to 5 need 0, 6, 20, 50, 120 and 260 points; a
chunk gets 2 points a second while you are in it and 1 a second while you are next to it. The Solaris map tints the
chunks it knows. Turn it off with the `familiarityGrowth` (server) and `familiarityOverlay` (client) config options.

`/conflux world familiarity <points>` sets the chunk you stand in, for testing.
