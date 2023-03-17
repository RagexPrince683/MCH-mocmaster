package mcheli.particles;

import net.minecraft.world.World;
import zmaster587.advancedRocketry.entity.fx.RocketFx;

public class MCH_EntityParticleRocket extends RocketFx {
    public MCH_EntityParticleRocket(World world, double x, double y, double z, double motx, double moty, double motz, float scale) {
        super(world, x, y, z, motx, moty, motz, scale);
    }

    public MCH_EntityParticleRocket(World world, double x, double y, double z, double motx, double moty, double motz) {
        super(world, x, y, z, motx, moty, motz);
    }

    @Override
    public void onUpdate(){
        super.onUpdate();

        if(this.particleAge >= particleMaxAge-1) {
            MCH_ParticlesUtil.spawnParticleDarkSmoke(this.worldObj, this.posX, this.posY, posZ, 1, this.particleRed, particleGreen, particleBlue, particleAlpha, (int) (8.0D / (Math.random() * 0.8D + 0.3D) * 2.5 * 2));
        }
    }
}
