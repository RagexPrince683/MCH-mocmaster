# HUD layout metadata

HUD pack authors may add layout metadata without changing existing drawing directives:

```text
LayoutGroup = radar, Radar, movable
DrawEntityRadar = 0, 144, 21, 64, 64
EndLayoutGroup
```

The first value is a stable, untranslated persistence ID. The second is an editor-only display name. The final value is either `movable` or `fixed`. Groups cannot be nested. Every `LayoutGroup` must have a matching `EndLayoutGroup`; malformed metadata produces the same clear HUD load error as malformed drawing directives. HUD files without metadata remain valid.

Use `fixed` for crosshairs, view-aligned reticles, CCIP cues, lock/target markers, and any element whose position represents a world projection. Full-screen noise, night vision, thermal, masks, and shader effects must remain outside movable groups. Do not infer this classification from displayed text.

The HUD text file owns author metadata and original coordinates. User changes never rewrite it. Offsets in logical scaled-GUI pixels are saved as JSON below `config/mcheli/hud_layouts/hud/` for parsed HUDs and `config/mcheli/hud_layouts/builtin/` for Java-rendered categories. Schema version 1 contains `profileId`, `source`, and an `elements` object keyed by stable element ID. Each entry contains `offsetX`, `offsetY`, a source fingerprint, source HUD, and source line.

Parsed identities contain the root HUD, nested call path (including the call source line), source HUD, source line, directive, duplicate ordinal, and optional stable group ID. This keeps repeated and nested `Call` instances independent. Resolution prefers an exact ID, then an unambiguous fingerprint in the same source HUD; ambiguous stale entries are retained but not applied.

## Scaling and multi-selection

In the direct HUD editor, right-click movable elements to add or remove them from an ordered selection; right-click empty space to clear it. Left-click any selected element and drag to move the entire selection without changing the spacing between elements. Left-clicking an unselected element replaces the selection. Arrow keys move the selection by one pixel, or five while Shift is held.

Use the mouse wheel over a movable element to change scale in 0.05 increments (0.25x through 4.00x). When the pointer is over a selected element, every selected element is adjusted around its own stable geometry center. Scale and position remain session-only until **Save**; **Cancel** restores both. Layout schema version 1 files are accepted and missing scale values migrate to 1.00x when next saved.
