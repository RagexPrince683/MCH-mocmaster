package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponAShM extends MCH_WeaponBase {

   public MCH_WeaponAShM(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
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

   public static double[] getRelOffset(double x, double z, double angle, double offX, double offZ) {
		angle = Math.toRadians(angle);
		double xPrime = x * Math.cos(angle) + z * Math.sin(angle);
		double zPrime = -x  * Math.sin(angle) + z * Math.cos(angle);
		
		zPrime += offZ;
		xPrime += offX;

		double x2 = xPrime * Math.cos(angle) - zPrime * Math.sin(angle);
		double z2 = xPrime  * Math.sin(angle) + zPrime * Math.cos(angle);
		
		return new double[] {x2,z2};
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
      MCH_EntityAShM e;
      if(prm.entity instanceof MCH_EntityAircraft) {
				//System.out.println("Yeet");
				e = new MCH_EntityAShM(this.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, this.acceleration);
				e.setName(this.name);    
				e.setParameterFromWeapon(this, prm.entity, prm.user);
				double[] target = getRelOffset(prm.posX, prm.posZ, yaw, 0, Math.min(this.weaponInfo.radius, 150));
				double[] target2 = getRelOffset(prm.posX, prm.posZ, yaw, 0, (prm.entity.posY - 65)*2);
				e.setTarget(target[0], target[1], target2[0], target2[1]);
				this.worldObj.spawnEntityInWorld(e);
				playSound(prm.entity);
				return true;

		}
      return false;
   }
}
