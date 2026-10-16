# Research Nodes

One unlockable point on the tree canvas. Fields (all in the node's JSON object under a tree's "nodes" map, keyed by node id):

- **title / lore / hint** - Display text shown in the node viewer (markdown is supported)

- **icon** - Item id rendered on the node

- **position** - [x, y] canvas coordinates

- **cost** - Map of ConfluxDataType -> amount spent to unlock

- **prerequisites** - Node ids that must ALL be unlocked first

- **prerequisites_any** - Node ids where unlocking ANY ONE is enough

- **exclusion_group** - Unlocking this node locks out every other node sharing it

- **commitment** - true = unlocking this commits the team to the tree's discipline

- **hidden** - true = invisible until its prerequisites are met

- **unlocks** - List of {type, value} - see the Unlock Types page
