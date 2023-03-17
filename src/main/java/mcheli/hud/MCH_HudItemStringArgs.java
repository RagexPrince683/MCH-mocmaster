package mcheli.hud;


public enum MCH_HudItemStringArgs {

   NONE("NONE", 0),
   NAME("NAME", 1),
   ALTITUDE("ALTITUDE", 2),
   DATE("DATE", 3),
   MC_THOR("MC_THOR", 4),
   MC_TMIN("MC_TMIN", 5),
   MC_TSEC("MC_TSEC", 6),
   MAX_HP("MAX_HP", 7),
   HP("HP", 8),
   HP_PER("HP_PER", 9),
   POS_X("POS_X", 10),
   POS_Y("POS_Y", 11),
   POS_Z("POS_Z", 12),
   MOTION_X("MOTION_X", 13),
   MOTION_Y("MOTION_Y", 14),
   MOTION_Z("MOTION_Z", 15),
   INVENTORY("INVENTORY", 16),
   WPN_NAME("WPN_NAME", 17),
   WPN_AMMO("WPN_AMMO", 18),
   WPN_RM_AMMO("WPN_RM_AMMO", 19),
   RELOAD_PER("RELOAD_PER", 20),
   RELOAD_SEC("RELOAD_SEC", 21),
   MORTAR_DIST("MORTAR_DIST", 22),
   MC_VER("MC_VER", 23),
   MOD_VER("MOD_VER", 24),
   MOD_NAME("MOD_NAME", 25),
   YAW("YAW", 26),
   PITCH("PITCH", 27),
   ROLL("ROLL", 28),
   PLYR_YAW("PLYR_YAW", 29),
   PLYR_PITCH("PLYR_PITCH", 30),
   TVM_POS_X("TVM_POS_X", 31),
   TVM_POS_Y("TVM_POS_Y", 32),
   TVM_POS_Z("TVM_POS_Z", 33),
   TVM_DIFF("TVM_DIFF", 34),
   CAM_ZOOM("CAM_ZOOM", 35),
   UAV_DIST("UAV_DIST", 36),
   KEY_GUI("KEY_GUI", 37),
   THROTTLE("THROTTLE", 38),
   TGT_AZ("TGT_AZ", 39),
   TGT_RANGE("RANGE", 40),
   TGT_ALT("TGT_ALT", 41),
   TGT_SPD("TGT_SPD", 42);
	
   // $FF: synthetic field
   private static final MCH_HudItemStringArgs[] $VALUES = new MCH_HudItemStringArgs[]{NONE, NAME, ALTITUDE, DATE, MC_THOR, MC_TMIN, MC_TSEC, MAX_HP, HP, HP_PER, POS_X, POS_Y, POS_Z, MOTION_X, MOTION_Y, MOTION_Z, INVENTORY, WPN_NAME, WPN_AMMO, WPN_RM_AMMO, RELOAD_PER, RELOAD_SEC, MORTAR_DIST, MC_VER, MOD_VER, MOD_NAME, YAW, PITCH, ROLL, PLYR_YAW, PLYR_PITCH, TVM_POS_X, TVM_POS_Y, TVM_POS_Z, TVM_DIFF, CAM_ZOOM, UAV_DIST, KEY_GUI, THROTTLE, TGT_AZ,TGT_RANGE,TGT_ALT,TGT_SPD};


   private MCH_HudItemStringArgs(String var1, int var2) {}

   public static MCH_HudItemStringArgs toArgs(String name) {
      MCH_HudItemStringArgs a = NONE;

      try {
         try {
        	
            a = valueOf(name.toUpperCase());
         } catch (Exception var6) {
            var6.printStackTrace();
         }

         return a;
      } finally {
         ;
      }
   }

}
