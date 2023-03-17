package mcheli.particles;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityBlockDustFX;
import net.minecraft.world.World;

public class MCH_EntityBlockDustFX extends EntityBlockDustFX {

   public MCH_EntityBlockDustFX(World p_i45072_1_, double p_i45072_2_, double p_i45072_4_, double p_i45072_6_, double p_i45072_8_, double p_i45072_10_, double p_i45072_12_, Block p_i45072_14_, int p_i45072_15_) {
      super(p_i45072_1_, p_i45072_2_, p_i45072_4_, p_i45072_6_, p_i45072_8_, p_i45072_10_, p_i45072_12_, p_i45072_14_, p_i45072_15_);
   }

   public void setScale(float s) {
      super.particleScale = s;
   }
}
