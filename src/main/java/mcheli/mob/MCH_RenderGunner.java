package mcheli.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderGunner extends RendererLivingEntity {
    private static final ResourceLocation steveTextures = new ResourceLocation("mcheli", "textures/mob/heligunner.png");
    public ModelBiped modelBipedMain;
    public ModelBiped modelArmorChestplate;
    public ModelBiped modelArmor;

    public MCH_RenderGunner() {
        super(new ModelBiped(0.0F), 0.5F);
        this.modelBipedMain = (ModelBiped) this.mainModel;
        this.modelArmorChestplate = new ModelBiped(1.0F);
        this.modelArmor = new ModelBiped(0.5F);
    }

    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return -1;
    }

    protected boolean canRenderName(EntityLivingBase entity) {
        return (entity.getTeam() != null);
    }

    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        this.modelBipedMain.isSneak = entity.isSneaking();
        double d3 = y - entity.yOffset;
        if (entity.isSneaking() && !(entity instanceof net.minecraft.client.entity.EntityPlayerSP)) {
            d3 -= 0.125D;
        }

        MCH_EntityAircraft ac = ((MCH_EntityGunner) entity).getAc();
        if (ac != null && ac.getAcInfo() != null && (!(ac.getAcInfo()).hideEntity || !ac.isPilot(entity))) {
            super.doRender(entity, x, d3, z, entityYaw, partialTicks);
        }

        this.modelBipedMain.aimedBow = false;
        this.modelBipedMain.isSneak = false;
        this.modelBipedMain.heldItemRight = 0;
    }

    protected void renderEquippedItems(EntityLivingBase entity, float partialTicks) {
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        super.renderEquippedItems(entity, partialTicks);
        renderArrowsStuckInEntity(entity, partialTicks);
    }

    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        float f1 = 0.9375F;
        GL11.glScalef(f1, f1, f1);
    }

    public void renderFirstPersonArm(EntityPlayer player) {
        float f = 1.0F;
        GL11.glColor3f(f, f, f);
        this.modelBipedMain.swingProgress = player.swingProgress;
        this.modelBipedMain.setRotationAngles(player.limbSwing, player.limbSwingAmount, player.ticksExisted, player.rotationYaw, player.rotationPitch, 0.0625F, player);
        this.modelBipedMain.bipedRightArm.render(0.0625F);
    }

    protected void renderOffsetLivingLabel(EntityLivingBase entity, double x, double y, double z, String str, float p_96449_9_, double p_96449_10_) {
        super.renderLivingLabel(entity, str, x, y, z, 64);
    }

    protected ResourceLocation getEntityTexture(Entity entity) {
        return steveTextures;
    }

    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        doRender((EntityLivingBase) entity, x, y, z, entityYaw, partialTicks);
    }
}