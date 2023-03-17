/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.crash.CrashReportCategory
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityFallingBlock
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.DamageSource
 *  net.minecraft.world.Explosion
 *  net.minecraft.world.World
 */
package mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public abstract class W_Entity
extends Entity {
    public W_Entity(World par1World) {
        super(par1World);
    }

    protected void entityInit() {
    }

    public static boolean isEntityFallingBlock(Entity entity) {
        return entity instanceof EntityFallingBlock;
    }

    public static int getEntityId(Entity entity) {
        return entity != null ? entity.getEntityId() : -1;
    }

    public static boolean isEqual(Entity e1, Entity e2) {
        int i2;
        int i1 = W_Entity.getEntityId(e1);
        return i1 == (i2 = W_Entity.getEntityId(e2));
    }

    public EntityItem dropItemWithOffset(Item item, int par2, float par3) {
        return this.entityDropItem(new ItemStack(item, par2, 0), par3);
    }

    public String getEntityName() {
        return super.getEntityString();
    }

    public boolean interactFirst(EntityPlayer par1EntityPlayer) {
        return this.interact(par1EntityPlayer);
    }

    public boolean interact(EntityPlayer par1EntityPlayer) {
        return false;
    }

    public boolean attackEntityFrom(DamageSource par1DamageSource, int par2) {
        return this.attackEntityFrom(par1DamageSource, (float)par2);
    }

    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        return false;
    }

    public static boolean attackEntityFrom(Entity entity, DamageSource ds, float par2) {
        return entity.attackEntityFrom(ds, par2);
    }

    public void addEntityCrashInfo(CrashReportCategory par1CrashReportCategory) {
        super.addEntityCrashInfo(par1CrashReportCategory);
    }

    public static float getBlockExplosionResistance(Entity entity, Explosion par1Explosion, World par2World, int par3, int par4, int par5, Block par6Block) {
        if (par6Block != null) {
            try {
                return entity.func_145772_a(par1Explosion, par2World, par3, par4, par5, par6Block);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0.0f;
    }

    public static boolean shouldExplodeBlock(Entity entity, Explosion par1Explosion, World par2World, int par3, int par4, int par5, int par6, float par7) {
        return entity.func_145774_a(par1Explosion, par2World, par3, par4, par5, W_Block.getBlockById(par6), par7);
    }

    public static PotionEffect getActivePotionEffect(Entity entity, Potion par1Potion) {
        return entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).getActivePotionEffect(par1Potion) : null;
    }

    public static void removePotionEffectClient(Entity entity, int id) {
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase)entity).removePotionEffectClient(id);
        }
    }

    public static void removePotionEffect(Entity entity, int id) {
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase)entity).removePotionEffect(id);
        }
    }

    public static void addPotionEffect(Entity entity, PotionEffect pe) {
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase)entity).addPotionEffect(pe);
        }
    }

    protected void doBlockCollisions() {
        super.func_145775_I();
    }
}

