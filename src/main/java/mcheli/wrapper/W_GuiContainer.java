/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.renderer.entity.RenderItem
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.inventory.Container
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  org.lwjgl.opengl.GL11
 */
package mcheli.wrapper;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public abstract class W_GuiContainer
extends GuiContainer {
    public W_GuiContainer(Container par1Container) {
        super(par1Container);
    }

    public void drawItemStack(ItemStack item, int x, int y) {
        if (item == null) {
            return;
        }
        if (item.getItem() == null) {
            return;
        }
        FontRenderer font = item.getItem().getFontRenderer(item);
        if (font == null) {
            font = this.fontRendererObj;
        }
        GL11.glEnable((int)2929);
        GL11.glEnable((int)2896);
        GuiScreen.itemRender.renderItemAndEffectIntoGUI(font, this.mc.getTextureManager(), item, x, y);
        GuiScreen.itemRender.renderItemOverlayIntoGUI(font, this.mc.getTextureManager(), item, x, y, null);

        this.zLevel = 0.0f;
        GuiScreen.itemRender.zLevel = 0.0f;
    }

    public void drawString(String s, int x, int y, int color) {
        this.drawString(this.fontRendererObj, s, x, y, color);
    }

    public void drawCenteredString(String s, int x, int y, int color) {
        this.drawCenteredString(this.fontRendererObj, s, x, y, color);
    }

    public int getStringWidth(String s) {
        return this.fontRendererObj.getStringWidth(s);
    }
}

