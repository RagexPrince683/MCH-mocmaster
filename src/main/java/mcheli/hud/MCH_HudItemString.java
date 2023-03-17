package mcheli.hud;

import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.Date;

public class MCH_HudItemString extends MCH_HudItem {

   private final String posX;
   private final String posY;
   private final String format;
   private final MCH_HudItemStringArgs[] args;
   private final boolean isCenteredString;


   public MCH_HudItemString(int fileLine, String posx, String posy, String fmt, String[] arg, boolean centered) {
      super(fileLine);
      this.posX = posx.toLowerCase();
      this.posY = posy.toLowerCase();
      this.format = fmt;
      int len = arg.length < 3?0:arg.length - 3;
      this.args = new MCH_HudItemStringArgs[len];

      for(int i = 0; i < len; ++i) {
         this.args[i] = MCH_HudItemStringArgs.toArgs(arg[3 + i]);
      }

      this.isCenteredString = centered;
   }

   public void execute() {
      int x = (int)(MCH_HudItem.centerX + calc(this.posX));
      int y = (int)(MCH_HudItem.centerY + calc(this.posY));
      long dateCount = Minecraft.getMinecraft().thePlayer.worldObj.getTotalWorldTime();
      int worldTime = (int)((MCH_HudItem.ac.worldObj.getWorldTime() + 6000L) % 24000L);
      Date date = new Date();
      Object[] prm = new Object[this.args.length];
      double hp_per = MCH_HudItem.ac.getMaxHP() > 0?(double)MCH_HudItem.ac.getHP() / (double)MCH_HudItem.ac.getMaxHP():0.0D;

      for(int i = 0; i < prm.length; ++i) {
         switch(MCH_HudItemString.NamelessClass1090813585.$SwitchMap$mcheli$hud$MCH_HudItemStringArgs[this.args[i].ordinal()]) {
         case 1:
            prm[i] = MCH_HudItem.ac.getAcInfo().displayName;
            break;
         case 2:
            prm[i] = Integer.valueOf(MCH_HudItem.Altitude);
            break;
         case 3:
            prm[i] = date;
            break;
         case 4:
            prm[i] = Integer.valueOf(worldTime / 1000);
            break;
         case 5:
            prm[i] = Integer.valueOf(worldTime % 1000 * 36 / 10 / 60);
            break;
         case 6:
            prm[i] = Integer.valueOf(worldTime % 1000 * 36 / 10 % 60);
            break;
         case 7:
            prm[i] = Integer.valueOf(MCH_HudItem.ac.getMaxHP());
            break;
         case 8:
            prm[i] = Integer.valueOf(MCH_HudItem.ac.getHP());
            break;
         case 9:
            prm[i] = Double.valueOf(hp_per * 100.0D);
            break;
         case 10:
            prm[i] = Double.valueOf(MCH_HudItem.ac.posX);
            break;
         case 11:
            prm[i] = Double.valueOf(MCH_HudItem.ac.posY);
            break;
         case 12:
            prm[i] = Double.valueOf(MCH_HudItem.ac.posZ);
            break;
         case 13:
            prm[i] = Double.valueOf(MCH_HudItem.ac.motionX);
            break;
         case 14:
            prm[i] = Double.valueOf(MCH_HudItem.ac.motionY);
            break;
         case 15:
            prm[i] = Double.valueOf(MCH_HudItem.ac.motionZ);
            break;
         case 16:
            prm[i] = Integer.valueOf(MCH_HudItem.ac.getSizeInventory());
            break;
         case 17:
            prm[i] = MCH_HudItem.WeaponName;
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }
            break;
         case 18:
            prm[i] = MCH_HudItem.WeaponAmmo;
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }

            if(MCH_HudItem.CurrentWeapon.getAmmoNumMax() <= 0) {
               return;
            }
            break;
         case 19:
            prm[i] = MCH_HudItem.WeaponAllAmmo;
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }

