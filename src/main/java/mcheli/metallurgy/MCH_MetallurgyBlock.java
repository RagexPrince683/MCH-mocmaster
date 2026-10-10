package mcheli.metallurgy;

import mcheli.MCH_MOD;
import mcheli.gui.MCH_GuiCommonHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Inventory controller; occupied sections are linked MCHeli-owned parts. */
public class MCH_MetallurgyBlock extends BlockContainer {
    private final boolean foundry;
    public MCH_MetallurgyBlock(boolean foundry) {
        super(foundry ? Material.iron : Material.rock);
        this.foundry = foundry;
        setBlockName("mcheli." + (foundry ? "steel_foundry" : "blast_furnace"));
        setBlockTextureName("mcheli:" + (foundry ? "steel_foundry" : "blast_furnace"));
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(foundry ? soundTypeMetal : soundTypeStone);
        setCreativeTab(MCH_MOD.creativeTabs);
    }
    @Override public int getRenderType() { return -1; }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    public boolean isFoundry() { return foundry; }
    @Override public int damageDropped(int metadata) { return 0; }
    @Override public TileEntity createNewTileEntity(World world, int metadata) {
        return foundry ? new MCH_FoundryTile() : new MCH_BlastFurnaceTile();
    }
    @Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
        int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (!world.isRemote) player.openGui(MCH_MOD.instance,
            foundry ? MCH_GuiCommonHandler.GUIID_STEEL_FOUNDRY : MCH_GuiCommonHandler.GUIID_BLAST_FURNACE,
            world, x, y, z);
        return true;
    }
    @Override public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof MCH_MetallurgyTile) {
            MCH_MetallurgyTile inventory = (MCH_MetallurgyTile) tile;
            MCH_MetallurgyStructure.removeParts(inventory, metadata);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlotOnClosing(slot);
                if (stack != null) world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
            }
            world.func_147453_f(x, y, z, block);
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }
}
