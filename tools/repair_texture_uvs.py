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

def bleed_transparent_rgb(image):
    rgba=image.convert("RGBA"); alpha=rgba.getchannel("A"); rgb=rgba.convert("RGB")
    # Repeated max-filter dilation supplies colour only; original alpha is restored exactly.
    opaque=alpha.point(lambda x: 255 if x else 0)
    for _ in range(4):
        grown=opaque.filter(ImageFilter.MaxFilter(3)); ring=ImageChops.subtract(grown, opaque)
        for channel in range(3):
            c=rgb.getchannel(channel); dilated=c.filter(ImageFilter.MaxFilter(3)); c.paste(dilated, mask=ring); rgb.putchannel(channel,c)
        opaque=grown
    return Image.merge("RGBA", (*rgb.split(), alpha))

def premultiplied_resize(image, size):
    rgba=image.convert("RGBA"); bands=list(rgba.split()); alpha=bands[3]
    premul=[ImageChops.multiply(b, alpha) for b in bands[:3]]
    premul=[b.resize(size, Image.Resampling.LANCZOS) for b in premul]
    out_a=alpha.resize(size, Image.Resampling.LANCZOS)
    out=[]
    for b in premul:
        # premul is divided by alpha in byte space.
        out.append(Image.eval(Image.merge("LA",(b,out_a)), lambda x:x) if False else b)
    pixels=[]
    for p0,p1,p2,a in zip(*(list(b.getdata()) for b in premul), list(out_a.getdata())):
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
      "texture_width":None,"texture_height":None,"texture_color_mode":None,"minimum_alpha":None,"alpha_coverage":None,
      "uv_min_u":min((u for u,v in uvs),default=None),"uv_max_u":max((u for u,v in uvs),default=None),"uv_min_v":min((v for u,v in uvs),default=None),"uv_max_v":max((v for u,v in uvs),default=None),
      "uv_triangle_count":len(triangles),"small_uv_triangle_count":None,"out_of_range_uv_count":sum(not(0<=u<=1 and 0<=v<=1) for u,v in uvs),"malformed_uv_count":len(errors),
      "missing_uv_faces":0,"transparent_covered_pixels":None,"sampling_edge_pixels":None,"suspected_cause":"missing asset","recommended_repair":"locate original source","source_texture_availability":False}
    if not texture.exists() or Image is None: return record
    with Image.open(texture) as source:
        image=source.convert("RGBA"); w,h=image.size; alpha=image.getchannel("A")
        record.update(texture_width=w,texture_height=h,texture_color_mode=source.mode,minimum_alpha=min(alpha.getdata()),alpha_coverage=sum(a>=ALPHA_THRESHOLD for a in alpha.getdata())/(w*h),small_uv_triangle_count=sum(triangle_area(t,w,h)<1 for t in triangles))
        if triangles and all(0<=u<=1 and 0<=v<=1 for u,v in uvs):
            mask=Image.new("1",(w,h)); draw=ImageDraw.Draw(mask)
            for tri in triangles: draw.polygon([(u*(w-1),v*(h-1)) for u,v in tri],fill=1)
            covered=list(mask.getdata()); av=list(alpha.getdata()); record["transparent_covered_pixels"]=sum(m and a<ALPHA_THRESHOLD for m,a in zip(covered,av))
            expanded=mask.filter(ImageFilter.MaxFilter(3)); edge=ImageChops.subtract(expanded,mask)
            record["sampling_edge_pixels"]=sum(m and a<ALPHA_THRESHOLD for m,a in zip(edge.getdata(),av))
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

def load_manifest(path): return json.loads(path.read_text())

def repair(args, apply=False):
    root=Path(args.assets); changes=[]
    for item in load_manifest(Path(args.manifest)).get("repairs",[]):
        if Image is None: raise SystemExit("Pillow is required for repair")
        source=Path(item["source_texture_path"]); source=source if source.is_absolute() else root/source
        target=root/item["current_texture_path"]
        if not source.exists(): print("UNRESOLVED %s: source %s missing"%(target,source)); continue
        if sha256(source.read_bytes())!=item["source_sha256"]: raise SystemExit("source hash mismatch: "+str(source))
        with Image.open(source) as image: result=premultiplied_resize(image,(item["output_width"],item["output_height"]))
        import io; buf=io.BytesIO(); result.save(buf,"PNG",optimize=False,compress_level=9); data=buf.getvalue()
        if sha256(data)!=item["output_sha256"]: raise SystemExit("output hash mismatch: "+str(target))
        changes.append(str(target));
        if apply: target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(data)
        transform=item.get("uv_transform")
        for model_name in item.get("models",[]):
            model=root/model_name; changed=rewrite_mqo(model.read_bytes(),transform) if transform else model.read_bytes()
            if changed!=model.read_bytes(): changes.append(str(model)); apply and model.write_bytes(changed)
    print(("APPLY" if apply else "DRY-RUN")+"\n"+"\n".join(changes)); return 0

def main(argv=None):
    parser=argparse.ArgumentParser(); parser.add_argument("--assets",default="src/main/resources/assets/mcheli"); parser.add_argument("--output",default="build/texture_uv_audit"); parser.add_argument("--manifest",default="tools/texture_downscale_manifest.json")
    subs=parser.add_subparsers(dest="command",required=True); subs.add_parser("audit"); rep=subs.add_parser("repair"); mode=rep.add_mutually_exclusive_group(required=True); mode.add_argument("--dry-run",action="store_true"); mode.add_argument("--apply",action="store_true"); subs.add_parser("verify")
    args=parser.parse_args(argv); root=Path(args.assets)
    if args.command=="repair": return repair(args,args.apply)
    records=[audit_entry(root,*pair) for pair in sorted(config_pairs(root),key=lambda p:(str(p[0]),str(p[1]),str(p[2])))]; write_reports(records,Path(args.output))
    manifest=load_manifest(Path(args.manifest))
    unresolved=list(manifest.get("unresolved",[]))+[x for x in manifest.get("repairs",[]) if not (root/x["source_texture_path"]).exists()]
    print("audited %d runtime model/texture selections; %d manifest sources unresolved"%(len(records),len(unresolved)))
    return 1 if args.command=="verify" and unresolved else 0

if __name__=="__main__": sys.exit(main())
