package mcheli.wrapper.modelloader;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.IModelCustomLoader;
import net.minecraftforge.client.model.ModelFormatException;

import java.net.URL;

public class W_MqoModelLoader implements IModelCustomLoader {

   private static final String[] types = new String[]{"mqo"};


   public String getType() {
      return "Metasequoia model";
   }

   public String[] getSuffixes() {
      return types;
   }

   public IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
      return new W_MetasequoiaObject(resource);
   }

   public IModelCustom loadInstance(String resourceName, URL resource) throws ModelFormatException {
      return new W_MetasequoiaObject(resourceName, resource);
   }

}
