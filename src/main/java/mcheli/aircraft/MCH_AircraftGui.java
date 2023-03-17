package mcheli.aircraft;

import cpw.mods.fml.common.registry.GameData;
import mcheli.MCH_Lib;
import mcheli.MCH_PacketIndOpenScreen;
import mcheli.command.MCH_PacketCommandSave;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.weapon.MCH_WeaponDummy;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class MCH_AircraftGui extends W_GuiContainer {

	private final EntityPlayer thePlayer;
	private final MCH_EntityAircraft aircraft;
	private int scaleFactor;
	private GuiButton buttonReload;
	private GuiButton buttonNext;
	private GuiButton buttonPrev;
	private GuiButton buttonInventory;
	private int currentWeaponId;
	private int reloadWait;
	private GuiTextField editCommand;
	public static final int BUTTON_RELOAD = 1;
	public static final int BUTTON_NEXT = 2;
	public static final int BUTTON_PREV = 3;
	public static final int BUTTON_CLOSE = 4;
	public static final int BUTTON_CONFIG = 5;
	public static final int BUTTON_INVENTORY = 6;


	public MCH_AircraftGui(EntityPlayer player, MCH_EntityAircraft ac) {
		super(new MCH_AircraftGuiContainer(player, ac));
		this.aircraft = ac;
		this.thePlayer = player;
		super.xSize = 210;
		super.ySize = 236;
		this.buttonReload = null;
		this.currentWeaponId = 0;
	}

	  public void initGui() {
	      super.initGui();
	      super.buttonList.clear();
	      this.buttonReload = new GuiButton(1, super.guiLeft + 85, super.guiTop + 40, 50, 20, "Reload");
	      this.buttonNext = new GuiButton(3, super.guiLeft + 140, super.guiTop + 40, 20, 20, "<<");
	      this.buttonPrev = new GuiButton(2, super.guiLeft + 160, super.guiTop + 40, 20, 20, ">>");
	      this.buttonReload.enabled = this.canReload(this.thePlayer);
	      this.buttonNext.enabled = this.aircraft.getWeaponNum() >= 2;
	      this.buttonPrev.enabled = this.aircraft.getWeaponNum() >= 2;
	      this.buttonInventory = new GuiButton(6, super.guiLeft + 210 - 30 - 60, super.guiTop + 90, 80, 20, "Inventory");
	      super.buttonList.add(new GuiButton(5, super.guiLeft + 210 - 30 - 60, super.guiTop + 110, 80, 20, "MOD Options"));
	      super.buttonList.add(new GuiButton(4, super.guiLeft + 210 - 30 - 20, super.guiTop + 10, 40, 20, "Close"));

		  super.buttonList.add(new GuiButton(7, super.guiLeft, super.guiTop + 110, 80, 20, "Hardpoints"));

		  super.buttonList.add(this.buttonReload);
	      super.buttonList.add(this.buttonNext);
	      super.buttonList.add(this.buttonPrev);
	      if(this.aircraft != null && this.aircraft.getSizeInventory() > 0) {
	         super.buttonList.add(this.buttonInventory);
	      }

	      this.editCommand = new GuiTextField(super.fontRendererObj, super.guiLeft + 25, super.guiTop + 215, 160, 15);
	      this.editCommand.setText(this.aircraft.getCommand());
	      this.editCommand.setMaxStringLength(512);
	      this.currentWeaponId = 0;
	      this.reloadWait = 10;
	   }



	public void closeScreen() {
		String[] split = editCommand.getText().split(" ");
		try {
			
			if((!split[0].equalsIgnoreCase("debug") && !split[0].equalsIgnoreCase("fuel"))|| (thePlayer.getDisplayName().equalsIgnoreCase("mocpages"))) {
				MCH_PacketCommandSave.send(this.editCommand.getText());
			}else {
				System.out.println(thePlayer.getDisplayName());
			}
			
			if(split[0].equalsIgnoreCase("tgt")) {
				int x = Integer.parseInt(split[1]);
				int y = Integer.parseInt(split[2]);
				int z = Integer.parseInt(split[3]);
				MCH_ParticlesUtil.spawnMarkPoint(this.thePlayer, x, y, z);
				aircraft.target = Vec3.createVectorHelper(x, y, z);
			}else if(split[0].equalsIgnoreCase("base")) {
				if(this.aircraft instanceof MCP_EntityPlane) {
					MCP_EntityPlane p = (MCP_EntityPlane)this.aircraft;
					if(p.onGround || MCH_Lib.getBlockIdY(p, 1, -2) > 0) {
						p.base.x = p.posX;
						p.base.y = p.posZ;
						this.thePlayer.addChatComponentMessage(new ChatComponentText("Base location set to X: " + p.base.x + " Z: " + p.base.y));
					}
				}
			}else if(split[0].equalsIgnoreCase("hardpoint")) {
				this.aircraft.weaponTest(split[1], split[2]);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		this.mc.thePlayer.closeScreen();	
	}

	public boolean canReload(EntityPlayer player) {
		return this.aircraft.canPlayerSupplyAmmo(player, this.currentWeaponId);
	}

	public void updateScreen() {
		super.updateScreen();
		if(this.reloadWait > 0) {
			--this.reloadWait;
			if(this.reloadWait == 0) {
				this.buttonReload.enabled = this.canReload(this.thePlayer);
				this.reloadWait = 20;
			}
		}
		try {
		this.editCommand.updateCursorCounter();
		}catch(Exception e) {}
	}

	protected void mouseClicked(int p_73864_1_, int p_73864_2_, int p_73864_3_) {
		this.editCommand.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
		super.mouseClicked(p_73864_1_, p_73864_2_, p_73864_3_);
	}	

	public void onGuiClosed() {
		super.onGuiClosed();
	}


	  protected void actionPerformed(GuiButton button) {
	      super.actionPerformed(button);
	      if(button.enabled) {
	         switch(button.id) {
	         case 1:
	            this.buttonReload.enabled = this.canReload(this.thePlayer);
	            if(this.buttonReload.enabled) {
	               MCH_PacketIndReload.send(this.aircraft, this.currentWeaponId);
	               this.aircraft.supplyAmmo(this.currentWeaponId);
	               this.reloadWait = 3;
	               this.buttonReload.enabled = false;
	            }
	            break;
	         case 2:
	            ++this.currentWeaponId;
	            if(this.currentWeaponId >= this.aircraft.getWeaponNum()) {
	               this.currentWeaponId = 0;
	            }

	            this.buttonReload.enabled = this.canReload(this.thePlayer);
	            break;
	         case 3:
	            --this.currentWeaponId;
	            if(this.currentWeaponId < 0) {
	               this.currentWeaponId = this.aircraft.getWeaponNum() - 1;
	            }

	            this.buttonReload.enabled = this.canReload(this.thePlayer);
	            break;
	         case 4:
	            this.closeScreen();
	            break;
	         case 5:
	            MCH_PacketIndOpenScreen.send(2);
	            break;
	         case 6:
	            MCH_PacketIndOpenScreen.send(3);
	            break;
			case 7:
			 MCH_PacketIndOpenScreen.send(6);
			 }
	      }
	   }



	   protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		      super.drawGuiContainerForegroundLayer(par1, par2);
		      MCH_EntityAircraft ac = this.aircraft;
		      this.drawString(ac.getGuiInventory().getInventoryName(), 10, 10, 16777215);
		      if(this.aircraft.getNumEjectionSeat() > 0) {
		         this.drawString("Parachute", 9, 95, 16777215);
		      }

		      if(this.aircraft.getWeaponNum() > 0) {
		         MCH_WeaponSet ws = this.aircraft.getWeapon(this.currentWeaponId);
		         if(ws != null && !(ws.getFirstWeapon() instanceof MCH_WeaponDummy)) {
		            this.drawString(ws.getName(), 79, 30, 16777215);
		            int rest = ws.getRestAllAmmoNum() + ws.getAmmoNum();
		            int color = rest == 0?16711680:(rest == ws.getAllAmmoNum()?2675784:16777215);
		            String s = String.format("%4d/%4d", new Object[]{Integer.valueOf(rest), Integer.valueOf(ws.getAllAmmoNum())});
		            this.drawString(s, 145, 70, color);
		            int itemPosX = 90;

		            Iterator i$;
		            MCH_WeaponInfo.RoundItem r;
		            for(i$ = ws.getInfo().roundItems.iterator(); i$.hasNext(); itemPosX += 20) {
		               r = (MCH_WeaponInfo.RoundItem)i$.next();
		               this.drawString("" + r.num, itemPosX, 80, 16777215);
						this.drawString("" + r.itemStack.getDisplayName(), itemPosX, 90, 16777215);

					}

		            itemPosX = 85;

		            for(i$ = ws.getInfo().roundItems.iterator(); i$.hasNext(); itemPosX += 20) {
		               r = (MCH_WeaponInfo.RoundItem)i$.next();
		               Item i = GameData.getItemRegistry().getObject(r.itemName);
					//	this.aircraft.print("Item Name: " + r.itemName + " r.ItemStack null: " + (r.itemStack.getItem() == null) + " item null: " + (i == null));

		              // ItemStack stack = new ItemStack(i);
		              // r.itemStack = stack;
		               this.drawItemStack(r.itemStack, itemPosX, 62);
		            }
		         }
		      } else {
		         this.drawString("None", 79, 45, 16777215);
		      }

		   }
	   
	protected void keyTyped(char c, int code) {
		if(code == 1) {
			this.closeScreen();
		} else if(code == 28) {
			String s = this.editCommand.getText().trim();
			if(s.startsWith("/")) {
				s = s.substring(1);
			}

			if(!s.isEmpty()) {
				// MCH_PacketIndMultiplayCommand.send(768, s);
			}
		} else {
			this.editCommand.textboxKeyTyped(c, code);
		}

	}


	   protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
	      W_ScaledResolution scaledresolution = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
	      this.scaleFactor = scaledresolution.getScaleFactor();
	      W_McClient.MOD_bindTexture("textures/gui/gui.png");
	      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	      int x = (super.width - super.xSize) / 2;
	      int y = (super.height - super.ySize) / 2;
	      this.drawTexturedModalRect(x, y, 0, 0, super.xSize, super.ySize);

	      int ff;
	      for(ff = 0; ff < this.aircraft.getNumEjectionSeat(); ++ff) {
	         this.drawTexturedModalRect(x + 10 + 18 * ff - 1, y + 105 - 1, 215, 55, 18, 18);
	      }

	      ff = (int)(this.aircraft.getFuelP() * 50.0F);
	      if(ff >= 99) {
	         ff = 100;
	      }

	      this.drawTexturedModalRect(x + 57, y + 30 + 50 - ff, 215, 0, 12, ff);
	      ff = (int)((double)(this.aircraft.getFuelP() * 100.0F) + 0.5D);
	      int color = ff > 20?-14101432:16711680;
	      this.drawString(String.format("%3d", new Object[]{Integer.valueOf(ff)}) + "%", x + 30, y + 65, color);
	      this.editCommand.drawTextBox();
	   }
}
