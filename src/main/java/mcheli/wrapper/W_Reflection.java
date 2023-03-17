/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  cpw.mods.fml.common.ObfuscationReflectionHelper
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.PlayerControllerMP
 *  net.minecraft.client.renderer.EntityRenderer
 *  net.minecraft.client.renderer.ItemRenderer
 *  net.minecraft.client.renderer.entity.Render
 *  net.minecraft.client.renderer.entity.RenderManager
 *  net.minecraft.entity.Entity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.NetworkSystem
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.AxisAlignedBB
 *  net.minecraft.util.MathHelper
 */
package mcheli.wrapper;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.util.List;
import java.util.Queue;

public class W_Reflection {

	   public static RenderManager getRenderManager(Render render) {
	      try {
	         return (RenderManager)ObfuscationReflectionHelper.getPrivateValue(Render.class, render, new String[]{"field_76990_c", "renderManager"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	         return null;
	      }
	   }

	   public static void restoreDefaultThirdPersonDistance() {
	      setThirdPersonDistance(4.0F);
	   }

	   public static void setThirdPersonDistance(float dist) {
	      if((double)dist >= 0.1D) {
	         try {
	            Minecraft e = Minecraft.getMinecraft();
	            ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, e.entityRenderer, Float.valueOf(dist), new String[]{"field_78490_B", "thirdPersonDistance"});
	         } catch (Exception var2) {
	            var2.printStackTrace();
	         }

	      }
	   }

	   public static float getThirdPersonDistance() {
	      try {
	         Minecraft e = Minecraft.getMinecraft();
	         return ((Float)ObfuscationReflectionHelper.getPrivateValue(EntityRenderer.class, e.entityRenderer, new String[]{"field_78490_B", "thirdPersonDistance"})).floatValue();
	      } catch (Exception var1) {
	         var1.printStackTrace();
	         return 4.0F;
	      }
	   }

	   public static void setCameraRoll(float roll) {
	      try {
	         roll = MathHelper.wrapAngleTo180_float(roll);
	         Minecraft e = Minecraft.getMinecraft();
	         ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, Float.valueOf(roll), new String[]{"field_78495_O", "camRoll"});
	         ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, Float.valueOf(roll), new String[]{"field_78505_P", "prevCamRoll"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static float getPrevCameraRoll() {
	      try {
	         Minecraft e = Minecraft.getMinecraft();
	         return ((Float)ObfuscationReflectionHelper.getPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, new String[]{"field_78505_P", "prevCamRoll"})).floatValue();
	      } catch (Exception var1) {
	         var1.printStackTrace();
	         return 0.0F;
	      }
	   }

	   public static void restoreCameraZoom() {
	      setCameraZoom(1.0F);
	   }

	   public static void setCameraZoom(float zoom) {
	      try {
	         Minecraft e = Minecraft.getMinecraft();
	         ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, e.entityRenderer, Float.valueOf(zoom), new String[]{"field_78503_V", "cameraZoom"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static void setItemRenderer(ItemRenderer r) {
	      try {
	         Minecraft e = Minecraft.getMinecraft();
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static void setCreativeDigSpeed(int n) {
	      try {
	         Minecraft e = Minecraft.getMinecraft();
	         ObfuscationReflectionHelper.setPrivateValue(PlayerControllerMP.class, e.playerController, Integer.valueOf(n), new String[]{"field_78781_i", "blockHitDelay"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static ItemRenderer getItemRenderer() {
	      return Minecraft.getMinecraft().entityRenderer.itemRenderer;
	   }

	   public static void setItemRenderer_ItemToRender(ItemStack itemToRender) {
	      try {
	         ObfuscationReflectionHelper.setPrivateValue(ItemRenderer.class, getItemRenderer(), itemToRender, new String[]{"field_78453_b", "itemToRender"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static ItemStack getItemRenderer_ItemToRender() {
	      try {
	         ItemStack e = (ItemStack)ObfuscationReflectionHelper.getPrivateValue(ItemRenderer.class, getItemRenderer(), new String[]{"field_78453_b", "itemToRender"});
	         return e;
	      } catch (Exception var1) {
	         var1.printStackTrace();
	         return null;
	      }
	   }

	   public static void setItemRendererProgress(float equippedProgress) {
	      try {
	         ObfuscationReflectionHelper.setPrivateValue(ItemRenderer.class, getItemRenderer(), Float.valueOf(equippedProgress), new String[]{"field_78454_c", "equippedProgress"});
	      } catch (Exception var2) {
	         var2.printStackTrace();
	      }

	   }

	   public static void setBoundingBox(Entity entity, AxisAlignedBB bb) {
	      try {
	         ObfuscationReflectionHelper.setPrivateValue(Entity.class, entity, bb, new String[]{"field_70121_D", "boundingBox"});
	      } catch (Exception var3) {
	         var3.printStackTrace();
	      }

	   }

	   public static List getNetworkManagers() {
	      try {
	         List e = (List)ObfuscationReflectionHelper.getPrivateValue(NetworkSystem.class, MinecraftServer.getServer().func_147137_ag(), new String[]{"field_151272_f", "networkManagers"});
	         return e;
	      } catch (Exception var1) {
	         var1.printStackTrace();
	         return null;
	      }
	   }

	   public static Queue getReceivedPacketsQueue(NetworkManager nm) {
	      try {
	         Queue e = (Queue)ObfuscationReflectionHelper.getPrivateValue(NetworkManager.class, nm, new String[]{"field_150748_i", "receivedPacketsQueue"});
	         return e;
	      } catch (Exception var2) {
	         var2.printStackTrace();
	         return null;
	      }
	   }

	   public static Queue getSendPacketsQueue(NetworkManager nm) {
	      try {
	         Queue e = (Queue)ObfuscationReflectionHelper.getPrivateValue(NetworkManager.class, nm, new String[]{"field_150745_j", "outboundPacketsQueue"});
	         return e;
	      } catch (Exception var2) {
	         var2.printStackTrace();
	         return null;
	      }
	   }
	}
