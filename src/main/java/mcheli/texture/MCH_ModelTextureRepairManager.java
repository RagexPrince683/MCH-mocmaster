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
import mcheli.wrapper.modelloader.W_Face;
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
   private ResourceLocation resolve0(ResourceLocation original,IModelCustom model,String modelPath){
      if(MCH_Config.EnableModelTextureRepair==null||!MCH_Config.EnableModelTextureRepair.prmBool||model==null)return original;
      Key key=new Key(original,model);Entry hit=(Entry)cache.get(key);if(hit!=null)return hit.location;
      Entry made=build(original,model,modelPath);cache.put(key,made);return made.location;
   }
   private Entry build(ResourceLocation source,IModelCustom model,String modelPath){InputStream in=null;try{
      IResource resource=Minecraft.getMinecraft().getResourceManager().getResource(source);in=resource.getInputStream();BufferedImage original=ImageIO.read(in);if(original==null)return new Entry(source,null);
      boolean[] coverage=new boolean[original.getWidth()*original.getHeight()];if(!coverage(model,coverage,original.getWidth(),original.getHeight()))return new Entry(source,null);
      MCH_ModelTextureRepairProcessor.Result result=MCH_ModelTextureRepairProcessor.repair(original,coverage,positive(MCH_Config.ModelTextureMaxHoleArea,16),positive(MCH_Config.ModelTextureMaxHoleThickness,2),positive(MCH_Config.ModelTextureRGBBleedRadius,2),positive(MCH_Config.ModelTextureAlphaExpansionRadius,1));
      if(result.repairedPixels==0)return new Entry(source,null); // RGB-only changes are deliberately not enough confidence for replacement.
      if(MCH_Config.EnableModelUVCorrection!=null&&MCH_Config.EnableModelUVCorrection.prmBool)correct(model,result.image,positive(MCH_Config.ModelTextureUVCorrectionRadius,2));
      DynamicTexture dynamic=new DynamicTexture(result.image);ResourceLocation location=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("mcheli_model_repair_"+(generation++),dynamic);
      if(MCH_Config.ModelTextureRepairDebugPreviews!=null&&MCH_Config.ModelTextureRepairDebugPreviews.prmBool)preview(modelPath,source,original,coverage,result);
      if(MCH_Config.ModelTextureRepairDebugLogging!=null&&MCH_Config.ModelTextureRepairDebugLogging.prmBool)MCH_Lib.Log("Texture repair: model=%s texture=%s pixels=%d",modelPath,source,Integer.valueOf(result.repairedPixels));
      return new Entry(location,dynamic);
   }catch(Exception e){if(MCH_Config.ModelTextureRepairDebugLogging!=null&&MCH_Config.ModelTextureRepairDebugLogging.prmBool)MCH_Lib.Log("Texture repair skipped: model=%s texture=%s (%s)",modelPath,source,e.getMessage());return new Entry(source,null);}finally{if(in!=null)try{in.close();}catch(Exception ignored){}}}
   private static int positive(mcheli.MCH_ConfigPrm p,int d){return p==null?d:Math.max(0,p.prmInt);}
   private static boolean coverage(IModelCustom model,boolean[] mask,int w,int h){Iterator groups=null;if(model instanceof W_MetasequoiaObject)groups=((W_MetasequoiaObject)model).groupObjects.iterator();else if(model instanceof W_WavefrontObject)groups=((W_WavefrontObject)model).groupObjects.iterator();if(groups==null)return false;boolean any=false;while(groups.hasNext()){W_GroupObject g=(W_GroupObject)groups.next();for(Object o:g.faces){W_Face f=(W_Face)o;int n=f.getTextureCoordinateCount();if(n!=3&&n!=4)continue;float[] uv=new float[n*2];for(int i=0;i<n;i++){uv[i*2]=f.getTextureU(i);uv[i*2+1]=f.getTextureV(i);}MCH_ModelTextureRepairProcessor.rasterizeFace(mask,w,h,uv);any=true;}}return any;}
   private static void correct(IModelCustom model,BufferedImage image,int radius){Iterator groups=model instanceof W_MetasequoiaObject?((W_MetasequoiaObject)model).groupObjects.iterator():model instanceof W_WavefrontObject?((W_WavefrontObject)model).groupObjects.iterator():null;if(groups==null)return;while(groups.hasNext())for(Object o:((W_GroupObject)groups.next()).faces){W_Face f=(W_Face)o;int n=f.getTextureCoordinateCount();float[] uv=new float[n*2];boolean changed=false;for(int i=0;i<n;i++){float u=f.getTextureU(i),v=f.getTextureV(i);float[] c=MCH_ModelTextureRepairProcessor.correctUV(u,v,image,radius);uv[i*2]=c[0];uv[i*2+1]=c[1];changed|=u!=c[0]||v!=c[1];}if(changed)f.setRepairedTextureCoordinates(uv);}}
   private static void preview(String model,ResourceLocation texture,BufferedImage original,boolean[] mask,MCH_ModelTextureRepairProcessor.Result result)throws Exception{File dir=new File(Minecraft.getMinecraft().mcDataDir,"mcheli-texture-repair/"+safe(model+"__"+texture));dir.mkdirs();ImageIO.write(original,"png",new File(dir,"original.png"));BufferedImage m=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB),components=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB),overlay=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int p=0;p<mask.length;p++){int x=p%original.getWidth(),y=p/original.getWidth();if(mask[p])m.setRGB(x,y,Color.WHITE.getRGB());if(result.components[p])components.setRGB(x,y,0xFFFF0000);overlay.setRGB(x,y,result.components[p]?0xFFFF00FF:original.getRGB(x,y));}ImageIO.write(m,"png",new File(dir,"uv-coverage.png"));ImageIO.write(components,"png",new File(dir,"transparent-components.png"));ImageIO.write(result.image,"png",new File(dir,"repaired.png"));ImageIO.write(overlay,"png",new File(dir,"uv-correction-overlay.png"));java.io.PrintWriter report=new java.io.PrintWriter(new File(dir,"report.txt"),"UTF-8");report.println("model="+model);report.println("texture="+texture);report.close();}
   private static String safe(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}
   public void onResourceManagerReload(IResourceManager ignored){TextureManager tm=Minecraft.getMinecraft().getTextureManager();for(Object o:cache.values()){Entry e=(Entry)o;if(e.dynamic!=null)tm.deleteTexture(e.location);}cache.clear();generation=0;}
   static final class Key{final ResourceLocation texture;final IModelCustom model;Key(ResourceLocation t,IModelCustom m){texture=t;model=m;}public int hashCode(){return texture.hashCode()*31+System.identityHashCode(model);}public boolean equals(Object o){return o instanceof Key&&((Key)o).texture.equals(texture)&&((Key)o).model==model;}}
   static final class Entry{final ResourceLocation location;final DynamicTexture dynamic;Entry(ResourceLocation l,DynamicTexture d){location=l;dynamic=d;}}
}
