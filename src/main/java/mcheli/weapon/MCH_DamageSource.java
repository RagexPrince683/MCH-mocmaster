package mcheli.weapon;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IChatComponent;

public class MCH_DamageSource extends EntityDamageSource{

	public MCH_DamageSource(String p_i1566_1_, MCH_EntityBaseBullet bullet) {
		super(p_i1566_1_, bullet);
	}
	
	@Override
	public IChatComponent func_151519_b(EntityLivingBase entity) {
		String s = "%1$s got some courtesy of %2$s";
		
		return new ChatComponentText(s);
	}

}
