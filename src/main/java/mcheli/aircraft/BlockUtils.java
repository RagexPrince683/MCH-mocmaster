package mcheli.aircraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class BlockUtils {
    public static boolean isSlowingBlock(Block block, World world, int x, int y, int z, Entity entity) {
        // Check for blocks with specific step sound types (like dirt, grass, podzol)
        if (block.stepSound == Block.soundTypeGrass ||
                block.stepSound == Block.soundTypeGravel ||
                block.stepSound == Block.soundTypeSand) {
            return true;
        }

        // Check for blocks with movement-debuff logic (like soul sand)
        if (block instanceof BlockSoulSand) {
            return true;
        }

        // Simulate the onEntityCollidedWithBlock behavior to detect custom debuff logic
        try {
            double originalMotionX = entity.motionX;
            double originalMotionZ = entity.motionZ;

            block.onEntityCollidedWithBlock(world, x, y, z, entity);

            // If motionX or motionZ is reduced, it's likely a movement-debuff block
            if (entity.motionX < originalMotionX || entity.motionZ < originalMotionZ) {
                return true;
            }
        } catch (Exception e) {
            // Handle cases where the block's method throws an error
            e.printStackTrace();
        }

        return false;
    }
}
