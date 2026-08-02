package mcheli.texture;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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

/** Client-only cache joining the exact rendered model instance to its repaired texture and UVs. */
@SideOnly(Side.CLIENT)
public final class MCH_ModelTextureRepairManager implements IResourceManagerReloadListener {
   private static final MCH_ModelTextureRepairManager INSTANCE=new MCH_ModelTextureRepairManager();
   private final Map cache=new HashMap();private int generation;
   private MCH_ModelTextureRepairManager(){}
   public static void register(){IResourceManager r=Minecraft.getMinecraft().getResourceManager();if(r instanceof IReloadableResourceManager)((IReloadableResourceManager)r).registerReloadListener(INSTANCE);}
   public static ResourceLocation resolve(ResourceLocation original,IModelCustom model,String modelPath){return INSTANCE.resolve0(original,model,modelPath);}
   private ResourceLocation resolve0(ResourceLocation original,IModelCustom model,String modelPath){if(MCH_Config.EnableModelTextureRepair==null||!MCH_Config.EnableModelTextureRepair.prmBool||model==null)return original;Key key=new Key(original,model);Entry hit=(Entry)cache.get(key);if(hit!=null)return hit.location;Entry made=build(original,model,modelPath);cache.put(key,made);return made.location;}

   private Entry build(ResourceLocation source,IModelCustom model,String modelPath){InputStream in=null;try{
      IResource resource=Minecraft.getMinecraft().getResourceManager().getResource(source);in=resource.getInputStream();BufferedImage original=ImageIO.read(in);if(original==null)return noOp(source,modelPath,source,"texture decoder returned no image");
      ArrayList groups=groups(model),faces=collect(groups);if(faces.isEmpty())return noOp(source,modelPath,source,"model has no rasterizable in-range UV faces");
      int w=original.getWidth(),h=original.getHeight();boolean[] coverage=new boolean[w*h];int skipped=0;for(Object o:faces){FaceRef f=(FaceRef)o;if(inRange(f.uv))MCH_ModelTextureRepairProcessor.rasterizeFace(coverage,w,h,f.uv);else skipped++;}
      MCH_ModelTextureRepairProcessor.Result result=MCH_ModelTextureRepairProcessor.repair(original,coverage,positive(MCH_Config.ModelTextureMaxHoleArea,16),positive(MCH_Config.ModelTextureThinMinLength,6),positive(MCH_Config.ModelTextureThinMaxThickness,1),positive(MCH_Config.ModelTextureThinExpansionRadius,1),positive(MCH_Config.ModelTextureRGBBleedRadius,2));
      UVAnalysis uv=new UVAnalysis(w,h,faces.size(),skipped);if(MCH_Config.EnableModelUVCorrection!=null&&MCH_Config.EnableModelUVCorrection.prmBool)uv=correctIslands(faces,groups,result.image,positive(MCH_Config.ModelTextureUVIslandSearchRadius,8),positive(MCH_Config.ModelTextureUVMinScoreGain,20)/100.0F,positive(MCH_Config.ModelTextureTinyIslandMinSize,1));
      result=result.withUV(uv.vertices,uv.faces,uv.changedIslands);boolean dynamicBound=false;DynamicTexture dynamic=null;ResourceLocation location=source;if(result.textureChanged()){dynamic=new DynamicTexture(result.image);location=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("mcheli_model_repair_"+(generation++),dynamic);dynamicBound=true;}
      boolean debug=enabled(MCH_Config.ModelTextureRepairDebugLogging),previews=enabled(MCH_Config.ModelTextureRepairDebugPreviews);if(previews)preview(modelPath,source,original,coverage,result,uv);
      if(debug){MCH_Lib.Log("Texture repair: model=%s texture=%s size=%dx%d faces=%d rasterized=%d skippedWrapping=%d islands=%d",modelPath,source,Integer.valueOf(w),Integer.valueOf(h),Integer.valueOf(faces.size()),Integer.valueOf(faces.size()-skipped),Integer.valueOf(skipped),Integer.valueOf(uv.islands));for(Object o:uv.suspicious){IslandScore s=(IslandScore)o;MCH_Lib.Log("Texture repair island=%d originalOpaque=%.3f bestOpaque=%.3f translation=%d,%d",Integer.valueOf(s.id),Float.valueOf(s.original),Float.valueOf(s.best),Integer.valueOf(s.dx),Integer.valueOf(s.dy));}MCH_Lib.Log("Texture repair result: alphaFilled=%d alphaExpanded=%d rgbBleed=%d uvVertices=%d uvFaces=%d uvIslands=%d dynamicBound=%s vbosInvalidated=%s",Integer.valueOf(result.alphaFilledPixels),Integer.valueOf(result.alphaExpandedPixels),Integer.valueOf(result.rgbBleedPixels),Integer.valueOf(result.correctedUVVertices),Integer.valueOf(result.correctedUVFaces),Integer.valueOf(result.correctedUVIslands),Boolean.valueOf(dynamicBound),Boolean.valueOf(uv.vbosInvalidated));if(!result.changed)MCH_Lib.Log("Texture repair no-op: model=%s reason=no safe texture component or UV-island translation exceeded conservative thresholds",modelPath);}
      return new Entry(location,dynamic,model);
   }catch(Exception e){if(enabled(MCH_Config.ModelTextureRepairDebugLogging))MCH_Lib.Log("Texture repair no-op: model=%s texture=%s reason=%s",modelPath,source,e.toString());return new Entry(source,null,model);}finally{if(in!=null)try{in.close();}catch(Exception ignored){}}}
   private Entry noOp(ResourceLocation source,String model,ResourceLocation texture,String reason){if(enabled(MCH_Config.ModelTextureRepairDebugLogging))MCH_Lib.Log("Texture repair no-op: model=%s texture=%s reason=%s",model,texture,reason);return new Entry(source,null,null);}
   private static boolean enabled(mcheli.MCH_ConfigPrm p){return p!=null&&p.prmBool;}private static int positive(mcheli.MCH_ConfigPrm p,int d){return p==null?d:Math.max(0,p.prmInt);}
   private static ArrayList groups(IModelCustom model){if(model instanceof W_MetasequoiaObject)return((W_MetasequoiaObject)model).groupObjects;if(model instanceof W_WavefrontObject)return((W_WavefrontObject)model).groupObjects;return new ArrayList();}
   private static ArrayList collect(ArrayList groups){ArrayList out=new ArrayList();for(Object go:groups){W_GroupObject g=(W_GroupObject)go;for(Object fo:g.faces){W_Face f=(W_Face)fo;int n=f.getTextureCoordinateCount();if(n!=3&&n!=4)continue;float[] uv=new float[n*2],xyz=new float[n*3];for(int i=0;i<n;i++){uv[i*2]=f.getTextureU(i);uv[i*2+1]=f.getTextureV(i);xyz[i*3]=f.getVertexX(i);xyz[i*3+1]=f.getVertexY(i);xyz[i*3+2]=f.getVertexZ(i);}out.add(new FaceRef(f,g,uv,xyz));}}return out;}
   private static boolean inRange(float[] uv){for(float f:uv)if(f<0||f>1)return false;return true;}

