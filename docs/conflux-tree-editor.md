# Conflux tree editor

`/conflux editor` (client command) opens the research tree editor. The canvas is the tree; the panel on the right holds the
tree's name, title and text, and the selected node's fields.

## Working on the canvas

- **Right click** on empty canvas for a menu: new node here, paste, check the tree, tree settings. Right click **on a node**
  for: select, duplicate, copy, link from here, unlink from the selected node, remove all links, make it a starting node,
  delete. (Right drag still pans; so do middle drag and Alt+drag.)
- **P** place, **L** link, **S** select still work. They are ignored while you are typing in a text box.
- **Ctrl+Z / Ctrl+Y** (or Ctrl+Shift+Z): real undo and redo, up to 100 steps, for anything that changes the tree: placing,
  moving, linking, deleting, duplicating, unlocks, loading, tree settings. Text you typed into a node's fields is kept as a
  step when you select something else.
- **Ctrl+D** duplicate the selected node, **Ctrl+C / Ctrl+V** copy and paste it, **Ctrl+S** save, **Delete** remove.
- A node you name takes its id from the title ("Ash Smelting" becomes `ash_smelting`) until you load a tree that already has
  ids. Ids are always kept unique.

## Check

**CHECK** in the toolbar lists the problems in the tree and turns red when there are errors. Click a problem to select that
node and centre the view on it.

- Errors: two nodes with the same id, a prerequisite that is not in the tree, a node that needs itself, a loop of
  prerequisites, no starting node at all.
- Warnings: no title, nothing costs or unlocks, an unknown unlock type (the known ones are `item`, `archive`, `multiblock`,
  `recipe_tag`, `flag`, `world_stage`), two nodes on the same spot, an exclusion group with one member, an empty tree.

## Tree settings

**TREE** opens a small dialog for what the research terminal needs to place the tree: the id **namespace** of the nodes, the
**category** path (`Discipline/Tier 1`, see `conflux-research-categories.md`), the **order** inside the category, and the
**discipline** and **requires discipline** of the tree.

## Saving and loading

- **SAVE FILE** writes `conflux_trees/<name>.json` in the game folder. When the game folder sits inside the project (a dev run), it
  also writes the tree into `src/main/resources/data/<namespace>/conflux/research/` so it is part of the mod; rebuild or
  `/reload` to see it in the terminal.
- **LOAD TREE** lists every tree the game has, and every file in `conflux_trees/`, and loads the one you click. (It used to load
  only the first tree, whichever you meant.)
- **COPY JSON** copies the tree to the clipboard.
