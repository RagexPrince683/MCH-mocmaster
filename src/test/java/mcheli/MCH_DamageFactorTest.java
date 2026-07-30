package mcheli;

import static org.junit.Assert.assertEquals;

import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityTurret;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import org.junit.Test;

public class MCH_DamageFactorTest {
   @Test
   public void usesPlayerFactorForPlayersVillagersAndLivingFallback() {
      MCH_DamageFactor factors = new MCH_DamageFactor();
      factors.add(EntityPlayer.class, 20.0F);

      assertFactor(20.0F, factors, EntityPlayer.class);
      assertFactor(20.0F, factors, EntityPlayerMP.class);
      assertFactor(20.0F, factors, EntityVillager.class);
      assertFactor(20.0F, factors, EntityCow.class);
      assertFactor(20.0F, factors, EntityZombie.class);
      assertFactor(20.0F, factors, ModdedLiving.class);
      assertFactor(1.0F, factors, Entity.class);
   }

   @Test
   public void usesOtherForNonPlayerLivingEntitiesOnly() {
      MCH_DamageFactor factors = new MCH_DamageFactor();
      factors.add(EntityPlayer.class, 20.0F);
      factors.add(EntityLivingBase.class, 3.0F);

      assertFactor(20.0F, factors, EntityPlayerMP.class);
      assertFactor(20.0F, factors, EntityVillager.class);
      assertFactor(3.0F, factors, EntityCow.class);
      assertFactor(3.0F, factors, EntityZombie.class);
      assertFactor(3.0F, factors, ModdedLiving.class);
      assertFactor(1.0F, factors, Entity.class);
   }

   @Test
   public void explicitVehicleFactorsApplyToSubclassesBeforeLivingFallbacks() {
      MCH_DamageFactor factors = new MCH_DamageFactor();
      factors.add(EntityPlayer.class, 20.0F);
      factors.add(EntityLivingBase.class, 3.0F);
      factors.add(MCP_EntityPlane.class, 4.0F);
      factors.add(MCH_EntityHeli.class, 5.0F);
      factors.add(MCH_EntityTank.class, 2.0F);
      factors.add(MCH_EntityTurret.class, 6.0F);
      factors.add(MCH_EntityShip.class, 7.0F);

      assertFactor(4.0F, factors, TestPlane.class);
      assertFactor(5.0F, factors, TestHeli.class);
      assertFactor(2.0F, factors, TestTank.class);
      assertFactor(6.0F, factors, TestTurret.class);
      assertFactor(7.0F, factors, TestShip.class);
   }

   private static void assertFactor(float expected, MCH_DamageFactor factors, Class target) {
      assertEquals(expected, factors.getDamageFactor(target), 0.0F);
   }

   private abstract static class ModdedLiving extends EntityLivingBase {
      protected ModdedLiving(World world) {
         super(world);
      }
   }

   private static class TestPlane extends MCP_EntityPlane {
      public TestPlane(World world) { super(world); }
   }

   private static class TestHeli extends MCH_EntityHeli {
      public TestHeli(World world) { super(world); }
   }

   private static class TestTank extends MCH_EntityTank {
      public TestTank(World world) { super(world); }
   }

   private static class TestTurret extends MCH_EntityTurret {
      public TestTurret(World world) { super(world); }
   }

   private static class TestShip extends MCH_EntityShip {
      public TestShip(World world) { super(world); }
   }
}
