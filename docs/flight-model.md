# Fixed-wing realistic flight model notes

The 1.7.10 fixed-wing `EnableRealisticFlightModel` path treats closed throttle as an aerodynamic energy state, not as a free thrust-vectoring mode. A plane that is airborne, not in VTOL/nozzle mode, at zero propulsive throttle, nose-high, and below stall-recovery speed is now considered an **idle unsupported climb**.

## Idle unsupported climb protection

Minecraft/MCHeli pitch is inverted: negative pitch is nose-up, positive pitch is nose-down. When an idle unsupported climb is detected:

- nose-up control input is fully suppressed as energy runs out;
- accumulated nose-up pitch rate is cancelled when the limit is reached; and
- aircraft pitch is clamped to `-NewFlightIdleNoseUpLimit` until airspeed recovers.

This prevents the exploit where a pilot chops throttle to 0%, pulls to a near-vertical attitude, then throttles back up for an unrealistic space-shuttle climb. The stall and throttle-deficit pitch-down forces still handle normal recovery; this clamp only stops the zero-throttle, below-recovery-speed vertical setup.

## Config key

`NewFlightIdleNoseUpLimit` controls the maximum nose-up pitch, in degrees, while airborne at zero propulsive throttle and below stall-recovery speed.

- Default: `38.0`
- Valid range: `5.0` to `89.0`
- Applies only to fixed-wing realistic flight (`EnableRealisticFlightModel = true`)
- Ignored while on/near the ground or in VTOL/nozzle mode
- Blends out as airspeed returns to `StallRecoverySpeed` (or `StallSpeed * 1.2` when no explicit recovery speed is configured)

Recommended starting points:

| Aircraft class | Suggested value | Notes |
| --- | ---: | --- |
| WW2 props / trainers | `34`-`42` | Lower values make idle stalls break earlier. |
| Modern fighters | `38`-`48` | High thrust only helps after throttle is restored and speed recovers. |
| Heavy bombers / transports | `28`-`38` | Heavier aircraft should not hold steep idle pitch. |
| VTOL aircraft | `38` default | Protection is bypassed in nozzle/VTOL mode. |

Keep `StallSpeed`, `StallRecoverySpeed`, `CriticalAoA`, `StallPitchRecoveryStrength`, and `StallBreakStrength` tuned from real-world class data where possible, then use `NewFlightIdleNoseUpLimit` as the final guard against idle vertical pitch exploits.
