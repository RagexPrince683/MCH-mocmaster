# Bundled civilian car reverse speeds

Research date: 2026-09-28. Settings apply to the passenger cars in
`src/main/resources/assets/mcheli/tanks/`. Read the [runtime/config contract](tanks.md#civilian-car-reverse-controls)
before treating these values as physical measurements.

## Inventory and identity

The 23 requested definitions are present. Inspection of display/radar names,
`TechYear`, tires, seats, model references and weapon/loadout entries adds
`bnr32_police.txt` (R32 passenger-car police conversion) and
`phantomarmored.txt` (armored passenger limousine with horn/smoke), for **25 cars**.
Police paint and civilian armoring do not turn these bodies into military vehicles.
Neither additional car receives a grip opt-in from this change.

This inventory is independent of `WeightType = Car`, `Category = C` and
`CivilianCarGrip`. Reviewed exclusions include the Hilux load carrier
`toyota_unarmed` and armed Hilux variants (pickups/utility vehicles), the
Lenco BearCat tactical armored vehicle, military UAZ/Iltis/Tigr vehicles,
`opel_blitz_fuel` (truck), `mc_atv_normal` (ATV), `mc-tractor` (tractor)
and `mc_bicycle` (bicycle). Tanks, trucks, military vehicles, aircraft and
boats retain their definitions and reverse behavior. External packs are not edited.
Historical eligibility counts in [car tire grip](../car-tire-grip.md) concern a
different opt-in and are not this inventory.

Most definitions name a generation/introduction year rather than a trim and
transmission. The table explicitly selects representative documented variants;
it does not change `TechYear`, tires, `Speed`, or forward acceleration.
Unresolved fitments stay unresolved rather than being used in a calculation.

## Evidence and arithmetic

- **Published limit** means a documented maximum reverse road speed. None was
  verified in the sources inspected; no row claims one.
- **Calculated ceiling** means the gear/redline theoretical result below, rounded
  down to whole km/h for configuration. It assumes the ordinary engine ceiling is
  available in reverse. It is not a road test or proof that no reverse-specific
  electronic limiter exists. Loaded tire radius, slip, drag and stability are not
  modeled in this arithmetic.
- **Estimate** means a deliberately conservative gameplay target when a complete
  reliable input set or a published reverse limit was not verified. The cited
  identity/fitment source does **not** establish that target as a measured speed.
  Ordinary passenger cars use 20–30 km/h; the selected Japanese sports-car estimates
  use 35–40 km/h. These are tuning choices, not precision claims.

For a metric radial tire, nominal diameter is
`D_m = (rim_inches * 25.4 + 2 * width_mm * aspect_percent / 100) / 1000`.
Theoretical reverse speed is
`v_kmh = rpm * pi * D_m * 60 / (reverse_ratio * final_drive_ratio * 1000)`.
Use the driven rear tire for these rear-drive calculated examples. Do not infer
a diameter from full-profile `175R14`, historical bias-ply notation or the spare.

The project's actual conversion is
`MCH_HudShared.getRawSpeedKmh = magnitude(motion) * 72`; thus
`CivilianCarReverseSpeed = target_kmh / 72`. The new cap controls horizontal
movement, so the HUD's inclusion of vertical speed on a slope is not a second cap.
Every row includes this game-unit calculation, including estimates.

## Per-car values

In the last two columns, **Reverse** is `CivilianCarReverseSpeed` in blocks/tick
and **Factor** is `ThrottleDownFactor`. F/R means front/rear; one size means both.
A dash in the input discussion means unverified, not zero.

| Definition | Chosen variant / transmission | Tire match and evidence gaps | Target km/h | Evidence / source | Conversion | Reverse | Factor |
|---|---|---|---:|---|---|---:|---:|
| `rx7.txt` | Representative 1986 US RX-7 FC GXL, 5MT; definition says 1985 | 205/60VR15; factory manual p.12-2 supports this non-turbo fitment. Complete variant-matched reverse ratio, final drive and engine ceiling not verified. | 35 | Estimate; [Mazda manual][rx7-tires], [Mazda 1986 GXL advertisement][rx7] | 35/72 | 0.486111 | 2.60 |
| `s15.txt` | 1999 JDM Silvia spec-R, 6MT | 205/55R16; reverse 3.437, final drive 3.692 in launch table. Peak-power 6400 rpm is not a redline; engine/reverse limiter unverified. | 35 | Estimate; [Nissan launch specifications][s15] | 35/72 | 0.486111 | 2.60 |
| `350z.txt` | 2003 Canadian 350Z Performance, FS6R31A 6MT; launch-year 2002 in config | P225/45WR18 F, P245/45WR18 R; reverse 3.446, final drive 3.538, maximum engine speed 6600 rpm in the Canadian factory brochure. Manual ratios corroborated by MT-61 / RFD-34. No automatic or US base tires substituted. | 69 | Calculated ceiling; [Nissan Canada brochure (mirror)][350z-brochure], [Nissan US transmission choices][350z], [MT manual][350z-mt], [RFD manual][350z-rfd] | 69/72 | 0.958333 | 4.20 |
| `rx-8.txt` | Representative 2008 US RX-8, 6MT; definition says introduction-year 2003 | 225/45R18; reverse 3.564, final drive 4.444, manual redline 9000 rpm. Calculation below; not the 6AT. | 70 | Calculated ceiling; [Mazda 2008 specifications][rx8] | 70/72 | 0.972222 | 4.20 |
| `ae86.txt` | Representative 1983 JDM Corolla Levin GT APEX, 5MT | Period release pp.12–13 confirms 5MT. Period tire fitment remains unresolved; retrospective GAZOO says 185/70HR14 but conflicts with other fitments. Ratios and usable reverse rpm unverified. | 30 | Estimate; [Toyota launch release][ae86], [Toyota retrospective][ae86-gazoo] | 30/72 | 0.416667 | 2.40 |
| `2105.txt` | Representative 1980 VAZ/Lada 2105 1.3, 4MT | 175/70R13 is a documented replacement fitment, not verification of the 1980 original. Four-speed workshop reproduction lists reverse 3.53, but period gearbox/final drive/governor not established. | 20 | Estimate; [Michelin fitment][2105-tire], [workshop reproduction][2105] | 20/72 | 0.277778 | 1.60 |
| `2102.txt` | Representative 1971 VAZ-2102 1.2 estate, 4MT | Historic 6.45-13 / 165-13 fitment appears in the reproduced operating instructions. Exact 1971 gearbox, tire circumference and rpm ceiling remain unverified. | 20 | Estimate; [operating instructions reproduction][2102] | 20/72 | 0.277778 | 1.60 |
| `w123.txt` | 1976 Mercedes-Benz W123 240D sedan, standard 4MT | 175 SR14 88S; reverse 3.66, final drive 3.69 for pre-09/1980 manual. No inferred aspect ratio; 4200 rpm is peak power, not documented governor speed. | 25 | Estimate; [Mercedes-Benz factory archive][w123] | 25/72 | 0.347222 | 2.00 |
| `dacia.txt` | 2009 Dutch Sandero 1.4 MPI 75, 5MT | 185/65R15 in manufacturer brochure p.11. Reverse ratio/final drive/limiter not provided. Display name says 2009 Sandero but `TechYear = 1969` is inconsistent. | 25 | Estimate; [Dacia 2009 brochure (club mirror)][dacia] | 25/72 | 0.347222 | 2.00 |
| `bnr32.txt` | 1989 JDM Skyline GT-R BNR32, standard 5MT | 225/50R16 factory heritage fitment. Complete gear/final-drive/redline evidence set not verified; no V-Spec tire assumption. | 35 | Estimate; [Nissan heritage][bnr32] | 35/72 | 0.486111 | 2.40 |
| `bnr34.txt` | 1999 JDM Skyline GT-R BNR34, Getrag 6MT | 245/40ZR18 factory table. Six-speed identity documented by Nissan; complete reverse gearing/limiter set not verified. | 40 | Estimate; [Nissan tire table][bnr34], [Nissan GT-R history brochure][gtr-history] | 40/72 | 0.555556 | 2.60 |
| `altis.txt` | Representative 2014 US Corolla L 1.8, 4AT; Altis market/trim unresolved | P195/65R15 documented for US L in Toyota brochure. Toyota documents 6MT, 4AT and CVTi-S alternatives; the 4AT is explicitly selected. Reverse gearing/limiter for chosen 4AT unverified. | 25 | Estimate; [Toyota transmission release][altis], [Toyota US brochure (mirror)][altis-tires] | 25/72 | 0.347222 | 2.00 |
| `bcnr33.txt` | 1995 JDM Skyline GT-R BCNR33, standard 5MT | Existing 245/45ZR17 fitment supported by tire specialist's guide; OEM catalog identifies 5MT. No NISMO 400R/LM or prototype gearing/tires assumed. Complete reverse calculation data unverified. | 35 | Estimate; [Nissan OEM catalog reproduction][bcnr33], [Longstone fitment guide][bcnr33-tires] | 35/72 | 0.486111 | 2.40 |
| `starion.txt` | Representative 1988 US Starion ESI-R, standard 5MT | Standard 205/55VR16 F, 225/50VR16 R in Mitsubishi brochure. No Sport Handling Package tires assumed. `TechYear = 1982` predates chosen ESI-R variant; ratios/engine ceiling unverified. | 30 | Estimate; [Mitsubishi 1988 brochure (mirror)][starion] | 30/72 | 0.416667 | 2.40 |
| `impreza.txt` | Representative 1994 US Impreza L 1.8 AWD, 5MT; introduction-year 1992 in config | P175/70HR14 for AWD in Subaru brochure pp.14–15. Generic name does not establish WRX/STi. Complete reverse ratios/limiter not verified. | 30 | Estimate; [Subaru 1994 brochure (mirror)][impreza] | 30/72 | 0.416667 | 2.20 |
| `phantom.txt` | 2003 Rolls-Royce Phantom VII Series I, ZF 6AT | Manufacturer describes 790-mm-diameter PAX tires; exact metric size/load rating unverified. Reverse ratio/final drive/electronic reverse limiter not verified. | 25 | Estimate; [Rolls-Royce Series I description][phantom] | 25/72 | 0.347222 | 2.00 |
| `delorean.txt` | 1981 DMC-12, 5MT | 195/60R14 F, 235/60R15 R. Handbook p.38: reverse 3.1818, final drive 3.44. Usable maximum reverse engine rpm unverified. | 30 | Estimate; [DMC owner handbook (mirror)][delorean], [DMC fitment statement][delorean-tires] | 30/72 | 0.416667 | 2.40 |
| `challenger.txt` | 2018 Dodge Challenger R/T 5.7, TR-6060 6MT | P245/45R20; reverse 2.90, final drive 3.90, electronically limited maximum engine speed 5800 rpm. Not an SRT/Hellcat. | 70 | Calculated ceiling; [Dodge factory specifications][challenger] | 70/72 | 0.972222 | 4.00 |
| `silvia_s14.txt` | Representative 1994 JDM Silvia K's Type S, FS5W71C 5MT; 1993 launch in config | 205/55R16 factory heritage fitment. Japanese OEM catalog confirms the SR20DET K's manual variant; complete reverse gearing/limiter evidence unverified. | 35 | Estimate; [Nissan heritage][s14], [Nissan OEM catalog reproduction][s14-mt] | 35/72 | 0.486111 | 2.60 |
| `carrera_gt.txt` | Representative MY2005 Porsche Carrera GT, 6MT; definition says 2004 | 265/35ZR19 F, 335/30ZR20 R; reverse 2.86, final drive 4.44, maximum engine speed 8400 rpm in MY2005 factory sheet. | 88 | Calculated ceiling; [Porsche technical sheet (mirror)][carrera] | 88/72 | 1.222222 | 4.60 |
| `fresh_auto.txt` | VAZ-2105 “Tsar Zhiga” Fresh Auto drift build, config year 2020; drivetrain unresolved | No reliable build-specific transmission, final drive, tire size or reverse limiter found. Stock 2105 gearing is not evidence for a modified drift car. | 20 | Estimate; [bundled build identity][fresh]; conservative custom-build fallback | 20/72 | 0.277778 | 1.50 |
| `fordpolice.txt` | Representative 1993 European Mondeo 1.6, MTX-75 5MT; NYPD livery identity unresolved | Existing 185/65R14 matches Ford workshop wheel table. No verified reverse limiter or complete variant-matched input set. Not silently identified as a Crown Victoria. | 25 | Estimate; [Ford factory workshop mirror][ford], [MTX-75 workshop reproduction][ford-mt] | 25/72 | 0.347222 | 2.00 |
| `bugattichiron.txt` | 2016 launch Chiron, 7-speed dual-clutch | 285/30R20 F, 355/25R21 R. No reliable reverse gear ratio/final drive/reverse electronic limit found; forward 420 km/h limit is not reverse evidence. | 30 | Estimate; [Bugatti launch release][bugatti], [Bugatti technical sheet][bugatti-spec] | 30/72 | 0.416667 | 2.00 |
| `bnr32_police.txt` | Representative 1989 GT-R BNR32 5MT police conversion | Baseline 225/50R16; conversion-specific tires/drivetrain unverified. Utility horn/siren/targeting selectors, not an armed military conversion. Baseline reverse evidence incomplete. | 35 | Estimate; [Nissan baseline][bnr32], [bundled police identity][bnr32-police] | 35/72 | 0.486111 | 2.40 |
| `phantomarmored.txt` | 2003 Phantom VII 6AT baseline, civilian armored conversion; builder unresolved | Baseline PAX diameter 790 mm; armor-specific tire rating/gearing/limiter unknown. Reduced gameplay target for the unidentified armored build. | 20 | Estimate; [Rolls-Royce baseline][phantom], [bundled armored identity][phantom-armored] | 20/72 | 0.277778 | 1.60 |

All **21 estimate rows** use the stated fallback policy. These targets must not be
quoted as manufacturer limits, tested speeds or calculated drivetrain ceilings.
The four calculation rows have complete nominal inputs:

| Car | Nominal driven tire diameter | Calculation | Theoretical result | Config target |
|---|---|---|---:|---:|
| 350Z Performance 6MT | `(18*25.4 + 2*245*0.45)/1000 = 0.6777 m` | `6600*pi*0.6777*0.06/(3.446*3.538)` | 69.1527 km/h | 69 km/h |
| RX-8 6MT | `(18*25.4 + 2*225*0.45)/1000 = 0.6597 m` | `9000*pi*0.6597*0.06/(3.564*4.444)` | 70.6608 km/h | 70 km/h |
| Challenger R/T 6MT | `(20*25.4 + 2*245*0.45)/1000 = 0.7285 m` | `5800*pi*0.7285*0.06/(2.90*3.90)` | 70.4200 km/h | 70 km/h |
| Carrera GT 6MT | `(20*25.4 + 2*335*0.30)/1000 = 0.7090 m` | `8400*pi*0.7090*0.06/(2.86*4.44)` | 88.4052 km/h | 88 km/h |

350Z inputs are on the Canadian brochure's specification page (PDF p.15); its
September 2002 printing date and 2003 model-year warranty are on PDF p.16.
RX-8 inputs are on Mazda's final specification page (PDF p.8: manual redline,
gearing and tires). Dodge inputs are on printed pp.2, 5 and 13. Porsche inputs
are on printed pp.173–175 (PDF pp.1–3). No published reverse-specific engine
or vehicle limiter was verified for these variants. The calculations assume their
documented general redline/engine ceiling, rather than substituting peak-power rpm.

