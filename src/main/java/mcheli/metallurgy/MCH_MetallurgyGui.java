package mcheli.metallurgy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

@SideOnly(Side.CLIENT)
public class MCH_MetallurgyGui extends GuiContainer {
    private final MCH_MetallurgyTile tile;
    public MCH_MetallurgyGui(InventoryPlayer player, MCH_MetallurgyTile tile) {
        super(new MCH_MetallurgyContainer(player, tile));
        this.tile = tile;
        xSize = 176;
        ySize = 194;
    }
    @Override public void initGui() {
        super.initGui();
        buttonList.clear();
        if (tile instanceof MCH_FoundryTile) {
            buttonList.add(new GuiButton(0, guiLeft + 94, guiTop + 28, 18, 20, "<"));
            buttonList.add(new GuiButton(1, guiLeft + 138, guiTop + 28, 18, 20, ">"));
        }
    }
    @Override protected void actionPerformed(GuiButton button) {
        // No coordinate-bearing XF packet: vanilla validates the current container/window.
        mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFB8B8B8);
        drawRect(guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + ySize - 4, 0xFFD4D4D4);
        for (Object value : inventorySlots.inventorySlots) {
            Slot slot = (Slot) value;
            drawRect(guiLeft + slot.xDisplayPosition - 1, guiTop + slot.yDisplayPosition - 1,
                guiLeft + slot.xDisplayPosition + 17, guiTop + slot.yDisplayPosition + 17, 0xFF555555);
            drawRect(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition,
                guiLeft + slot.xDisplayPosition + 16, guiTop + slot.yDisplayPosition + 16, 0xFF999999);
        }
        if (tile instanceof MCH_BlastFurnaceTile) {
            MCH_BlastFurnaceTile blast = (MCH_BlastFurnaceTile) tile;
            bar(98, 62, 54, blast.progress, MCH_Metallurgy.BLAST_TICKS, 0xFFD98627);
            bar(98, 77, 54, blast.fuel, MCH_Metallurgy.MAX_FUEL, 0xFFBD5232);
        } else {
            MCH_FoundryTile foundry = (MCH_FoundryTile) tile;
            bar(26, 91, 124, foundry.steel, MCH_Metallurgy.MAX_STEEL, 0xFFD98627);
            bar(66, 68, 10, foundry.heat, MCH_Metallurgy.MAX_HEAT, 0xFFBD5232);
            bar(80, 68, 26, foundry.progress, MCH_Metallurgy.CAST_TICKS, 0xFF447F42);
            MCH_Metallurgy.Cast cast = foundry.selected();
            if (cast != null) {
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), cast.stack(), guiLeft + 116, guiTop + 30);
                RenderHelper.disableStandardItemLighting();
            }
        }
    }
    private void bar(int x, int y, int width, int value, int max, int color) {
        drawRect(guiLeft + x, guiTop + y, guiLeft + x + width, guiTop + y + 7, 0xFF555555);
        int fill = Math.max(0, Math.min(width, value * width / max));
        if (fill > 0) drawRect(guiLeft + x, guiTop + y, guiLeft + x + fill, guiTop + y + 7, color);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(tile.getInventoryName());
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, 102, 0x404040);
        if (tile instanceof MCH_BlastFurnaceTile) {
            MCH_BlastFurnaceTile blast = (MCH_BlastFurnaceTile) tile;
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.iron"), 26, 20, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.steel"), 98, 20, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.coal"), 26, 54, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.operations", blast.fuel), 80, 88, 0x404040);
            if (!MCH_Metallurgy.canProduceSteel()) {
                fontRendererObj.drawString(I18n.format("mcheli.metallurgy.provider"), 8, 49, 0xA02020);
            }
        } else {
            MCH_FoundryTile foundry = (MCH_FoundryTile) tile;
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.steel"), 26, 20, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.mold"), 102, 20, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.coal"), 26, 54, 0x404040);
            MCH_Metallurgy.Cast cast = foundry.selected();
            if (cast != null) fontRendererObj.drawString(I18n.format("mcheli.metallurgy.cost", cast.steel), 80, 54, 0x404040);
            fontRendererObj.drawString(I18n.format("mcheli.metallurgy.molten", foundry.steel), 26, 82, 0x404040);
            if (cast != null && mouseX >= guiLeft + 114 && mouseX < guiLeft + 134
                && mouseY >= guiTop + 28 && mouseY < guiTop + 49) {
                renderToolTip(cast.stack(), mouseX - guiLeft, mouseY - guiTop);
            }
        }
    }
}
