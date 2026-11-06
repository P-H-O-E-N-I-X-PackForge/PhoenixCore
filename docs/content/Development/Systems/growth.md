# Growth

Growth is the framework for multiblocks that grow: `GrowthMultiblockMachine` builds its own shell outward stage by stage as recipes finish, and sends out tendrils from anchor points. A machine extends it and supplies its stages (`getGrowthStages`), its shell and tendril blocks, and optionally tendril anchors. The three machines in `growth.machine` (Crystal Garden, Verdant Bloom, Bloomvine Core) are the pack's growing machines.

## Stages matter

A `GrowthStage` carries the bounds of the shell, the cost of reaching it, and three bonuses that apply to every recipe the machine runs while it is at that stage:

- `speedBonus`: share of the recipe time saved (0.1 is 10% faster).
- `outputBonus`: extra share of every output (0.1 is 10% more).
- `batch`: how many recipes' worth run at once; inputs, outputs and power are all multiplied.

Machines get this by listing `GrowthMultiblockMachine::stageModifier` in their definition's `recipeModifiers` (all three examples do). The controller screen shows the current stage's bonus, what the next stage needs, and the tendril count.

A recipe can also require a stage: add `.addData(GrowthMultiblockMachine.MIN_STAGE_KEY, n)` to it and the machine will not start it before stage n (the recipe viewer shows "Needs growth stage n"). The harvest recipes of all three machines use this (see Machines and recipes).

## Tendrils

Tendrils grow one block per finished recipe along a `TendrilShape` from an anchor, stopping at a growth ward or an unbreakable block. They are now saved with the machine (`tendrilStore`), so a reload no longer forgets them. A tendril that finishes or is stopped retires, freeing its slot (its blocks stay), and the machine counts it in `maturedTendrils`. Once `getMaxMaturedTendrils()` (24 by default) have been grown, no new ones are sent out; override it to change that.

## Paying for automatic growth

By default a machine advances a stage for free as its recipes finish; only the controller's button charges the stage's cost. Override `chargeOnAutoGrowth()` to return true and automatic growth charges it too. When the next stage is about to be reached, the machine first checks nothing is in the way (so a stage that cannot be built is never paid for), then takes the stage's `cost` from its item input buses. If they cannot pay, growth waits at the edge of the stage until they can, and the controller screen and Jade say so (`growthStall`). Crystal Garden and Verdant Bloom charge; Bloomvine Core (whose stages are free) does not.

## Look

