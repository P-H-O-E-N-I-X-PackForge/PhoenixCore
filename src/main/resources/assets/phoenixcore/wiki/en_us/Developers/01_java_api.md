# Java API

Other mods should go through `net.phoenix.core.api.ConfluxApi`. It resolves the player's team the same way the terminal does, so callers never deal with team ids.

```
ConfluxApi.hasFlag(serverPlayer, "px_awakened");
ConfluxApi.isUnlocked(serverPlayer, nodeId);
ConfluxApi.discipline(serverPlayer);
ConfluxApi.grantFlag(serverPlayer, "my_flag");
ConfluxApi.grantUnlock(serverPlayer, nodeId);
```

- **hasFlag / isUnlocked** - read a team's progress.
- **discipline** - the discipline the team chose, or null.
- **grantFlag / grantUnlock** - free grants with no terminal or cost involved, for quest rewards and similar. They sync to the team's clients.
- **archiveUnlocker** - a hook Phoenix Archive sets so `archive` unlocks can unlock entries.

## Internals

The authoritative store is `WorldResearchData` in `net.phoenix.core.integration.conflux.research`, a team-scoped SavedData. `tryUnlock(team, node, terminal, registry, player)` is the cost-spending path the terminal uses.

## Depending on PhoenixCore

Publish PhoenixCore to mavenLocal and add it as `compileOnly` with `transitive = false`. Touch `ConfluxApi` only from a class that is loaded after you have checked the mod is present, so packs without PhoenixCore never resolve it.
