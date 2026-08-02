import contextlib, hashlib, io, json, tempfile, unittest
from pathlib import Path
import repair_texture_uvs as tool
Image=tool.Image

class ParserTests(unittest.TestCase):
    def test_triangle(self):
        face=tool.parse_face_line("3 V(0 1 2) M(0) UV(0 0 1 0 0 1)")
        self.assertEqual((3,((0.,0.),(1.,0.),(0.,1.))),(face.vertex_count,face.uvs))

    def test_quad_and_wrapping(self):
        face=tool.parse_face_line("4 V(0 1 2 3) UV(-.25 0 1.25 0 1.25 2 -.25 2)")
        self.assertEqual(4,face.vertex_count); self.assertEqual((-0.25,0),face.uvs[0]); self.assertEqual((1.25,2),face.uvs[2])

    def test_unchanged_round_trip(self):
        data=b"Metasequoia Document\r\n3 V(0 1 2) UV(0 0 1 0 0 1)\r\n"
        self.assertEqual(data,tool.rewrite_mqo(data,None))

class TransformTests(unittest.TestCase):
    def test_proportional_requires_no_transform(self):
        data=b"3 V(0 1 2) UV(0.25 0.75 1 0 0 1)\n"
        self.assertEqual(data,tool.rewrite_mqo(data,None))

    def test_crop(self):
        t=dict(old_width=100,old_height=100,new_width=50,new_height=50,crop_x=25,crop_y=25)
        self.assertEqual((0.,0.),tool.transform_uv((.25,.25),t))

    def test_padding(self):
        t=dict(old_width=50,old_height=50,new_width=100,new_height=100,pad_x=25,pad_y=25)
        self.assertEqual((.25,.25),tool.transform_uv((0,0),t))

    def test_non_proportional(self):
        t=dict(old_width=100,old_height=50,new_width=50,new_height=100,scale_x=.5,scale_y=2)
        self.assertEqual((.5,.5),tool.transform_uv((.5,.5),t))

@unittest.skipIf(Image is None,"Pillow development dependency is unavailable")
class ImageTests(unittest.TestCase):
    def make_image(self):
        image=Image.new("RGBA",(16,16),(200,20,10,0))
        for y in range(4,12):
            for x in range(4,12): image.putpixel((x,y),(240,80,40,255))
        return image

    def test_premultiplied_resize_and_alpha_coverage(self):
        result=tool.premultiplied_resize(self.make_image(),(8,8))
        self.assertEqual((8,8),result.size)
        self.assertGreater(sum(a>=128 for *_,a in tool.flattened_data(result)),8)
        self.assertGreater(result.getpixel((4,4))[3],240)

    def test_bleed_does_not_expand_alpha(self):
        image=self.make_image(); before=list(tool.flattened_data(image.getchannel("A")))
        result=tool.bleed_transparent_rgb(image)
        self.assertEqual(before,list(tool.flattened_data(result.getchannel("A"))))
        self.assertNotEqual((0,0,0),result.getpixel((3,3))[:3])

    def test_each_bleed_pass_propagates_farther(self):
        image=Image.new("RGBA",(9,1),(0,0,0,0)); image.putpixel((0,0),(90,40,10,255))
        one=tool.bleed_transparent_rgb(image,1); four=tool.bleed_transparent_rgb(image,4)
        self.assertEqual((0,0,0),one.getpixel((4,0))[:3])
        self.assertNotEqual((0,0,0),four.getpixel((4,0))[:3])

    def test_opaque_and_transparent_extremes(self):
        opaque=tool.premultiplied_resize(Image.new("RGBA",(4,4),(3,8,20,255)),(7,7))
        transparent=tool.premultiplied_resize(Image.new("RGBA",(4,4),(3,8,20,0)),(7,7))
        self.assertTrue(all(a==255 for *_,a in tool.flattened_data(opaque)))
        self.assertTrue(all(a==0 for *_,a in tool.flattened_data(transparent)))

    def test_semitransparent_and_border_rgb_are_valid(self):
        result=tool.premultiplied_resize(self.make_image(),(8,8))
        semi=[p for p in tool.flattened_data(result) if 0<p[3]<255]
        self.assertTrue(semi); self.assertTrue(all(any(p[:3]) for p in semi))
        self.assertNotEqual((0,0,0),result.getpixel((1,1))[:3])

    def test_deterministic_hash(self):
        hashes=[]
        for _ in range(2):
            result=tool.premultiplied_resize(self.make_image(),(8,8)); out=io.BytesIO()
            result.save(out,"PNG",compress_level=9,optimize=False); hashes.append(hashlib.sha256(out.getvalue()).hexdigest())
        self.assertEqual(hashes[0],hashes[1])

    def test_repair_dry_run_apply_and_complete_verification(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); (root/"sources").mkdir(); source=root/"sources/original.png"
            Image.new("RGBA",(4,4),(20,40,60,255)).save(source)
            result=tool.premultiplied_resize(Image.open(source),(2,2)); encoded=io.BytesIO()
            result.save(encoded,"PNG",optimize=False,compress_level=9)
            manifest=root/"manifest.json"; manifest.write_text(json.dumps({"repairs":[{
                "current_texture_path":"textures/out.png","source_texture_path":"sources/original.png",
                "source_sha256":tool.sha256(source.read_bytes()),"output_sha256":tool.sha256(encoded.getvalue()),
                "output_width":2,"output_height":2,"models":[],"uv_transform":None}],"unresolved":[]}))
            common=["--assets",str(root),"--manifest",str(manifest),"repair"]
            output=io.StringIO()
            with contextlib.redirect_stdout(output): self.assertEqual(0,tool.main(common+["--dry-run"]))
            self.assertIn(str(root/"textures/out.png"),output.getvalue()); self.assertFalse((root/"textures/out.png").exists())
            with contextlib.redirect_stdout(io.StringIO()): self.assertEqual(0,tool.main(common+["--apply"]))
            self.assertTrue((root/"textures/out.png").exists())
            with contextlib.redirect_stdout(io.StringIO()): self.assertEqual(0,tool.main(["--assets",str(root),"--manifest",str(manifest),"verify"]))

class CommandTests(unittest.TestCase):
    def manifest(self, root, content):
        path=root/"manifest.json"; path.write_text(json.dumps(content)); return path

    def test_empty_manifest_messages_and_apply_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); manifest=self.manifest(root,{"repairs":[],"unresolved":[]})
            for mode, expected in (("--dry-run",0),("--apply",1)):
                output=io.StringIO()
                with contextlib.redirect_stdout(output): result=tool.main(["--assets",str(root),"--manifest",str(manifest),"repair",mode])
                self.assertEqual(expected,result); self.assertIn("No repair entries are defined",output.getvalue()); self.assertIn("No files were changed",output.getvalue())

    def test_verify_rejects_unresolved_and_missing_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            unresolved=self.manifest(root,{"repairs":[],"unresolved":[{"required_source":"original.png"}]})
            with contextlib.redirect_stdout(io.StringIO()): self.assertEqual(1,tool.main(["--assets",str(root),"--manifest",str(unresolved),"verify"]))
            missing=self.manifest(root,{"repairs":[{"source_texture_path":"missing.png","current_texture_path":"out.png","source_sha256":"0","output_sha256":"0"}],"unresolved":[]})
            with contextlib.redirect_stdout(io.StringIO()): self.assertEqual(1,tool.main(["--assets",str(root),"--manifest",str(missing),"verify"]))

if __name__=="__main__": unittest.main()
