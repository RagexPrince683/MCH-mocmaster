/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.OpenGlHelper
 *  org.lwjgl.opengl.GL11
 */
package mcheli.wrapper;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class W_GuiButton
extends GuiButton {
    public List<String> hoverStringList = null;

    public W_GuiButton(int par1, int par2, int par3, int par4, int par5, String par6Str) {
        super(par1, par2, par3, par4, par5, par6Str);
    }

    public void addHoverString(String s) {
        if (this.hoverStringList == null) {
            this.hoverStringList = new ArrayList<String>();
        }
        this.hoverStringList.add(s);
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean b) {
        this.visible = b;
    }

    public static void setVisible(GuiButton button, boolean b) {
        button.visible = b;
    }

    public void enableBlend() {
        GL11.glEnable((int)3042);
        OpenGlHelper.glBlendFunc((int)770, (int)771, (int)1, (int)0);
        GL11.glBlendFunc((int)770, (int)771);
    }

    public boolean isOnMouseOver() {
        return this.field_146123_n;
    }

    public void setOnMouseOver(boolean b) {
        this.field_146123_n = b;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}

