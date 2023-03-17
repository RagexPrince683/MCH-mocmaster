package mcheli.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.wrapper.IconRegister;
import mcheli.wrapper.W_BlockContainer;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class MCH_DraftingTableBlock extends W_BlockContainer implements ITileEntityProvider {

   private final boolean isLighting;


   public MCH_DraftingTableBlock(int blockId, boolean p_i45421_1_) {
      super(blockId, Material.iron);
      this.setStepSound(Block.soundTypeMetal);
      this.setHardness(0.2F);
      this.isLighting = p_i45421_1_;
      if(p_i45421_1_) {
         this.setLightLevel(1.0F);
      }

   }

   public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
      if(!world.isRemote) {
         if(!player.isSneaking()) {
            MCH_Lib.DbgLog(player.worldObj, "MCH_DraftingTableGui.MCH_DraftingTableGui OPEN GUI (%d, %d, %d)", new Object[]{Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)});
            player.openGui(MCH_MOD.instance, 4, world, x, y, z);
         } else {
            int yaw = world.getBlockMetadata(x, y, z);
            MCH_Lib.DbgLog(world, "MCH_DraftingTableBlock.onBlockActivated:yaw=%d Light %s", new Object[]{Integer.valueOf(yaw), this.isLighting?"OFF->ON":"ON->OFF"});
            if(this.isLighting) {
               W_WorldFunc.setBlock(world, x, y, z, MCH_MOD.blockDraftingTable, yaw + 180, 2);
            } else {
               W_WorldFunc.setBlock(world, x, y, z, MCH_MOD.blockDraftingTableLit, yaw + 180, 2);
            }

            world.setBlockMetadataWithNotify(x, y, z, yaw, 2);
            world.playSoundEffect((double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D, "random.click", 0.3F, 0.5F);
         }
      }

      return true;
   }

   public TileEntity createNewTileEntity(World world, int a) {
      return new MCH_DraftingTableTileEntity();
   }

   public TileEntity createNewTileEntity(World world) {
      return new MCH_DraftingTableTileEntity();
   }

   public boolean shouldSideBeRendered(IBlockAccess p_149646_1_, int p_149646_2_, int p_149646_3_, int p_149646_4_, int p_149646_5_) {
      return true;
   }

   public boolean renderAsNormalBlock() {
      return false;
   }

   public boolean isOpaqueCube() {
      return false;
   }

   public boolean canHarvestBlock(EntityPlayer player, int meta) {
      return true;
   }

   public boolean canRenderInPass(int pass) {
      return false;
   }

   public int getMobilityFlag() {
      return 1;
   }

   public void onBlockPlacedBy(World world, int par2, int par3, int par4, EntityLivingBase entity, ItemStack itemStack) {
      float pyaw = (float)MCH_Lib.getRotate360((double)entity.rotationYaw);
      pyaw += 22.5F;
      int yaw = (int)(pyaw / 45.0F);
      if(yaw < 0) {
         yaw = yaw % 8 + 8;
      }

      world.setBlockMetadataWithNotify(par2, par3, par4, yaw, 2);
      MCH_Lib.DbgLog(world, "MCH_DraftingTableBlock.onBlockPlacedBy:yaw=%d", new Object[]{Integer.valueOf(yaw)});
   }

   public boolean getUseNeighborBrightness() {
      return true;
   }

   @SideOnly(Side.CLIENT)
   public void registerBlockIcons(IIconRegister par1IconRegister) {
      super.blockIcon = par1IconRegister.registerIcon("mcheli:drafting_table");
   }

   public void registerIcons(IconRegister par1IconRegister) {
      super.blockIcon = par1IconRegister.registerIcon("mcheli:drafting_table");
   }

   public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
      return W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable);
   }

   @SideOnly(Side.CLIENT)
   public Item getItem(World world, int p_149694_2_, int p_149694_3_, int p_149694_4_) {
      return W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable);
   }

   protected ItemStack createStackedBlock(int p_149644_1_) {
      return new ItemStack(MCH_MOD.blockDraftingTable);
   }
}
