package mcheli.texture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.wrapper.modelloader.W_GroupObject;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

/** Client-only, lazy cache joining each rendered model instance to its vehicle texture. */
@SideOnly(Side.CLIENT)
public final class MCH_ModelTextureRepairManager implements IResourceManagerReloadListener {
   private static final MCH_ModelTextureRepairManager INSTANCE=new MCH_ModelTextureRepairManager();
   private final Map cache=new HashMap(); private int generation;
   private MCH_ModelTextureRepairManager(){}
   public static void register(){IResourceManager r=Minecraft.getMinecraft().getResourceManager();if(r instanceof IReloadableResourceManager)((IReloadableResourceManager)r).registerReloadListener(INSTANCE);}
   public static ResourceLocation resolve(ResourceLocation original,IModelCustom model,String modelPath){return INSTANCE.resolve0(original,model,modelPath);}
   /** Inventory capture is lower priority and must not initiate synchronous texture repair. */
   public static ResourceLocation resolveForBackgroundCapture(ResourceLocation original,IModelCustom model){
      if(MCH_Config.EnableModelTextureRepair==null||!MCH_Config.EnableModelTextureRepair.prmBool||model==null)return original;
      Entry hit=(Entry)INSTANCE.cache.get(new Key(original,model));return hit!=null?hit.location:original;
   }
   private ResourceLocation resolve0(ResourceLocation original,IModelCustom model,String modelPath){
      if(MCH_Config.EnableModelTextureRepair==null||!MCH_Config.EnableModelTextureRepair.prmBool||model==null)return original;
      Key key=new Key(original,model);Entry hit=(Entry)cache.get(key);if(hit!=null)return hit.location;
      Entry made=build(original,model,modelPath);cache.put(key,made);return made.location;
   }
   private Entry build(ResourceLocation source,IModelCustom model,String modelPath){InputStream in=null;try{
      long totalStarted=System.nanoTime(),stageStarted=totalStarted;
      IResource resource=Minecraft.getMinecraft().getResourceManager().getResource(source);in=resource.getInputStream();BufferedImage original=ImageIO.read(in);long loadNanos=System.nanoTime()-stageStarted;if(original==null)return new Entry(source,null);
      stageStarted=System.nanoTime();boolean[] coverage=new boolean[original.getWidth()*original.getHeight()];if(!coverage(model,coverage,original.getWidth(),original.getHeight()))return new Entry(source,null);long coverageNanos=System.nanoTime()-stageStarted;
      stageStarted=System.nanoTime();
      MCH_ModelTextureRepairProcessor.Result result=MCH_ModelTextureRepairProcessor.repair(original,coverage,positive(MCH_Config.ModelTextureMaxHoleArea,16),positive(MCH_Config.ModelTextureMaxHoleThickness,2),positive(MCH_Config.ModelTextureRGBBleedRadius,2),positive(MCH_Config.ModelTextureAlphaExpansionRadius,1));
      long repairNanos=System.nanoTime()-stageStarted;
      if(result.repairedPixels==0)return new Entry(source,null); // RGB-only changes are deliberately not enough confidence for replacement.
      stageStarted=System.nanoTime();int[] corrected=new int[2];if(MCH_Config.EnableModelUVCorrection!=null&&MCH_Config.EnableModelUVCorrection.prmBool)corrected=correct(model,result.image,positive(MCH_Config.ModelTextureUVCorrectionRadius,2));long correctionNanos=System.nanoTime()-stageStarted;
      stageStarted=System.nanoTime();DynamicTexture dynamic=new DynamicTexture(result.image);ResourceLocation location=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("mcheli_model_repair_"+(generation++),dynamic);long uploadNanos=System.nanoTime()-stageStarted;
      if(MCH_Config.DebugVehicleIconCache!=null&&MCH_Config.DebugVehicleIconCache.prmBool)MCH_Lib.Log("Icon texture repair: model=%s loadMs=%.3f coverageMs=%.3f repairMs=%.3f uvMs=%.3f uploadMs=%.3f totalMs=%.3f correctedVertices=%d invalidatedVboGroups=%d",modelPath,Double.valueOf(loadNanos/1e6),Double.valueOf(coverageNanos/1e6),Double.valueOf(repairNanos/1e6),Double.valueOf(correctionNanos/1e6),Double.valueOf(uploadNanos/1e6),Double.valueOf((System.nanoTime()-totalStarted)/1e6),Integer.valueOf(corrected[0]),Integer.valueOf(corrected[1]));
      if(MCH_Config.ModelTextureRepairDebugPreviews!=null&&MCH_Config.ModelTextureRepairDebugPreviews.prmBool)preview(modelPath,source,original,coverage,result);
      if(MCH_Config.ModelTextureRepairDebugLogging!=null&&MCH_Config.ModelTextureRepairDebugLogging.prmBool)MCH_Lib.Log("Texture repair: model=%s texture=%s pixels=%d",modelPath,source,Integer.valueOf(result.repairedPixels));
      return new Entry(location,dynamic);
   }catch(Exception e){if(MCH_Config.ModelTextureRepairDebugLogging!=null&&MCH_Config.ModelTextureRepairDebugLogging.prmBool)MCH_Lib.Log("Texture repair skipped: model=%s texture=%s (%s)",modelPath,source,e.getMessage());return new Entry(source,null);}finally{if(in!=null)try{in.close();}catch(Exception ignored){}}}
   private static int positive(mcheli.MCH_ConfigPrm p,int d){return p==null?d:Math.max(0,p.prmInt);}
   private static boolean coverage(IModelCustom model, boolean[] mask, int width, int height) {
      Iterator groups = groups(model);
      if(groups == null) {
         return false;
      }
      boolean any = false;
      while(groups.hasNext()) {
         W_GroupObject group = (W_GroupObject)groups.next();
         int vertex = 0;
         for(int face = 0; face < group.getFaceCount(); ++face) {
            int count = group.getFaceVertexCount(face);
            if(count == 3 || count == 4) {
               float[] uv = new float[count * 2];
               for(int index = 0; index < count; ++index) {
                  uv[index * 2] = group.getTextureU(vertex + index);
                  uv[index * 2 + 1] = group.getTextureV(vertex + index);
               }
               MCH_ModelTextureRepairProcessor.rasterizeFace(mask, width, height, uv);
               any = true;
            }
            vertex += count;
         }
      }
      return any;
   }

