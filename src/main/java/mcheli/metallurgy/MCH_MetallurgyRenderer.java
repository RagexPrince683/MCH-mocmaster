package mcheli.metallurgy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/** Original XF native-scale transforms and section textures, owned by MCHeli. */
@SideOnly(Side.CLIENT)
public class MCH_MetallurgyRenderer extends TileEntitySpecialRenderer implements IItemRenderer {
    private final boolean foundry;
    private final W_WavefrontObject model;
    private final ResourceLocation foundryTexture = texture("steel_foundry");
    private static final String[] PARTS = {"Wall", "WallInner", "Crubicle", "Roof", "Tower", "TowerInner"};
    private final ResourceLocation[] textures = {texture("text_wall"), texture("text_wall_inner"),
        texture("text_furnace"), texture("text_slab"), texture("text_tower"), texture("text_tower_inner")};
    private final ResourceLocation on = texture("text_furnace_on"), off = texture("text_furnace_off");
    private final ResourceLocation lava = texture("lava");

    public MCH_MetallurgyRenderer(boolean foundry) {
        this.foundry = foundry;
        model = new W_WavefrontObject(new ResourceLocation("mcheli", "models/metallurgy/"
            + (foundry ? "steel_foundry" : "blast_furnace") + ".obj"), true);
        String[] required = foundry ? new String[] {"Scaffold", "Crucible"}
            : new String[] {"Wall", "WallInner", "Crubicle", "Roof", "Tower", "TowerInner", "Furnace"};
        for (String part : required) {
            if (!model.containsPart(part)) throw new IllegalStateException("Missing metallurgy model section: " + part);
        }
    }
    private static ResourceLocation texture(String name) {
        return new ResourceLocation("mcheli", "textures/models/metallurgy/" + name + ".png");
    }
    private static float rotation(int facing) {
        return facing == 4 ? 90 : facing == 3 ? 180 : facing == 5 ? 270 : 0;
    }
    @Override public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        float tilt = 0, level = 0;
        if (tile instanceof MCH_FoundryTile) {
            MCH_FoundryTile caster = (MCH_FoundryTile) tile;
            tilt = caster.lastTilt + (caster.tilt - caster.lastTilt) * partialTicks;
        } else level = ((MCH_BlastFurnaceTile) tile).level;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        GL11.glRotatef(rotation(tile.getBlockMetadata()), 0, 1, 0);
        float slide = (float) ((tile.getWorldObj().getTotalWorldTime() + partialTicks) % 20) * -0.05F;
        render(level, tilt, slide);
        GL11.glPopMatrix();
    }
    private void render(float level, float tilt, float slide) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPushMatrix();
        // No world scaling or bounds-based recentering: OBJ coordinates are block units.
        if (foundry) {
            GL11.glTranslated(-0.5, 0, -0.25);
            bindTexture(foundryTexture);
            model.renderPart("Scaffold");
            GL11.glPushMatrix();
            GL11.glTranslated(0.425, 3.75, 0);
            GL11.glRotatef(-tilt, 0, 0, 1);
            GL11.glTranslated(-0.425, -3.75, 0);
            model.renderPart("Crucible");
            GL11.glPopMatrix();
            if (tilt >= 30) renderPour(slide);
        } else {
            // XF's outer -90 and north +90 cancel; its four facing transforms match rotation().
            GL11.glShadeModel(GL11.GL_SMOOTH);
            for (int i = 0; i < PARTS.length; i++) {
                bindTexture(textures[i]);
                model.renderPart(PARTS[i]);
            }
            bindTexture(level > 0 ? on : off);
            model.renderPart("Furnace");
            if (level > 0) renderFurnaceSurface(level);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
    private void renderFurnaceSurface(float level) {
        float oldX = OpenGlHelper.lastBrightnessX, oldY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        bindTexture(lava);
        Tessellator t = Tessellator.instance;
        double y = level * 4 + 0.5;
        t.startDrawingQuads();
        t.addVertexWithUV(-0.75, y, -0.75, 0, 0);
        t.addVertexWithUV(-0.75, y, 0.75, 0, 1);
        t.addVertexWithUV(0.75, y, 0.75, 1, 1);
        t.addVertexWithUV(0.75, y, -0.75, 1, 0);
        t.draw();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
        GL11.glPopAttrib();
    }
    private void renderPour(float slide) {
        float oldX = OpenGlHelper.lastBrightnessX, oldY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        bindTexture(lava);
        Tessellator t = Tessellator.instance;
        double pixel = 0.0625, offset = 1 - pixel * 1.5, height = 2.5, min = 5 * pixel;
        t.startDrawingQuads();
        t.addVertexWithUV(offset + pixel, height, -pixel, slide, 0);
        t.addVertexWithUV(offset + pixel, height, pixel, slide, 0.125);
        t.addVertexWithUV(offset + pixel, min, pixel, slide + 1, 0.125);
        t.addVertexWithUV(offset + pixel, min, -pixel, slide + 1, 0);
        t.addVertexWithUV(offset, height, pixel, slide, 0.125);
        t.addVertexWithUV(offset, height, -pixel, slide, 0);
        t.addVertexWithUV(offset, min, -pixel, slide + 1, 0);
        t.addVertexWithUV(offset, min, pixel, slide + 1, 0.125);
        t.addVertexWithUV(offset + pixel, height, pixel, slide, 0.1875);
        t.addVertexWithUV(offset, height, pixel, slide, 0.125);
        t.addVertexWithUV(offset, min, pixel, slide + 1, 0.125);
        t.addVertexWithUV(offset + pixel, min, pixel, slide + 1, 0.1875);
        t.addVertexWithUV(offset, height, -pixel, slide, 0.125);
        t.addVertexWithUV(offset + pixel, height, -pixel, slide, 0.1875);
        t.addVertexWithUV(offset + pixel, min, -pixel, slide + 1, 0.1875);
        t.addVertexWithUV(offset, min, -pixel, slide + 1, 0.125);
        double size = 8;
        t.addVertexWithUV(offset - pixel * size, min, -pixel * size, 0, 0);
        t.addVertexWithUV(offset - pixel * size, min, pixel * size, 0, 1);
        t.addVertexWithUV(offset + pixel * size, min, pixel * size, 1, 1);
        t.addVertexWithUV(offset + pixel * size, min, -pixel * size, 1, 0);
        t.draw();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
        GL11.glPopAttrib();
    }
    @Override public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }
    @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) { return true; }
    @Override public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glPushMatrix();
        // Only the held/dropped/inventory representation is reduced; placed machines stay full size.
        if (type != ItemRenderType.INVENTORY && type != ItemRenderType.ENTITY) GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glScalef(0.14F, 0.14F, 0.14F);
        GL11.glTranslatef(0, -2.5F, 0);
        render(0, 0, 0);
        GL11.glPopMatrix();
    }
}