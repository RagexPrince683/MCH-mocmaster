package mcheli.hud.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Non-pausing working-copy editor. JSON is touched only by Save. */
public class MCH_GuiHudLayoutEditor extends GuiScreen {
    private final GuiScreen parent;
    private MCH_HudLayoutProfile working;
    private final List<String> ids = new ArrayList<String>();
    private int selected;
    private boolean snap;

    public MCH_GuiHudLayoutEditor(GuiScreen parent) { this.parent = parent; }

    public void initGui() {
        MCH_HudLayoutProfile profile = chooseProfile();
        working = profile.copy();
        ids.clear(); ids.addAll(working.elements.keySet()); selected = ids.isEmpty() ? -1 : 0;
        buttonList.clear();
        buttonList.add(new GuiButton(1, 10, height - 24, 55, 20, "Save"));
        buttonList.add(new GuiButton(2, 68, height - 24, 55, 20, "Cancel"));
        buttonList.add(new GuiButton(3, 126, height - 24, 80, 20, "Reset Element"));
        buttonList.add(new GuiButton(4, 209, height - 24, 70, 20, "Reset HUD"));
        buttonList.add(new GuiButton(5, 282, height - 24, 80, 20, "Grid: OFF"));
    }

    private MCH_HudLayoutProfile chooseProfile() {
        List<MCH_HudLayoutProfile> profiles = MCH_HudLayoutManager.profiles();
        if(!profiles.isEmpty()) return profiles.get(0);
        return MCH_HudLayoutManager.profile("builtin:common", "Java-rendered HUD groups");
    }

    protected void actionPerformed(GuiButton b) {
        if(b.id == 1) { MCH_HudLayoutManager.save(working); mc.displayGuiScreen(parent); }
        else if(b.id == 2) mc.displayGuiScreen(parent);
        else if(b.id == 3 && current() != null) { current().offsetX = current().offsetY = 0; }
        else if(b.id == 4) for(MCH_HudLayoutElement e : working.elements.values()) { e.offsetX = e.offsetY = 0; }
        else if(b.id == 5) { snap = !snap; b.displayString = snap ? "Grid: ON" : "Grid: OFF"; }
    }

    protected void keyTyped(char c, int key) {
        MCH_HudLayoutElement e = current();
        int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 5 : 1;
        if(e != null) {
            if(key == Keyboard.KEY_LEFT) e.offsetX -= step;
            else if(key == Keyboard.KEY_RIGHT) e.offsetX += step;
            else if(key == Keyboard.KEY_UP) e.offsetY -= step;
            else if(key == Keyboard.KEY_DOWN) e.offsetY += step;
            else if(key == Keyboard.KEY_PRIOR && selected > 0) --selected;
            else if(key == Keyboard.KEY_NEXT && selected + 1 < ids.size()) ++selected;
        }
        if(key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    protected void mouseClicked(int x, int y, int button) {
        try { super.mouseClicked(x, y, button); } catch(Exception ignored) {}
        if(x < 220 && y >= 38 && y < height - 30) {
            int index = (y - 38) / 11;
            if(index >= 0 && index < ids.size()) selected = index;
        } else if(button == 1 && current() != null) current().offsetX = current().offsetY = 0;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "HUD Layout Editor", width / 2, 8, 0xFFFFFF);
        drawString(fontRendererObj, "Profile: " + working.profileId, 10, 22, 0xA0E0FF);
        int max = Math.min(ids.size(), Math.max(0, (height - 70) / 11));
        for(int i = 0; i < max; i++) drawString(fontRendererObj, (i == selected ? "> " : "  ") + ids.get(i), 10, 38 + i * 11, i == selected ? 0xFFFF80 : 0xC0C0C0);
        MCH_HudLayoutElement e = current();
        if(e != null) {
            drawString(fontRendererObj, "Selected: " + ids.get(selected), 230, 42, 0xFFFFFF);
            drawString(fontRendererObj, "Offset X: " + e.offsetX + "  Y: " + e.offsetY, 230, 55, 0xFFFFFF);
            drawString(fontRendererObj, "Arrows: move  Shift: 5px  PgUp/PgDn: select", 230, 70, 0xA0A0A0);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private MCH_HudLayoutElement current() { return selected >= 0 && selected < ids.size() ? working.elements.get(ids.get(selected)) : null; }
    public boolean doesGuiPauseGame() { return false; }
}
