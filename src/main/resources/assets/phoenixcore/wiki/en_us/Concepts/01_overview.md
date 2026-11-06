# Conflux: Research & Discipline System

A datapack-driven research tree, browsed and unlocked at a Research Terminal. Progress is tracked per-team (or per-player if no team mod is installed), server-side, in WorldResearchData.

---

## Core concepts

- **Research tree** - A JSON file: a set of nodes, optionally tied to a discipline

- **Research node** - One unlockable point in a tree: costs, prerequisites, unlocks

- **Unlock** - What a node grants when unlocked: a flag, a multiblock gate, ...

- **Flag** - An arbitrary string id other systems can check with hasFlag(...)

- **Discipline** - A named tree a team commits to (Phoenix, Sculk, ...)

---

## Where to go from here

Disciplines/Research Nodes/Unlock Types explain the concepts; JSON Format is the actual file schema for pack authors; Java API is for other mods.
