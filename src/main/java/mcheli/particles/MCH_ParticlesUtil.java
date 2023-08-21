package mcheli.particles;

import cpw.mods.fml.client.FMLClientHandler;
import mcheli.particles.MCH_EntityBlockDustFX;
import mcheli.particles.MCH_EntityParticleBase;
import mcheli.particles.MCH_EntityParticleExplode;
import mcheli.particles.MCH_EntityParticleMarkPoint;
import mcheli.particles.MCH_EntityParticleSmoke;
import mcheli.particles.MCH_EntityParticleSplash;
import mcheli.particles.MCH_ParticleParam;
import mcheli.wrapper.W_Particle;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityAuraFX;
import net.minecraft.client.particle.EntityBreakingFX;
import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.client.particle.EntityCritFX;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityDropParticleFX;
import net.minecraft.client.particle.EntityEnchantmentTableParticleFX;
import net.minecraft.client.particle.EntityExplodeFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFireworkSparkFX;
import net.minecraft.client.particle.EntityFishWakeFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntityFootStepFX;
import net.minecraft.client.particle.EntityHeartFX;
import net.minecraft.client.particle.EntityHugeExplodeFX;
import net.minecraft.client.particle.EntityLargeExplodeFX;
import net.minecraft.client.particle.EntityLavaFX;
import net.minecraft.client.particle.EntityNoteFX;
import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.client.particle.EntitySnowShovelFX;
import net.minecraft.client.particle.EntitySpellParticleFX;
import net.minecraft.client.particle.EntitySplashFX;
import net.minecraft.client.particle.EntitySuspendFX;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class MCH_ParticlesUtil {

   public static MCH_EntityParticleMarkPoint markPoint = null;


   public static void spawnParticleExplode(World w, double x, double y, double z, float size, float r, float g, float b, float a, int age) {
      MCH_EntityParticleExplode epe = new MCH_EntityParticleExplode(w, x, y, z, (double)size, (double)age, 0.0D);
      epe.setParticleMaxAge(age);
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

   public static EntityFX doSpawnParticle(String p_72726_1_, double p_72726_2_, double p_72726_4_, double p_72726_6_, double p_72726_8_, double p_72726_10_, double p_72726_12_) {
      Minecraft mc = Minecraft.getMinecraft();
      RenderGlobal renderGlobal = mc.renderGlobal;
      if(mc != null && mc.renderViewEntity != null && mc.effectRenderer != null) {
         int i = mc.gameSettings.particleSetting;
         if(i == 1 && mc.theWorld.rand.nextInt(3) == 0) {
            i = 2;
         }

         double d6 = mc.renderViewEntity.posX - p_72726_2_;
         double d7 = mc.renderViewEntity.posY - p_72726_4_;
         double d8 = mc.renderViewEntity.posZ - p_72726_6_;
         Object entityfx = null;
         if(p_72726_1_.equalsIgnoreCase("hugeexplosion")) {
            mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityHugeExplodeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_)));
         } else if(p_72726_1_.equalsIgnoreCase("largeexplode")) {
            mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityLargeExplodeFX(mc.renderEngine, mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_)));
         } else if(p_72726_1_.equalsIgnoreCase("fireworksSpark")) {
            mc.effectRenderer.addEffect((EntityFX) (entityfx = new EntityFireworkSparkFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, mc.effectRenderer)));
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
               if(p_72726_1_.equalsIgnoreCase("bubble")) {
                  entityfx = new EntityBubbleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("suspended")) {
                  entityfx = new EntitySuspendFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("depthsuspend")) {
                  entityfx = new EntityAuraFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("townaura")) {
                  entityfx = new EntityAuraFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("crit")) {
                  entityfx = new EntityCritFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("magicCrit")) {
                  entityfx = new EntityCritFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setRBGColorF(((EntityFX)entityfx).getRedColorF() * 0.3F, ((EntityFX)entityfx).getGreenColorF() * 0.8F, ((EntityFX)entityfx).getBlueColorF());
                  ((EntityFX)entityfx).nextTextureIndexX();
               } else if(p_72726_1_.equalsIgnoreCase("smoke")) {
                  entityfx = new EntitySmokeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("mobSpell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, 0.0D, 0.0D, 0.0D);
                  ((EntityFX)entityfx).setRBGColorF((float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("mobSpellAmbient")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, 0.0D, 0.0D, 0.0D);
                  ((EntityFX)entityfx).setAlphaF(0.15F);
                  ((EntityFX)entityfx).setRBGColorF((float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("spell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("instantSpell")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntitySpellParticleFX)entityfx).setBaseSpellTextureIndex(144);
               } else if(p_72726_1_.equalsIgnoreCase("witchMagic")) {
                  entityfx = new EntitySpellParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntitySpellParticleFX)entityfx).setBaseSpellTextureIndex(144);
                  float k = mc.theWorld.rand.nextFloat() * 0.5F + 0.35F;
                  ((EntityFX)entityfx).setRBGColorF(1.0F * k, 0.0F * k, 1.0F * k);
               } else if(p_72726_1_.equalsIgnoreCase("note")) {
                  entityfx = new EntityNoteFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("portal")) {
                  entityfx = new EntityPortalFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("enchantmenttable")) {
                  entityfx = new EntityEnchantmentTableParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("explode")) {
                  entityfx = new EntityExplodeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("flame")) {
                  entityfx = new EntityFlameFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("lava")) {
                  entityfx = new EntityLavaFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_);
               } else if(p_72726_1_.equalsIgnoreCase("footstep")) {
                  entityfx = new EntityFootStepFX(mc.renderEngine, mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_);
               } else if(p_72726_1_.equalsIgnoreCase("splash")) {
                  entityfx = new EntitySplashFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("wake")) {
                  entityfx = new EntityFishWakeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("largesmoke")) {
                  entityfx = new EntitySmokeFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, 2.5F);
               } else if(p_72726_1_.equalsIgnoreCase("cloud")) {
                  entityfx = new EntityCloudFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("reddust")) {
                  entityfx = new EntityReddustFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, (float)p_72726_8_, (float)p_72726_10_, (float)p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("snowballpoof")) {
                  entityfx = new EntityBreakingFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, Items.snowball);
               } else if(p_72726_1_.equalsIgnoreCase("dripWater")) {
                  entityfx = new EntityDropParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, Material.water);
               } else if(p_72726_1_.equalsIgnoreCase("dripLava")) {
                  entityfx = new EntityDropParticleFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, Material.lava);
               } else if(p_72726_1_.equalsIgnoreCase("snowshovel")) {
                  entityfx = new EntitySnowShovelFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("slime")) {
                  entityfx = new EntityBreakingFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, Items.slime_ball);
               } else if(p_72726_1_.equalsIgnoreCase("heart")) {
                  entityfx = new EntityHeartFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
               } else if(p_72726_1_.equalsIgnoreCase("angryVillager")) {
                  entityfx = new EntityHeartFX(mc.theWorld, p_72726_2_, p_72726_4_ + 0.5D, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setParticleTextureIndex(81);
                  ((EntityFX)entityfx).setRBGColorF(1.0F, 1.0F, 1.0F);
               } else if(p_72726_1_.equalsIgnoreCase("happyVillager")) {
                  entityfx = new EntityAuraFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_);
                  ((EntityFX)entityfx).setParticleTextureIndex(82);
                  ((EntityFX)entityfx).setRBGColorF(1.0F, 1.0F, 1.0F);
               } else {
                  String[] astring;
                  int k1;
                  if(p_72726_1_.startsWith("iconcrack_")) {
                     astring = p_72726_1_.split("_", 3);
                     int block = Integer.parseInt(astring[1]);
                     if(astring.length > 2) {
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = new EntityBreakingFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, Item.getItemById(block), k1);
                     } else {
                        entityfx = new EntityBreakingFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, Item.getItemById(block), 0);
                     }
                  } else {
                     Block block1;
                     if(p_72726_1_.startsWith("blockcrack_")) {
                        astring = p_72726_1_.split("_", 3);
                        block1 = Block.getBlockById(Integer.parseInt(astring[1]));
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = (new EntityDiggingFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, block1, k1)).applyRenderColor(k1);
                     } else if(p_72726_1_.startsWith("blockdust_")) {
                        astring = p_72726_1_.split("_", 3);
                        block1 = Block.getBlockById(Integer.parseInt(astring[1]));
                        k1 = Integer.parseInt(astring[2]);
                        entityfx = (new MCH_EntityBlockDustFX(mc.theWorld, p_72726_2_, p_72726_4_, p_72726_6_, p_72726_8_, p_72726_10_, p_72726_12_, block1, k1)).applyRenderColor(k1);
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
