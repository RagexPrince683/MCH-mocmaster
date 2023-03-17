package mcheli.weapon;

import com.hbm.entity.effect.EntityNukeCloudSmall;
import com.hbm.entity.logic.EntityNukeExplosionMK4;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityNuke extends MCH_EntityBomb{
	public MCH_EntityNuke(World par1World) { super(par1World); }
	public MCH_EntityNuke(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) { super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration); }
	
	@Override
	public void onImpact(MovingObjectPosition pos, float damageFactor){
		//System.out.println("old power: " + this.getPower());
		//this.setPower(this.getPower() * (1+this.getInfo().explosionAltitude/64));
		//System.out.println("new power: " + this.getPower());
		//System.out.println("MOC YOU MOTHERFUCKER");
		worldObj.spawnEntityInWorld(EntityNukeExplosionMK4.statFac(worldObj, this.getPower(), this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5));
		worldObj.spawnEntityInWorld(EntityNukeCloudSmall.statFac(worldObj, this.posX, this.posY, this.posZ, this.getPower() * 10));
		this.setDead();
	}
}
