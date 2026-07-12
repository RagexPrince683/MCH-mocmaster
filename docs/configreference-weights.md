# Config reference weight audit

The `configreference/` `Weight =` values are stored as non-negative integer pounds.

## Weight definitions

- Civilian cars and trucks use curb weight.
- Military ground vehicles and tanks use combat weight.
- Aircraft and helicopters use empty weight unless the configuration clearly represents a loaded or operating UAV weight.
- Small consumer/FPV drones use operating or takeoff weight.
- Ships and submarines use represented displacement.
- Static weapons, launchers, and turrets use complete system weight.
- Trailers and equipment use empty system weight unless a loaded configuration is explicit.

## Audit notes

The audit identifies vehicles from `DisplayName`, `AddDisplayName`, role, and in-file configuration content rather than file names alone. Ambiguous labels such as Challenger, Apache, Mustang, Phantom, Tiger, Panther, and type-numbered vehicles were reviewed against their displayed platform identity before selecting a weight.

Major fixes included correcting placeholder combat-vehicle values such as `100000`, aircraft values that were metric-tonne or kilogram-like numbers, helicopter entries that were maximum/placeholder weights instead of empty weights, and ambiguous civilian vehicles in the tank config folder such as Dodge Challenger R/T and Rolls-Royce Phantom.

No known vehicle identity remains unresolved after this pass; closest represented variants were used where the config display name is broader than one production subvariant.
