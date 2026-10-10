package mcheli.metallurgy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Only the two metallurgy footprints; no shared XF machinery or energy interfaces. */
public final class MCH_MetallurgyStructure {
    private MCH_MetallurgyStructure() {}

    public static int facing(EntityPlayer player) {
        return player == null ? 2 : new int[] {2, 5, 3, 4}[
            MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3];
    }

    /** Controller is one block behind the placement point, as in XF. */
    public static int[] controller(int x, int y, int z, int facing) {
        ForgeDirection direction = ForgeDirection.getOrientation(facing);
        return new int[] {x - direction.offsetX, y, z - direction.offsetZ};
    }

    /** Absolute occupied cells, with the blast furnace's open central chimney preserved. */
    public static List<int[]> cells(boolean foundry, int x, int y, int z, int facing) {
        List<int[]> cells = new ArrayList<int[]>(foundry ? 60 : 41);
        // North-facing source foundry extends three blocks north and one south.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = foundry ? -3 : -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= (foundry ? 3 : 4); dy++) {
                    if (!foundry && dx == 0 && dz == 0 && dy > 0) continue;
                    int rx = dx, rz = dz;
                    switch (facing) {
                        case 4: rx = dz; rz = -dx; break;
                        case 3: rx = -dx; rz = -dz; break;
                        case 5: rx = -dz; rz = dx; break;
                        default: break;
                    }
                    cells.add(new int[] {x + rx, y + dy, z + rz});
                }
            }
        }
        return cells;
    }

    public static boolean foundation(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!world.blockExists(x + dx, y - 1, z + dz)) return false;
                Block block = world.getBlock(x + dx, y - 1, z + dz);
                if (block != Blocks.hopper && !world.isSideSolid(x + dx, y - 1, z + dz, ForgeDirection.UP)) return false;
            }
        }
        return true;
    }

    public static boolean canPlace(MCH_MetallurgyBlock block, World world, int x, int y, int z,
        int facing, EntityPlayer player, ItemStack stack, int side) {
        if (y < 1 || y + (block.isFoundry() ? 4 : 5) > world.getHeight()) return false;
        if (!block.isFoundry() && !foundation(world, x, y, z)) return false;
        for (int[] pos : cells(block.isFoundry(), x, y, z, facing)) {
            if (!world.blockExists(pos[0], pos[1], pos[2])) return false;
            Block old = world.getBlock(pos[0], pos[1], pos[2]);
            if (!old.isReplaceable(world, pos[0], pos[1], pos[2]) || world.getTileEntity(pos[0], pos[1], pos[2]) != null) return false;
            if (player != null && !player.canPlayerEdit(pos[0], pos[1], pos[2], side, stack)) return false;
            if (!world.checkNoEntityCollision(AxisAlignedBB.getBoundingBox(pos[0], pos[1], pos[2],
                pos[0] + 1, pos[1] + 1, pos[2] + 1))) return false;
        }
        // Keep the open shaft empty even though it has no dummy/collision blocks.
        if (!block.isFoundry()) {
            for (int dy = 1; dy <= 4; dy++) {
                if (!world.getBlock(x, y + dy, z).isReplaceable(world, x, y + dy, z)) return false;
            }
        }
        return true;
    }

    public static boolean place(MCH_MetallurgyBlock block, World world, int x, int y, int z,
        int facing, EntityPlayer player, ItemStack stack, int side) {
        if (!canPlace(block, world, x, y, z, facing, player, stack, side)) return false;
        if (world.isRemote) return true; // Server creates and links the complete footprint.
        List<int[]> cells = cells(block.isFoundry(), x, y, z, facing);
        Block[] oldBlocks = new Block[cells.size()];
        int[] oldMetadata = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            int[] pos = cells.get(i);
            oldBlocks[i] = world.getBlock(pos[0], pos[1], pos[2]);
            oldMetadata[i] = world.getBlockMetadata(pos[0], pos[1], pos[2]);
        }
        // Forge captures these writes as one MultiPlaceEvent and defers notifications until approval.
        boolean complete = world.setBlock(x, y, z, block, facing, 3);
        MCH_MetallurgyTile controller = complete ? (MCH_MetallurgyTile) world.getTileEntity(x, y, z) : null;
        for (int[] pos : cells) {
            if (!complete) break;
            if (pos[0] == x && pos[1] == y && pos[2] == z) continue;
            complete = world.setBlock(pos[0], pos[1], pos[2], MCH_Metallurgy.part, block.isFoundry() ? 1 : 0, 3);
            TileEntity tile = world.getTileEntity(pos[0], pos[1], pos[2]);
            if (complete && tile instanceof MCH_MetallurgyPartTile) {
                ((MCH_MetallurgyPartTile) tile).link(x, y, z, facing, block.isFoundry());
            } else complete = false;
        }
        if (!complete || controller == null) {
            if (controller != null) controller.dismantling = true;
            for (int i = cells.size() - 1; i >= 0; i--) {
                int[] pos = cells.get(i);
                if (world.getBlock(pos[0], pos[1], pos[2]) == MCH_Metallurgy.part
                    || pos[0] == x && pos[1] == y && pos[2] == z) {
                    world.setBlock(pos[0], pos[1], pos[2], oldBlocks[i], oldMetadata[i], 2);
                }
            }
            return false;
        }
        controller.formed = true;
        controller.markDirty();
        world.markBlockForUpdate(x, y, z);
        return true;
    }

    public static void removeParts(MCH_MetallurgyTile controller, int facing) {
        controller.dismantling = true;
        World world = controller.getWorldObj();
        for (int[] pos : cells(controller instanceof MCH_FoundryTile,
            controller.xCoord, controller.yCoord, controller.zCoord, facing)) {
            // Unloaded edge parts validate their saved owner on load; never load chunks to delete them.
            if (!world.blockExists(pos[0], pos[1], pos[2]) || world.getBlock(pos[0], pos[1], pos[2]) != MCH_Metallurgy.part) continue;
            TileEntity tile = world.getTileEntity(pos[0], pos[1], pos[2]);
            if (tile instanceof MCH_MetallurgyPartTile && ((MCH_MetallurgyPartTile) tile).belongsTo(controller)) {
                world.setBlockToAir(pos[0], pos[1], pos[2]);
            }
        }
    }
}
