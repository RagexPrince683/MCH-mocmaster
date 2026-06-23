# Vehicle machine-gun recoil reference pass

This config pass intentionally keeps rifle-caliber and heavy machine-gun view recoil below autocannon recoil. The in-game recoil numbers are gameplay-scale camera values, not direct Newtons, so the pass uses real weapon / cartridge energy brackets as ordering constraints.

## Source basis

* **Vehicle-specific Bradley example:** War Thunder lists the M3 Bradley as carrying the 25 mm M242 cannon plus a 7.62 mm M240C machine gun, matching the `bradm242*` and `bradm240_r*` config pairing.
* **ODIN / U.S. Army WEG:** ODIN identifies M240 variants as 7.62 mm NATO belt-fed machine guns used across ground, naval, and aviation mounts.
* **Wikipedia / manufacturer technical data:** The M242 Bushmaster is a 25 x 137 mm externally powered autocannon used on M2/M3 Bradley vehicles; typical M242 ammunition velocities are about 1,100-1,385 m/s depending on round. Northrop Grumman's M242 data sheet describes the weapon as an automatic cannon.
* **Wikipedia cartridge/weapon data:** 7.62 mm medium machine guns such as FN MAG/M240 and M1919-class guns are rifle-caliber weapons; .50 BMG / M2 Browning is the common 12.7 mm heavy machine-gun bracket; KPV/KPVT 14.5 x 114 mm reaches roughly 31 kJ muzzle energy.

## Gameplay recoil tiers applied

* 5.56-8 mm LMG/MMG/GPMG/coaxial machine guns: `RecoilPitch = 0.08F`, `RecoilPitchRange = 0.03F`, `RecoilYawRange = 0.02F`.
* 12.7-13.2 mm heavy machine guns / aircraft HMGs: `RecoilPitch = 0.13F`, `RecoilPitchRange = 0.05F`, `RecoilYawRange = 0.04F`.
* 14.5 mm KPV/KPVT heavy machine guns: `RecoilPitch = 0.18F`, `RecoilPitchRange = 0.06F`, `RecoilYawRange = 0.04F`.

These tiers make the Bradley M240C and comparable vehicle MGs visibly lighter than the Bradley M242 autocannon while still ranking 12.7 mm and 14.5 mm HMGs above rifle-caliber MGs.

## URLs checked

* War Thunder M3 Bradley wiki: https://wiki.warthunder.com/unit/us_m3_bradley
* ODIN M240 page: https://odin.t2com.army.mil/WEG/Asset/dd79f5543bfa56ed3e074a91b557afc2
* Wikipedia M242 Bushmaster: https://en.wikipedia.org/wiki/M242_Bushmaster
* Northrop Grumman M242 data sheet: https://www.northropgrumman.com/wp-content/uploads/M242-25mm-Bushmaster-Chain-Gun.pdf
* Wikipedia M2 Browning: https://en.wikipedia.org/wiki/M2_Browning
* Wikipedia KPV heavy machine gun: https://en.wikipedia.org/wiki/KPV_heavy_machine_gun
* Wikipedia M1919 Browning machine gun: https://en.wikipedia.org/wiki/M1919_Browning_machine_gun
