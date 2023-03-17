package mcheli.block;

import mcheli.MCH_IRecipeList;
import mcheli.MCH_ItemRecipe;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.gui.MCH_GuiSliderVertical;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.*;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_DraftingTableGui extends W_GuiContainer {

   private final EntityPlayer thePlayer;
   private int scaleFactor;
   private MCH_GuiSliderVertical listSlider;
   private GuiButton buttonCreate;
   private GuiButton buttonNext;
   private GuiButton buttonPrev;
   private GuiButton buttonNextPage;
   private GuiButton buttonPrevPage;
   private int drawFace;
   private int buttonClickWait;
   public static final int RECIPE_HELI = 0;
   public static final int RECIPE_PLANE = 1;
   public static final int RECIPE_VEHICLE = 2;
   public static final int RECIPE_TANK = 3;
   public static final int RECIPE_ITEM = 4;
   public MCH_IRecipeList currentList;
   public MCH_CurrentRecipe current;
   public static final int BUTTON_HELI = 10;
   public static final int BUTTON_PLANE = 11;
   public static final int BUTTON_VEHICLE = 12;
   public static final int BUTTON_TANK = 13;
   public static final int BUTTON_ITEM = 14;
   public static final int BUTTON_NEXT = 20;
   public static final int BUTTON_PREV = 21;
   public static final int BUTTON_CREATE = 30;
   public static final int BUTTON_SELECT = 40;
   public static final int BUTTON_NEXT_PAGE = 50;
   public static final int BUTTON_PREV_PAGE = 51;
   public List screenButtonList;
   public int screenId = 0;
   public static final int SCREEN_MAIN = 0;
   public static final int SCREEN_LIST = 1;
   public static float modelZoom = 1.0F;
   public static float modelRotX = 0.0F;
   public static float modelRotY = 0.0F;
   public static float modelPosX = 0.0F;
   public static float modelPosY = 0.0F;


   public MCH_DraftingTableGui(EntityPlayer player, int posX, int posY, int posZ) {
      super(new MCH_DraftingTableGuiContainer(player, posX, posY, posZ));
      this.thePlayer = player;
      super.xSize = 400;
      super.ySize = 240;
      this.screenButtonList = new ArrayList();
      this.drawFace = 0;
      this.buttonClickWait = 0;
      MCH_Lib.DbgLog(player.worldObj, "MCH_DraftingTableGui.MCH_DraftingTableGui", new Object[0]);
   }

   public void initGui() {
      super.initGui();
      super.buttonList.clear();
      this.screenButtonList.clear();
      this.screenButtonList.add(new ArrayList());
      this.screenButtonList.add(new ArrayList());
      List list = null;
      list = (List)this.screenButtonList.get(0);
      GuiButton btnHeli = new GuiButton(10, super.guiLeft + 20, super.guiTop + 20, 90, 20, "Helicopter List");
      GuiButton btnPlane = new GuiButton(11, super.guiLeft + 20, super.guiTop + 40, 90, 20, "Plane List");
      GuiButton btnVehicle = new GuiButton(12, super.guiLeft + 20, super.guiTop + 60, 90, 20, "Vehicle List");
      GuiButton btnTank = new GuiButton(13, super.guiLeft + 20, super.guiTop + 80, 90, 20, "Tank List");
      GuiButton btnItem = new GuiButton(14, super.guiLeft + 20, super.guiTop + 100, 90, 20, "Item List");
      btnHeli.enabled = MCH_HeliInfoManager.getInstance().getRecipeListSize() > 0;
      btnPlane.enabled = MCP_PlaneInfoManager.getInstance().getRecipeListSize() > 0;
      btnVehicle.enabled = MCH_VehicleInfoManager.getInstance().getRecipeListSize() > 0;
      btnTank.enabled = MCH_TankInfoManager.getInstance().getRecipeListSize() > 0;
      btnItem.enabled = MCH_ItemRecipe.getInstance().getRecipeListSize() > 0;
      list.add(btnHeli);
      list.add(btnPlane);
      list.add(btnVehicle);
      list.add(btnTank);
      list.add(btnItem);
      this.buttonCreate = new GuiButton(30, super.guiLeft + 120, super.guiTop + 89, 50, 20, "Create");
      this.buttonPrev = new GuiButton(21, super.guiLeft + 120, super.guiTop + 111, 36, 20, "<<");
      this.buttonNext = new GuiButton(20, super.guiLeft + 155, super.guiTop + 111, 35, 20, ">>");
      list.add(this.buttonCreate);
      list.add(this.buttonPrev);
      list.add(this.buttonNext);
      this.buttonPrevPage = new GuiButton(51, super.guiLeft + 210, super.guiTop + 210, 60, 20, "Prev Page");
      this.buttonNextPage = new GuiButton(50, super.guiLeft + 270, super.guiTop + 210, 60, 20, "Next Page");
      list.add(this.buttonPrevPage);
      list.add(this.buttonNextPage);
      list = (List)this.screenButtonList.get(1);
      int i = 0;

      int j;
      for(j = 0; i < 3; ++i) {
         for(int x = 0; x < 2; ++j) {
            int px = super.guiLeft + 30 + x * 140;
            int py = super.guiTop + 40 + i * 70;
            list.add(new GuiButton(40 + j, px, py, 45, 20, "Select"));
            ++x;
         }
      }

      this.listSlider = new MCH_GuiSliderVertical(0, super.guiLeft + 360, super.guiTop + 20, 20, 200, "", 0.0F, 0.0F, 0.0F, 1.0F);
      list.add(this.listSlider);

      for(i = 0; i < this.screenButtonList.size(); ++i) {
         list = (List)this.screenButtonList.get(i);

         for(j = 0; j < list.size(); ++j) {
            super.buttonList.add(list.get(j));
         }
      }

      this.switchScreen(0);
      initModelTransform();
      modelRotX = 180.0F;
      modelRotY = 90.0F;
      if(MCH_ItemRecipe.getInstance().getRecipeListSize() > 0) {
         this.switchRecipeList(MCH_ItemRecipe.getInstance());
      } else if(MCH_HeliInfoManager.getInstance().getRecipeListSize() > 0) {
         this.switchRecipeList(MCH_HeliInfoManager.getInstance());
      } else if(MCP_PlaneInfoManager.getInstance().getRecipeListSize() > 0) {
         this.switchRecipeList(MCP_PlaneInfoManager.getInstance());
      } else if(MCH_VehicleInfoManager.getInstance().getRecipeListSize() > 0) {
         this.switchRecipeList(MCH_VehicleInfoManager.getInstance());
      } else if(MCH_TankInfoManager.getInstance().getRecipeListSize() > 0) {
         this.switchRecipeList(MCH_TankInfoManager.getInstance());
      } else {
         this.switchRecipeList(MCH_ItemRecipe.getInstance());
      }

   }

   public static void initModelTransform() {
      modelRotX = 0.0F;
      modelRotY = 0.0F;
      modelPosX = 0.0F;
      modelPosY = 0.0F;
      modelZoom = 1.0F;
   }

   public void updateListSliderSize(int listSize) {
      int s = listSize / 2;
      if(listSize % 2 != 0) {
         ++s;
      }

      if(s > 3) {
         this.listSlider.valueMax = (float)(s - 3);
      } else {
         this.listSlider.valueMax = 0.0F;
      }

      this.listSlider.setSliderValue(0.0F);
   }

   public void switchScreen(int id) {
      this.screenId = id;

      for(int list = 0; list < super.buttonList.size(); ++list) {
         W_GuiButton.setVisible((GuiButton)super.buttonList.get(list), false);
      }

      if(id < this.screenButtonList.size()) {
         List var5 = (List)this.screenButtonList.get(id);
         Iterator i$ = var5.iterator();

         while(i$.hasNext()) {
            GuiButton b = (GuiButton)i$.next();
            W_GuiButton.setVisible(b, true);
         }
      }

      if(this.getScreenId() == 0 && this.current != null && this.current.getDescMaxPage() > 1) {
         W_GuiButton.setVisible(this.buttonNextPage, true);
         W_GuiButton.setVisible(this.buttonPrevPage, true);
      } else {
         W_GuiButton.setVisible(this.buttonNextPage, false);
         W_GuiButton.setVisible(this.buttonPrevPage, false);
      }

   }

   public void setCurrentRecipe(MCH_CurrentRecipe currentRecipe) {
      modelPosX = 0.0F;
      modelPosY = 0.0F;
      if(this.current == null || currentRecipe == null || !this.current.recipe.getRecipeOutput().isItemEqual(currentRecipe.recipe.getRecipeOutput())) {
         this.drawFace = 0;
      }

      this.current = currentRecipe;
      if(this.getScreenId() == 0 && this.current != null && this.current.getDescMaxPage() > 1) {
         W_GuiButton.setVisible(this.buttonNextPage, true);
         W_GuiButton.setVisible(this.buttonPrevPage, true);
      } else {
         W_GuiButton.setVisible(this.buttonNextPage, false);
         W_GuiButton.setVisible(this.buttonPrevPage, false);
      }

   }

   public MCH_IRecipeList getCurrentList() {
      return this.currentList;
   }

   public void switchRecipeList(MCH_IRecipeList list) {
      if(this.getCurrentList() != list) {
         this.setCurrentRecipe(new MCH_CurrentRecipe(list, 0));
         this.currentList = list;
         this.updateListSliderSize(list.getRecipeListSize());
      } else {
         this.listSlider.setSliderValue((float)(this.current.index / 2));
      }

   }

   public void updateScreen() {
      super.updateScreen();
      MCH_DraftingTableGuiContainer container = (MCH_DraftingTableGuiContainer)super.inventorySlots;
      this.buttonCreate.enabled = false;
      if(!container.getSlot(container.outputSlotIndex).getHasStack() && MCH_Lib.canPlayerCreateItem(this.current.recipe, this.thePlayer.inventory)) {
         this.buttonCreate.enabled = true;
      }

      if(this.thePlayer.capabilities.isCreativeMode) {
         this.buttonCreate.enabled = true;
      }

      if(this.buttonClickWait > 0) {
         --this.buttonClickWait;
      }

   }

   public void onGuiClosed() {
      super.onGuiClosed();
      MCH_Lib.DbgLog(this.thePlayer.worldObj, "MCH_DraftingTableGui.onGuiClosed", new Object[0]);
   }

   protected void actionPerformed(GuiButton button) {
      super.actionPerformed(button);
      if(this.buttonClickWait <= 0) {
         if(button.enabled) {
            this.buttonClickWait = 3;
            boolean index = false;
            int page = this.current.getDescCurrentPage();
            int index1;
            switch(button.id) {
            case 10:
               initModelTransform();
               modelRotX = 180.0F;
               modelRotY = 90.0F;
               this.switchRecipeList(MCH_HeliInfoManager.getInstance());
               this.switchScreen(1);
               break;
            case 11:
               initModelTransform();
               modelRotX = 90.0F;
               modelRotY = 180.0F;
               this.switchRecipeList(MCP_PlaneInfoManager.getInstance());
               this.switchScreen(1);
               break;
            case 12:
               initModelTransform();
               modelRotX = 180.0F;
               modelRotY = 90.0F;
               this.switchRecipeList(MCH_VehicleInfoManager.getInstance());
               this.switchScreen(1);
               break;
            case 13:
               initModelTransform();
               modelRotX = 180.0F;
               modelRotY = 90.0F;
               this.switchRecipeList(MCH_TankInfoManager.getInstance());
               this.switchScreen(1);
               break;
            case 14:
               this.switchRecipeList(MCH_ItemRecipe.getInstance());
               this.switchScreen(1);
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 46:
            case 47:
            case 48:
            case 49:
            default:
               break;
            case 20:
               if(this.current.isCurrentPageTexture()) {
                  page = 0;
               }

               index1 = (this.current.index + 1) % this.getCurrentList().getRecipeListSize();
               this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index1));
               this.current.setDescCurrentPage(page);
               break;
            case 21:
               if(this.current.isCurrentPageTexture()) {
                  page = 0;
               }

               index1 = this.current.index - 1;
               if(index1 < 0) {
                  index1 = this.getCurrentList().getRecipeListSize() - 1;
               }

               this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index1));
               this.current.setDescCurrentPage(page);
               break;
            case 30:
               MCH_DraftingTableCreatePacket.send(this.current.recipe);
               break;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
               index1 = (int)this.listSlider.getSliderValue() * 2 + (button.id - 40);
               if(index1 < this.getCurrentList().getRecipeListSize()) {
                  this.setCurrentRecipe(new MCH_CurrentRecipe(this.getCurrentList(), index1));
                  this.switchScreen(0);
               }
               break;
            case 50:
               if(this.current != null) {
                  this.current.switchNextPage();
               }
               break;
            case 51:
               if(this.current != null) {
                  this.current.switchPrevPage();
               }
            }

         }
      }
   }

   protected void keyTyped(char par1, int keycode) {
      if(keycode == 1 || keycode == W_KeyBinding.getKeyCode(Minecraft.getMinecraft().gameSettings.keyBindInventory)) {
         if(this.getScreenId() == 0) {
            super.mc.thePlayer.closeScreen();
         } else {
            this.switchScreen(0);
         }
      }

      if(this.getScreenId() == 0) {
         if(keycode == 205) {
            this.actionPerformed(this.buttonNext);
         }

         if(keycode == 203) {
            this.actionPerformed(this.buttonPrev);
         }
      } else if(this.getScreenId() == 1) {
         if(keycode == 200) {
            this.listSlider.scrollDown(1.0F);
         }

         if(keycode == 208) {
            this.listSlider.scrollUp(1.0F);
         }
      }

   }

   protected void drawGuiContainerForegroundLayer(int mx, int my) {
      super.drawGuiContainerForegroundLayer(mx, my);
      float z = super.zLevel;
      super.zLevel = 0.0F;
      GL11.glEnable(3042);
      int i;
      int r;
      if(this.getScreenId() == 0) {
         ArrayList index = new ArrayList();
         if(this.current != null) {
            if(this.current.isCurrentPageTexture()) {
               GL11.glColor4d(1.0D, 1.0D, 1.0D, 1.0D);
               super.mc.getTextureManager().bindTexture(this.current.getCurrentPageTexture());
               this.drawTexturedModalRect(210, 20, 170, 190, 0, 0, 340, 380);
            } else if(this.current.isCurrentPageAcInfo()) {
               i = -9491968;

               for(r = 0; r < this.current.infoItem.size(); ++r) {
                  super.fontRendererObj.drawString((String)this.current.infoItem.get(r), 210, 40 + 10 * r, -9491968);
                  String c = (String)this.current.infoData.get(r);
                  if(!c.isEmpty()) {
                     super.fontRendererObj.drawString(c, 280, 40 + 10 * r, -9491968);
                  }
               }
            } else {
               W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
               this.drawTexturedModalRect(340, 215, 45, 15, 400, 60, 90, 30);
               if(mx >= 350 && mx <= 400 && my >= 214 && my <= 230) {
                  boolean var12 = Mouse.isButtonDown(0);
                  boolean var13 = Mouse.isButtonDown(1);
                  boolean var14 = Mouse.isButtonDown(2);
                  index.add((var12?EnumChatFormatting.AQUA:"") + "Mouse left button drag : Rotation model");
                  index.add((var13?EnumChatFormatting.AQUA:"") + "Mouse right button drag : Zoom model");
                  index.add((var14?EnumChatFormatting.AQUA:"") + "Mouse middle button drag : Move model");
               }
            }
         }

         this.drawString(this.current.displayName, 120, 20, -1);
         this.drawItemRecipe(this.current.recipe, 121, 34);
         if(index.size() > 0) {
            this.drawHoveringText(index, mx - 30, my - 0, super.fontRendererObj);
         }
      }

      if(this.getScreenId() == 1) {
         int var11 = 2 * (int)this.listSlider.getSliderValue();
         i = 0;

         int rx;
         int ry;
         int var15;
         for(r = 0; r < 3; ++r) {
            for(var15 = 0; var15 < 2; ++var15) {
               if(var11 + i < this.getCurrentList().getRecipeListSize()) {
                  rx = 110 + 140 * var15;
                  ry = 20 + 70 * r;
                  String s = this.getCurrentList().getRecipe(var11 + i).getRecipeOutput().getDisplayName();
                  this.drawCenteredString(s, rx, ry, -1);
               }

               ++i;
            }
         }

         W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
         i = 0;

         for(r = 0; r < 3; ++r) {
            for(var15 = 0; var15 < 2; ++var15) {
               if(var11 + i < this.getCurrentList().getRecipeListSize()) {
                  rx = 80 + 140 * var15 - 1;
                  ry = 30 + 70 * r - 1;
                  this.drawTexturedModalRect(rx, ry, 400, 0, 75, 54);
               }

               ++i;
            }
         }

         i = 0;

         for(r = 0; r < 3; ++r) {
            for(var15 = 0; var15 < 2; ++var15) {
               if(var11 + i < this.getCurrentList().getRecipeListSize()) {
                  rx = 80 + 140 * var15;
                  ry = 30 + 70 * r;
                  this.drawItemRecipe(this.getCurrentList().getRecipe(var11 + i), rx, ry);
               }

               ++i;
            }
         }
      }

   }

   protected void handleMouseClick(Slot p_146984_1_, int p_146984_2_, int p_146984_3_, int p_146984_4_) {
      if(this.getScreenId() != 1) {
         super.handleMouseClick(p_146984_1_, p_146984_2_, p_146984_3_, p_146984_4_);
      }

   }

   private int getScreenId() {
      return this.screenId;
   }

   public void drawItemRecipe(IRecipe recipe, int x, int y) {
      if(recipe != null) {
         if(recipe.getRecipeOutput() != null) {
            if(recipe.getRecipeOutput().getItem() != null) {
               int i;
               if(recipe instanceof ShapedRecipes) {
                  ShapedRecipes rcp = (ShapedRecipes)recipe;
                  i = rcp.recipeHeight;

                  for(int h = 0; h < i; ++h) {
                     for(int w = 0; w < rcp.recipeWidth; ++w) {
                        int IDX = h * i + w;
                        if(IDX < rcp.recipeItems.length) {
                           this.drawItemStack(rcp.recipeItems[IDX], x + w * 18, y + h * 18);
                        }
                     }
                  }
               } else if(recipe instanceof ShapelessRecipes) {
                  ShapelessRecipes var9 = (ShapelessRecipes)recipe;

                  for(i = 0; i < var9.recipeItems.size(); ++i) {
                     this.drawItemStack((ItemStack)var9.recipeItems.get(i), x + i % 3 * 18, y + i / 3 * 18);
                  }
               }

               this.drawItemStack(recipe.getRecipeOutput(), x + 54 + 3, y + 18);
            }
         }
      }
   }

   public void handleMouseInput() {
      super.handleMouseInput();
      int dx = Mouse.getEventDX();
      int dy = Mouse.getEventDY();
      if(this.getScreenId() == 0 && Mouse.getX() > super.mc.displayWidth / 2) {
         if(Mouse.isButtonDown(0) && (dx != 0 || dy != 0)) {
            modelRotX = (float)((double)modelRotX - (double)dy / 2.0D);
            modelRotY = (float)((double)modelRotY - (double)dx / 2.0D);
            if(modelRotX > 360.0F) {
               modelRotX -= 360.0F;
            }

            if(modelRotX < -360.0F) {
               modelRotX += 360.0F;
            }

            if(modelRotY > 360.0F) {
               modelRotY -= 360.0F;
            }

            if(modelRotY < -360.0F) {
               modelRotY += 360.0F;
            }
         }

         if(Mouse.isButtonDown(2) && (dx != 0 || dy != 0)) {
            modelPosX = (float)((double)modelPosX + (double)dx / 2.0D);
            modelPosY = (float)((double)modelPosY - (double)dy / 2.0D);
            if(modelRotX > 1000.0F) {
               modelRotX = 1000.0F;
            }

            if(modelRotX < -1000.0F) {
               modelRotX = -1000.0F;
            }

            if(modelRotY > 1000.0F) {
               modelRotY = 1000.0F;
            }

            if(modelRotY < -1000.0F) {
               modelRotY = -1000.0F;
            }
         }

         if(Mouse.isButtonDown(1) && dy != 0) {
            modelZoom = (float)((double)modelZoom + (double)dy / 100.0D);
            if((double)modelZoom < 0.1D) {
               modelZoom = 0.1F;
            }

            if(modelZoom > 10.0F) {
               modelZoom = 10.0F;
            }
         }
      }

      int wheel = Mouse.getEventDWheel();
      if(wheel != 0) {
         if(this.getScreenId() == 1) {
            if(wheel > 0) {
               this.listSlider.scrollDown(1.0F);
            } else if(wheel < 0) {
               this.listSlider.scrollUp(1.0F);
            }
         } else if(this.getScreenId() == 0) {
            if(wheel > 0) {
               this.actionPerformed(this.buttonPrev);
            } else if(wheel < 0) {
               this.actionPerformed(this.buttonNext);
            }
         }
      }

   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if(this.getScreenId() == 0) {
         super.drawScreen(mouseX, mouseY, partialTicks);
      } else {
         List inventory = super.inventorySlots.inventorySlots;
         super.inventorySlots.inventorySlots = new ArrayList();
         super.drawScreen(mouseX, mouseY, partialTicks);
         super.inventorySlots.inventorySlots = inventory;
      }

      if(this.getScreenId() == 0 && this.current.isCurrentPageModel()) {
         RenderHelper.enableGUIStandardItemLighting();
         this.drawModel(partialTicks);
      }

   }

   public void drawModel(float partialTicks) {
      W_ModelCustom model = this.current.getModel();
      double scl = 162.0D / ((double)MathHelper.abs(model.size) < 0.01D?0.01D:(double)model.size);
      super.mc.getTextureManager().bindTexture(this.current.getModelTexture());
      GL11.glPushMatrix();
      double cx = (double)(model.maxX - model.minX) * 0.5D + (double)model.minX;
      double cy = (double)(model.maxY - model.minY) * 0.5D + (double)model.minY;
      double cz = (double)(model.maxZ - model.minZ) * 0.5D + (double)model.minZ;
      if(this.current.modelRot == 0) {
         GL11.glTranslated(cx * scl, cz * scl, 0.0D);
      } else {
         GL11.glTranslated(cz * scl, cy * scl, 0.0D);
      }

      GL11.glTranslated((double)((float)(super.guiLeft + 300) + modelPosX), (double)((float)(super.guiTop + 110) + modelPosY), 550.0D);
      GL11.glRotated((double)modelRotX, 1.0D, 0.0D, 0.0D);
      GL11.glRotated((double)modelRotY, 0.0D, 1.0D, 0.0D);
      GL11.glScaled(scl * (double)modelZoom, scl * (double)modelZoom, -scl * (double)modelZoom);
      GL11.glDisable('\u803a');
      GL11.glDisable(2896);
      GL11.glEnable(3008);
      GL11.glEnable(3042);
      int faceNum = model.getFaceNum();
      if(this.drawFace < faceNum * 2) {
         GL11.glColor4d(0.10000000149011612D, 0.10000000149011612D, 0.10000000149011612D, 1.0D);
         GL11.glDisable(3553);
         GL11.glPolygonMode(1032, 6913);
         float lw = GL11.glGetFloat(2849);
         GL11.glLineWidth(1.0F);
         model.renderAll(this.drawFace - faceNum, this.drawFace);
         MCH_RenderAircraft.renderCrawlerTrack((MCH_EntityAircraft)null, this.current.getAcInfo(), partialTicks);
         GL11.glLineWidth(lw);
         GL11.glPolygonMode(1032, 6914);
         GL11.glEnable(3553);
      }

      if(this.drawFace >= faceNum) {
         GL11.glColor4d(1.0D, 1.0D, 1.0D, 1.0D);
         model.renderAll(0, this.drawFace - faceNum);
         MCH_RenderAircraft.renderCrawlerTrack((MCH_EntityAircraft)null, this.current.getAcInfo(), partialTicks);
      }

      GL11.glEnable('\u803a');
      GL11.glEnable(2896);
      GL11.glPopMatrix();
      if(this.drawFace < 10000000) {
         this.drawFace = (int)((float)this.drawFace + 20.0F);
      }

   }

   protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
      W_ScaledResolution scaledresolution = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
      this.scaleFactor = scaledresolution.getScaleFactor();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float z = super.zLevel;
      super.zLevel = 0.0F;
      W_McClient.MOD_bindTexture("textures/gui/drafting_table.png");
      if(this.getScreenId() == 0) {
         this.drawTexturedModalRect(super.guiLeft, super.guiTop, 0, 0, super.xSize, super.ySize);
      }

      if(this.getScreenId() == 1) {
         this.drawTexturedModalRect(super.guiLeft, super.guiTop, 0, super.ySize, super.xSize, super.ySize);
         List list = (List)this.screenButtonList.get(1);
         int index = (int)this.listSlider.getSliderValue() * 2;

         for(int i = 0; i < 6; ++i) {
            W_GuiButton.setVisible((GuiButton)list.get(i), index + i < this.getCurrentList().getRecipeListSize());
         }
      }

      super.zLevel = z;
   }

   public void drawTexturedModalRect(int par1, int par2, int par3, int par4, int par5, int par6) {
      float w = 0.001953125F;
      float h = 0.001953125F;
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV((double)(par1 + 0), (double)(par2 + par6), (double)super.zLevel, (double)((float)(par3 + 0) * w), (double)((float)(par4 + par6) * h));
      tessellator.addVertexWithUV((double)(par1 + par5), (double)(par2 + par6), (double)super.zLevel, (double)((float)(par3 + par5) * w), (double)((float)(par4 + par6) * h));
      tessellator.addVertexWithUV((double)(par1 + par5), (double)(par2 + 0), (double)super.zLevel, (double)((float)(par3 + par5) * w), (double)((float)(par4 + 0) * h));
      tessellator.addVertexWithUV((double)(par1 + 0), (double)(par2 + 0), (double)super.zLevel, (double)((float)(par3 + 0) * w), (double)((float)(par4 + 0) * h));
      tessellator.draw();
   }

   public void drawTexturedModalRect(int dx, int dy, int dw, int dh, int u, int v, int tw, int th) {
      float w = 0.001953125F;
      float h = 0.001953125F;
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV((double)(dx + 0), (double)(dy + dh), (double)super.zLevel, (double)((float)(u + 0) * w), (double)((float)(v + th) * h));
      tessellator.addVertexWithUV((double)(dx + dw), (double)(dy + dh), (double)super.zLevel, (double)((float)(u + tw) * w), (double)((float)(v + th) * h));
      tessellator.addVertexWithUV((double)(dx + dw), (double)(dy + 0), (double)super.zLevel, (double)((float)(u + tw) * w), (double)((float)(v + 0) * h));
      tessellator.addVertexWithUV((double)(dx + 0), (double)(dy + 0), (double)super.zLevel, (double)((float)(u + 0) * w), (double)((float)(v + 0) * h));
      tessellator.draw();
   }

}
