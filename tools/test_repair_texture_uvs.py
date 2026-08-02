import hashlib, io, tempfile, unittest
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
        self.assertGreater(sum(a>=128 for *_,a in result.getdata()),8)
        self.assertGreater(result.getpixel((4,4))[3],240)

    def test_bleed_does_not_expand_alpha(self):
        image=self.make_image(); before=list(image.getchannel("A").getdata())
        result=tool.bleed_transparent_rgb(image)
        self.assertEqual(before,list(result.getchannel("A").getdata()))
        self.assertNotEqual((0,0,0),result.getpixel((3,3))[:3])

    def test_deterministic_hash(self):
        hashes=[]
        for _ in range(2):
            result=tool.premultiplied_resize(self.make_image(),(8,8)); out=io.BytesIO()
            result.save(out,"PNG",compress_level=9,optimize=False); hashes.append(hashlib.sha256(out.getvalue()).hexdigest())
        self.assertEqual(hashes[0],hashes[1])

if __name__=="__main__": unittest.main()
