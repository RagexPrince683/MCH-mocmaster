package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderTvMissile extends MCH_RenderBulletBase {

   public MCH_RenderTvMissile() {
      super.shadowSize = 0.5F;
   }

   public void renderBullet(Entity entity, double posX, double posY, double posZ, float par8, float par9) {
      MCH_EntityAircraft ac = null;
      Entity ridingEntity = Minecraft.getMinecraft().thePlayer.ridingEntity;
      if(ridingEntity instanceof MCH_EntityAircraft) {
         ac = (MCH_EntityAircraft)ridingEntity;
      } else if(ridingEntity instanceof MCH_EntitySeat) {
         ac = ((MCH_EntitySeat)ridingEntity).getParent();
      } else if(ridingEntity instanceof MCH_EntityUavStation) {
         ac = ((MCH_EntityUavStation)ridingEntity).getControlAircract();
      }

      if(ac == null || ac.isRenderBullet(entity, Minecraft.getMinecraft().thePlayer)) {
         if(entity instanceof MCH_EntityBaseBullet) {
            MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)entity;
            GL11.glPushMatrix();
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
            this.renderModel(bullet);
            GL11.glPopMatrix();
         }

      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
