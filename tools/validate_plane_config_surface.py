#!/usr/bin/env python3
"""Validate the unreleased new fixed-wing config surface against docs/assets."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REMOVED = {
    "ClimbEnergyLoss", "DiveEnergyGain", "TakeoffDistanceMultiplier", "NewFlightIdleNoseUpLimit",
    "EnergyRetentionMultiplier", "ClimbEnergyCostMultiplier", "PitchEnergyCostMultiplier",
    "VerticalClimbEnergyCostMultiplier", "StallRecoveryEnergyThreshold", "SustainedClimbEnergyRequirement",
    "MassKg", "PhysicalMassKg", "EngineThrustN", "MaximumLevelSpeed", "MaxLevelSpeedKmh",
    "StallSpeedKmh", "StallRecoverySpeedKmh", "CompressibilitySpeedKmh", "NeverExceedSpeed",
    "VNEKmh", "MaxSafeSpeedKmh",
}
CORE = {"PhysicalMass", "EngineThrust", "BaseDrag", "InducedDrag", "StallSpeed", "CriticalAoA"}
REDUNDANT = [
    {"PhysicalMass", "Mass"},
]
PARSER = ROOT / "src/main/java/mcheli/plane/MCP_PlaneInfo.java"
DOC = ROOT / "docs/vehicle-config/planes.md"
CONFIG_DIR = ROOT / "configreference/planes"

def keys_in_file(path):
    out = set()
    for line in path.read_text(errors="ignore").splitlines():
        m = re.match(r"\s*([A-Za-z][A-Za-z0-9_]*)\s*=", line)
        if m:
            out.add(m.group(1))
    return out

def main():
    errors = []
    parser_text = PARSER.read_text(errors="ignore")
    parsed = set(re.findall(r'equalsIgnoreCase\("([A-Za-z][A-Za-z0-9_]*)"\)', parser_text))
    doc_text = DOC.read_text(errors="ignore")
    documented = set(re.findall(r"`([A-Za-z][A-Za-z0-9_]*)`", doc_text))

    for removed in sorted(REMOVED):
        if re.search(rf'equalsIgnoreCase\("{removed}"\)', parser_text):
            errors.append(f"removed key is still parsed: {removed}")

    for p in sorted(CONFIG_DIR.glob("*.txt")):
        keys = keys_in_file(p)
        bad = sorted(keys & REMOVED)
        if bad:
            errors.append(f"{p.relative_to(ROOT)} contains removed keys: {', '.join(bad)}")
        if "useNewMobilitySystem" in keys or keys & CORE:
            missing = sorted(CORE - keys)
            if missing:
                errors.append(f"{p.relative_to(ROOT)} is missing core new-flight keys: {', '.join(missing)}")
        for group in REDUNDANT:
            both = sorted(keys & group)
            if len(both) > 1:
                errors.append(f"{p.relative_to(ROOT)} contains mutually redundant keys: {', '.join(both)}")

    cleanup_note = re.search(r"Removed unreleased new-flight-model keys.*", doc_text)
    docs_without_note = doc_text.replace(cleanup_note.group(0), "") if cleanup_note else doc_text
    for key in sorted(documented & REMOVED):
        if re.search(rf"`{key}`", docs_without_note):
            errors.append(f"removed key is documented outside the cleanup note: {key}")

    # Keep docs/code drift checks focused on the public new-flight config surface.
    table_keys = set(re.findall(r"^\| `([A-Za-z][A-Za-z0-9_]*)`", doc_text, re.MULTILINE))
    parsed_plane_keys = {k for k in parsed if k not in {"AddPartPylon", "RollDamping", "RollTorque", "YawDamping", "YawTorque"}}
    undocumented = sorted(k for k in parsed_plane_keys - table_keys if k[:1].isupper() and k not in REMOVED)
    if undocumented:
        errors.append("parsed but undocumented in the plane key table: " + ", ".join(undocumented))

    if errors:
        print("Plane config surface validation failed:")
        for e in errors:
            print(" - " + e)
        return 1
    print("Plane config surface validation passed.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
