package mcheli;

import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class MCH_TextureManagerDummy extends TextureManager {

   public static ResourceLocation R = new ResourceLocation("mcheli", "textures/test.png");
   private TextureManager tm;


   public MCH_TextureManagerDummy(TextureManager t) {
      super((IResourceManager)null);
      this.tm = t;
   }

   public void bindTexture(ResourceLocation resouce) {
      if(MCH_ClientCommonTickHandler.cameraMode == 2) {
         this.tm.bindTexture(R);
      } else {
         this.tm.bindTexture(resouce);
      }

   }

   public ResourceLocation getResourceLocation(int p_130087_1_) {
      return this.tm.getResourceLocation(p_130087_1_);
   }

   public boolean loadTextureMap(ResourceLocation p_130088_1_, TextureMap p_130088_2_) {
      return this.tm.loadTextureMap(p_130088_1_, p_130088_2_);
   }

   public boolean loadTickableTexture(ResourceLocation p_110580_1_, ITickableTextureObject p_110580_2_) {
      return this.tm.loadTickableTexture(p_110580_1_, p_110580_2_);
   }

   public boolean loadTexture(ResourceLocation p_110579_1_, ITextureObject p_110579_2_) {
      return this.tm.loadTexture(p_110579_1_, p_110579_2_);
   }

   public ITextureObject getTexture(ResourceLocation p_110581_1_) {
      return this.tm.getTexture(p_110581_1_);
   }

   public ResourceLocation getDynamicTextureLocation(String p_110578_1_, DynamicTexture p_110578_2_) {
      return this.tm.getDynamicTextureLocation(p_110578_1_, p_110578_2_);
   }

   public void tick() {
      this.tm.tick();
   }

   public void deleteTexture(ResourceLocation p_147645_1_) {
      this.tm.deleteTexture(p_147645_1_);
   }

   public void onResourceManagerReload(IResourceManager p_110549_1_) {
      this.tm.onResourceManagerReload(p_110549_1_);
   }

}
