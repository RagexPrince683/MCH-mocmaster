package mcheli.weapon;

import mcheli.MCH_Explosion;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponBomb extends MCH_WeaponBase {

   public MCH_WeaponBomb(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 0.5F;
      super.explosionPower = 9;
      super.power = 35;
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   
   
   public void update(int countWait) {
	   super.update(countWait);
	   
	   doCCIP();
   }
   
   public void doCCIP() {
	   Vec3 pos = Vec3.createVectorHelper(this.position.xCoord, this.position.yCoord, this.position.zCoord);
	   pos = MCH_Lib.RotVec3(pos, -aircraft.rotationYaw, -aircraft.rotationPitch, -aircraft.rotationRoll);
	   double vx, vy, vz, x, y, z;
	   vx = aircraft.motionX;
	   vy = aircraft.motionY;
	   vz = aircraft.motionZ;
	   x = aircraft.posX + pos.xCoord;
	   y = aircraft.posY + pos.yCoord;
	   z = aircraft.posZ + pos.zCoord;
	   
	   for(int i = 0; i<= 1000; i++) {
		   
		   vy += super.weaponInfo.gravity;
		   x += vx * this.acceleration;
		   y += vy * this.acceleration;
		   z += vz * this.acceleration;
		   vx *= 0.999;
		   vz *= 0.999;
		   
		   Block b = MCH_Lib.getBlockY(worldObj, x, y, z, 1, 1, true);
		   if(b != null && b != Blocks.air) {
			   try {
				   aircraft.target.xCoord = x;
				   aircraft.target.yCoord = y;
				   aircraft.target.zCoord = z;
			   }catch(Exception e){
				   aircraft.print(e.toString());
			   }
			   return;
		   }
	   }
   }
   
   public boolean shot(MCH_WeaponParam prm) {
      if(this.getInfo() != null && this.getInfo().destruct) {
        // if(prm.entity instanceof MCH_EntityHeli) {
            MCH_EntityAircraft e1 = (MCH_EntityAircraft)prm.entity;
            //if(e1.isUAV() && e1.getSeatNum() == 0) {
               if(!super.worldObj.isRemote) {
                  MCH_Explosion.newExplosion(super.worldObj, (Entity)null, prm.user, e1.posX, e1.posY, e1.posZ, (float)this.getInfo().explosion, (float)this.getInfo().explosionBlock, true, true, this.getInfo().flaming, true, 0);
                  this.playSound(prm.entity);
               }

               e1.destruct();
            //}
        // }
      } else if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.motionX = prm.entity.motionX;
         e.motionY = prm.entity.motionY;
         e.motionZ = prm.entity.motionZ;
         super.worldObj.spawnEntityInWorld(e);
      }

      return true;
   }
}
