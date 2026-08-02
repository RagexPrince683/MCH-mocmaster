package mcheli.texture;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import mcheli.wrapper.modelloader.W_Face;
import mcheli.wrapper.modelloader.W_GroupObject;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the shipped model and texture rather than an invented alpha mask. */
public class MCH_MerkavaTextureRepairIntegrationTest {
 @Test public void rearRailingAssetReceivesAConservativeVisibleRepair() throws Exception {
  InputStream modelIn=getClass().getResourceAsStream("/assets/mcheli/models/tanks/merkava_mk4.mqo");
  InputStream textureIn=getClass().getResourceAsStream("/assets/mcheli/textures/tanks/merkava_mk4.png");
  assertNotNull(modelIn);assertNotNull(textureIn);
  W_MetasequoiaObject model=new W_MetasequoiaObject("merkava_mk4.mqo",modelIn);BufferedImage texture=ImageIO.read(textureIn);
  boolean[] coverage=new boolean[texture.getWidth()*texture.getHeight()];int faces=0;
  for(Object go:model.groupObjects)for(Object fo:((W_GroupObject)go).faces){W_Face f=(W_Face)fo;int n=f.getTextureCoordinateCount();if(n!=3&&n!=4)continue;float[] uv=new float[n*2];for(int i=0;i<n;i++){uv[i*2]=f.getTextureU(i);uv[i*2+1]=f.getTextureV(i);}MCH_ModelTextureRepairProcessor.rasterizeFace(coverage,texture.getWidth(),texture.getHeight(),uv);faces++;}
  assertTrue("model must contribute real UV faces",faces>0);
  MCH_ModelTextureRepairProcessor.Result repaired=MCH_ModelTextureRepairProcessor.repair(texture,coverage,16,6,1,1,2);
  assertTrue("pipeline must report texture changes, UV changes, or an explicit safe no-op",repaired.changed||(!repaired.textureChanged()&&repaired.correctedUVVertices==0));
  assertTrue("Merkava railing needs a real pixel repair",repaired.alphaExpandedPixels>0||repaired.alphaFilledPixels>0);
  assertFalse("repair must not turn every UV-covered transparent pixel into a panel",allCoveredPixelsOpaque(repaired.image,coverage));
 }
 private static boolean allCoveredPixelsOpaque(BufferedImage image,boolean[] coverage){for(int p=0;p<coverage.length;p++)if(coverage[p]&&((image.getRGB(p%image.getWidth(),p/image.getWidth())>>>24)&255)==0)return false;return true;}
}
