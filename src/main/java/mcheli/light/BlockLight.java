package mcheli.light;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLight extends Block {

    public BlockLight() {
        super(Material.circuits); // Non-solid, doesn't block light
        this.setLightLevel(1.0F); // Max light level
        this.setBlockUnbreakable();
        this.setResistance(6000000F);
        this.setTickRandomly(false);
        this.setCreativeTab(null); // Hidden from creative menu
        this.setBlockName("mcheli_lightblock"); // Required
    }

    @Override
    public int getRenderType() {
        return -1; // Don't render like a normal block
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean canCollideCheck(int meta, boolean hitIfLiquid) {
        return false;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isAir(IBlockAccess world, int x, int y, int z) {
        return true;
    }
}