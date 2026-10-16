# Research Trees

A tree is one JSON file in `data/<ns>/conflux/research/`. Besides `title`, `description` (markdown) and `nodes`, a tree can set:

- **discipline** - marks it as that discipline's main tree. Researching in it commits the team to the discipline.
- **requires_discipline** - the tree is only visible to, and only researchable by, teams that chose this discipline.
- **group** - the heading the terminal's sidebar files it under.
- **switch_cost** - what leaving the discipline costs once committed.

## Trees every discipline gets

Each of the five Conflux disciplines has, on top of its main tree:

- **A Dispensations tree** - the concessions that lift that discipline's recipe restrictions, for a price.
- **A named Chronicle tree** - the deep one: *The Ashen Codex* (Phoenix), *The Listening Library* (Sculk), *The Unlit Atlas* (Void), *The First Lock* and *The Second Seal* (the sealed disciplines). They open after the discipline's commitment node.

Foundations (Material Science) and Space (Deep Space) are shared by everyone.

## Writing lore

Node `lore` and tree `description` are markdown, rendered with the Phoenix Wiki renderer: headings, bold, lists, callouts, tables and item icons all work.
