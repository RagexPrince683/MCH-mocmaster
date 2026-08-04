package mcheli.hud.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Non-pausing direct-manipulation editor. Persistence only occurs on Save. */
public class MCH_GuiHudLayoutEditor extends GuiScreen {
    private final GuiScreen parent;
    private MCH_HudLayoutElement selected, hovered;
    private boolean snap, dragging, closed;
    private int dragMouseX, dragMouseY, dragOffsetX, dragOffsetY;
    private int lastClickX = Integer.MIN_VALUE, lastClickY, overlapIndex;
    private String message = "";

    public MCH_GuiHudLayoutEditor(GuiScreen parent) { this.parent=parent; }

    public void initGui() {
        if(!MCH_HudLayoutManager.isEditing()) MCH_HudLayoutManager.beginEditSession();
        buttonList.clear(); int y=height-24;
        buttonList.add(new GuiButton(1,6,y,52,20,"Save")); buttonList.add(new GuiButton(2,61,y,52,20,"Cancel"));
        buttonList.add(new GuiButton(3,116,y,84,20,"Reset Element")); buttonList.add(new GuiButton(4,203,y,70,20,"Reset HUD"));
        buttonList.add(new GuiButton(5,276,y,76,20,"Grid: OFF"));
    }
    protected void actionPerformed(GuiButton b) {
        if(b.id==1) { if(MCH_HudLayoutManager.commitEditSession()){closed=true;mc.displayGuiScreen(parent);} else message="Could not save HUD layout; check the game log"; }
        else if(b.id==2) close(false);
        else if(b.id==3 && selected!=null){selected.offsetX=selected.offsetY=0;}
        else if(b.id==4) for(MCH_HudLayoutProfile p:MCH_HudLayoutManager.workingProfiles()) for(MCH_HudLayoutElement e:p.elements.values()){e.offsetX=e.offsetY=0;}
        else if(b.id==5){snap=!snap;b.displayString=snap?"Grid: ON":"Grid: OFF";}
    }
    private void close(boolean unexpected){if(!closed){MCH_HudLayoutManager.cancelEditSession();closed=true;}mc.displayGuiScreen(parent);}
    public void onGuiClosed(){if(!closed)MCH_HudLayoutManager.cancelEditSession();closed=true;}

