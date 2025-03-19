package mcheli.uav;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_ItemTank;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.uav.MCH_ContainerUavStation;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_UavPacketStatus;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

public class MCH_GuiUavStation
           extends W_GuiContainer
         {
      final MCH_EntityUavStation uavStation;
      static final int BX = 20;
      static final int BY = 22;
      private final int BUTTON_ID_CONTINUE = 256;
      EntityPlayer EntityPlayer;
      private GuiButton buttonContinue;
      //MCH_EntityAircraft aircraft = this; // Assuming 'this' is an instance of MCH_EntityAircraft

      public MCH_GuiUavStation(InventoryPlayer inventoryPlayer, MCH_EntityUavStation uavStation) {
           super(new MCH_ContainerUavStation(inventoryPlayer, uavStation));
           this.uavStation = uavStation;
         }

      protected void drawGuiContainerForegroundLayer(int param1, int param2) {
           if (this.uavStation != null) {
                ItemStack item = this.uavStation.getStackInSlot(0);
                Object info = null;
                if (item != null && item.getItem() instanceof mcheli.plane.MCP_ItemPlane) {
                     info = MCP_PlaneInfoManager.getFromItem(item.getItem());
                   }

               if (item != null && item.getItem() instanceof mcheli.ship.MCH_ItemShip) {
                   info = MCH_ShipInfoManager.getFromItem(item.getItem());
               }

                if (item != null && item.getItem() instanceof mcheli.helicopter.MCH_ItemHeli) {
                     info = MCH_HeliInfoManager.getFromItem(item.getItem());
                   }

                if (item != null && item.getItem() instanceof mcheli.tank.MCH_ItemTank) {
                     info = MCH_TankInfoManager.getFromItem(item.getItem());
                   }

                if (item != null && (item == null || info == null || !((MCH_AircraftInfo)info).isUAV || !((MCH_AircraftInfo)info).isNewUAV)) {
                     if (item != null) {
                          drawString("Not UAV", 8, 6, 16711680);
                        }
                   } else if (this.uavStation.getKind() <= 1) {
                     drawString("UAV Station", 8, 6, 16777215);
                   } else if (item != null && !((MCH_AircraftInfo)info).isSmallUAV) {
                     drawString("Small UAV only", 8, 6, 16711680);
                   } else {
                     drawString("UAV Controller", 8, 6, 16777215);
                   }

                drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 16777215);
                drawString(String.format("X.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavX) }), 58, 15, 16777215);
                drawString(String.format("Y.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavY) }), 58, 37, 16777215);
                drawString(String.format("Z.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavZ) }), 58, 59, 16777215);
              }
         }

      protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
           W_McClient.MOD_bindTexture("textures/gui/uav_station.png");
           GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
           int x = (this.width - this.xSize) / 2;
           int y = (this.height - this.ySize) / 2;
           drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
         }

      protected void actionPerformed(GuiButton btn) {
                     System.out.println("actionPerformed fired");
           if (btn != null && btn.enabled) {
                        System.out.println("btn enabled");
                if (btn.id == 256) {
                              this.uavStation.setContinuePressed(true);
                     if (this.uavStation != null && !this.uavStation.isDead && this.uavStation.getLastControlAircraft() != null && !(this.uavStation.getLastControlAircraft()).isDead) {
                          System.out.println("found uav station and last control aircraft");
                         MCH_EntityAircraft aircraft = getCurrentAircraft(this.uavStation.getLastControlAircraft());
                         //MCH_EntityAircraft aircraft1 = getCurrentAircraft(aircraft);
                         if (aircraft != null) {
                             System.out.println("found aircraft, binding");
                             aircraft.castuavid(this.mc.thePlayer);
                         }
                          MCH_UavPacketStatus pos = new MCH_UavPacketStatus();
                          pos.posUavX = (byte)this.uavStation.posUavX;
                          pos.posUavY = (byte)this.uavStation.posUavY;
                          pos.posUavZ = (byte)this.uavStation.posUavZ;
                          pos.continueControl = true;
                          W_Network.sendToServer((W_PacketBase)pos);
                        }

                     this.buttonContinue.enabled = false;
                   } else {
                     int[] pos1 = { this.uavStation.posUavX, this.uavStation.posUavY, this.uavStation.posUavZ };
                     int i = btn.id >> 4 & 0xF;
                     int j = (btn.id & 0xF) - 1;
                     int[] BTN = { -10, -1, 1, 10 };
                     pos1[i] = pos1[i] + BTN[j];
                     if (pos1[i] < -50) {
                          pos1[i] = -50;
                        }

                     if (pos1[i] > 50) {
                          pos1[i] = 50;
                        }

                     if (this.uavStation.posUavX != pos1[0] || this.uavStation.posUavY != pos1[1] || this.uavStation.posUavZ != pos1[2]) {
                          MCH_UavPacketStatus data = new MCH_UavPacketStatus();
                          data.posUavX = (byte)pos1[0];
                          data.posUavY = (byte)pos1[1];
                          data.posUavZ = (byte)pos1[2];
                          W_Network.sendToServer((W_PacketBase)data);
                        }
                   }
              }
         }

             private MCH_EntityAircraft getCurrentAircraft(MCH_EntityAircraft aircraft) {
                    return aircraft;
             }


             public void initGui() {
           super.initGui();
           this.buttonList.clear();
           int x = this.width / 2 - 5;
           int y = this.height / 2 - 76;
           String[] BTN = { "-10", "-1", "+1", "+10" };

           for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                     int id = row << 4 | col + 1;
                     this.buttonList.add(new GuiButton(id, x + col * 20, y + row * 22, 20, 20, BTN[col]));
                   }
              }

           this.buttonContinue = new GuiButton(256, x - 80 + 3, y + 44, 50, 20, "Continue");
           this.buttonContinue.enabled = false;
           if (this.uavStation != null && !this.uavStation.isDead && this.uavStation.getAndSearchLastControlAircraft() != null) {
                this.buttonContinue.enabled = true;
              }

           this.buttonList.add(this.buttonContinue);
         }
    }
