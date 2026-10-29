# Cinema screens

A cinema screen is a block (place several next to each other, facing the same way, to make one big screen; use odd
sizes such as 3x3 or 5x3). It shows one **page** at a time over a shader background, on a solid plate so the text is
always readable.

## Using one
- Click a **button** (or the `<` `>` arrows at the bottom) to use it. A button lights up when your crosshair is on it.
- **Right-click** anywhere else on a screen with its own pages to go to the next page.
- **Shift + right-click** edits the current page; **Ctrl + right-click** adds a page; **Alt + right-click** cycles the
  text colour; **Shift + scroll** changes the text size (add **Alt** to change the background); **Shift + left-click**
  removes the page. The edit box is one line: type `\n` for a line break.

## Writing a page
A page is plain text. `\n` (or a real line break) starts a new line.

```
THE CHOICE\nYour first choice is free.\n@button Choose your path | open_discipline_picker
```

| line | meaning |
| --- | --- |
| first line (when the page has more than one) | the title; `# Title` also makes a title |
| other lines | the body: it wraps to the screen |
| `@button Label \| action` | a button |
| `@if condition` | show this page only when the condition holds (put it last) |

Text can use the usual `§` colour codes, and these tokens: `{player}`, `{discipline}`, and `{stat:playtime}` (also
`deaths`, `mobkills`, `jumps`, `walked`, `flown`, `trades`, ...).

### Button actions
`next`, `prev`, `first`, `last`, `goto:N` (a page number), `open_discipline_picker`, `command:<command>` (run as the
player, with their own permissions), `sound:<sound id>`, `cutscene:<cutscene id>` (for example
`cutscene:phoenixcore:dream`).

### Conditions
Separate several with spaces (all must hold); `!` reverses one: `no_discipline`, `has_discipline`, `committed`,
`discipline:<id>`, `flag:<research flag>`. For example `@if has_discipline !committed`.

## The screens in the starting place
They are described by `data/phoenixcore/ethereal_spawn/screens.json` (a data pack can replace it, or edit it in the
resources): each screen has `pos` (its middle block), `facing` (which way the front points: north, south, east or west),
`cols` and `rows`, `color`, `background` (`void_galaxy`, `nebula`, `sculk_abyss`, `sealed_industrial`, `sealed_chaos`,
`sunflare`) and `pages`. After changing it, run **`/conflux dimension spawndecor`** (it clears the old screens and builds
the new ones); `/conflux dimension spawndecor clear` only removes them. A screen you place by hand and edit in the game
works the same way.

## Pictures
| line | meaning |
| --- | --- |
| `@image namespace:textures/path.png \| caption` | a texture from a mod or resource pack (a 16x16 icon is fine) |
| `@item minecraft:blaze_rod \| caption` | an item's icon |
| `@continuum star ffcc66 \| caption` | a live Continuum body (`star`, `black_hole`, `gas_cloud`, `protostar`; the colour is optional). It is drawn by Continuum's shaders and **turns to follow the player looking at it**: walk round the screen and you see the other side. Redrawn about 15 times a second, a few at a time, and only within 24 blocks. |

Add a second `|` and an action to make a picture clickable: `@image phoenixcore:textures/gui/suite_conflux.png | Research | hud:conflux`. It lights up when pointed at. `hud:solaris`, `hud:chronicles`, `hud:conflux` and `hud:continuum` open what the matching HUD bar button opens (a mod that is not installed says so); any other action works too.

Several of these lines on one page make a row under the title, each with its caption under it. Textures that do not
exist show as the purple-and-black missing texture, so check the id.

## Scrolling text (the end poem)
Put `@scroll` (or `@scroll 0.4`, the speed in blocks a second) on a page and its body rises slowly through the free part of
the screen, fading in at the bottom and out at the top, then starts again. The title, pictures and buttons stay where they
are. Blank lines in the body are kept, so verses can be separated.

Because pages can have `@if` conditions and the tokens `{player}`, `{discipline}` and `{stat:...}`, a poem can change with
the player: give each discipline (or each stage of progress) its own page with `@if discipline:phoenix`, `@if flag:...` and so on.
The poem screen in the starting place (east wall, north end) is an example: one page before a path is chosen, another after.
