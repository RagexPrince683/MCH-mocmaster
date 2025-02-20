/*     */ package mcheli.mob;
/*     */
/*     */ import cpw.mods.fml.relauncher.Side;
/*     */ import cpw.mods.fml.relauncher.SideOnly;
/*     */ import java.util.List;
/*     */ import mcheli.aircraft.MCH_EntityAircraft;
/*     */ import mcheli.aircraft.MCH_EntitySeat;
/*     */ import mcheli.wrapper.W_Item;
/*     */ import mcheli.wrapper.W_WorldFunc;
/*     */ import net.minecraft.client.renderer.texture.IIconRegister;
/*     */ import net.minecraft.creativetab.CreativeTabs;
/*     */ import net.minecraft.entity.Entity;
/*     */ import net.minecraft.entity.player.EntityPlayer;
/*     */ import net.minecraft.item.ItemStack;
/*     */ import net.minecraft.scoreboard.ScorePlayerTeam;
/*     */ import net.minecraft.util.ChatComponentText;
/*     */ import net.minecraft.util.EnumChatFormatting;
/*     */ import net.minecraft.util.IChatComponent;
/*     */ import net.minecraft.util.IIcon;
/*     */ import net.minecraft.util.MathHelper;
/*     */ import net.minecraft.util.Vec3;
/*     */ import net.minecraft.world.World;
/*     */
/*     */ public class MCH_ItemSpawnGunner
        /*     */   extends W_Item {
    /*  26 */   public int primaryColor = 16777215;
    /*  27 */   public int secondaryColor = 16777215;
    /*  28 */   public int targetType = 0;
    /*     */   @SideOnly(Side.CLIENT)
    /*     */   private IIcon theIcon;
    /*     */
    /*     */   public MCH_ItemSpawnGunner() {
        /*  33 */     this.maxStackSize = 1;
        /*  34 */     setCreativeTab(CreativeTabs.tabTransport); } public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        /*     */     MCH_EntityGunner mCH_EntityGunner = null;
        /*     */     MCH_EntitySeat mCH_EntitySeat = null;
        /*     */     MCH_EntityAircraft mCH_EntityAircraft = null;
        /*  38 */     float f = 1.0F;
        /*  39 */     float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        /*  40 */     float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        /*  41 */     double dx = player.prevPosX + (player.posX - player.prevPosX) * f;
        /*  42 */     double dy = player.prevPosY + (player.posY - player.prevPosY) * f + 1.62D - player.yOffset;
        /*  43 */     double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
        /*  44 */     Vec3 vec3 = Vec3.createVectorHelper(dx, dy, dz);
        /*  45 */     float f3 = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        /*  46 */     float f4 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        /*  47 */     float f5 = -MathHelper.cos(-pitch * 0.017453292F);
        /*  48 */     float f6 = MathHelper.sin(-pitch * 0.017453292F);
        /*  49 */     float f7 = f4 * f5;
        /*  50 */     float f8 = f3 * f5;
        /*  51 */     double d3 = 5.0D;
        /*  52 */     Vec3 vec31 = vec3.addVector(f7 * d3, f6 * d3, f8 * d3);
        /*  53 */     List<MCH_EntityGunner> list = world.getEntitiesWithinAABB(MCH_EntityGunner.class, player.boundingBox.expand(5.0D, 5.0D, 5.0D));
        /*  54 */     Entity target = null;
        /*     */
        /*     */     int i;
        /*  57 */     for (i = 0; i < list.size(); i++) {
            /*  58 */       MCH_EntityGunner gunner = list.get(i);
            /*  59 */       if (gunner.boundingBox.calculateIntercept(vec3, vec31) != null && (target == null || player.getDistanceSqToEntity((Entity)gunner) < player.getDistanceSqToEntity(target))) {
                /*  60 */         mCH_EntityGunner = gunner;
                /*     */       }
            /*     */     }
        /*     */
        /*  64 */     if (mCH_EntityGunner == null) {
            /*  65 */       list = world.getEntitiesWithinAABB(MCH_EntitySeat.class, player.boundingBox.expand(5.0D, 5.0D, 5.0D));
            /*     */
            /*  67 */       for (i = 0; i < list.size(); i++) {
                /*  68 */         MCH_EntitySeat seat = (MCH_EntitySeat)list.get(i);
                /*  69 */         if (seat.getParent() != null && seat.getParent().getAcInfo() != null && seat.boundingBox.calculateIntercept(vec3, vec31) != null && (mCH_EntityGunner == null || player.getDistanceSqToEntity((Entity)seat) < player.getDistanceSqToEntity((Entity)mCH_EntityGunner))) {
                    /*  70 */           if (seat.riddenByEntity instanceof MCH_EntityGunner) {
                        /*  71 */             Entity entity = seat.riddenByEntity;
                        /*     */           } else {
                        /*  73 */             mCH_EntitySeat = seat;
                        /*     */           }
                    /*     */         }
                /*     */       }
            /*     */     }
        /*     */
        /*  79 */     if (mCH_EntitySeat == null) {
            /*  80 */       list = world.getEntitiesWithinAABB(MCH_EntityAircraft.class, player.boundingBox.expand(5.0D, 5.0D, 5.0D));
            /*     */
            /*  82 */       for (i = 0; i < list.size(); i++) {
                /*  83 */         MCH_EntityAircraft ac = (MCH_EntityAircraft)list.get(i);
                /*  84 */         if (!ac.isUAV() && ac.getAcInfo() != null && ac.boundingBox.calculateIntercept(vec3, vec31) != null && (mCH_EntitySeat == null || player.getDistanceSqToEntity((Entity)ac) < player.getDistanceSqToEntity((Entity)mCH_EntitySeat))) {
                    /*  85 */           if (ac.getRiddenByEntity() instanceof MCH_EntityGunner) {
                        /*  86 */             Entity entity = ac.getRiddenByEntity();
                        /*     */           } else {
                        /*  88 */             mCH_EntityAircraft = ac;
                        /*     */           }
                    /*     */         }
                /*     */       }
            /*     */     }
        /*     */
        /*  94 */     if (mCH_EntityAircraft instanceof MCH_EntityGunner) {
            /*  95 */       mCH_EntityAircraft.interactFirst(player);
            /*  96 */       return itemStack;
            /*  97 */     }  if (this.targetType == 1 && !world.isRemote && player.getTeam() == null) {
            /*  98 */       player.addChatMessage((IChatComponent)new ChatComponentText("You are not on team."));
            /*  99 */       return itemStack;
            /* 100 */     }  if (mCH_EntityAircraft == null) {
            /* 101 */       if (!world.isRemote) {
                /* 102 */         player.addChatMessage((IChatComponent)new ChatComponentText("Right click to seat."));
                /*     */       }
            /*     */
            /* 105 */       return itemStack;
            /*     */     }
        /* 107 */     if (!world.isRemote) {
            /* 108 */       MCH_EntityGunner gunner = new MCH_EntityGunner(world, ((Entity)mCH_EntityAircraft).posX, ((Entity)mCH_EntityAircraft).posY, ((Entity)mCH_EntityAircraft).posZ);
            /* 109 */       gunner.rotationYaw = (((MathHelper.floor_double((player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 0x3) - 1) * 90);
            /* 110 */       gunner.isCreative = player.capabilities.isCreativeMode;
            /* 111 */       gunner.targetType = this.targetType;
            /* 112 */       gunner.ownerUUID = player.getUniqueID().toString();
            /* 113 */       ScorePlayerTeam team = world.getScoreboard().getPlayersTeam(player.getDisplayName());
            /* 114 */       if (team != null) {
                /* 115 */         gunner.setTeamName(team.getRegisteredName());
                /*     */       }
            /*     */
            /* 118 */       world.spawnEntityInWorld((Entity)gunner);
            /* 119 */       gunner.mountEntity((Entity)mCH_EntityAircraft);
            /* 120 */       W_WorldFunc.MOD_playSoundAtEntity((Entity)gunner, "wrench", 1.0F, 3.0F);
            /* 121 */       MCH_EntityAircraft ac = (mCH_EntityAircraft instanceof MCH_EntityAircraft) ? mCH_EntityAircraft : ((MCH_EntitySeat)mCH_EntityAircraft).getParent();
            /* 122 */       player.addChatMessage((IChatComponent)new ChatComponentText("The gunner was put on " + EnumChatFormatting.GOLD + (ac.getAcInfo()).displayName + EnumChatFormatting.RESET + " seat " + (ac.getSeatIdByEntity((Entity)gunner) + 1) + " by " + ScorePlayerTeam.formatPlayerName(player.getTeam(), player.getDisplayName())));
            /*     */     }
        /*     */
        /* 125 */     if (!player.capabilities.isCreativeMode) {
            /* 126 */       itemStack.stackSize--;
            /*     */     }
        /*     */
        /* 129 */     return itemStack;
        /*     */   }
    /*     */
    /*     */
    /*     */   @SideOnly(Side.CLIENT)
    /*     */   public int getColorFromItemStack(ItemStack itemStack, int layer) {
        /* 135 */     return (layer == 0) ? this.primaryColor : this.secondaryColor;
        /*     */   }
    /*     */
    /*     */   @SideOnly(Side.CLIENT)
    /*     */   public boolean requiresMultipleRenderPasses() {
        /* 140 */     return true;
        /*     */   }
    /*     */
    /*     */   @SideOnly(Side.CLIENT)
    /*     */   public IIcon getIconFromDamageForRenderPass(int par1, int par2) {
        /* 145 */     return (par2 > 0) ? this.theIcon : super.getIconFromDamageForRenderPass(par1, par2);
        /*     */   }
    /*     */
    /*     */   @SideOnly(Side.CLIENT)
    /*     */   public void registerIcons(IIconRegister icon) {
        /* 150 */     super.registerIcons(icon);
        /* 151 */     this.theIcon = icon.registerIcon(getIconString() + "_overlay");
        /*     */   }
    /*     */ }