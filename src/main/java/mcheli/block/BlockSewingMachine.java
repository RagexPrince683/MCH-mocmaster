package mcheli.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.wrapper.IconRegister;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockSewingMachine extends Block implements ITileEntityProvider {

    private final boolean isLighting;
    public BlockSewingMachine(int blockId, boolean p_i45421_1_) {
        super(Material.iron);
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(0.2F);
        this.isLighting = p_i45421_1_;
        if(p_i45421_1_) {
            this.setLightLevel(1.0F);
        }

    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        //TileTownHall tile = (TileTownHall) world.getTileEntity(x, y, z);
        //if(tile!=null){InventoryTools.dropInventoryInWorld(world, tile, x, y, z);}
        super.breakBlock(world, x, y, z, block, meta);
    }


    public void onBlockPlacedBy(World world, int par2, int par3, int par4, EntityLivingBase entity, ItemStack itemStack) {
        float pyaw = (float) MCH_Lib.getRotate360((double)entity.rotationYaw);
        pyaw += 22.5F;
        int yaw = (int)(pyaw / 45.0F);
        if(yaw < 0) {
            yaw = yaw % 8 + 8;
        }

        world.setBlockMetadataWithNotify(par2, par3, par4, yaw, 2);
        MCH_Lib.DbgLog(world, "MCH_DraftingTableBlock.onBlockPlacedBy:yaw=%d", new Object[]{Integer.valueOf(yaw)});
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

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        super.blockIcon = par1IconRegister.registerIcon("mcheli:sewing_machine");
    }

    public void registerIcons(IconRegister par1IconRegister) {
        super.blockIcon = par1IconRegister.registerIcon("mcheli:sewing_machine");
    }

    @Override
    public boolean hasTileEntity(int metadata)
    {
        return true;
    }


    public TileEntity createNewTileEntity(World world, int a) {
        return new SewingTileEntity();
    }

    public TileEntity createNewTileEntity(World world) {
        return new SewingTileEntity();
    }

}
