package mcheli.mob;

import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class MCH_EntityGunner extends EntityLivingBase {
    public boolean isCreative;
    public int targetType;
    public String ownerUUID;
    private String teamName;

    public MCH_EntityGunner(World world) {
        super(world);
    }

    public MCH_EntityGunner(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
    }

    @Override
    protected void entityInit() {
        // Initialize data watchers or other entity data here
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        // Read entity data from NBT
        this.isCreative = tagCompund.getBoolean("isCreative");
        this.targetType = tagCompund.getInteger("targetType");
        this.ownerUUID = tagCompund.getString("ownerUUID");
        this.teamName = tagCompund.getString("teamName");
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public ItemStack getEquipmentInSlot(int p_71124_1_) {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int p_70062_1_, ItemStack p_70062_2_) {

    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return new ItemStack[0];
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        // Write entity data to NBT
        tagCompound.setBoolean("isCreative", this.isCreative);
        tagCompound.setInteger("targetType", this.targetType);
        tagCompound.setString("ownerUUID", this.ownerUUID);
        tagCompound.setString("teamName", this.teamName);
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamName() {
        return this.teamName;
    }


    public boolean interactFirst(EntityPlayer player) {
        // Handle interaction with the player
        return super.interactFirst(player);
    }

    public MCH_EntityAircraft getAc() {
        return null;
        //this is actual hell
    }
}