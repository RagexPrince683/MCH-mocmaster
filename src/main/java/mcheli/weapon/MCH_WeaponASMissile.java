package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.command.MCH_PacketCommandSave;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.particles.MCH_EntityParticleMarkPoint;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_WeaponASMissile extends MCH_WeaponBase {
    Random random = new Random();


   public MCH_WeaponASMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 3.0F;
      super.explosionPower = 9;
      super.power = 40;
      super.interval = -350;
      if(w.isRemote) {
         super.interval -= 10;
      }
   }

   public boolean isCooldownCountReloadTime() {
      return true;
   }

   public void update(int countWait) {
      super.update(countWait);
   }

   public boolean shot(MCH_WeaponParam prm) {
      float yaw = prm.user.rotationYaw;
      float pitch = prm.user.rotationPitch;
      double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      if(super.worldObj.isRemote) {
         tX = tX * 200.0D / dist;
         tY = tY * 200.0D / dist;
         tZ = tZ * 200.0D / dist;
      } else {
         tX = tX * 250.0D / dist;
         tY = tY * 250.0D / dist;
         tZ = tZ * 250.0D / dist;
      }

      Vec3 src = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX, prm.entity.posY + 1.62D, prm.entity.posZ);
      Vec3 dst = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX + tX, prm.entity.posY + 1.62D + tY, prm.entity.posZ + tZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, src, dst);
      MCH_EntityASMissile e;
      
      if(prm.entity instanceof MCH_EntityAircraft) {
    	  MCH_EntityAircraft ac = (MCH_EntityAircraft)prm.entity;
    	  if(this.worldObj.isRemote) {
    		  if(MCH_ParticlesUtil.markPoint != null) {
  				
  				//System.out.println("Yeet");
  				MCH_EntityParticleMarkPoint target = MCH_ParticlesUtil.markPoint;
  				if(target.posY >= 500){
                    MCH_Multiplay.markPoint((EntityPlayer)prm.user, prm.posX, prm.posY, prm.posZ);
  				}
  				if(ac.getDistance(target.posX, ac.posY, target.posZ) > this.getInfo().radius) {return false;}
  				MCH_PacketCommandSave.send("tgt " + (int)target.posX + " " + (int)target.posY + " " + (int)target.posZ);
  				return true;
  			}{return false;}
    	  }else { //Server
    		  e = new MCH_EntityASMissile(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, this.acceleration);
    		  e.setName(this.name);    
    		  e.setParameterFromWeapon(this, prm.entity, prm.user);
    		  //System.out.println("AC tgt " + ac.target[0] + " "+ ac.target[1] + " "+ ac.target[2]);


    		  e.targetPosX = ac.target.xCoord  + random.nextGaussian() * this.weaponInfo.accuracy;
    		  e.targetPosY = ac.target.yCoord;
    		  e.targetPosZ = ac.target.zCoord  + random.nextGaussian() * this.weaponInfo.accuracy;
    		  this.worldObj.spawnEntityInWorld(e);
    		  playSound(prm.entity);
    		  return true;
    	  }
		}
      return false;
   }
}
