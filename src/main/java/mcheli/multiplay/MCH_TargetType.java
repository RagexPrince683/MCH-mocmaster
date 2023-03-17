package mcheli.multiplay;


public enum MCH_TargetType {

   NONE("NONE", 0),
   OTHER_MOB("OTHER_MOB", 1),
   MONSTER("MONSTER", 2),
   NO_TEAM_PLAYER("NO_TEAM_PLAYER", 3),
   SAME_TEAM_PLAYER("SAME_TEAM_PLAYER", 4),
   OTHER_TEAM_PLAYER("OTHER_TEAM_PLAYER", 5),
   POINT("POINT", 6);
   // $FF: synthetic field
   private static final MCH_TargetType[] $VALUES = new MCH_TargetType[]{NONE, OTHER_MOB, MONSTER, NO_TEAM_PLAYER, SAME_TEAM_PLAYER, OTHER_TEAM_PLAYER, POINT};


   private MCH_TargetType(String var1, int var2) {}

}
