package mcheli.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.mob.MCH_EntityGunner;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class MCH_RenderGunner extends RendererLivingEntity {
    private static final ResourceLocation steveTextures = new ResourceLocation("mcheli", "textures/mob/heligunner.png");

    public ModelBiped modelBipedMain;

    public ModelBiped modelArmorChestplate;

    public ModelBiped modelArmor;

    public MCH_RenderGunner() {
        super((ModelBase)new ModelBiped(0.0F), 0.5F);
        this.modelBipedMain = (ModelBiped)this.mainModel;
        this.modelArmorChestplate = new ModelBiped(1.0F);
        this.modelArmor = new ModelBiped(0.5F);
    }


    protected int shouldRenderPass(EntityLivingBase p_77032_1_, int p_77032_2_, float p_77032_3_) {
        ItemStack itemstack = null;
        return -1;
    }

    protected boolean canRenderName(EntityLivingBase targetEntity) {
        return (targetEntity.getTeam() != null);
    }

    public void doRender(EntityLivingBase p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        this.modelBipedMain.isSneak = p_76986_1_.isSneaking();
        double d3 = p_76986_4_ - p_76986_1_.yOffset;
        if (p_76986_1_.isSneaking() && !(p_76986_1_ instanceof net.minecraft.client.entity.EntityPlayerSP))
            d3 -= 0.125D;
        MCH_EntityAircraft ac = ((MCH_EntityGunner)p_76986_1_).getAc();
        if (ac != null && ac.getAcInfo() != null && (!(ac.getAcInfo()).hideEntity || !ac.isPilot((Entity)p_76986_1_)))
            super.doRender(p_76986_1_, p_76986_2_, d3, p_76986_6_, p_76986_8_, p_76986_9_);
        this.modelBipedMain.aimedBow = false;
        this.modelBipedMain.isSneak = false;
        this.modelBipedMain.heldItemRight = 0;
    }

    protected void renderEquippedItems(EntityLivingBase p_77029_1_, float p_77029_2_) {
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        super.renderEquippedItems(p_77029_1_, p_77029_2_);
        renderArrowsStuckInEntity(p_77029_1_, p_77029_2_);
        ItemStack itemstack = null;
        boolean flag = false;
        ItemStack itemstack1 = null;
    }

    protected void func_77041_b(EntityLivingBase p_77041_1_, float p_77041_2_) {
        float f1 = 0.9375F;
        GL11.glScalef(f1, f1, f1);
    }

    public void renderFirstPersonArm(EntityPlayer p_82441_1_) {
        float f = 1.0F;
        GL11.glColor3f(f, f, f);
        this.modelBipedMain.onGround = 0.0F;
        this.modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, (Entity)p_82441_1_);
        this.modelBipedMain.bipedRightArm.render(0.0625F);
    }

    protected void _func_96449_a(EntityLivingBase p_96449_1_, double p_96449_2_, double p_96449_4_, double p_96449_6_, String p_96449_8_, float p_96449_9_, double p_96449_10_) {
        func_96449_a(p_96449_1_, p_96449_2_, p_96449_4_, p_96449_6_, p_96449_8_, p_96449_9_, p_96449_10_);
    }

    protected void _preRenderCallback(EntityLivingBase p_77041_1_, float p_77041_2_) {
        func_77041_b(p_77041_1_, p_77041_2_);
    }

    protected void _func_82408_c(EntityLivingBase p_82408_1_, int p_82408_2_, float p_82408_3_) {
        func_82408_c(p_82408_1_, p_82408_2_, p_82408_3_);
    }

    protected int _shouldRenderPass(EntityLivingBase p_77032_1_, int p_77032_2_, float p_77032_3_) {
        return shouldRenderPass(p_77032_1_, p_77032_2_, p_77032_3_);
    }

    protected void _renderEquippedItems(EntityLivingBase p_77029_1_, float p_77029_2_) {
        renderEquippedItems(p_77029_1_, p_77029_2_);
    }

    protected void _rotateCorpse(EntityLivingBase p_77043_1_, float p_77043_2_, float p_77043_3_, float p_77043_4_) {
        rotateCorpse(p_77043_1_, p_77043_2_, p_77043_3_, p_77043_4_);
    }

    protected void _renderLivingAt(EntityLivingBase p_77039_1_, double p_77039_2_, double p_77039_4_, double p_77039_6_) {
        renderLivingAt(p_77039_1_, p_77039_2_, p_77039_4_, p_77039_6_);
    }

    public void _doRender(EntityLivingBase p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }

    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return steveTextures;
    }

    public void _doRender(Entity p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }
}
