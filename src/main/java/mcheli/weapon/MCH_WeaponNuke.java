package mcheli.weapon;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponNuke extends MCH_WeaponBomb {
	public MCH_WeaponNuke(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
		super(w,v,yaw,pitch,nm,wi);
	}

	@Override
	public boolean shot(MCH_WeaponParam prm) {
		if (!this.worldObj.isRemote) {
			System.out.println("Dropping");
			playSound(prm.entity);
			MCH_EntityNuke e = new MCH_EntityNuke(this.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, this.acceleration);
			e.setName(this.name);
			e.setParameterFromWeapon(this, prm.entity, prm.user);
			e.motionX = prm.entity.motionX;
			e.motionY = prm.entity.motionY;
			e.motionZ = prm.entity.motionZ;
			//e.getInfo().explosionAltitude = ((MCH_EntityAircraft)prm.entity).airburst;
			this.worldObj.spawnEntityInWorld(e);
		} 
		return true;
	}
}
