# Vehicle paint

The Overdrive Drafting Table can add a translucent color layer without replacing a vehicle's
normal texture or skin overlay.

## Painting a vehicle

1. Put an existing helicopter, plane, ship, tank, or turret item in the **Vehicle paint input**
   slot below the output slot. The table selects that vehicle's recipe and loads the paint stored
   on that individual item. Leave the slot empty to paint the currently selected vehicle recipe;
   **Create** then crafts a new painted item normally.
2. Select **Paint...**. Use **R-/R+**, **G-/G+**, and **B-/B+** to adjust the paint color, and
   **A-/A+** to adjust opacity. The base vehicle texture remains visible through this layer.
3. The part buttons list only groups actually present in the selected model, including conventional
   names such as `$body` and `$wheel`. Use **Parts &lt;** and **Parts &gt;** when a model has more than
   eight groups. A checked part receives paint; an unchecked part retains its original appearance.
4. Select **Done**, then **Create**. With an input item this moves the same vehicle design to the
   output without consuming recipe ingredients. Without one it creates a new vehicle item.

Missing model groups are ignored safely. Models with no named groups cannot receive a selective
paint layer, but their normal texture and rendering remain unchanged.

## Per-player defaults

**Use for every vehicle of this type** stores the current design in the server-side persistent data
of the player using the table. The server applies it to an unpainted matching vehicle item found in
that player's inventory, whether it came from crafting, commands, loot, another player, or another
mod, and checks again immediately before player placement. Existing item paint always wins.

Turning the toggle off and completing the item removes that player's default for the type. It does
not remove paint already stored on items or placed entities. Defaults are separate for each player
and survive world reloads. Item paint is copied to placed vehicles and synchronized in entity spawn
data and distant-render snapshots so observers use the authoritative server design.

### Acquisition limitation

A player default cannot be selected for a source that creates or places a vehicle without a player
identity, such as a dispenser or server automation that spawns an entity directly. If automation
puts an item into a player's inventory, the next server player tick applies the default before normal
use. Directly spawned entities retain only paint explicitly supplied by that source.
