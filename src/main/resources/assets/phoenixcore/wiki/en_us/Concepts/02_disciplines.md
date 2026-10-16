# Disciplines

A discipline is just a string id (the "discipline" field on a research tree JSON) - there's no separate discipline registry. A team "commits" to a discipline the first time they unlock a node flagged commitment: true in that tree; WorldResearchData then remembers which discipline id that team belongs to and whether they're committed.

- **Switching discipline** - Only possible before committing, or by paying the tree's switch_cost (a ConfluxDataType -> amount map) once committed.

---

## Built-in discipline picker cards

DisciplinePickerScreen shows a fixed set of cosmetic cards (Phoenix, Sculk, Void, Sealed) - these are UI presentation only and are separate from whatever discipline ids your actual tree JSON files declare.
