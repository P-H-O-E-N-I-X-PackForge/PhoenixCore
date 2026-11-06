# Phoenix Chronicles Bridge

Phoenix Chronicles has optional Conflux support, built on `ConfluxApi`. Chronicles works with or without PhoenixCore installed.

- **Quest task** `conflux_research` - complete when a node is researched, or a flag is set (`flag_mode`).
- **Quest reward** `conflux_unlock` - grants a node or flag when the quest is claimed.
- **Quest condition** `conflux:flag:<flag>` - use it in `enable_if` to show a quest only to teams with a flag.

## Per-discipline questlines

`questlines/conflux/` in the PhoenixCore repo holds a generated questline per discipline. Every quest is hidden until the team has the discipline's first research flag, so each player only sees their own line. Regenerate them with `tools/gen_conflux_questlines.py`.

Quests can also grant the flags that lift a discipline's recipe restrictions, so a quest and a research node can be two routes to the same recipe.
