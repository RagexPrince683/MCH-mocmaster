# Ship OBB Side Collision Resolution

- Fixed ship oriented bounding box horizontal offset calculations so vanilla X/Z movement resolution clamps entities at the maximum safe non-penetrating movement, including when the entity starts exactly touching an OBB side.
- Kept ship horizontal collision delegation routed through `MCH_BoundingBox.calculateXOffset()` and `calculateZOffset()` so vanilla movement receives the corrected OBB-safe offsets.
- Reduced ship player side collision push-out to an opt-in safety/debug fallback via the `mcheli.ship.sideCollisionFallback` system property; ordinary side collision should now be handled during vanilla offset resolution.
