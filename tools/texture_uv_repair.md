# Texture and UV audit

Run from the repository root (Pillow is a development-only requirement):

```sh
python -m pip install Pillow
python tools/repair_texture_uvs.py audit
python tools/repair_texture_uvs.py repair --dry-run
python tools/repair_texture_uvs.py repair --apply
python tools/repair_texture_uvs.py verify
python -m unittest discover -s tools -p 'test_repair_texture_uvs.py'
```

All commands accept `--assets`, `--output`, and `--manifest` before the subcommand. Reports are deterministic JSON and CSV files in `build/texture_uv_audit`, which is ignored. Runtime pairs come from the vehicle configuration filename (or explicit `Model`/`Texture`) plus `AddTexture`; MQO material paths are deliberately not treated as runtime bindings.

## Diagnosis

Commit `d89a647` newly tracked the resource tree while migrating it to the classpath. Its parent `b71d72b` has none of these assets, so it cannot supply pre-resize pixels. Repository history, branches, tags, ignored build outputs, and local JARs contain no validated original high-resolution source. The MQO loader passes normalized U and V directly to the tessellator; proportional resizing therefore leaves UVs unchanged. Values outside 0–1 are preserved because they may intentionally wrap.

Static renderer inspection confirms ordinary vehicle textures resolve as `textures/<vehicle-directory>/<selected texture>.png`. The renderer uses the legacy alpha test threshold `GL_GREATER, 0.001`, while skin overlays deliberately use blending. Consequently transparency can originate in alpha damaged by downsampling, sub-texel islands, or filtered transparent borders; it is not evidence for globally scaling UVs. The audit measures those conditions. No canvas crop, padding, or non-proportional resize has yet been proven, and no model UV is changed.

## Manifest repairs

Only add an entry after locating a genuine source. Record its repository-relative source and destination, dimensions, SHA-256 values, `premultiplied_lanczos` resize, `transparent_rgb_edge_bleed` alpha repair, reason, and affected `models`. Use `uv_transform: null` for proportional resizing. For a proven canvas change, specify old/new dimensions, crop, padding, and independent scales; only the listed MQOs are rewritten.

Run `audit`, inspect its transparent coverage and edge sampling counts, then run dry-run. After reviewing its exact file list, use `--apply` and `verify`. Hash checks make an asset update fail safely until the manifest is deliberately refreshed.

## Unresolved sources

No texture is represented as restored today: original pre-downscale sources and their canvas dimensions are unavailable. The manifest records this requirement. Do not upscale a 512-pixel file as a substitute, and do not claim affected visual defects repaired until the relevant originals are found and graphical close/medium/long-distance and mipmap testing is completed.