## Acceleration choices and verification

Factors tune reverse buildup independently of the speed ceiling. With default
ground `MotionFactor = 0.96`, level body pitch and no brake, reverse demand tends
toward `0.0125 * ThrottleUpDown * Factor` before the new 0.1 bound. Horizontal
reverse thrust uses `cos(10 degrees)`; the uncapped level-ground equilibrium is
approximately `24 * cos(10 degrees) * demand` blocks/tick. Each selected factor
provides enough demand to reach its target under this simplified level-road
model. Collision contact, slopes, steering, damage and non-default global speed
multipliers can lower achievable speed. These factors do not encode measured
real-car acceleration.

For cars with `DriveType`, the server now bounds reverse engine force by the selected
axles' contact and remaining traction before this existing drag/clamp sequence.
Those conditions can further reduce achievable speed; the reverse ceilings and
throttle factors in this table are unchanged. See [drivetrain behavior](../car-tire-grip.md#throttle-and-drivetrain).

Targeted headless checks exercised the real tank control branch, bounded long-held reverse,
immediate W response, unchanged forward braking/legacy reverse, heading-dependent
clamping, coasting and forward preservation, parser/reload defaults, and the exact
25-definition/table correspondence. A sustained flat-road integration model checked
the configured targets against thrust/drag and the shared clamp. This is headless
verification, not a live Forge multiplayer road test.

Verification on 2026-09-28: `compileJava test --offline --no-daemon` completed
successfully against Forge 1.7.10-10.13.4.1614. All 11 temporary JUnit checks passed with no
failures/errors/skips using a Java 8 test launcher. Gradle 9.3.1 itself needs a newer launch
JDK; this checkout's cached convention/Jabel compiler targets Java 8. The four
changed/new production classes have class-file major version 52. The resource
diff audit found exactly 25 edited definitions, only reverse fields/factors/evidence
comments changed, and every original `Speed` and other asset setting preserved.
The validation-only harness and research scratch files were removed to comply
with the workspace instructions; no test infrastructure is shipped by this change.

Compilation on this Windows checkout uses the existing cached Gradle home:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.8.9-hotspot'
.\gradlew.bat compileJava --offline --no-daemon --gradle-user-home C:/Users/Owner/.gradle
```

During the headless checks only, an existing local init script selected a Java 8
test launcher. The project compiler, build configuration and shipped compatibility
were not changed.

## Source URLs

Manufacturer publications are preferred. “Mirror” means the manufacturer document
is hosted by an archive; “reproduction” does not establish the exact period edition.
The latter cannot turn missing evidence into an exact speed.

[rx7-tires]: https://rotaryheads.com/PDF/2nd_gen/Mazda%20Service%20Manuals/1986-88%20Factory%20Service%20Manual/12_wheels_and_tires.pdf
[rx7]: https://foxed.ca/rx7manual/manuals/1986%20RX-7.pdf
[s15]: https://global.nissannews.com/ja-JP/releases/19990119-e_presskit
[350z]: https://usa.nissannews.com/en-US/releases/2003-350z-press-kit
[350z-mt]: https://boredmder.com/FSMs/Nissan/350z/2003/MT.pdf
[350z-rfd]: https://boredmder.com/FSMs/Nissan/350z/2003/RFD.pdf
[350z-brochure]: https://www.centuryu.com/uploads/1/4/4/6/144698822/2003_350z.pdf
[rx8]: https://news.mazdausa.com/download/2008-RX-8-Spec.pdf
[ae86]: https://www.toyota.co.jp/jpn/company/history/75years/vehicle_lineage/car/id60003763/news/60003763.pdf
[ae86-gazoo]: https://gazoo.com/catalog/maker/TOYOTA/COROLLA_LEVIN/198301/990003040/
[2105-tire]: https://www.michelin.cz/auto/vyrobci/vaz/2105/2105
[2105]: https://www.vazbook.ru/en/05/2105/main/system/ustroystvo-chetyrehstupenchatoy-korobki-peredach
[2102]: https://www.vazbook.ru/en/01/2101/main/manual/neobhodimye-zapasnye-chasti
[w123]: https://mercedes-benz-publicarchive.com/marsClassic/en/instance/ko/240-D--W-123-D-24-1976---1985.xhtml?oid=5094
[dacia]: https://daciaclubnederland.nl/storage/downloads/netherlands/nl-brochure-dacia-sandero-2009-08.pdf
[bnr32]: https://www.nissan-global.com/EN/HERITAGE_COLLECTION/skyline_gt-r_1989.html
[bnr34]: https://faq2.nissan.co.jp/faq/show/988?category_id=63&site_domain=default
[gtr-history]: https://www-asia.nissan-cdn.net/content/dam/Nissan/th/brochures/th/Brochure/Nissan-GT-R-Brochure-temp.pdf
[altis]: https://pressroom.toyota.com/toyota-2014-corolla-efficiency-driving-dynamics/
[altis-tires]: https://www.onlymanuals.com/toyota/corolla/toyota_corolla_brochure_2014_2014_2
[bcnr33]: https://www.nengun.com/oem/nissan/skyline-gt-r-bcnr33
[bcnr33-tires]: https://www.longstonetires.com/classic-car-tires/nissan/nissan-skyline.html
[starion]: https://xr793.com/wp-content/uploads/2024/02/1988-Mitsubishi-Starion.pdf
[impreza]: https://www.auto-brochures.com/makes/Subaru/Impreza/Subaru_US%20Impreza_1994.pdf
[phantom]: https://www.press.bmwgroup.com/south-africa/article/detail/T0129326EN/the-rolls-royce-phantom
[delorean]: https://grupomotor.net/descriptions/zss1199_0.6.pdf
[delorean-tires]: https://support.delorean.com/kb/a31/tire-choices-updated.aspx
[challenger]: https://www.media.stellantis.com/uploads/me/ME/2018/Dodge/Technical-sheet/1806_Dodge_Challenger.pdf
[s14]: https://www.nissan-global.com/EN/HERITAGE_COLLECTION/silvia_ks_s14.html
[s14-mt]: https://www.megazip.net/zapchasti-dlya-avtomobilej/nissan/silvia-2084/s14-6129/cs14-620862/manual-transmission-transaxle-fitting-7816184
[carrera]: https://www.ausmotive.com/downloads/Porsche/Carrera-GT-specs.pdf
[fresh]: https://github.com/RagexPrince683/MCH-mocmaster/blob/MCHRgithub/src/main/resources/assets/mcheli/tanks/fresh_auto.txt
[ford]: https://workshop-manuals.com/ford/mondeo_1993_01.1993-07.1996/mechanical_repairs/2_chassis/211_wheels_tyres/211-01_wheels_tyres/specificationsgeneral_specifications/
[ford-mt]: https://www.fordbook.ru/en/mondeo/1/transmission/manual/specifications
[bugatti]: https://newsroom.bugatti.com/press-releases/geneva-international-motor-show-2016
[bugatti-spec]: https://bugatti-newsroom.imgix.net/66703700d9bf8f4b7ce9211c/211122_BU_Chiron%20ENG.pdf
[bnr32-police]: https://github.com/RagexPrince683/MCH-mocmaster/blob/MCHRgithub/src/main/resources/assets/mcheli/tanks/bnr32_police.txt
[phantom-armored]: https://github.com/RagexPrince683/MCH-mocmaster/blob/MCHRgithub/src/main/resources/assets/mcheli/tanks/phantomarmored.txt
