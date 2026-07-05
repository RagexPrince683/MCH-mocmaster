# Changelog

## Use OBBs for ship walkability instead of using the AABBs

- Changed ship moving-deck detection and carry anchors for extra bounding boxes to use the rotated OBB support surface instead of the legacy unrotated AABB.
- Added reusable OBB deck helpers on `MCH_BoundingBox` for top-center, top-height, multi-point entity-on-top checks, and OBB Y-support resolution.
- Updated ship collision support to resolve walking/falling against extra-box OBB deck tops instead of the legacy extra-box AABBs.


## Unreleased

### Changed

- Converted extra vehicle bounding-box projectile hit processing to use vehicle-rotated oriented boxes while preserving existing damage-factor armor behavior.
- Added a single-player test-mode toggle for rendering debug hit boxes as either oriented OBBs or legacy AABBs.

### Documentation

- Rewrote `README.md` with a modern project overview, feature list, installation steps, compatibility/client-server requirements, configuration overview, command examples, troubleshooting, FAQ, and confirmed resource links.
- Added `docs/getting-started.md` for new-player installation, asset layout, creative tabs, basic controls, and first configuration changes.
- Added `docs/configuration.md` with source-verified configuration defaults, keybind defaults, damage/ignore-list behavior, and hidden/advanced config notes.
- Added `docs/commands.md` with source-verified `/mcheli` command syntax, permission model, examples, and safety notes.
- Added `docs/server-administration.md` with server installation, safe survival defaults, permission guidance, runtime reloads, diagnostics, and performance notes.
- Added `docs/documentation-audit.md` listing discovered undocumented systems and remaining documentation gaps.
