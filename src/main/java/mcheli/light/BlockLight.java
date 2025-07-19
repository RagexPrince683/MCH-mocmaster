package mcheli.light;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
//import net.minecraft.world.EnumSkyBlock;


public class BlockLight extends Block {
    private IIcon icon;

    public BlockLight() {
        super(Material.circuits); // Better than air
        this.setLightLevel(1.0F); // Light level 15
        this.setBlockUnbreakable();
        this.setResistance(6000000F);
        this.setTickRandomly(false);
        this.setCreativeTab(null);
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    //@Override
    //public boolean canCollideCheck(int meta, boolean hitIfLiquid) {
    //    return false;
    //}

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isAir(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        // No collision box at all
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z,
                                        AxisAlignedBB mask,
                                        List list,
                                        Entity entity) {
        // Don’t add any boxes, so nothing collides
    }

    @Override
    public boolean isCollidable() {
        // Tells the engine “don’t even bother checking collision”
        return false;
    }

    @Override
    public boolean canCollideCheck(int meta, boolean fullHit) {
        // Prevent any ray‑tracing collision checks (e.g. for clicking)
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        // “mymod” here must match your mod ID!
        this.icon = reg.registerIcon("mymod:lightblock");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        return icon;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        world.markBlockForUpdate(x, y, z);
        world.updateLightByType(EnumSkyBlock.Block, x, y, z);
    }
}