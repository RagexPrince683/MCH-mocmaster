/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockContainer
 *  net.minecraft.block.material.Material
 */
package mcheli.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;

public abstract class W_BlockContainer
extends BlockContainer {
    protected W_BlockContainer(int par1, Material par2Material) {
        super(par2Material);
    }

    public Block setLightLevel(float f) {
        return super.setLightLevel(f);
    }

    public Block setBlockName(String s) {
        return super.setBlockName(s);
    }
}

