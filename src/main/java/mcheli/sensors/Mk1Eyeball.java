package mcheli.sensors;

import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_PacketAircraftLocation;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Mk1Eyeball {
    private static Mk1Eyeball instance = new Mk1Eyeball();

    public List<MCH_VisualContact> contacts = new ArrayList<>();

    public static Mk1Eyeball getInstance(){
        return instance;
    }


    public void addContact(MCH_PacketAircraftLocation pc) {
        //System.out.println("mk1 eyeball loaded");
        for(MCH_VisualContact c : this.contacts) {
            //System.out.println("mk1 eyeball loaded2");
            if(c.entityId == pc.entityId) {
                //System.out.println("mk1 eyeball loaded3");
                c.updated = 0;
                c.x = pc.x;
                c.y = pc.y;
                c.z = pc.z;
                c.rotX = pc.rotX;
                c.rotZ = pc.rotZ;
                c.rotY = pc.rotY;

                return;
            }
        }
        //System.out.println("mk1 eyeball loaded4");
        this.contacts.add(new MCH_VisualContact(pc.x, pc.y, pc.z, pc.rotX, pc.rotY, pc.rotZ, pc.model, pc.texture, pc.entityId, pc.type));

    }

    public void clean_contacts() {
        ArrayList<MCH_VisualContact> toRemove = new ArrayList<MCH_VisualContact>(); //don't edit on an arraylist as we iterate over it
        for (MCH_VisualContact c : contacts) {
            c.updated++;
            //System.out.println("Updated: " + c.updated);
            if (c.updated >= 20) {
                //System.out.println("Removing Client. Remote: " + this.worldObj.isRemote);
                toRemove.add(c);

            }
        }
        contacts.removeAll(toRemove);
    }

    public static boolean shouldRender(MCH_VisualContact c){
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Entity e = player.worldObj.getEntityByID(c.entityId);
        if(e == null){return true;}
        else{
            return !e.isInRangeToRender3d(player.posX, player.posY, player.posZ);
        }
    }



    public static void renderAircraft(float x, float y, float z, float rotX, float rotY, float rotZ, String texture, String model, float partialTick){
        boolean fogSetting = GL11.glIsEnabled(GL11.GL_FOG);
        EntityPlayer p = Minecraft.getMinecraft().thePlayer;

        GL11.glDisable(GL11.GL_FOG);

        try {

            RenderHelper.enableStandardItemLighting();
            GL11.glTranslated(- RenderManager.renderPosX, - RenderManager.renderPosY, - RenderManager.renderPosZ);
            GL11.glTranslated(x, y, z);

            GL11.glRotated(rotZ, 0.0F, -1.0F, 0.0F);
            GL11.glRotated(rotY, 1.0F, 0.0F, 0.0F);
            GL11.glRotated(-rotX, 0.0F, 0.0F, 1.0F);

            int i = Minecraft.getMinecraft().theWorld.getLightBrightnessForSkyBlocks((int)p.posX, 256,(int) p.posZ, 0);
            int j = i % 65536;
            int k = i / 65536;

            Minecraft.getMinecraft().entityRenderer.enableLightmap(partialTick);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j / 1.0F, (float)k / 1.0F);


            W_McClient.MOD_bindTexture(texture);
            MCH_ModelManager.render("planes", model);
            MCH_ModelManager.render("helicopters", model);
            MCH_ModelManager.render("tanks", model);
            MCH_ModelManager.render("vehicles", model);
            RenderHelper.disableStandardItemLighting();
            Minecraft.getMinecraft().entityRenderer.disableLightmap(partialTick);


        }catch(Exception e){
            e.printStackTrace();
        }
        if(fogSetting){
            GL11.glEnable(GL11.GL_FOG);
        }
    }

    public static void renderContact(MCH_VisualContact contact, float partialTick) {
        if(!shouldRender(contact)){return;}
        GL11.glPushMatrix();
        try {
            String texture;

            try {
                texture = contact.texture;
                if (texture == null) {
                    switch (contact.type) {
                        case 0:
                            texture = "textures/planes/" + contact.model + ".png";
                            break;
                        case 1:
                            texture = "textures/helicopters/" + contact.model + ".png";
                            break;
                        case 2:
                            texture = "textures/tanks/" + contact.model + ".png";
                            break;
                        case 3:
                            texture = "textures/vehicles/" + contact.model + ".png";
                            break;

                }
                }
            } catch (Exception e) {
                texture = "ERROR";
                e.printStackTrace();
            }
            renderAircraft((float) contact.x, (float) contact.y, (float) contact.z, (float) contact.rotX,(float) contact.rotY,(float)  contact.rotZ, texture, contact.model, partialTick);
        }catch(Exception e){
            e.printStackTrace();
        }
        GL11.glPopMatrix();

    }

    public void update() {
        try {
            clean_contacts();



        }catch(Exception e){}
    }
}
