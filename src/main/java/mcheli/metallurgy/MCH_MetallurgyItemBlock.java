package mcheli.metallurgy;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Validate the whole footprint before vanilla consumes the one machine item. */
public class MCH_MetallurgyItemBlock extends ItemBlock {
    public MCH_MetallurgyItemBlock(Block block) { super(block); }

    @Override public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
        int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        int facing = MCH_MetallurgyStructure.facing(player);
        int[] core = MCH_MetallurgyStructure.controller(x, y, z, facing);
        return MCH_MetallurgyStructure.place((MCH_MetallurgyBlock) field_150939_a,
            world, core[0], core[1], core[2], facing, player, stack, side);
    }
}
