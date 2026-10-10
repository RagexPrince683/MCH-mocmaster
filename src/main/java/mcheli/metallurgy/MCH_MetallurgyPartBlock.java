package mcheli.metallurgy;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/** Invisible occupied sections use the source's full-cell collision and selection bounds. */
public class MCH_MetallurgyPartBlock extends BlockContainer {
    public MCH_MetallurgyPartBlock() {
        super(Material.iron);
        setBlockName("mcheli.metallurgy_part");
        setBlockTextureName("mcheli:blast_furnace");
        setHardness(5.0F); setResistance(10.0F); setStepSound(soundTypeMetal);
    }
    @Override public int getRenderType() { return -1; }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public TileEntity createNewTileEntity(World world, int metadata) { return new MCH_MetallurgyPartTile(); }
    @Override public IIcon getIcon(int side, int metadata) {
        return (metadata == 1 ? MCH_Metallurgy.foundry : MCH_Metallurgy.blastFurnace).getIcon(side, 0);
    }
    @Override public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return new ArrayList<ItemStack>();
    }
    @Override public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) { return false; }
    @Override public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return new ItemStack(world.getBlockMetadata(x, y, z) == 1 ? MCH_Metallurgy.foundry : MCH_Metallurgy.blastFurnace);
    }
    private MCH_MetallurgyPartTile part(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof MCH_MetallurgyPartTile ? (MCH_MetallurgyPartTile) tile : null;
    }
    @Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
        int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (world.isRemote) return true;
        MCH_MetallurgyPartTile part = part(world, x, y, z);
        MCH_MetallurgyTile core = part == null ? null : part.controller();
        return core != null && core.getBlockType().onBlockActivated(world, core.xCoord, core.yCoord, core.zCoord,
            player, side, hitX, hitY, hitZ);
    }
    @Override public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote) world.scheduleBlockUpdate(x, y, z, this, 1);
    }
    @Override public void updateTick(World world, int x, int y, int z, Random random) {
        MCH_MetallurgyPartTile part = part(world, x, y, z);
        if (part != null) part.validateOwner();
        else if (!world.isRemote) world.setBlockToAir(x, y, z);
    }
    @Override public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harvest) {
        MCH_MetallurgyPartTile part = part(world, x, y, z);
        MCH_MetallurgyTile core = part == null ? null : part.controller();
        if (!world.isRemote && core != null && !core.dismantling) {
            if (player != null && player.capabilities.isCreativeMode) world.setBlockToAir(core.xCoord, core.yCoord, core.zCoord);
            else world.func_147480_a(core.xCoord, core.yCoord, core.zCoord, true);
        }
        if (world.getBlock(x, y, z) == this) world.setBlockToAir(x, y, z);
        return true;
    }
    @Override public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        MCH_MetallurgyPartTile part = part(world, x, y, z);
        MCH_MetallurgyTile core = part == null ? null : part.controller();
        if (!world.isRemote && core != null && !core.dismantling) world.func_147480_a(core.xCoord, core.yCoord, core.zCoord, true);
        // Parts never own contents and never emit their own item drop.
        super.breakBlock(world, x, y, z, block, metadata);
    }
}
