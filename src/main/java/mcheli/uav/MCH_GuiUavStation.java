package mcheli.uav;

import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.plane.MCP_ItemPlane;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_ItemTank;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.uav.MCH_ContainerUavStation;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_UavPacketStatus;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

public class MCH_GuiUavStation
        /*     */   extends W_GuiContainer
        /*     */ {
   /*     */   final MCH_EntityUavStation uavStation;
   /*     */   static final int BX = 20;
   /*     */   static final int BY = 22;
   /*  27 */   private final int BUTTON_ID_CONTINUE = 256;
   /*     */
   /*     */   private GuiButton buttonContinue;
   /*     */
   /*     */   public MCH_GuiUavStation(InventoryPlayer inventoryPlayer, MCH_EntityUavStation uavStation) {
      /*  32 */     super(new MCH_ContainerUavStation(inventoryPlayer, uavStation));
      /*  33 */     this.uavStation = uavStation;
      /*     */   }
   /*     */
   /*     */   protected void drawGuiContainerForegroundLayer(int param1, int param2) {
      /*  37 */     if (this.uavStation != null) {
         /*  38 */       ItemStack item = this.uavStation.getStackInSlot(0);
         /*  39 */       Object info = null;
         /*  40 */       if (item != null && item.getItem() instanceof mcheli.plane.MCP_ItemPlane) {
            /*  41 */         info = MCP_PlaneInfoManager.getFromItem(item.getItem());
            /*     */       }
         /*     */
         /*  44 */       if (item != null && item.getItem() instanceof mcheli.helicopter.MCH_ItemHeli) {
            /*  45 */         info = MCH_HeliInfoManager.getFromItem(item.getItem());
            /*     */       }
         /*     */
         /*  48 */       if (item != null && item.getItem() instanceof mcheli.tank.MCH_ItemTank) {
            /*  49 */         info = MCH_TankInfoManager.getFromItem(item.getItem());
            /*     */       }
         /*     */
         /*  52 */       if (item != null && (item == null || info == null || !((MCH_AircraftInfo)info).isUAV || !((MCH_AircraftInfo)info).isNewUAV)) {
            /*  53 */         if (item != null) {
               /*  54 */           drawString("Not UAV", 8, 6, 16711680);
               /*     */         }
            /*  56 */       } else if (this.uavStation.getKind() <= 1) {
            /*  57 */         drawString("UAV Station", 8, 6, 16777215);
            /*  58 */       } else if (item != null && !((MCH_AircraftInfo)info).isSmallUAV) {
            /*  59 */         drawString("Small UAV only", 8, 6, 16711680);
            /*     */       } else {
            /*  61 */         drawString("UAV Controller", 8, 6, 16777215);
            /*     */       }
         /*     */
         /*  64 */       drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 16777215);
         /*  65 */       drawString(String.format("X.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavX) }), 58, 15, 16777215);
         /*  66 */       drawString(String.format("Y.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavY) }), 58, 37, 16777215);
         /*  67 */       drawString(String.format("Z.%+2d", new Object[] { Integer.valueOf(this.uavStation.posUavZ) }), 58, 59, 16777215);
         /*     */     }
      /*     */   }
   /*     */
   /*     */   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      /*  72 */     W_McClient.MOD_bindTexture("textures/gui/uav_station.png");
      /*  73 */     GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      /*  74 */     int x = (this.width - this.xSize) / 2;
      /*  75 */     int y = (this.height - this.ySize) / 2;
      /*  76 */     drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
      /*     */   }
   /*     */
   /*     */   protected void actionPerformed(GuiButton btn) {
      /*  80 */     if (btn != null && btn.enabled) {
         /*  81 */       if (btn.id == 256) {
            /*  82 */         if (this.uavStation != null && !this.uavStation.isDead && this.uavStation.getLastControlAircraft() != null && !(this.uavStation.getLastControlAircraft()).isDead) {
               /*  83 */           MCH_UavPacketStatus pos = new MCH_UavPacketStatus();
               /*  84 */           pos.posUavX = (byte)this.uavStation.posUavX;
               /*  85 */           pos.posUavY = (byte)this.uavStation.posUavY;
               /*  86 */           pos.posUavZ = (byte)this.uavStation.posUavZ;
               /*  87 */           pos.continueControl = true;
               /*  88 */           W_Network.sendToServer((W_PacketBase)pos);
               /*     */         }
            /*     */
            /*  91 */         this.buttonContinue.enabled = false;
            /*     */       } else {
            /*  93 */         int[] pos1 = { this.uavStation.posUavX, this.uavStation.posUavY, this.uavStation.posUavZ };
            /*  94 */         int i = btn.id >> 4 & 0xF;
            /*  95 */         int j = (btn.id & 0xF) - 1;
            /*  96 */         int[] BTN = { -10, -1, 1, 10 };
            /*  97 */         pos1[i] = pos1[i] + BTN[j];
            /*  98 */         if (pos1[i] < -50) {
               /*  99 */           pos1[i] = -50;
               /*     */         }
            /*     */
            /* 102 */         if (pos1[i] > 50) {
               /* 103 */           pos1[i] = 50;
               /*     */         }
            /*     */
            /* 106 */         if (this.uavStation.posUavX != pos1[0] || this.uavStation.posUavY != pos1[1] || this.uavStation.posUavZ != pos1[2]) {
               /* 107 */           MCH_UavPacketStatus data = new MCH_UavPacketStatus();
               /* 108 */           data.posUavX = (byte)pos1[0];
               /* 109 */           data.posUavY = (byte)pos1[1];
               /* 110 */           data.posUavZ = (byte)pos1[2];
               /* 111 */           W_Network.sendToServer((W_PacketBase)data);
               /*     */         }
            /*     */       }
         /*     */     }
      /*     */   }
   /*     */
   /*     */
   /*     */   public void initGui() {
      /* 119 */     super.initGui();
      /* 120 */     this.buttonList.clear();
      /* 121 */     int x = this.width / 2 - 5;
      /* 122 */     int y = this.height / 2 - 76;
      /* 123 */     String[] BTN = { "-10", "-1", "+1", "+10" };
      /*     */
      /* 125 */     for (int row = 0; row < 3; row++) {
         /* 126 */       for (int col = 0; col < 4; col++) {
            /* 127 */         int id = row << 4 | col + 1;
            /* 128 */         this.buttonList.add(new GuiButton(id, x + col * 20, y + row * 22, 20, 20, BTN[col]));
            /*     */       }
         /*     */     }
      /*     */
      /* 132 */     this.buttonContinue = new GuiButton(256, x - 80 + 3, y + 44, 50, 20, "Continue");
      /* 133 */     this.buttonContinue.enabled = false;
      /* 134 */     if (this.uavStation != null && !this.uavStation.isDead && this.uavStation.getAndSearchLastControlAircraft() != null) {
         /* 135 */       this.buttonContinue.enabled = true;
         /*     */     }
      /*     */
      /* 138 */     this.buttonList.add(this.buttonContinue);
      /*     */   }
   /*     */ }