   private static UVAnalysis correctIslands(ArrayList faces,ArrayList groups,BufferedImage image,int radius,float minimumGain,int tinySize){
      int count=faces.size(),w=image.getWidth(),h=image.getHeight();int[] parent=new int[count];for(int i=0;i<count;i++)parent[i]=i;Map shared=new HashMap();
      // Physical position is part of the key: disconnected objects which merely overlap in the atlas remain separate.
      for(int fi=0;fi<count;fi++){FaceRef f=(FaceRef)faces.get(fi);for(int v=0;v<f.uv.length/2;v++){String key=q(f.xyz[v*3])+":"+q(f.xyz[v*3+1])+":"+q(f.xyz[v*3+2])+":"+q(f.uv[v*2])+":"+q(f.uv[v*2+1]);Integer other=(Integer)shared.put(key,Integer.valueOf(fi));if(other!=null)union(parent,fi,other.intValue());}}
      Map islands=new HashMap();for(int i=0;i<count;i++){Integer root=Integer.valueOf(find(parent,i));ArrayList island=(ArrayList)islands.get(root);if(island==null){island=new ArrayList();islands.put(root,island);}island.add(faces.get(i));}
      UVAnalysis result=new UVAnalysis(w,h,count,0);result.islands=islands.size();int id=0;
      for(Object value:islands.values()){ArrayList island=(ArrayList)value;boolean[] mask=new boolean[w*h];float minU=2,maxU=-1,minV=2,maxV=-1;for(Object o:island){FaceRef f=(FaceRef)o;if(!inRange(f.uv))continue;MCH_ModelTextureRepairProcessor.rasterizeFace(mask,w,h,f.uv);for(int j=0;j<f.uv.length;j+=2){minU=Math.min(minU,f.uv[j]);maxU=Math.max(maxU,f.uv[j]);minV=Math.min(minV,f.uv[j+1]);maxV=Math.max(maxV,f.uv[j+1]);}}
         Score original=score(mask,image,0,0);if(original.samples==0){id++;continue;}float originalOpaque=original.opaque/(float)original.samples;boolean tiny=(maxU-minU)*w<tinySize||(maxV-minV)*h<tinySize;if(originalOpaque>=0.55F&&!tiny){id++;continue;}int bestDx=0,bestDy=0;float best=originalOpaque;
         for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){if(dx==0&&dy==0)continue;Score candidate=score(mask,image,dx,dy);float s=candidate.samples==0?0:candidate.opaque/(float)candidate.samples-candidate.outside/(float)original.samples;if(s>best){best=s;bestDx=dx;bestDy=dy;}}
         IslandScore diagnostic=new IslandScore(id,originalOpaque,best,bestDx,bestDy);result.suspicious.add(diagnostic);if(bestDx!=0||bestDy!=0)if(best-originalOpaque>=minimumGain){int changedFaces=0;for(Object o:island){FaceRef f=(FaceRef)o;float[] moved=new float[f.uv.length];boolean valid=true;for(int j=0;j<moved.length;j+=2){moved[j]=f.uv[j]+bestDx/(float)w;moved[j+1]=f.uv[j+1]+bestDy/(float)h;if(moved[j]<0||moved[j]>1||moved[j+1]<0||moved[j+1]>1)valid=false;}if(valid){f.face.setRepairedTextureCoordinates(moved);result.vertices+=moved.length/2;changedFaces++;}}if(changedFaces>0){result.faces+=changedFaces;result.changedIslands++;}}
         id++;
      }
      if(result.vertices>0)for(Object o:groups){((W_GroupObject)o).invalidateVbo();result.vbosInvalidated=true;}return result;
   }
   private static Score score(boolean[] mask,BufferedImage image,int dx,int dy){int w=image.getWidth(),h=image.getHeight(),samples=0,opaque=0,out=0,semi=0;for(int p=0;p<mask.length;p++)if(mask[p]){samples++;int x=p%w+dx,y=p/w+dy;if(x<0||y<0||x>=w||y>=h){out++;continue;}int a=(image.getRGB(x,y)>>>24)&255;if(a>=250)opaque++;else if(a>0)semi++;}return new Score(samples,opaque,semi,out);}
   private static int q(float v){return Math.round(v*100000.0F);}private static int find(int[] p,int x){while(p[x]!=x){p[x]=p[p[x]];x=p[x];}return x;}private static void union(int[] p,int a,int b){a=find(p,a);b=find(p,b);if(a!=b)p[b]=a;}

   private static void preview(String model,ResourceLocation texture,BufferedImage original,boolean[] coverage,MCH_ModelTextureRepairProcessor.Result r,UVAnalysis uv)throws Exception{
      File dir=new File(Minecraft.getMinecraft().mcDataDir,"mcheli-texture-repair/"+safe(model+"__"+texture));dir.mkdirs();BufferedImage cov=mask(original,coverage,0xFFFFFFFF),islands=mask(original,coverage,0xFF55AAFF),transparent=mask(original,r.components,0xFFFF00FF),thin=mask(original,r.thinComponents,0xFFFFFF00),alpha=overlay(original,r.components,0xFFFF00FF),translated=mask(original,coverage,uv.changedIslands>0?0xFF00FF00:0x66FF0000);ImageIO.write(original,"png",new File(dir,"original.png"));ImageIO.write(cov,"png",new File(dir,"uv-coverage.png"));ImageIO.write(islands,"png",new File(dir,"uv-islands.png"));ImageIO.write(transparent,"png",new File(dir,"transparent-face-coverage.png"));ImageIO.write(thin,"png",new File(dir,"thin-components.png"));ImageIO.write(alpha,"png",new File(dir,"proposed-alpha-repair.png"));ImageIO.write(translated,"png",new File(dir,"proposed-uv-translation.png"));ImageIO.write(r.image,"png",new File(dir,"final.png"));PrintWriter report=new PrintWriter(new File(dir,"report.txt"),"UTF-8");report.println("model="+model);report.println("texture="+texture);report.println("islands="+uv.islands);for(Object o:uv.suspicious){IslandScore s=(IslandScore)o;report.println("island="+s.id+" originalOpaque="+s.original+" bestOpaque="+s.best+" translation="+s.dx+","+s.dy);}report.println("alphaFilledPixels="+r.alphaFilledPixels);report.println("alphaExpandedPixels="+r.alphaExpandedPixels);report.println("rgbBleedPixels="+r.rgbBleedPixels);report.println("correctedUVVertices="+r.correctedUVVertices);report.println("changed="+r.changed);if(!r.changed)report.println("noOpReason=no safe change exceeded conservative thresholds");report.close();
   }
   private static BufferedImage mask(BufferedImage source,boolean[] m,int color){BufferedImage o=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int p=0;p<m.length;p++)if(m[p])o.setRGB(p%source.getWidth(),p/source.getWidth(),color);return o;}private static BufferedImage overlay(BufferedImage source,boolean[] m,int color){BufferedImage o=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);for(int p=0;p<m.length;p++)o.setRGB(p%source.getWidth(),p/source.getWidth(),m[p]?color:source.getRGB(p%source.getWidth(),p/source.getWidth()));return o;}private static String safe(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}

   public void onResourceManagerReload(IResourceManager ignored){TextureManager tm=Minecraft.getMinecraft().getTextureManager();for(Object o:cache.values()){Entry e=(Entry)o;if(e.dynamic!=null)tm.deleteTexture(e.location);clearModel(e.model);}cache.clear();generation=0;}
   private static void clearModel(IModelCustom model){if(model==null)return;for(Object go:groups(model)){W_GroupObject g=(W_GroupObject)go;for(Object fo:g.faces)((W_Face)fo).clearRepairedTextureCoordinates();g.invalidateVbo();}}
   static final class FaceRef{final W_Face face;final W_GroupObject group;final float[] uv,xyz;FaceRef(W_Face f,W_GroupObject g,float[] u,float[] x){face=f;group=g;uv=u;xyz=x;}}
   static final class Score{final int samples,opaque,semi,outside;Score(int s,int o,int semi,int out){samples=s;opaque=o;this.semi=semi;outside=out;}}
   static final class IslandScore{final int id,dx,dy;final float original,best;IslandScore(int id,float o,float b,int x,int y){this.id=id;original=o;best=b;dx=x;dy=y;}}
   static final class UVAnalysis{final int width,height,totalFaces,skipped;int islands,vertices,faces,changedIslands;boolean vbosInvalidated;final ArrayList suspicious=new ArrayList();UVAnalysis(int w,int h,int f,int s){width=w;height=h;totalFaces=f;skipped=s;}}
   static final class Key{final ResourceLocation texture;final IModelCustom model;Key(ResourceLocation t,IModelCustom m){texture=t;model=m;}public int hashCode(){return texture.hashCode()*31+System.identityHashCode(model);}public boolean equals(Object o){return o instanceof Key&&((Key)o).texture.equals(texture)&&((Key)o).model==model;}}
   static final class Entry{final ResourceLocation location;final DynamicTexture dynamic;final IModelCustom model;Entry(ResourceLocation l,DynamicTexture d,IModelCustom m){location=l;dynamic=d;model=m;}}
}
