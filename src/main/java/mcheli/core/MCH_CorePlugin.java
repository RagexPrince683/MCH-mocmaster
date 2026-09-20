package mcheli.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.Name("MCHeli input gate")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("mcheli.core")
public final class MCH_CorePlugin implements IFMLLoadingPlugin {

   @Override
   public String[] getASMTransformerClass() {
      return new String[]{MCH_DismountInputTransformer.class.getName()};
   }

   @Override
   public String getModContainerClass() {
      return null;
   }

   @Override
   public String getSetupClass() {
      return null;
   }

   @Override
   public void injectData(Map<String, Object> data) {
   }

   @Override
   public String getAccessTransformerClass() {
      return null;
   }
}
