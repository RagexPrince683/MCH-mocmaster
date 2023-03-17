package mcheli.block;

import mcheli.MCH_Lib;
import net.minecraft.tileentity.TileEntity;

public class MCH_DraftingTableTileEntity extends TileEntity {

   public int getBlockMetadata() {
      if(super.blockMetadata == -1) {
         super.blockMetadata = super.worldObj.getBlockMetadata(super.xCoord, super.yCoord, super.zCoord);
         MCH_Lib.DbgLog(super.worldObj, "MCH_DraftingTableTileEntity.getBlockMetadata : %d(0x%08X)", new Object[]{Integer.valueOf(super.blockMetadata), Integer.valueOf(super.blockMetadata)});
      }

      return super.blockMetadata;
   }
}
