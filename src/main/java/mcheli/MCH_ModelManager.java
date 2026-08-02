package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import mcheli.wrapper.W_ModelBase;
import mcheli.wrapper.W_ResourcePath;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.client.model.IModelCustom;

@SideOnly(Side.CLIENT)
public class MCH_ModelManager extends W_ModelBase {

   private static final ConcurrentHashMap<String, IModelCustom> MAP = new ConcurrentHashMap<>();
   private static final Random RAND = new Random();

   private static final ModelRenderer DEFAULT_MODEL;

   private static volatile boolean forceReloadMode = false;

   static {
      DEFAULT_MODEL = new ModelRenderer(new MCH_ModelManager(), 0, 0);
      DEFAULT_MODEL.addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10, 0.0F);
   }

   private MCH_ModelManager() {}

   public static void setForceReloadMode(boolean b) {
      forceReloadMode = b;
   }

   /** Drops all model objects before a complete client-thread registration pass. */
   public static void clearForReload() {
      MAP.clear();
      mcheli.tank.MCH_TurretPopModelCache.clear();
      forceReloadMode = true;
   }

   public static IModelCustom load(String path, String name) {
      return (name != null && !name.isEmpty()) ? load(path + "/" + name) : null;
   }

   public static IModelCustom load(String name) {
      if (name == null || name.isEmpty()) return null;

      if (!forceReloadMode) {
         IModelCustom existing = MAP.get(name);
         if (existing != null) return existing;
      }

      return MAP.compute(name, (key, existing) -> {
         if (existing != null && !forceReloadMode) {
            return existing;
         }

         try {
            String mqoPath = "assets/mcheli/models/" + name + ".mqo";
            String objPath = "assets/mcheli/models/" + name + ".obj";
            String tcnPath = "assets/mcheli/models/" + name + ".tcn";

            String modelPath = null;

            if (MCH_ResourceHelper.resourceExists(mqoPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + name + ".mqo";
            } else if (MCH_ResourceHelper.resourceExists(objPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + name + ".obj";
            } else if (MCH_ResourceHelper.resourceExists(tcnPath)) {
               modelPath = W_ResourcePath.getModelPath() + "models/" + name + ".tcn";
            }

            return (modelPath != null) ? W_ModelBase.loadModel(modelPath) : null;

         } catch (Exception e) {
            e.printStackTrace();
            return null;
         }
      });
   }

   public static void render(String path, String name) {
      render(path + "/" + name);
   }

   public static void render(String name) {
      IModelCustom model = MAP.get(name);

      if (model == null) {
         model = load(name); // triggers load if not present
      }

      if (model != null) {
         model.renderAll();
      }
   }

   public static void renderPart(String name, String partName) {
      IModelCustom model = MAP.get(name);
      if (model != null) {
         model.renderPart(partName);
      }
   }

   public static void renderLine(String path, String name, int startLine, int maxLine) {
      IModelCustom model = MAP.get(path + "/" + name);
      if (model instanceof W_ModelCustom) {
         ((W_ModelCustom) model).renderAllLine(startLine, maxLine);
      }
   }



   public static void render(String path, String name, int startFace, int maxFace) {
      IModelCustom model = MAP.get(path + "/" + name);
      if (model instanceof W_ModelCustom) {
         ((W_ModelCustom) model).renderAll(startFace, maxFace);
      }
   }

   public static int getVertexNum(String path, String name) {
      IModelCustom model = MAP.get(path + "/" + name);
      return (model instanceof W_ModelCustom)
              ? ((W_ModelCustom) model).getVertexNum()
              : 0;
   }

   public static W_ModelCustom get(String path, String name) {
      IModelCustom model = MAP.get(path + "/" + name);
      return (model instanceof W_ModelCustom)
              ? (W_ModelCustom) model
              : null;
   }

   public static W_ModelCustom getRandom() {
      Object[] values = MAP.values().toArray();
      if (values.length == 0) return null;

      for (int i = 0; i < 10; i++) {
         Object obj = values[RAND.nextInt(values.length)];
         if (obj instanceof W_ModelCustom) {
            return (W_ModelCustom) obj;
         }
      }

      return null;
   }

   public static boolean containsModel(String path, String name) {
      return containsModel(path + "/" + name);
   }

   public static boolean containsModel(String name) {
      return MAP.containsKey(name);
   }
}