Growing machines build with their own blocks: `GROWTH_SHELL` (the living shell, accepted by the structure wherever it accepts the casing, so the controller's own casing and the grown shell can mix) and `GROWTH_TENDRIL`, both letting off spores on the client; `GROWTH_WARD` has its own texture too. Advancing a stage bursts spores where the shell grew and plays a soft sound, each tendril step puffs a few, and the machine's sky effect shifts from pale green when young to deep teal and violet when grown. The three block textures are generated placeholders (a noisy base with a winding vein): repaint them.

## Declaring stages and machines

`GrowthStages` builds a stage list in one expression instead of a loop in every machine: `GrowthStages.of(bounds(2, 1, 1, 1, 0, 2), bounds(2, 1, 2, 2, 0, 3), ...)` gives each stage its own shell shape, or `GrowthStages.fixed(bounds, n)` for a shell that never changes and a machine that only matures; `costItems`, `cost(...)`, `speedPerStage`, `outputPerStage`, `batchFrom` and `batchAtMax` cover the rest. `GrowthMachines.cubeGrower(id, name, factory, maxBounds)` registers a growing-box machine in one call, with the stage modifier, the casing-or-shell structure and the model already set up. Adding one is a small machine class (its stages, its blocks) plus one `cubeGrower` line.

## Jade

Looking at a growing machine in Jade shows its stage out of the maximum, progress to the next, what the stage gives (faster, more output, batch), whether it is waiting on cost or a blockage, and its tendrils (`GrowthMachineProvider`).

## Control, and wild growth

A growing machine is **wild** unless a growth control hatch is part of its structure. Wild growth never stops, and it **overrides** what is in its way: a new stage destroys the breakable blocks inside its footprint (dropping them), and tendrils break through whatever they meet. Only unbreakable blocks and the area around a Growth Ward stop it (override `overridesBlocks()` to make a machine never break anything).

Fit a **Growth Control Hatch** (any hatch cell of the structure) and the machine is under the player's control, and safe:

- it **pauses and resumes** (pause only holds automatic growth; the controller's button still works),
- it stops at a **target stage** the player sets,
- its **tendrils** can be switched off, or limited to chosen anchor sides,
- and nothing is overridden: growth that would run into a block waits for it to be cleared (`growthStall`).

The buttons are on the controller screen and do nothing without the hatch. The hatch is a normal part (`GrowthControlHatch`, ability `GROWTH_CONTROL_ABILITY`); its overlay is the aether catalyst hatch's for now.

A stage always needs its **whole footprint**: if any block of it cannot be cleared, nothing is built and nothing is paid. Tendrils are the opposite: they may stop part way, and a partly grown tendril still counts.

## Tendrils boost recipes

Every tendril block still standing makes the machine faster and richer, on top of its stage: 0.4% faster per block (up to 25%) and 0.5% more output per block (up to 30%), shared by all the machine's recipes (`getTendrilSpeedPerBlock`, `getMaxTendrilSpeedBonus`, `getTendrilOutputPerBlock`, `getMaxTendrilOutputBonus`). The machine remembers the position of each tendril block it placed (saved with it) and, as it grows, spot-checks some of them: a block that has been broken stops counting. Jade, the controller screen and EMI show the bonus.

## Machines and recipes

Each machine has its own recipe type and its own seed, and every recipe needs a stage, so a machine that has grown further makes better things (all of them use one seed per craft, which is also what a stage costs, so the seeds are the machine's fuel):

- **Crystal Garden** (`growth`, crystal seeds, a shell that grows outward): amethyst, then emerald (stage 2), diamond (3) and echo shards (4). Faster each stage, a double batch at full size.
- **Verdant Bloom** (`botanical_growth`, growth seeds, a shell that grows outward): logs, flowers (stage 1), moss and azaleas (2), cherry (3) and the rare ancient plants (4). A little faster and richer each stage.
- **Bloomvine Core** (`vine_growth`, growth seeds, a small fixed core that matures): vines, slime (stage 1), honeycomb (2), glow berries and shroomlight (3), weeping and twisting vines (4). Its tendrils matter most, and it runs a double batch at full maturity.

The **Growth Seed** and **Crystal Seed** are assembler recipes (bone meal, seeds and glow berries; amethyst, quartz and bone meal). Their textures are generated placeholders. Stage costs, recipes and bonuses are all declared in Java (`GrowthStages`, `GrowthRecipes`) to be rewritten for the pack.

## EMI

EMI has a page per machine listing what each stage costs and gives, and one on growth control and tendrils (`GrowthEmi`), drawn in the suite theme. The harvest recipes are ordinary GT recipe pages, one type per machine.

## Structures

A growing machine is a hollow box of casing or living shell with its controller **on the front face** (so you can walk up to it), one block above the floor. A shape is six numbers, `bounds(up, down, left, right, front, back)`, measured from the controller; a front of 0 puts the controller on the front face, and a down of 0 puts it on the floor row. The box is only its surface: the inside is open, and hatches, buses and the maintenance hatch go in any wall cell (the control hatch too).

- **Crystal Garden**: a terraced cavern that grows wide and deep as well as tall, from 3 x 4 x 3 at stage 0 to 7 x 6 x 7 at stage 4.
- **Verdant Bloom**: a greenhouse that stays narrow and climbs, from 3 x 4 x 3 to 5 x 9 x 5. Its tendrils climb out of the roof corners.
- **Bloomvine Core**: a low, wide bed, 7 x 3 x 7, with the controller at floor level. It never grows; it matures, and its tendrils climb out of the roof corners (`roofAnchors()`).

Each stage's new shell must be completely clear (or, for a wild machine, gets cleared) before it is built; the biggest single step adds about 60 blocks.
