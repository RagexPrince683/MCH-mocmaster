#!/usr/bin/env python3
"""Verify vehicle handlers update the shared radar key exactly once per tick."""

from pathlib import Path
import sys

SOURCE_ROOT = Path("src/main/java")
BASE_CLASS = "extends MCH_BaseVehicleClientTickHandler"
CONTROL_CALL = "commonPlayerControl("
KEY_ENTRY = "super.KeyRadar"


def main():
    failures = []
    handlers = []
    for path in SOURCE_ROOT.rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        if BASE_CLASS not in source or CONTROL_CALL not in source:
            continue
        handlers.append(path)
        count = source.count(KEY_ENTRY)
        if count != 1:
            failures.append("{} contains {} {} entries (expected 1)".format(path, count, KEY_ENTRY))
        if "MCH_Key[] Keys" not in source or ".update();" not in source:
            failures.append("{} does not expose and update its Keys array".format(path))

    if not handlers:
        failures.append("no vehicle handlers using commonPlayerControl() were found")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print("Verified radar key registration in {} vehicle handlers.".format(len(handlers)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
