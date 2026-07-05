## Introduce ship OBB collision abstraction

- Added a dedicated `MCH_ShipOBB` abstraction for ship-oriented bounding-box centers, half extents, axes, transforms, intersection, sweep, ray, and deck-support logic.
- Kept legacy `MCH_BoundingBox.boundingBox` AABB behavior available for non-ship vehicles while ship callers route physical checks through `MCH_ShipOBB`.
- Clarified that enclosing AABBs are broad-phase search volumes only, not physical ship collision volumes.

## Fix far ship bounding-box side collision

- Replaced ship OBB-vs-entity side intersection checks with a full separating-axis test so configured `BoundingBox` collision remains solid even when boxes are far from the ship origin.
- Increased horizontal side-offset resolution precision and snaps tiny residual movement to zero to reduce jitter when walking into ship bounding-box sides.

## Fix remote ship bounding-box side collision

- Returned the composite ship collision resolver with the expanded extra-box search bounds for entity collision boxes, so player movement discovers and resolves side contact on ship BoundingBox volumes far from the vehicle origin without making the broad-phase enclosure solid.

## Fix ship bounding-box collision jitter

- Kept ship broad-phase lookup expanded to all configured extra boxes while returning the composite collision resolver, preventing the enclosing search AABB from becoming a physical jitter source during jumping, deck walking, or side contact.

## Fix ship bounding-box side collision

- Restored ship `BoundingBox` side collision by returning the composite vehicle collision resolver for ship entity collision checks.
- Re-exposed the ship extra-box enclosure for broad-phase lookup so player movement can discover remote ship collision boxes while resolving against the precise configured boxes instead of the enclosing search volume.

## Fix ship-origin deck bounding-box jitter

- Stopped returning the composite ship deck-search AABB from `MCH_EntityShip.getBoundingBox()` so vanilla player movement no longer treats the broad-phase deck search volume as a physical collision box near the ship origin.
- Kept walkable ship support on the explicit rotated extra-box deck search/carry path, preserving remote OBB deck support without reintroducing origin AABB collisions.

## Fix ship-origin deck movement jitter

- Prevented the ship composite deck search AABB from being used as a vanilla entity collision box, so players near the ship origin no longer collide with the broad-phase search volume while walking or jumping.
- Kept ship walkability on the precise rotated extra bounding-box deck support path instead of reintroducing physical base/composite AABB collision.

## Disable ship AABBs entirely

- Forced ship debug hit-box rendering to draw configured extra bounding boxes as OBBs, independent of the single-player OBB debug toggle.
- Filtered ship entity collision damage against ship OBBs instead of applying broad-phase AABB hits from their enclosing search boxes.
- Updated ship hit detection so ship extra bounding boxes use oriented-box intersection checks rather than their legacy axis-aligned boxes.

## Disable ship AABB collision

- Disabled ship base AABB collision, ray hits, and broad-phase damage/pushback so ships no longer use the vanilla axis-aligned body box as a physical collision volume.
- Limited ship deck discovery and moving-deck support to configured extra bounding-box OBBs; ships without extra boxes now expose only an empty point search box instead of falling back to the base AABB.
- Updated ship collision documentation to clarify that ship walkable/collision surfaces must be configured as `BoundingBox` OBBs.

# Changelog

## Smooth ship AABB deck collision

- Kept ship base AABB deck collision enabled while matching the extra OBB deck support path for upward water-bob transitions.
- Added previous-top Y support for floating ship AABB decks so players remain smoothly carried instead of being pushed sideways or jittering when the ship rises.

## Keep remote ship OBB decks walkable

- Expanded ship deck broad-phase searches with the full rotated corner extents of each extra OBB, so deck collision remains discoverable even when the walkable OBB is far from the ship origin.
- Added a reusable enclosing-AABB helper for calculated vehicle OBBs before ship walkability resolves the precise OBB top contact.

## Use OBBs for ship walkability instead of using the AABBs

- Changed ship moving-deck detection and carry anchors for extra bounding boxes to use the rotated OBB support surface instead of the legacy unrotated AABB.
- Added reusable OBB deck helpers on `MCH_BoundingBox` for top-center, top-height, multi-point entity-on-top checks, and OBB Y-support resolution.
- Updated ship collision support to resolve walking/falling against extra-box OBB deck tops instead of the legacy extra-box AABBs.
- Expanded the ship collision broad-phase box to include rotated extra-box deck extents so far-from-origin OBB decks are considered for player collision instead of falling back to nearby base AABB behavior.


## Fix remote ship side bounding-box player collision

- Added a server-side player separation pass for ship extra bounding-box side contacts so players cannot phase through hull or wall boxes that are far from the ship vehicle origin.
- Added an OBB horizontal push-out helper for ship bounding boxes without changing damage-factor or projectile hit behavior.

## Align remote ship side collision to OBBs

- Changed ship side collision offset resolution to use the rotated ship OBB volume instead of each extra box's phased-out legacy AABB.
- Refined the player side separation fallback to compute player extents in ship OBB local space, reducing jitter and keeping far-from-origin side pushes aligned with the actual OBB position.

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
