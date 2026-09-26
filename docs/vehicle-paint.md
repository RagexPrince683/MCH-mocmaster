# Vehicle paint

The Overdrive Drafting Table can add a Vehicle Camo Skin and a translucent color layer without
replacing a vehicle's normal texture.

## Painting a vehicle

1. Put an existing helicopter, plane, ship, tank, or turret item in the **Vehicle paint input**
   slot below the output slot. The table selects that vehicle's recipe and loads the design stored
   on that individual item. Leave the slot empty to paint the selected recipe; **Create** then crafts
   a new painted vehicle normally.
2. Optionally put one existing item whose definition has `textureoverlay = true` in the labeled
   **Vehicle Camo Skin** slot. Other items are rejected. The selected skin is shown immediately in
   the vehicle preview.
3. Select **Paint...**. Use **R-/R+**, **G-/G+**, and **B-/B+** to adjust the paint color and
   **A-/A+** to adjust opacity. The base vehicle texture remains visible through this layer.
4. The part buttons list groups actually present in the selected model, including conventional names
   such as `$body` and `$wheel`. Use **Parts <** and **Parts >** when there are more than eight.
   Both camo and RGB paint are drawn only on checked parts; unchecked parts keep their normal texture.
5. Select **Done**, then **Create**. With an input item this moves that vehicle to the output without
   consuming recipe ingredients. Without one it creates a new vehicle item.

The Paint screen hides and locks all inventory slots. Clicks, drags, number-key swaps, and shift-clicks
cannot move items there. Select **Done** to return to the main screen, where the vehicle input, camo
input, output, and player inventory are usable.

The server verifies the vehicle recipe/input and the `textureoverlay` item before changing either
slot. Applying a different camo consumes one Vehicle Camo Skin (except in creative mode). Editing RGB
values or reapplying the camo already stored on the vehicle does not consume another skin.

Missing model groups are ignored safely. Models with no named groups cannot receive either effect
selectively, but retain normal textures and rendering. All bundled helicopter, plane, ship, tank,
turret, animated-part, preview, and distant render paths that expose named groups support selected
camo; no named-part vehicle renderer is unsupported.

## Per-player defaults

**Use for every vehicle of this type** stores the current RGB, opacity, parts, and camo design in the
server-side persistent data of the player using the table. The server applies it to an unpainted
matching vehicle item found in that player's inventory, whether it came from crafting, commands,
loot, another player, or another mod, and checks again immediately before placement. Existing item
paint always wins.

The first save/application consumes the supplied skin. The server can subsequently apply that saved
default to future matching vehicles without another camo item.

Turning the toggle off and completing the item removes that player's default for the type. It does
not remove paint already stored on items or placed entities. Defaults are separate for each player
and survive world reloads. Item paint is copied to placed vehicles and synchronized in entity spawn
data and distant-render snapshots so observers use the authoritative server design.

### Acquisition limitation

A player default cannot be selected for a source that creates or places a vehicle without a player
identity, such as a dispenser or server automation that spawns an entity directly. If automation
puts an item into a player's inventory, the next server player tick applies the default before normal
use. Directly spawned entities retain only paint explicitly supplied by that source.