   private static int[] correct(IModelCustom model, BufferedImage image, int radius) {
      Iterator groups = groups(model);
      if(groups == null) {
         return new int[2];
      }
      int correctedVertices=0,invalidatedGroups=0;
      while(groups.hasNext()) {
         W_GroupObject group = (W_GroupObject)groups.next();
         boolean changed=false;
         for(int vertex = 0; vertex < group.getVertexCount(); ++vertex) {
            float u = group.getTextureU(vertex);
            float v = group.getTextureV(vertex);
            float[] corrected = MCH_ModelTextureRepairProcessor.correctUV(u, v, image, radius);
            if(u != corrected[0] || v != corrected[1]) {
               group.setTextureCoordinates(vertex, corrected[0], corrected[1]);
               ++correctedVertices;changed=true;
            }
         }
         if(changed)++invalidatedGroups;
      }
      return new int[]{correctedVertices,invalidatedGroups};
   }

   private static Iterator groups(IModelCustom model) {
      if(model instanceof W_MetasequoiaObject) {
         return ((W_MetasequoiaObject)model).groupObjects.iterator();
      }
      if(model instanceof W_WavefrontObject) {
         return ((W_WavefrontObject)model).groupObjects.iterator();
      }
      return null;
   }

   private static void preview(String model,ResourceLocation texture,BufferedImage original,boolean[] mask,MCH_ModelTextureRepairProcessor.Result result)throws Exception{File dir=new File(Minecraft.getMinecraft().mcDataDir,"mcheli-texture-repair/"+safe(model+"__"+texture));dir.mkdirs();ImageIO.write(original,"png",new File(dir,"original.png"));BufferedImage m=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB),components=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB),overlay=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int p=0;p<mask.length;p++){int x=p%original.getWidth(),y=p/original.getWidth();if(mask[p])m.setRGB(x,y,Color.WHITE.getRGB());if(result.components[p])components.setRGB(x,y,0xFFFF0000);overlay.setRGB(x,y,result.components[p]?0xFFFF00FF:original.getRGB(x,y));}ImageIO.write(m,"png",new File(dir,"uv-coverage.png"));ImageIO.write(components,"png",new File(dir,"transparent-components.png"));ImageIO.write(result.image,"png",new File(dir,"repaired.png"));ImageIO.write(overlay,"png",new File(dir,"uv-correction-overlay.png"));java.io.PrintWriter report=new java.io.PrintWriter(new File(dir,"report.txt"),"UTF-8");report.println("model="+model);report.println("texture="+texture);report.close();}
   private static String safe(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}
   public void onResourceManagerReload(IResourceManager ignored){TextureManager tm=Minecraft.getMinecraft().getTextureManager();for(Object o:cache.values()){Entry e=(Entry)o;if(e.dynamic!=null)tm.deleteTexture(e.location);}cache.clear();generation=0;}
   static final class Key{final ResourceLocation texture;final IModelCustom model;Key(ResourceLocation t,IModelCustom m){texture=t;model=m;}public int hashCode(){return texture.hashCode()*31+System.identityHashCode(model);}public boolean equals(Object o){return o instanceof Key&&((Key)o).texture.equals(texture)&&((Key)o).model==model;}}
   static final class Entry{final ResourceLocation location;final DynamicTexture dynamic;Entry(ResourceLocation l,DynamicTexture d){location=l;dynamic=d;}}
}
