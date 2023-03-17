package mcheli.particles;

import com.hbm.particle.ParticleRocketFlame;
import cpw.mods.fml.client.FMLClientHandler;
import mcheli.wrapper.W_Particle;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.entity.fx.RocketFx;

public class MCH_ParticlesUtil {

   public static MCH_EntityParticleMarkPoint markPoint = null;


   public static void spawnParticleExplode(World w, double x, double y, double z, float size, float r, float g, float b, float a, int age) {
      MCH_EntityParticleExplode epe = new MCH_EntityParticleExplode(w, x, y, z, (double)size, (double)age, 0.0D);
      epe.setParticleMaxAge(age);
      epe.setRBGColorF(r, g, b);
      epe.setAlphaF(a);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(epe);
   }

   public static void spawnParticleExplodeLarge(World w, double x, double y, double z, float size, float r, float g, float b, float a, int age) {
      MCH_EntityParticleExplodeLarge epe = new MCH_EntityParticleExplodeLarge(w, x, y, z, (double)size, (double)age, 0.0D);
      epe.setParticleMaxAge(age);
      epe.setRBGColorF(r, g, b);
      epe.setAlphaF(a);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(epe);
   }

   public static void spawnParticleDarkSmoke(World w, double x, double y, double z, float size, float r, float g, float b, float a, int age) {
      MCH_EntityParticleDarkSmoke epe = new MCH_EntityParticleDarkSmoke(w, x, y, z, size, age, 0.0D);
      epe.setRBGColorF(r, g, b);
      epe.setAlphaF(a);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(epe);
   }


      public static void spawnParticleTileCrack(World w, int blockX, int blockY, int blockZ, double x, double y, double z, double mx, double my, double mz) {
      String name = W_Particle.getParticleTileCrackName(w, blockX, blockY, blockZ);
      if(!name.isEmpty()) {
         DEF_spawnParticle(name, x, y, z, mx, my, mz, 20.0F);
      }

   }

