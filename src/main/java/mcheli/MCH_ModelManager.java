package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_ModelBase;
import mcheli.wrapper.W_ResourcePath;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.client.model.IModelCustom;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class MCH_ModelManager extends W_ModelBase {

   private static MCH_ModelManager instance = new MCH_ModelManager();
   private static HashMap map;
   private static ModelRenderer defaultModel;
   private static boolean forceReloadMode = false;
   private static Random rand = new Random();


   private MCH_ModelManager() {
      map = new HashMap();
      defaultModel = null;
      defaultModel = new ModelRenderer(this, 0, 0);
      defaultModel.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
   }

   public static void setForceReloadMode(boolean b) {
      forceReloadMode = b;
   }

   public static IModelCustom load(String path, String name) {
      return name != null && !name.isEmpty()?load(path + "/" + name):null;
   }

   public static IModelCustom load(String name) {
      if(name != null && !name.isEmpty()) {
         IModelCustom obj = (IModelCustom)map.get(name);
         if(obj != null) {
            if(!forceReloadMode) {
               return obj;
            }

            map.remove(name);
         }

         IModelCustom model = null;

         try {
            String e = "/assets/mcheli/models/" + name + ".mqo";
            String filePathObj = "/assets/mcheli/models/" + name + ".obj";
            String filePathTcn = "/assets/mcheli/models/" + name + ".tcn";
            if((new File(MCH_MOD.sourcePath + e)).exists()) {
               e = W_ResourcePath.getModelPath() + "models/" + name + ".mqo";
               model = W_ModelBase.loadModel(e);
            } else if((new File(MCH_MOD.sourcePath + filePathObj)).exists()) {
               filePathObj = W_ResourcePath.getModelPath() + "models/" + name + ".obj";
               model = W_ModelBase.loadModel(filePathObj);
            } else if((new File(MCH_MOD.sourcePath + filePathTcn)).exists()) {
               filePathTcn = W_ResourcePath.getModelPath() + "models/" + name + ".tcn";
               model = W_ModelBase.loadModel(filePathTcn);
            }
         } catch (Exception var6) {
            var6.printStackTrace();
            model = null;
         }

         if(model != null) {
            map.put(name, model);
            return model;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void render(String path, String name) {
      render(path + "/" + name);
   }

   public static void render(String name) {
      IModelCustom model = (IModelCustom)map.get(name);
      if(model != null) {
         model.renderAll();
      } else if(defaultModel != null) {
         ;
      }

   }

   public static void renderPart(String name, String partName) {
      IModelCustom model = (IModelCustom)map.get(name);
      if(model != null) {
         model.renderPart(partName);
      }

   }

   public static void renderLine(String path, String name, int startLine, int maxLine) {
      IModelCustom model = (IModelCustom)map.get(path + "/" + name);
      if(model instanceof W_ModelCustom) {
         ((W_ModelCustom)model).renderAllLine(startLine, maxLine);
      }

   }

   public static void render(String path, String name, int startFace, int maxFace) {
      IModelCustom model = (IModelCustom)map.get(path + "/" + name);
      if(model instanceof W_ModelCustom) {
         ((W_ModelCustom)model).renderAll(startFace, maxFace);
      }

   }

   public static int getVertexNum(String path, String name) {
      IModelCustom model = (IModelCustom)map.get(path + "/" + name);
      return model instanceof W_ModelCustom?((W_ModelCustom)model).getVertexNum():0;
   }

   public static W_ModelCustom get(String path, String name) {
      IModelCustom model = (IModelCustom)map.get(path + "/" + name);
      return model instanceof W_ModelCustom?(W_ModelCustom)model:null;
   }

   public static W_ModelCustom getRandome() {
      int size = map.size();

      for(int i = 0; i < 10; ++i) {
         int idx = 0;
         int index = rand.nextInt(size);

         for(Iterator i$ = map.values().iterator(); i$.hasNext(); ++idx) {
            IModelCustom model = (IModelCustom)i$.next();
            if(idx >= index && model instanceof W_ModelCustom) {
               return (W_ModelCustom)model;
            }
         }
      }

      return null;
   }

   public static boolean containsModel(String path, String name) {
      return containsModel(path + "/" + name);
   }

   public static boolean containsModel(String name) {
      return map.containsKey(name);
   }

}
