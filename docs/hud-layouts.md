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