            if(MCH_HudItem.CurrentWeapon.getAmmoNumMax() <= 0) {
               return;
            }
            break;
         case 20:
            prm[i] = Float.valueOf(MCH_HudItem.ReloadPer);
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }
            break;
         case 21:
            prm[i] = Float.valueOf(MCH_HudItem.ReloadSec);
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }
            break;
         case 22:
            prm[i] = Float.valueOf(MCH_HudItem.MortarDist);
            if(MCH_HudItem.CurrentWeapon == null) {
               return;
            }
            break;
         case 23:
            prm[i] = "1.7.10";
            break;
         case 24:
            prm[i] = MCH_MOD.VER; 
            break;
         case 25:
            prm[i] = "MC Helicopter MOD";
            break;
         case 26:
            prm[i] = Double.valueOf(MCH_Lib.getRotate360((double)(MCH_HudItem.ac.getRotYaw() + 180.0F)));
            break;
         case 27:
            prm[i] = Float.valueOf(-MCH_HudItem.ac.getRotPitch());
            break;
         case 28:
            prm[i] = Float.valueOf(MathHelper.wrapAngleTo180_float(MCH_HudItem.ac.getRotRoll()));
            break;
         case 29:
            prm[i] = Double.valueOf(MCH_Lib.getRotate360((double)(MCH_HudItem.player.rotationYaw + 180.0F)));
            break;
         case 30:
            prm[i] = Float.valueOf(-MCH_HudItem.player.rotationPitch);
            break;
         case 31:
            prm[i] = Double.valueOf(MCH_HudItem.TVM_PosX);
            break;
         case 32:
            prm[i] = Double.valueOf(MCH_HudItem.TVM_PosY);
            break;
         case 33:
            prm[i] = Double.valueOf(MCH_HudItem.TVM_PosZ);
            break;
         case 34:
            prm[i] = Double.valueOf(MCH_HudItem.TVM_Diff);
            break;
         case 35:
            prm[i] = Float.valueOf(MCH_HudItem.ac.camera.getCameraZoom());
            break;
         case 36:
            prm[i] = Double.valueOf(MCH_HudItem.UAV_Dist);
            break;
         case 37:
            MCH_Config var10002 = MCH_MOD.config;
            prm[i] = MCH_KeyName.getDescOrName(MCH_Config.KeyGUI.prmInt);
            break;
         case 38:
            prm[i] = Double.valueOf(MCH_HudItem.ac.getCurrentThrottle() * 100.0D);
         case 39:
        
         }
      }

      if(this.isCenteredString) {
         this.drawCenteredString(String.format(this.format, prm), x, y, MCH_HudItem.colorSetting);
      } else {
         this.drawString(String.format(this.format, prm), x, y, MCH_HudItem.colorSetting);
      }

   }

   // $FF: synthetic class
   static class NamelessClass1090813585 {

      // $FF: synthetic field
      static final int[] $SwitchMap$mcheli$hud$MCH_HudItemStringArgs = new int[MCH_HudItemStringArgs.values().length];


      static {
         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.NAME.ordinal()] = 1;
         } catch (NoSuchFieldError var39) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.ALTITUDE.ordinal()] = 2;
         } catch (NoSuchFieldError var38) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.DATE.ordinal()] = 3;
         } catch (NoSuchFieldError var37) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MC_THOR.ordinal()] = 4;
         } catch (NoSuchFieldError var36) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MC_TMIN.ordinal()] = 5;
         } catch (NoSuchFieldError var35) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MC_TSEC.ordinal()] = 6;
         } catch (NoSuchFieldError var34) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MAX_HP.ordinal()] = 7;
         } catch (NoSuchFieldError var33) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.HP.ordinal()] = 8;
         } catch (NoSuchFieldError var32) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.HP_PER.ordinal()] = 9;
         } catch (NoSuchFieldError var31) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.POS_X.ordinal()] = 10;
         } catch (NoSuchFieldError var30) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.POS_Y.ordinal()] = 11;
         } catch (NoSuchFieldError var29) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.POS_Z.ordinal()] = 12;
         } catch (NoSuchFieldError var28) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MOTION_X.ordinal()] = 13;
         } catch (NoSuchFieldError var27) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MOTION_Y.ordinal()] = 14;
         } catch (NoSuchFieldError var26) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MOTION_Z.ordinal()] = 15;
         } catch (NoSuchFieldError var25) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.INVENTORY.ordinal()] = 16;
         } catch (NoSuchFieldError var24) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.WPN_NAME.ordinal()] = 17;
         } catch (NoSuchFieldError var23) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.WPN_AMMO.ordinal()] = 18;
         } catch (NoSuchFieldError var22) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.WPN_RM_AMMO.ordinal()] = 19;
         } catch (NoSuchFieldError var21) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.RELOAD_PER.ordinal()] = 20;
         } catch (NoSuchFieldError var20) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.RELOAD_SEC.ordinal()] = 21;
         } catch (NoSuchFieldError var19) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MORTAR_DIST.ordinal()] = 22;
         } catch (NoSuchFieldError var18) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MC_VER.ordinal()] = 23;
         } catch (NoSuchFieldError var17) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MOD_VER.ordinal()] = 24;
         } catch (NoSuchFieldError var16) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.MOD_NAME.ordinal()] = 25;
         } catch (NoSuchFieldError var15) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.YAW.ordinal()] = 26;
         } catch (NoSuchFieldError var14) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.PITCH.ordinal()] = 27;
         } catch (NoSuchFieldError var13) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.ROLL.ordinal()] = 28;
         } catch (NoSuchFieldError var12) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.PLYR_YAW.ordinal()] = 29;
         } catch (NoSuchFieldError var11) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.PLYR_PITCH.ordinal()] = 30;
         } catch (NoSuchFieldError var10) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TVM_POS_X.ordinal()] = 31;
         } catch (NoSuchFieldError var9) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TVM_POS_Y.ordinal()] = 32;
         } catch (NoSuchFieldError var8) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TVM_POS_Z.ordinal()] = 33;
         } catch (NoSuchFieldError var7) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TVM_DIFF.ordinal()] = 34;
         } catch (NoSuchFieldError var6) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.CAM_ZOOM.ordinal()] = 35;
         } catch (NoSuchFieldError var5) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.UAV_DIST.ordinal()] = 36;
         } catch (NoSuchFieldError var4) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.KEY_GUI.ordinal()] = 37;
         } catch (NoSuchFieldError var3) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.THROTTLE.ordinal()] = 38;
         } catch (NoSuchFieldError var2) {
            ;
         }

         try {
            $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TGT_AZ.ordinal()] = 39;
         } catch (NoSuchFieldError var1) {
            ;
         }
         
         try {
             $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TGT_RANGE.ordinal()] = 40;
          } catch (NoSuchFieldError var1) {
             ;
          }
         
         try {
             $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TGT_ALT.ordinal()] = 41;
          } catch (NoSuchFieldError var1) {
             ;
          }
         
         try {
             $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.TGT_SPD.ordinal()] = 42;
          } catch (NoSuchFieldError var1) {
             ;
          }
         
         
         try {
             $SwitchMap$mcheli$hud$MCH_HudItemStringArgs[MCH_HudItemStringArgs.NONE.ordinal()] = 43	;
          } catch (NoSuchFieldError var1) {
             ;
          }
         
      }
   }
}
