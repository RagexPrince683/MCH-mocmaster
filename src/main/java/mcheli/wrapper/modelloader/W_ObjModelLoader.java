package mcheli.wrapper.modelloader;

import java.net.URL;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.IModelCustomLoader;
import net.minecraftforge.client.model.ModelFormatException;

public class W_ObjModelLoader implements IModelCustomLoader {

   private static final String[] types = new String[]{"obj"};


   public String getType() {
      return "OBJ model";
   }

   public String[] getSuffixes() {
      return types;
   }

   public IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
      return new W_WavefrontObject(resource);
   }

   public IModelCustom loadInstance(String resourceName, URL resource) throws ModelFormatException {
      return new W_WavefrontObject(resourceName, resource);
   }

}
