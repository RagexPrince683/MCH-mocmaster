package mcheli.aircraft;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class BlockUtils {
    public static boolean isSlowingBlock(Block block) {
        // Check for dirt-related blocks or specific blocks like soul sand
        return block == Blocks.dirt ||
                block == Blocks.grass ||
                block == Blocks.soul_sand ||
                block == Blocks.mycelium ||
                block.getSoundType() == SoundType.GROUND; // For dirt step sound
    }
}
