# Fix missile target-domain classification

## Changed

- Added shared Air, Ground, Surface, Underwater, and Unknown target-domain classification for missile guidance.
- Made MCHeli planes and helicopters stable air-role targets; tanks, turrets, and ground stations stable ground-role targets; and ships stable surface/underwater-role targets.
- Reused the shared classification for initial locks, continued locks, active missile scans, and AA missile terminal validation.
- Kept flare/chaff filtering, manual/TV/laser guidance, custom lock checkers, water permissions, and missile-lock permissions intact.
- Added a repository-wide AT/AS/TV missile configuration audit and documented configurations needing manual review.
