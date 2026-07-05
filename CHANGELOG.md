# Changelog

## Preserve OBB collision resolver for ship deck broad-phase

- Changed ship deck broad-phase boxes to expand the vehicle's custom collision box instead of creating a plain `AxisAlignedBB`, preventing composite deck discovery from bypassing the OBB-only collision resolver.

## Disable AABB player/entity vehicle collision

- Removed legacy primary vehicle AABB and extra-box AABB side-wall resolution from player/entity movement collision, leaving configured OBB deck surfaces as the only solid player/entity support path.
- Stopped vehicles from returning another entity's AABB from `getCollisionBox`, preventing the vanilla entity-collision pass from reintroducing body AABB collision.

## Stop ship AABB deck fallback jitter

- Disabled primary ship AABB deck-top support whenever extra OBB deck boxes are configured, leaving the AABB as broad-phase/body coverage while player walkability resolves against the intended OBB deck surfaces.
- Updated moving-deck contact detection to avoid selecting the primary AABB as a carried deck surface on ships that define OBB decks.

## Keep remote ship OBB decks walkable

- Expanded ship deck broad-phase searches with the full rotated corner extents of each extra OBB, so deck collision remains discoverable even when the walkable OBB is far from the ship origin.
- Added a reusable enclosing-AABB helper for calculated vehicle OBBs before ship walkability resolves the precise OBB top contact.

## Use OBBs for ship walkability instead of using the AABBs

- Changed ship moving-deck detection and carry anchors for extra bounding boxes to use the rotated OBB support surface instead of the legacy unrotated AABB.
- Added reusable OBB deck helpers on `MCH_BoundingBox` for top-center, top-height, multi-point entity-on-top checks, and OBB Y-support resolution.
- Updated ship collision support to resolve walking/falling against extra-box OBB deck tops instead of the legacy extra-box AABBs.
- Expanded the ship collision broad-phase box to include rotated extra-box deck extents so far-from-origin OBB decks are considered for player collision instead of falling back to nearby base AABB behavior.


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
