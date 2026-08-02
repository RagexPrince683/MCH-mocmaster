package mcheli.vehicle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import mcheli.MCH_Lib;
import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_RenderBaseVehicle;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderTurret extends MCH_RenderBaseVehicle {

   public MCH_RenderTurret() {
      super.shadowSize = 2.0F;
   }

   public void renderBaseVehicle(MCH_EntityBaseVehicle entity, double posX, double posY, double posZ, float yaw, float pitch, float roll, float tickTime) {
      MCH_TurretInfo turretInfo = null;
      if(entity != null && entity instanceof MCH_EntityTurret) {
         MCH_EntityTurret vehicle = (MCH_EntityTurret)entity;
         turretInfo = vehicle.getTurretInfo();
         if(turretInfo != null) {
            this.renderDebugHitBox(vehicle, posX, posY, posZ, yaw, pitch, roll);
            this.renderDebugPilotSeat(vehicle, posX, posY, posZ, yaw, pitch, roll);
            GL11.glTranslated(posX, posY, posZ);
            GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            try {
            this.bindTexture(MCH_EntityBaseVehicle.getTexturePath(turretInfo.getDirectoryName(), vehicle.getTextureName()), vehicle);
            } catch (Exception var15) {
               System.out.println("Texture not found : " + vehicle.getTextureName());
               this.bindTexture(new ResourceLocation("textures/blocks/planks_oak.png"));
            }
            renderBodyWithSkinOverlay(turretInfo.model, turretInfo.getDirectoryName(), vehicle);
            MCH_WeaponSet ws = vehicle.getFirstSeatWeapon();
            this.drawPart(vehicle, turretInfo, yaw, pitch, ws, tickTime);
         }
      }
   }

   public void drawPart(MCH_EntityTurret vehicle, MCH_TurretInfo info, float yaw, float pitch, MCH_WeaponSet ws, float tickTime) {
      float rotBrl = ws.prevRotBarrel + (ws.rotBarrel - ws.prevRotBarrel) * tickTime;
      int index = 0;

      MCH_TurretInfo.VPart vp;
      for(Iterator i$ = info.partList.iterator(); i$.hasNext(); index = this.drawPart(vp, vehicle, info, yaw, pitch, rotBrl, tickTime, ws, index)) {
         vp = (MCH_TurretInfo.VPart)i$.next();
      }

   }

   int drawPart(MCH_TurretInfo.VPart vp, MCH_EntityTurret vehicle, MCH_TurretInfo info, float yaw, float pitch, float rotBrl, float tickTime, MCH_WeaponSet ws, int index) {
      GL11.glPushMatrix();
      float recoilBuf = 0.0F;
      if(index < ws.getWeaponNum()) {
         MCH_WeaponSet.Recoil bkIndex = ws.recoilBuf[index];
         recoilBuf = bkIndex.prevRecoilBuf + (bkIndex.recoilBuf - bkIndex.prevRecoilBuf) * tickTime;
      }

      if(vp.rotPitch || vp.rotYaw || vp.type == 1) {
         GL11.glTranslated(vp.pos.xCoord, vp.pos.yCoord, vp.pos.zCoord);
         if(vp.rotYaw) {
            GL11.glRotatef(-vehicle.getLastRiderYaw() + yaw, 0.0F, 1.0F, 0.0F);
         }

         if(vp.rotPitch) {
            float i$ = MCH_Lib.RNG(vehicle.getLastRiderPitch(), info.minRotationPitch, info.maxRotationPitch);
            GL11.glRotatef(i$ - pitch, 1.0F, 0.0F, 0.0F);
         }

         if(vp.type == 1) {
            GL11.glRotatef(rotBrl, 0.0F, 0.0F, -1.0F);
         }

         GL11.glTranslated(-vp.pos.xCoord, -vp.pos.yCoord, -vp.pos.zCoord);
      }

      if(vp.type == 2) {
         GL11.glTranslated(0.0D, 0.0D, (double)(-vp.recoilBuf * recoilBuf));
      }

      if(vp.type == 2 || vp.type == 3) {
         ++index;
      }

      MCH_TurretInfo.VPart vcp;
      if(vp.child != null) {
         for(Iterator var14 = vp.child.iterator(); var14.hasNext(); index = this.drawPart(vcp, vehicle, info, yaw, pitch, rotBrl, recoilBuf, ws, index)) {
            vcp = (MCH_TurretInfo.VPart)var14.next();
         }
      }

      if((vp.drawFP || !W_Lib.isClientPlayer(vehicle.riddenByEntity) || !W_Lib.isFirstPerson()) && (vp.type != 3 || !vehicle.isWeaponNotCooldown(ws, index))) {
         renderPart(vp.model, info.model, vp.modelName);
         MCH_ModelManager.render(info.getDirectoryName(), vp.modelName);
      }

      GL11.glPopMatrix();
      return index;
   }

   public static void drawSnapshotParts(MCH_TurretInfo info, float hullYaw, float hullPitch, float aimYaw,
      float aimPitch, float barrelRotation, PacketVehicleLODSnapshot.TurretPartPose[] poses, float tickTime) {
      SnapshotCursor cursor = new SnapshotCursor();
      for(Object object : info.partList) drawSnapshotPart((MCH_TurretInfo.VPart)object, info, hullYaw, hullPitch,
         aimYaw, aimPitch, barrelRotation, poses, tickTime, cursor);
   }

   private static void drawSnapshotPart(MCH_TurretInfo.VPart part, MCH_TurretInfo info, float hullYaw,
      float hullPitch, float aimYaw, float aimPitch, float barrelRotation,
      PacketVehicleLODSnapshot.TurretPartPose[] poses, float tickTime, SnapshotCursor cursor) {
      int poseIndex = cursor.part++;
      PacketVehicleLODSnapshot.TurretPartPose pose = poseIndex < poses.length ? poses[poseIndex] : null;
      GL11.glPushMatrix();
      try {
         if(part.rotPitch || part.rotYaw || part.type == 1) {
            GL11.glTranslated(part.pos.xCoord, part.pos.yCoord, part.pos.zCoord);
            if(part.rotYaw) GL11.glRotatef(-aimYaw + hullYaw, 0.0F, 1.0F, 0.0F);
            if(part.rotPitch) GL11.glRotatef(MCH_Lib.RNG(aimPitch, info.minRotationPitch, info.maxRotationPitch) - hullPitch, 1.0F, 0.0F, 0.0F);
            if(part.type == 1) GL11.glRotatef(barrelRotation, 0.0F, 0.0F, -1.0F);
            GL11.glTranslated(-part.pos.xCoord, -part.pos.yCoord, -part.pos.zCoord);
         }
         if(part.type == 2 && pose != null) {
            float recoil = pose.prevRecoil + (pose.recoil - pose.prevRecoil) * tickTime;
            GL11.glTranslated(0.0D, 0.0D, -part.recoilBuf * recoil);
         }
         if(part.child != null) for(Object child : part.child) drawSnapshotPart((MCH_TurretInfo.VPart)child,
            info, hullYaw, hullPitch, aimYaw, aimPitch, barrelRotation, poses, tickTime, cursor);
         if(pose == null || pose.visible) {
            renderPart(part.model, info.model, part.modelName);
            MCH_ModelManager.render(info.getDirectoryName(), part.modelName);
         }
      } finally {
         GL11.glPopMatrix();
      }
   }

   private static final class SnapshotCursor { private int part; }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
