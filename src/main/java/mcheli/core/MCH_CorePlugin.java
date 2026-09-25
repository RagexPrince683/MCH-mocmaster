package mcheli.core;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@IFMLLoadingPlugin.Name("MCHeli mixin loader")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("mcheli.core")
public final class MCH_CorePlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

   private static final String MIXIN_CONFIG = "mixins.mcheli.json";

   @Override
   public String[] getASMTransformerClass() {
      return new String[0];
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

   @Override
   public String getMixinConfig() {
      return MIXIN_CONFIG;
   }

   @Override
   public List<String> getMixins(Set<String> loadedCoreMods) {
      return FMLLaunchHandler.side().isClient()
            ? Arrays.asList("MovementInputFromOptionsMixin", "EntityClientPlayerMPMixin", "EntityPlayerMixin")
            : Arrays.asList("EntityPlayerMixin");
   }
}