    public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        boolean vehicle=MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(mc.thePlayer)!=null;
        MCH_HudLayoutManager.beginEditorFrame();
        boolean rendered=vehicle && MCH_ClientCommonTickHandler.instance!=null && MCH_ClientCommonTickHandler.instance.drawHudLayoutEditorPreview(partialTicks);
        MCH_HudLayoutManager.endEditorFrame();
        ((GuiButton)buttonList.get(0)).enabled=rendered && !MCH_HudLayoutManager.workingProfiles().isEmpty();
        if(dragging && selected!=null) moveSelected(mouseX,mouseY);
        hovered=controlAt(mouseX,mouseY)?null:hit(mouseX,mouseY,0);
        if(hovered!=null && hovered!=selected) outline(hovered.bounds,0xFFEEEEEE,0xFF202020);
        if(selected!=null && selected.bounds!=null){outline(selected.bounds,0xFFFFFF00,0xFF202020);String n=name(selected);drawString(fontRendererObj,n,(int)selected.bounds.left,(int)Math.max(2,selected.bounds.top-10),0xFFFFFF80);}
        if(!rendered){drawRect(width/2-145,height/2-14,width/2+145,height/2+14,0xB0000000);drawCenteredString(fontRendererObj,"Enter or control a vehicle to edit its HUD",width/2,height/2-4,0xFFFFFFFF);}
        drawRect(0,height-29,Math.min(width,358),height,0x90000000);
        if(selected!=null){String status=name(selected)+"  offset "+selected.offsetX+", "+selected.offsetY;int w=fontRendererObj.getStringWidth(status)+10;drawRect(width-w,2,width,16,0x90000000);drawString(fontRendererObj,status,width-w+5,5,0xFFFFFFFF);}
        if(message.length()>0){int w=fontRendererObj.getStringWidth(message)+10;drawRect(width/2-w/2,20,width/2+w/2,34,0xC0800000);drawCenteredString(fontRendererObj,message,width/2,23,0xFFFFFFFF);}
        super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private boolean controlAt(int x,int y){return y>=height-29&&x<=358;}
    protected void mouseClicked(int x,int y,int button){try{super.mouseClicked(x,y,button);}catch(Exception ignored){}if(button!=0||controlAt(x,y))return;List<MCH_HudLayoutElement> hits=hits(x,y);if(hits.isEmpty()){selected=null;return;}if(Math.abs(x-lastClickX)<=2&&Math.abs(y-lastClickY)<=2)overlapIndex=(overlapIndex+1)%hits.size();else overlapIndex=0;lastClickX=x;lastClickY=y;selected=hits.get(overlapIndex);dragging=true;dragMouseX=x;dragMouseY=y;dragOffsetX=selected.offsetX;dragOffsetY=selected.offsetY;}
    protected void mouseMovedOrUp(int x,int y,int button){if(button==0)dragging=false;super.mouseMovedOrUp(x,y,button);}
    private void moveSelected(int x,int y){int nx=dragOffsetX+x-dragMouseX,ny=dragOffsetY+y-dragMouseY;if(snap){nx=Math.round(nx/5.0F)*5;ny=Math.round(ny/5.0F)*5;}MCH_HudLayoutBounds b=selected.bounds;int dx=nx-selected.offsetX,dy=ny-selected.offsetY;nx-=Math.max(0,(int)(b.left+dx)-width+4);nx+=Math.max(0,4-(int)(b.right+dx));ny-=Math.max(0,(int)(b.top+dy)-height+4);ny+=Math.max(0,4-(int)(b.bottom+dy));selected.offsetX=nx;selected.offsetY=ny;}
    protected void keyTyped(char c,int key){if(key==Keyboard.KEY_ESCAPE){close(false);return;}if(selected!=null){int step=Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)?5:1;if(key==Keyboard.KEY_LEFT)selected.offsetX-=step;else if(key==Keyboard.KEY_RIGHT)selected.offsetX+=step;else if(key==Keyboard.KEY_UP)selected.offsetY-=step;else if(key==Keyboard.KEY_DOWN)selected.offsetY+=step;}}
    private MCH_HudLayoutElement hit(int x,int y,int index){List<MCH_HudLayoutElement> h=hits(x,y);return h.isEmpty()?null:h.get(Math.min(index,h.size()-1));}
    private List<MCH_HudLayoutElement> hits(final int x,final int y){List<MCH_HudLayoutElement> h=new ArrayList<MCH_HudLayoutElement>();for(MCH_HudLayoutElement e:MCH_HudLayoutManager.frameElements())if(e.movable&&e.bounds!=null&&x>=e.bounds.left-4&&x<=e.bounds.right+4&&y>=e.bounds.top-4&&y<=e.bounds.bottom+4)h.add(e);Collections.sort(h,new Comparator<MCH_HudLayoutElement>(){public int compare(MCH_HudLayoutElement a,MCH_HudLayoutElement b){double aa=(a.bounds.right-a.bounds.left)*(a.bounds.bottom-a.bounds.top),bb=(b.bounds.right-b.bounds.left)*(b.bounds.bottom-b.bounds.top);return aa<bb?-1:aa>bb?1:0;}});return h;}
    private void outline(MCH_HudLayoutBounds b,int bright,int dark){int l=(int)b.left-2,t=(int)b.top-2,r=(int)b.right+2,bot=(int)b.bottom+2;drawHorizontalLine(l,r,t,dark);drawHorizontalLine(l,r,bot,dark);drawVerticalLine(l,t,bot,dark);drawVerticalLine(r,t,bot,dark);drawHorizontalLine(l+1,r-1,t+1,bright);drawHorizontalLine(l+1,r-1,bot-1,bright);drawVerticalLine(l+1,t+1,bot-1,bright);drawVerticalLine(r-1,t+1,bot-1,bright);}
    private String name(MCH_HudLayoutElement e){return e.displayName==null||e.displayName.length()==0?"HUD element":e.displayName;}
    public boolean doesGuiPauseGame(){return false;}
}