   public static void spawnParticleRocketFx(World w, double x, double y, double z, double motx, double moty, double motz, float size){
      RocketFx fx = new RocketFx(w, x, y, z, motx, moty, motz, size);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public static void spawnParticleRocketFlame(World w, double x, double y, double z, float size) {
      ParticleRocketFlame fx = new ParticleRocketFlame(Minecraft.getMinecraft().renderEngine, w, x + w.rand.nextGaussian() * size /2 , y, z + w.rand.nextGaussian() * size / 2);
      fx.motionY = 0.75 + w.rand.nextDouble() * 0.5;
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public static void spawnParticleRocketSmoke(World w, double x, double y, double z, double motx, double moty, double motz, float size){
      MCH_EntityParticleRocket fx = new MCH_EntityParticleRocket(w, x, y, z, motx, moty, motz, size);
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
   }

   public static void spawnParticleFlame(World world, double x, double y, double z, double size, int age, boolean onGround) {
      MCH_EntityParticleFlame epe = new MCH_EntityParticleFlame(world, x, y , z, size, (double)age, 0.0D);
      epe.nowCount = age - 11;
      epe.setParticleMaxAge(age);
      epe.setRBGColorF(100, 100, 100);
      epe.setAlphaF(100);
      if(onGround) {
         epe.setVelocity(0, 0.05, 0);
      }
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(epe);
   }



   public static boolean spawnParticleTileDust(World w, int blockX, int blockY, int blockZ, double x, double y, double z, double mx, double my, double mz, float scale) {
      boolean ret = false;
      int[][] offset = new int[][]{{0, 0, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
      int len = offset.length;

      for(int i = 0; i < len; ++i) {
         String name = W_Particle.getParticleTileDustName(w, blockX + offset[i][0], blockY + offset[i][1], blockZ + offset[i][2]);
         if(!name.isEmpty()) {
            EntityFX e = DEF_spawnParticle(name, x, y, z, mx, my, mz, 20.0F);
            if(e instanceof MCH_EntityBlockDustFX) {
               ((MCH_EntityBlockDustFX)e).setScale(scale * 2.0F);
               ret = true;
               break;
            }
         }
      }

      return ret;
   }

   public static EntityFX DEF_spawnParticle(String s, double x, double y, double z, double mx, double my, double mz, float dist) {
      EntityFX e = doSpawnParticle(s, x, y, z, mx, my, mz);
      if(e != null) {
         e.renderDistanceWeight *= (double)dist;
      }

      return e;
   }

   public static EntityFX doSpawnParticle(String name, double x, double y, double z, double p_72726_8_, double p_72726_10_, double p_72726_12_) {
      Minecraft mc = Minecraft.getMinecraft();
      RenderGlobal renderGlobal = mc.renderGlobal;
      if(mc != null && mc.renderViewEntity != null && mc.effectRenderer != null) {
         int i = mc.gameSettings.particleSetting;
         if(i == 1 && mc.theWorld.rand.nextInt(3) == 0) {
            i = 2;
         }

         double d6 = mc.renderViewEntity.posX - x;
         double d7 = mc.renderViewEntity.posY - y;
         double d8 = mc.renderViewEntity.posZ - z;
         Object entityfx = null;
         if(name.equalsIgnoreCase("hugeexplosion")) {
        	 spawnParticleExplode(mc.theWorld, x, y, z, 10, 0, 0, 0, 0, 10);
        	 //mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityHugeExplodeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_)));
         } else if(name.equalsIgnoreCase("largeexplode")) {
        	 spawnParticleExplodeLarge(mc.theWorld, x, y, z, 10, 0, 0, 0, 0, 10);

            mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityLargeExplodeFX(mc.renderEngine, mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_)));
         } else if(name.equalsIgnoreCase("fireworksSpark")) {
            mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityFireworkSparkFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, mc.effectRenderer)));
         } else if(name.equalsIgnoreCase("darksmoke")) {
            MCH_EntityParticleDarkSmoke epe = new MCH_EntityParticleDarkSmoke(mc.theWorld, x, y, z, 1, (int)(8.0D / (Math.random() * 0.8D + 0.3D) * 2.5*2), 0.0D);
            epe.setRBGColorF(100, 100, 100);
            epe.setAlphaF(100);
            FMLClientHandler.instance().getClient().effectRenderer.addEffect(epe);
         }else if(name.equalsIgnoreCase("rocket")){
            spawnParticleRocketSmoke(mc.theWorld, x, y, z, 0, 0, 0, 1);
         }

         if(entityfx != null) {
            return (EntityFX)entityfx;
         } else {
            double d9 = 300.0D;
            if(d6 * d6 + d7 * d7 + d8 * d8 > d9 * d9) {
               return null;
            } else if(i > 1) {
               return null;
            } else {

               if(name.equalsIgnoreCase("bubble")) {
                  entityfx = new EntityBubbleFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("suspended")) {
                  entityfx = new EntitySuspendFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("depthsuspend")) {
                  entityfx = new EntityAuraFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("townaura")) {
                  entityfx = new EntityAuraFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("crit")) {
                  entityfx = new EntityCritFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("magicCrit")) {
                  entityfx = new EntityCritFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setRBGColorF(((EntityFX)entityfx).getRedColorF() * 0.3F, ((EntityFX)entityfx).getGreenColorF() * 0.8F, ((EntityFX)entityfx).getBlueColorF());
                  ((EntityFX)entityfx).nextTextureIndexX();
               } else if(name.equalsIgnoreCase("smoke")) {
                  entityfx = new EntitySmokeFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("mobSpell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, x, y, z, 0.0D, 0.0D, 0.0D);
                  ((EntityFX)entityfx).setRBGColorF((float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(name.equalsIgnoreCase("mobSpellAmbient")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, x, y, z, 0.0D, 0.0D, 0.0D);
                  ((EntityFX)entityfx).setAlphaF(0.15F);
                  ((EntityFX)entityfx).setRBGColorF((float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(name.equalsIgnoreCase("spell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("instantSpell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntitySpellParticleFX)entityfx).setBaseSpellTextureIndex(144);
               } else if(name.equalsIgnoreCase("witchMagic")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntitySpellParticleFX)entityfx).setBaseSpellTextureIndex(144);
                  float k = mc.theWorld.rand.nextFloat() * 0.5F + 0.35F;
                  ((EntityFX)entityfx).setRBGColorF(1.0F * k, 0.0F * k, 1.0F * k);
               } else if(name.equalsIgnoreCase("note")) {
                  entityfx = new EntityNoteFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("portal")) {
                  entityfx = new EntityPortalFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("enchantmenttable")) {
                  entityfx = new EntityEnchantmentTableParticleFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("explode")) {
                  entityfx = new EntityExplodeFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("flame")) {
                  entityfx = new EntityFlameFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("lava")) {
                  entityfx = new EntityLavaFX(mc.theWorld, x, y, z);
               } else if(name.equalsIgnoreCase("footstep")) {
                  entityfx = new EntityFootStepFX(mc.renderEngine, mc.theWorld, x, y, z);
               } else if(name.equalsIgnoreCase("splash")) {
                  entityfx = new EntitySplashFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("wake")) {
                  entityfx = new EntityFishWakeFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("largesmoke")) {
                  entityfx = new EntitySmokeFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, 2.5F);
               } else if(name.equalsIgnoreCase("cloud")) {
                  entityfx = new EntityCloudFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("reddust")) {
                  entityfx = new EntityReddustFX(mc.theWorld, x, y, z, (float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(name.equalsIgnoreCase("snowballpoof")) {
                  entityfx = new EntityBreakingFX(mc.theWorld, x, y, z, Items.snowball);
               } else if(name.equalsIgnoreCase("dripWater")) {
                  entityfx = new EntityDropParticleFX(mc.theWorld, x, y, z, Material.water);
               } else if(name.equalsIgnoreCase("dripLava")) {
                  entityfx = new EntityDropParticleFX(mc.theWorld, x, y, z, Material.lava);
               } else if(name.equalsIgnoreCase("snowshovel")) {
                  entityfx = new EntitySnowShovelFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("slime")) {
                  entityfx = new EntityBreakingFX(mc.theWorld, x, y, z, Items.slime_ball);
               } else if(name.equalsIgnoreCase("heart")) {
                  entityfx = new EntityHeartFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(name.equalsIgnoreCase("angryVillager")) {
                  entityfx = new EntityHeartFX(mc.theWorld, x, y + 0.5D, z, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setParticleTextureIndex(81);
                  ((EntityFX)entityfx).setRBGColorF(1.0F, 1.0F, 1.0F);
               } else if(name.equalsIgnoreCase("happyVillager")) {
                  entityfx = new EntityAuraFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setParticleTextureIndex(82);
                  ((EntityFX)entityfx).setRBGColorF(1.0F, 1.0F, 1.0F);
               } else {
                  String[] astring;
                  int k1;
                  if(name.startsWith("iconcrack_")) {
                     astring = name.split("_", 3);
                     int block = Integer.parseInt(astring[1]);
                     if(astring.length > 2) {
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = new EntityBreakingFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, Item.getItemById(block), k1);
                     } else {
                        entityfx = new EntityBreakingFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, Item.getItemById(block), 0);
                     }
                  } else {
                     Block block1;
                     if(name.startsWith("blockcrack_")) {
                        astring = name.split("_", 3);
                        block1 = Block.getBlockById(Integer.parseInt(astring[1]));
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = (new EntityDiggingFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, block1, k1)).applyRenderColor(k1);
                     } else if(name.startsWith("blockdust_")) {
                        astring = name.split("_", 3);
                        block1 = Block.getBlockById(Integer.parseInt(astring[1]));
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = (new MCH_EntityBlockDustFX(mc.theWorld, x, y, z, p_72726_8_, p_72726_10_, p_72726_12_, block1, k1)).applyRenderColor(k1);
                     }
                  }
               }

               if(entityfx != null) {
                  mc.effectRenderer.addEffect((EntityFX)entityfx);
               }

               return (EntityFX)entityfx;
            }
         }
      } else {
         return null;
      }
   }

   public static void spawnParticle(MCH_ParticleParam p) {
      if(p.world.isRemote) {
         Object entityFX = null;
         if(p.name.equalsIgnoreCase("Splash")) {
            entityFX = new MCH_EntityParticleSplash(p.world, p.posX, p.posY, p.posZ, p.motionX, p.motionY, p.motionZ);
         } else {
            entityFX = new MCH_EntityParticleSmoke(p.world, p.posX, p.posY, p.posZ, p.motionX, p.motionY, p.motionZ);
         }

         ((MCH_EntityParticleBase)entityFX).setRBGColorF(p.r, p.g, p.b);
         ((MCH_EntityParticleBase)entityFX).setAlphaF(p.a);
         if(p.age > 0) {
            ((MCH_EntityParticleBase)entityFX).setParticleMaxAge(p.age);
         }

         ((MCH_EntityParticleBase)entityFX).moutionYUpAge = p.motionYUpAge;
         ((MCH_EntityParticleBase)entityFX).gravity = p.gravity;
         ((MCH_EntityParticleBase)entityFX).isEffectedWind = p.isEffectWind;
         ((MCH_EntityParticleBase)entityFX).diffusible = p.diffusible;
         ((MCH_EntityParticleBase)entityFX).toWhite = p.toWhite;
         if(p.diffusible) {
            ((MCH_EntityParticleBase)entityFX).setParticleScale(p.size * 0.2F);
            ((MCH_EntityParticleBase)entityFX).particleMaxScale = p.size * 2.0F;
         } else {
            ((MCH_EntityParticleBase)entityFX).setParticleScale(p.size);
         }

         FMLClientHandler.instance().getClient().effectRenderer.addEffect((EntityFX)entityFX);
      }

   }

   public static void spawnMarkPoint(EntityPlayer player, double x, double y, double z) {
      clearMarkPoint();
      markPoint = new MCH_EntityParticleMarkPoint(player.worldObj, x, y, z, player.getTeam());
      FMLClientHandler.instance().getClient().effectRenderer.addEffect(markPoint);
   }

   public static void clearMarkPoint() {
      if(markPoint != null) {
         markPoint.setDead();
         markPoint = null;
      }

   }

}
