# Changelog

## CCIP Camera Projection Fix
- Reprojected the plane CCIP pipper through the active render camera so freelook changes where the predicted world impact appears on screen without changing the ballistic solution.
- Removed CCIP screen smoothing/state from the pipper path so the marker draws at the current predicted impact projection every frame.

## Unreleased

### Changed

- Replaced vehicle extra bounding-box narrow-phase checks with oriented bounding boxes while retaining enclosing AABBs for vanilla broad-phase compatibility. Added optional rectangular `depth` syntax for diagonal/rotated OBB footprints.

### Documentation

- Rewrote `README.md` with a modern project overview, feature list, installation steps, compatibility/client-server requirements, configuration overview, command examples, troubleshooting, FAQ, and confirmed resource links.
- Added `docs/getting-started.md` for new-player installation, asset layout, creative tabs, basic controls, and first configuration changes.
- Added `docs/configuration.md` with source-verified configuration defaults, keybind defaults, damage/ignore-list behavior, and hidden/advanced config notes.
- Added `docs/commands.md` with source-verified `/mcheli` command syntax, permission model, examples, and safety notes.
- Added `docs/server-administration.md` with server installation, safe survival defaults, permission guidance, runtime reloads, diagnostics, and performance notes.
- Added `docs/documentation-audit.md` listing discovered undocumented systems and remaining documentation gaps.

## CCIP Static Bomb Reticle Fix
- Fixed the plane CCIP pipper so it draws directly at the predicted bomb impact projection instead of smoothing toward a look-following cursor.
- Documented that CCIP is projected in the aircraft body frame and is not driven by player freelook or mouse aim.
