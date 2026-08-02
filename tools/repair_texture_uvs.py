#!/usr/bin/env python3
"""Deterministic MCHeli model/texture audit and manifest-driven repair tool."""
from __future__ import annotations

import argparse, csv, hashlib, json, math, re, sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence, Tuple

try:
    from PIL import Image, ImageChops, ImageDraw, ImageFilter
except ImportError:  # audit without a PNG is still useful
    Image = ImageChops = ImageDraw = ImageFilter = None

FACE_RE = re.compile(r"^\s*(\d+)\s+.*?(?:UV\s*\(([^)]*)\))", re.I)
UV_NUMBER_RE = re.compile(r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?")
CONFIG_DIRS = {"helicopters", "planes", "tanks", "vehicles", "ships"}
ALPHA_THRESHOLD = 1  # W_Render uses GL_GREATER, 0.001: byte alpha >= 1 passes.
_MODEL_CACHE = {}

def flattened_data(image):
    """Return flattened pixels without triggering Pillow 12's getdata warning."""
    method = getattr(image, "get_flattened_data", None)
    return method() if method is not None else image.getdata()

@dataclass(frozen=True)
class Face:
    vertex_count: int
    uvs: Tuple[Tuple[float, float], ...]
    line_number: int
    raw: str

def parse_face_line(line: str, line_number: int = 0) -> Optional[Face]:
    """Parse an MQO UV face without normalising or clamping wrapping UVs."""
    match = FACE_RE.search(line)
    if not match:
        return None
    count = int(match.group(1)); values = UV_NUMBER_RE.findall(match.group(2))
    if len(values) != count * 2:
        raise ValueError("line %d: expected %d UV values, found %d" % (line_number, count * 2, len(values)))
    return Face(count, tuple((float(values[i]), float(values[i + 1])) for i in range(0, len(values), 2)), line_number, line)

def parse_mqo(path: Path) -> Tuple[List[Face], List[str]]:
    faces, errors = [], []
    text = path.read_text(encoding="utf-8", errors="surrogateescape")
    for number, line in enumerate(text.splitlines(), 1):
        if "UV" not in line.upper():
            continue
        try:
            face = parse_face_line(line, number)
            if face: faces.append(face)
        except ValueError as error: errors.append(str(error))
    return faces, errors

def triangulate(face: Face):
    for i in range(1, face.vertex_count - 1):
        yield (face.uvs[0], face.uvs[i], face.uvs[i + 1])

def triangle_area(triangle, width=1, height=1):
    (a,b,c)=triangle
    return abs((b[0]-a[0])*(c[1]-a[1])-(c[0]-a[0])*(b[1]-a[1])) * width * height / 2.0

def transform_uv(uv, transform):
    """Map old normalised UV through crop then scale/padding into a new canvas."""
    ow, oh = transform["old_width"], transform["old_height"]
    nw, nh = transform["new_width"], transform["new_height"]
    sx, sy = transform.get("scale_x", 1.0), transform.get("scale_y", 1.0)
    cx, cy = transform.get("crop_x", 0.0), transform.get("crop_y", 0.0)
    px, py = transform.get("pad_x", 0.0), transform.get("pad_y", 0.0)
    # MQO loader passes V through unchanged; image coordinates and V both increase downwards.
    return ((uv[0]*ow-cx)*sx/nw+px/nw, (uv[1]*oh-cy)*sy/nh+py/nh)

def rewrite_mqo(data: bytes, transform) -> bytes:
    if transform is None:
        return data
    text = data.decode("utf-8", "surrogateescape")
    def replace(match):
        count = int(match.group(1)); values = UV_NUMBER_RE.findall(match.group(2))
        if len(values) != count * 2: return match.group(0)
        pairs = [transform_uv((float(values[i]),float(values[i+1])), transform) for i in range(0,len(values),2)]
        body = " ".join("%.9g %.9g" % pair for pair in pairs)
        return match.group(0).replace(match.group(2), body)
    return FACE_RE.sub(replace, text).encode("utf-8", "surrogateescape")

def bleed_transparent_rgb(image, passes=4):
    rgba=image.convert("RGBA"); alpha=rgba.getchannel("A"); channels=list(rgba.convert("RGB").split())
    # Repeated max-filter dilation supplies colour only; original alpha is restored exactly.
    opaque=alpha.point(lambda x: 255 if x else 0)
    for _ in range(passes):
        grown=opaque.filter(ImageFilter.MaxFilter(3)); ring=ImageChops.subtract(grown, opaque)
        updated=[]
        for channel in channels:
            dilated=channel.filter(ImageFilter.MaxFilter(3))
            channel.paste(dilated, mask=ring)
            updated.append(channel)
        # The next dilation starts with the channels produced by this pass.
        channels=list(Image.merge("RGB", updated).split())
        opaque=grown
    return Image.merge("RGBA", (*channels, alpha))

def premultiplied_resize(image, size):
    rgba=image.convert("RGBA"); bands=list(rgba.split()); alpha=bands[3]
    premul=[ImageChops.multiply(b, alpha) for b in bands[:3]]
    premul=[b.resize(size, Image.Resampling.LANCZOS) for b in premul]
    out_a=alpha.resize(size, Image.Resampling.LANCZOS)
    pixels=[]
    for p0,p1,p2,a in zip(*(flattened_data(b) for b in premul), flattened_data(out_a)):
        pixels.append(tuple(0 if not a else min(255,(p*255+a//2)//a) for p in (p0,p1,p2))+(a,))
    result=Image.new("RGBA",size); result.putdata(pixels)
    return bleed_transparent_rgb(result)

def sha256(data: bytes): return hashlib.sha256(data).hexdigest()

def config_pairs(root: Path):
    pairs=[]
    for directory in sorted(CONFIG_DIRS):
        for config in sorted((root/directory).glob("*.txt")):
            values={}; additions=[]
            for line in config.read_text(errors="replace").splitlines():
                if "=" not in line: continue
                key,value=(x.strip() for x in line.split("=",1))
                if key.lower()=="addtexture": additions.append(value)
                else: values[key.lower()]=value
            name=values.get("model",config.stem); textures=[values.get("texture",config.stem)]+additions
            model=root/"models"/directory/name
            model=next((model.with_suffix(x) for x in (".mqo",".obj") if model.with_suffix(x).exists()), model.with_suffix(".mqo"))
            for texture in textures: pairs.append((model,root/"textures"/directory/(texture+".png"),config))
    return pairs

def audit_entry(root, model, texture, config, previews=False):
    key=str(model)
    if key not in _MODEL_CACHE:
        _MODEL_CACHE.clear()  # pairs are model-sorted; bound memory for very large MQOs
        _MODEL_CACHE[key]=(parse_mqo(model) if model.suffix.lower()==".mqo" and model.exists() else ([],[]))
    faces,errors=_MODEL_CACHE[key]
    uvs=[uv for face in faces for uv in face.uvs]; triangles=[t for f in faces for t in triangulate(f)]
    record={"model_path":str(model.relative_to(root)) if model.exists() else str(model),"texture_path":str(texture.relative_to(root)),"configuration_path":str(config.relative_to(root)),
      "model_exists":model.exists(),"texture_exists":texture.exists(),
      "texture_width":None,"texture_height":None,"texture_color_mode":None,"minimum_alpha":None,"alpha_coverage":None,
      "uv_min_u":min((u for u,v in uvs),default=None),"uv_max_u":max((u for u,v in uvs),default=None),"uv_min_v":min((v for u,v in uvs),default=None),"uv_max_v":max((v for u,v in uvs),default=None),
      "uv_triangle_count":len(triangles),"small_uv_triangle_count":None,"out_of_range_uv_count":sum(not(0<=u<=1 and 0<=v<=1) for u,v in uvs),"malformed_uv_count":len(errors),
      "missing_uv_faces":0,"transparent_covered_pixels":None,"sampling_edge_pixels":None,"suspected_cause":"missing asset","recommended_repair":"locate original source","source_texture_availability":False}
    if not texture.exists() or Image is None: return record
    with Image.open(texture) as source:
        image=source.convert("RGBA"); w,h=image.size; alpha=image.getchannel("A")
        alpha_values=list(flattened_data(alpha))
        record.update(texture_width=w,texture_height=h,texture_color_mode=source.mode,minimum_alpha=min(alpha_values),alpha_coverage=sum(a>=ALPHA_THRESHOLD for a in alpha_values)/(w*h),small_uv_triangle_count=sum(triangle_area(t,w,h)<1 for t in triangles))
        if triangles and all(0<=u<=1 and 0<=v<=1 for u,v in uvs):
            mask=Image.new("1",(w,h)); draw=ImageDraw.Draw(mask)
            for tri in triangles: draw.polygon([(u*(w-1),v*(h-1)) for u,v in tri],fill=1)
            covered=list(flattened_data(mask)); av=alpha_values; record["transparent_covered_pixels"]=sum(m and a<ALPHA_THRESHOLD for m,a in zip(covered,av))
            expanded=mask.filter(ImageFilter.MaxFilter(3)); edge=ImageChops.subtract(expanded,mask)
            record["sampling_edge_pixels"]=sum(m and a<ALPHA_THRESHOLD for m,a in zip(flattened_data(edge),av))
        if record["minimum_alpha"]==255: record.update(suspected_cause="none detected",recommended_repair="none")
        elif record["transparent_covered_pixels"]: record.update(suspected_cause="UV coverage intersects transparent texels",recommended_repair="restore source and premultiplied resize; validate intentional transparency")
        else: record.update(suspected_cause="intentional or unproven transparency",recommended_repair="manual review")
    return record

def write_reports(records, output):
    output.mkdir(parents=True,exist_ok=True)
    (output/"audit.json").write_text(json.dumps({"schema":1,"records":records},indent=2,sort_keys=True)+"\n")
    fields=list(records[0]) if records else []
    with (output/"audit.csv").open("w",newline="") as stream:
        writer=csv.DictWriter(stream,fields); writer.writeheader(); writer.writerows(records)

def write_suspicious_report(records, output):
    """Rank likely opaque-surface problems while retaining measurements for review."""
    intentional=("glass","window","sight","rotor","propeller","exhaust","lamp","decal","hud")
    priority=("body","turret","wheel","track","skin","hull","weapon")
    ranked=[]
    for record in records:
        names=(record["model_path"]+" "+record["texture_path"]).lower()
        missing=not record["model_exists"] or not record["texture_exists"]
        transparent=record["transparent_covered_pixels"] or 0
        edge=record["sampling_edge_pixels"] or 0
        small=record["small_uv_triangle_count"] or 0
        out=record["out_of_range_uv_count"] or 0
        if not (missing or transparent or edge or small or out): continue
        classification="intentional transparency review" if any(word in names for word in intentional) else "likely opaque vehicle surface"
        score=(1000000 if missing else 0)+(transparent*10)+edge+(small*4)+(out*2)
        if any(word in names for word in priority): score+=10000
        if classification.startswith("intentional"): score-=10000
        reason=("missing model or texture; " if missing else "")+("UV-covered transparent pixels; " if transparent else "")+("transparent sampling edge; " if edge else "")+("sub-texel UV triangles; " if small else "")+("out-of-range UVs (may wrap); " if out else "")
        ranked.append({"rank":0,"score":score,"classification":classification,"reason":reason.rstrip("; "),**record})
    ranked.sort(key=lambda row:(-row["score"],row["model_path"],row["texture_path"]))
    for number,row in enumerate(ranked,1): row["rank"]=number
    fields=list(ranked[0]) if ranked else ["rank","score","classification","reason"]
    with (output/"suspicious_assets.csv").open("w",newline="") as stream:
        writer=csv.DictWriter(stream,fields); writer.writeheader(); writer.writerows(ranked)

def load_manifest(path): return json.loads(path.read_text())

def repair(args, apply=False):
    root=Path(args.assets); changes=[]; failures=[]; manifest=load_manifest(Path(args.manifest)); entries=manifest.get("repairs",[])
    if not entries:
        print("No repair entries are defined in %s.\nNo files were changed." % args.manifest)
        return 1 if apply else 0
    if apply and manifest.get("unresolved"):
        print("APPLY refused: %d unresolved manifest entries remain." % len(manifest["unresolved"]))
        return 1
    for item in entries:
        if Image is None: raise SystemExit("Pillow is required for repair")
        source=Path(item["source_texture_path"]); source=source if source.is_absolute() else root/source
        target=root/item["current_texture_path"]
        if not source.exists(): failures.append("UNRESOLVED %s: source %s missing"%(target,source)); continue
        if sha256(source.read_bytes())!=item["source_sha256"]: failures.append("source hash mismatch: "+str(source)); continue
        with Image.open(source) as image: result=premultiplied_resize(image,(item["output_width"],item["output_height"]))
        import io; buf=io.BytesIO(); result.save(buf,"PNG",optimize=False,compress_level=9); data=buf.getvalue()
        if sha256(data)!=item["output_sha256"]: failures.append("output hash mismatch: "+str(target)); continue
        changes.append(str(target));
        if apply: target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(data)
        transform=item.get("uv_transform")
        for model_name in item.get("models",[]):
            model=root/model_name; changed=rewrite_mqo(model.read_bytes(),transform) if transform else model.read_bytes()
            if changed!=model.read_bytes(): changes.append(str(model)); apply and model.write_bytes(changed)
    print(("APPLY" if apply else "DRY-RUN")+"\n"+"\n".join(changes+failures))
    if not changes: print("No files were changed.")
    return 1 if apply and failures else 0

def verify_manifest(args):
    root=Path(args.assets); manifest=load_manifest(Path(args.manifest)); failures=[]
    for entry in manifest.get("unresolved",[]):
        failures.append("UNRESOLVED: "+entry.get("required_source",entry.get("reason","manifest entry")))
    for item in manifest.get("repairs",[]):
        source=Path(item["source_texture_path"]); source=source if source.is_absolute() else root/source
        target=root/item["current_texture_path"]
        if not source.exists(): failures.append("MISSING SOURCE: "+str(source))
        elif sha256(source.read_bytes()) != item["source_sha256"]: failures.append("SOURCE HASH MISMATCH: "+str(source))
        if not target.exists(): failures.append("MISSING OUTPUT: "+str(target))
        elif sha256(target.read_bytes()) != item["output_sha256"]: failures.append("OUTPUT HASH MISMATCH: "+str(target))
    if failures:
        print("Verification failed:\n"+"\n".join(failures)); return 1
    print("Verification succeeded: %d repair entries are complete." % len(manifest.get("repairs",[]))); return 0

def main(argv=None):
    parser=argparse.ArgumentParser(); parser.add_argument("--assets",default="src/main/resources/assets/mcheli"); parser.add_argument("--output",default="build/texture_uv_audit"); parser.add_argument("--manifest",default="tools/texture_downscale_manifest.json")
    subs=parser.add_subparsers(dest="command",required=True); subs.add_parser("audit"); rep=subs.add_parser("repair"); mode=rep.add_mutually_exclusive_group(required=True); mode.add_argument("--dry-run",action="store_true"); mode.add_argument("--apply",action="store_true"); subs.add_parser("verify")
    args=parser.parse_args(argv); root=Path(args.assets)
    if args.command=="repair": return repair(args,args.apply)
    if args.command=="verify": return verify_manifest(args)
    records=[audit_entry(root,*pair) for pair in sorted(config_pairs(root),key=lambda p:(str(p[0]),str(p[1]),str(p[2])))]; write_reports(records,Path(args.output)); write_suspicious_report(records,Path(args.output))
    manifest=load_manifest(Path(args.manifest))
    unresolved=list(manifest.get("unresolved",[]))+[x for x in manifest.get("repairs",[]) if not (root/x["source_texture_path"]).exists()]
    print("audited %d runtime model/texture selections; %d manifest sources unresolved"%(len(records),len(unresolved)))
    return 0

if __name__=="__main__": sys.exit(main())
