# Conflux research categories

The research terminal's sidebar groups trees into categories, and categories nest.

## Putting a tree in a category

A tree's `group` is a path. Each `/` goes one category deeper, to any depth:

```json
{ "title": "Ashen Foundations", "discipline": "phoenix", "group": "Discipline/Tier 1", "order": 1, "nodes": [ ... ] }
```

puts the tree in the category **Tier 1** inside **Discipline**. A group with no slash, such as `"Chronicle"`, is a top
level category like before, so existing trees keep working. A tree with a discipline and no group is in `Discipline`.

- `order` (a number, default 0): where the tree sits among the others of its category, lowest first. It also orders
  subcategories (a category sorts by the lowest `order` of the trees inside it), so give each tier's tree the tier's
  number and the tiers come out in order.
- Categories need no file of their own: they exist because a tree names them. The name shown is the text between the
  slashes.

### A tree per discipline per tier

```
data/phoenixcore/conflux/research/phoenix_tier1.json   "discipline": "phoenix", "group": "Discipline/Tier 1", "order": 1
data/phoenixcore/conflux/research/phoenix_tier2.json   "discipline": "phoenix", "group": "Discipline/Tier 2", "order": 2
data/phoenixcore/conflux/research/sculk_tier1.json     "discipline": "sculk",   "group": "Discipline/Tier 1", "order": 1
```

A player sees the trees of their own discipline (and the ones with no discipline), so each sees **Discipline → Tier 1,
Tier 2, ...** with only their own trees. Deeper nesting works the same way: `"Discipline/Tier 2/Side Branches"`.

## In the terminal

- A category header shows an arrow, its name, and how many nodes are done out of all the nodes inside it, at any depth.
- Click a header to fold it shut or open it. A folded category hides everything inside it. The terminal remembers what
  you folded until the game closes.
- Nested categories and their trees are indented under their parent.
- The editor keeps a loaded tree's `group`, `order`, `discipline` and `requires_discipline` when it saves it again.
