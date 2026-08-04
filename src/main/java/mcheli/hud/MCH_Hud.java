package mcheli.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mcheli.hud.layout.MCH_HudLayoutManager;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.hud.MCH_HudItem;
import mcheli.hud.MCH_HudItemCall;
import mcheli.hud.MCH_HudItemCameraRot;
import mcheli.hud.MCH_HudItemColor;
import mcheli.hud.MCH_HudItemConditional;
import mcheli.hud.MCH_HudItemExit;
import mcheli.hud.MCH_HudItemGraduation;
import mcheli.hud.MCH_HudItemLine;
import mcheli.hud.MCH_HudItemLineStipple;
import mcheli.hud.MCH_HudItemRadar;
import mcheli.hud.MCH_HudItemRect;
import mcheli.hud.MCH_HudItemString;
import mcheli.hud.MCH_HudItemTexture;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_Hud extends MCH_BaseInfo {

   public static final MCH_Hud NoDisp = new MCH_Hud("none", "none");
   public final String name;
   public final String fileName;
   private List list;
   public boolean isWaitEndif;
   private boolean isDrawing;
   public boolean isIfFalse;
   public boolean exit;
   private String layoutGroupId = "";
   private String layoutGroupName = "";
   private boolean layoutGroupMovable = true;
   private final Map<String, Integer> layoutOrdinals = new HashMap<String, Integer>();


   public MCH_Hud(String name, String fname) {
      this.name = name;
      this.fileName = fname;
      this.list = new ArrayList();
      this.isDrawing = false;
      this.isIfFalse = false;
      this.exit = false;
   }

   public void checkData() {
      MCH_HudItem hud;
      for(Iterator i$ = this.list.iterator(); i$.hasNext(); hud.parent = this) {
         hud = (MCH_HudItem)i$.next();
      }

      if(this.isWaitEndif) {
         throw new RuntimeException("Endif not found!");
      }
      if(this.layoutGroupId.length() > 0) throw new RuntimeException("EndLayoutGroup not found for " + this.layoutGroupId);
   }

   public void loadItemData(int fileLine, String item, String data) {
      if(item.equalsIgnoreCase("LayoutGroup")) {
         if(this.layoutGroupId.length() > 0) throw new RuntimeException("Nested LayoutGroup is not supported");
         String[] group = data.split("\\s*,\\s*");
         if(group.length != 3 || (!group[2].equalsIgnoreCase("movable") && !group[2].equalsIgnoreCase("fixed")))
            throw new RuntimeException("LayoutGroup requires: stable-id, display name, movable|fixed");
         this.layoutGroupId = MCH_HudLayoutManager.safeId(group[0]);
         this.layoutGroupName = group[1];
         this.layoutGroupMovable = group[2].equalsIgnoreCase("movable");
         return;
      }
      if(item.equalsIgnoreCase("EndLayoutGroup")) {
         if(this.layoutGroupId.length() == 0) throw new RuntimeException("EndLayoutGroup without LayoutGroup");
         this.layoutGroupId = ""; this.layoutGroupName = ""; this.layoutGroupMovable = true;
         return;
      }
      String[] prm = data.split("\\s*,\\s*");
      int previousSize = this.list.size();
      if(prm != null && prm.length != 0) {
         if(item.equalsIgnoreCase("If")) {
            if(this.isWaitEndif) {
               throw new RuntimeException("Endif not found!");
            }

            this.list.add(new MCH_HudItemConditional(fileLine, false, prm[0]));
            this.isWaitEndif = true;
         } else if(item.equalsIgnoreCase("Endif")) {
            if(!this.isWaitEndif) {
               throw new RuntimeException("IF in a pair can not be found!");
            }

            this.list.add(new MCH_HudItemConditional(fileLine, true, ""));
            this.isWaitEndif = false;
         } else {
            String type;
            if(!item.equalsIgnoreCase("DrawString") && !item.equalsIgnoreCase("DrawCenteredString")) {
               if(item.equalsIgnoreCase("Exit")) {
                  this.list.add(new MCH_HudItemExit(fileLine));
               } else if(item.equalsIgnoreCase("Color")) {
                  MCH_HudItemColor type1;
                  if(prm.length == 1) {
                     type1 = MCH_HudItemColor.createByParams(fileLine, new String[]{prm[0]});
                     if(type1 != null) {
                        this.list.add(type1);
                     }
                  } else if(prm.length == 4) {
                     String[] s = new String[]{prm[0], prm[1], prm[2], prm[3]};
                     type1 = MCH_HudItemColor.createByParams(fileLine, s);
                     if(type1 != null) {
                        this.list.add(type1);
                     }
                  }
               } else if(item.equalsIgnoreCase("DrawTexture")) {
                  if(prm.length >= 9 && prm.length <= 10) {
                     type = prm.length == 10?prm[9]:"0";
                     this.list.add(new MCH_HudItemTexture(fileLine, prm[0], prm[1], prm[2], prm[3], prm[4], prm[5], prm[6], prm[7], prm[8], type));
                  }
               } else if(item.equalsIgnoreCase("DrawRect")) {
                  if(prm.length == 4) {
                     this.list.add(new MCH_HudItemRect(fileLine, prm[0], prm[1], prm[2], prm[3]));
                  }
               } else {
                  int type2;
                  if(item.equalsIgnoreCase("DrawLine")) {
                     type2 = prm.length;
                     if(type2 >= 4 && type2 % 2 == 0) {
                        this.list.add(new MCH_HudItemLine(fileLine, prm));
                     }
                  } else if(item.equalsIgnoreCase("DrawLineStipple")) {
                     type2 = prm.length;
                     if(type2 >= 6 && type2 % 2 == 0) {
                        this.list.add(new MCH_HudItemLineStipple(fileLine, prm));
                     }
                  } else if(item.equalsIgnoreCase("Call")) {
                     type2 = prm.length;
                     if(type2 == 1) {
                        this.list.add(new MCH_HudItemCall(fileLine, prm[0]));
                     }
                  } else if(!item.equalsIgnoreCase("DrawEntityRadar") && !item.equalsIgnoreCase("DrawEnemyRadar")) {
                     if(!item.equalsIgnoreCase("DrawGraduationYaw") && !item.equalsIgnoreCase("DrawGraduationPitch1") && !item.equalsIgnoreCase("DrawGraduationPitch2") && !item.equalsIgnoreCase("DrawGraduationPitch3")) {
                        if(item.equalsIgnoreCase("DrawCameraRot") && prm.length == 2) {
                           this.list.add(new MCH_HudItemCameraRot(fileLine, prm[0], prm[1]));
                        }
                     } else if(prm.length == 4) {
                        byte type3 = -1;
                        if(item.equalsIgnoreCase("DrawGraduationYaw")) {
                           type3 = 0;
                        }

                        if(item.equalsIgnoreCase("DrawGraduationPitch1")) {
                           type3 = 1;
                        }

                        if(item.equalsIgnoreCase("DrawGraduationPitch2")) {
                           type3 = 2;
                        }

                        if(item.equalsIgnoreCase("DrawGraduationPitch3")) {
                           type3 = 3;
                        }

                        this.list.add(new MCH_HudItemGraduation(fileLine, type3, prm[0], prm[1], prm[2], prm[3]));
                     }
                  } else if(prm.length == 5) {
                     this.list.add(new MCH_HudItemRadar(fileLine, item.equalsIgnoreCase("DrawEntityRadar"), prm[0], prm[1], prm[2], prm[3], prm[4]));
                  }
               }
            } else if(prm.length >= 3) {
               type = prm[2];
               if(type.charAt(0) == 34 && type.charAt(type.length() - 1) == 34) {
                  type = type.substring(1, type.length() - 1);
                  this.list.add(new MCH_HudItemString(fileLine, prm[0], prm[1], type, prm, item.equalsIgnoreCase("DrawCenteredString")));
               }
            }
         }

      }
      if(this.list.size() > previousSize) {
         MCH_HudItem added = (MCH_HudItem)this.list.get(this.list.size() - 1);
         boolean control = item.equalsIgnoreCase("If") || item.equalsIgnoreCase("Endif") || item.equalsIgnoreCase("Color") || item.equalsIgnoreCase("Exit");
         boolean viewAligned = added instanceof MCH_HudItemGraduation || added instanceof MCH_HudItemCameraRot;
         boolean implicitCall = added instanceof MCH_HudItemCall && this.layoutGroupId.length() == 0;
         String key = item.toLowerCase() + ":" + this.layoutGroupId;
         Integer ordinal = this.layoutOrdinals.get(key);
         int value = ordinal == null ? 0 : ordinal.intValue();
         this.layoutOrdinals.put(key, Integer.valueOf(value + 1));
         added.setLayoutMetadata(item, fingerprint(item, prm), this.layoutGroupId, this.layoutGroupName,
               !control && !viewAligned && !implicitCall && this.layoutGroupMovable, value);
      }
   }

   private static String fingerprint(String directive, String[] prm) {
      StringBuilder b = new StringBuilder(directive);
      int start = directive.equalsIgnoreCase("DrawLineStipple") ? 2 : 0;
      for(int i = start; i < prm.length; ++i) {
         // Most directives store their first two expressions as the movable position.
         if(i == start || i == start + 1) continue;
         b.append(':').append(prm[i].trim().toLowerCase());
      }
      return b.toString();
   }

   public List<MCH_HudItem> getItems() {
      return Collections.unmodifiableList(new ArrayList<MCH_HudItem>(this.list));
   }

   public void draw(MCH_EntityBaseVehicle ac, EntityPlayer player, float partialTicks) {
      MCH_HudItem.ac = ac;
      MCH_HudItem.player = player;
      MCH_HudItem.partialTicks = partialTicks;
      W_ScaledResolution scaledresolution = new W_ScaledResolution(MCH_HudItem.mc, MCH_HudItem.mc.displayWidth, MCH_HudItem.mc.displayHeight);
      MCH_HudItem.scaleFactor = scaledresolution.getScaleFactor();
      if(MCH_HudItem.scaleFactor <= 0) {
         MCH_HudItem.scaleFactor = 1;
      }

      MCH_HudItem.width = (double)MCH_HudItem.mc.displayWidth / (double)MCH_HudItem.scaleFactor;
      MCH_HudItem.height = (double)MCH_HudItem.mc.displayHeight / (double)MCH_HudItem.scaleFactor;
      MCH_HudItem.centerX = MCH_HudItem.width / 2.0D;
      MCH_HudItem.centerY = MCH_HudItem.height / 2.0D;
      this.isIfFalse = false;
      this.isDrawing = false;
      this.exit = false;
      if(ac != null && ac.getAcInfo() != null && player != null) {
         MCH_HudItem.update();
         MCH_HudLayoutManager.beginHud(this.name);
         try { this.drawItems(); } finally { MCH_HudLayoutManager.endHud(); }
         MCH_HudItem.drawVarMap();
      }

   }

   protected void drawItems() {
      if(!this.isDrawing) {
         this.isDrawing = true;
         Iterator i$ = this.list.iterator();

         while(i$.hasNext()) {
            MCH_HudItem hud = (MCH_HudItem)i$.next();
            byte line = -1;

            try {
               int line1 = hud.fileLine;
               if(hud.canExecute()) {
                  if(hud.isLayoutMovable()) {
                     final MCH_HudItem drawItem = hud;
                     String id = MCH_HudLayoutManager.parsedId(this.name, hud.fileLine, hud.getLayoutDirective(), hud.getLayoutOrdinal(), hud.getLayoutGroupId());
                     MCH_HudLayoutManager.renderParsed(MCH_HudLayoutManager.currentParsedProfileId(), id, this.name,
                           hud.getLayoutFingerprint(), hud.fileLine, hud.getLayoutDisplayName(), hud.isLayoutMovable(),
                           new Runnable() { public void run() { drawItem.execute(); } });
                  } else hud.execute();
                  if(this.exit) {
                     break;
                  }
               }
            } catch (Exception var5) {
               MCH_Lib.Log("#### Draw HUD Error!!!: line=%d, file=%s", new Object[]{Integer.valueOf(line), this.fileName});
               var5.printStackTrace();
               throw new RuntimeException(var5);
            }
         }

         this.exit = false;
         this.isIfFalse = false;
         this.isDrawing = false;
      }

   }

}
