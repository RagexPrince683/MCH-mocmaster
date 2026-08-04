package mcheli.hud.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Non-pausing direct-manipulation editor. Persistence only occurs on Save. */
public class MCH_GuiHudLayoutEditor extends GuiScreen {
    private final GuiScreen parent;
    private final LinkedHashMap<String, MCH_HudLayoutElement> selected = new LinkedHashMap<String, MCH_HudLayoutElement>();
    private final LinkedHashMap<String, MCH_HudLayoutElement> boxSelectionSnapshot = new LinkedHashMap<String, MCH_HudLayoutElement>();
    private final LinkedHashMap<String, MCH_HudLayoutElement> boxHits = new LinkedHashMap<String, MCH_HudLayoutElement>();
    private final LinkedHashMap<String, int[]> dragOffsets = new LinkedHashMap<String, int[]>();
    private MCH_HudLayoutElement primary, hovered;
    private boolean snap, dragging, closed;
    private int dragMouseX, dragMouseY;
    private MCH_HudLayoutBounds dragBounds;
    private int lastClickX = Integer.MIN_VALUE, lastClickY, overlapIndex;
    private boolean boxSelecting, boxSelectionStarted;
    private int boxStartX, boxStartY, boxCurrentX, boxCurrentY;
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
        if(b.id==1) { clearBoxSelection(); if(MCH_HudLayoutManager.commitEditSession()){closed=true;mc.displayGuiScreen(parent);} else message="Could not save HUD layout; check the game log"; }
        else if(b.id==2) close(false);
        else if(b.id==3){for(MCH_HudLayoutElement e:selected.values()){e.offsetX=e.offsetY=0;e.scale=1.0F;}}
        else if(b.id==4) for(MCH_HudLayoutProfile p:MCH_HudLayoutManager.workingProfiles()) for(MCH_HudLayoutElement e:p.elements.values()){e.offsetX=e.offsetY=0;e.scale=1.0F;}
        else if(b.id==5){snap=!snap;b.displayString=snap?"Grid: ON":"Grid: OFF";}
    }
    private void close(boolean unexpected){clearBoxSelection();clearSelection();if(!closed){MCH_HudLayoutManager.cancelEditSession();closed=true;}mc.displayGuiScreen(parent);}
    public void onGuiClosed(){clearBoxSelection();clearSelection();if(!closed)MCH_HudLayoutManager.cancelEditSession();closed=true;}

    public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        boolean vehicle=MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(mc.thePlayer)!=null;
        MCH_HudLayoutManager.beginEditorFrame(); boolean rendered=vehicle&&MCH_ClientCommonTickHandler.instance!=null&&MCH_ClientCommonTickHandler.instance.drawHudLayoutEditorPreview(partialTicks); MCH_HudLayoutManager.endEditorFrame();
        ((GuiButton)buttonList.get(0)).enabled=rendered&&!MCH_HudLayoutManager.workingProfiles().isEmpty(); validateSelection();
        if(!rendered) clearBoxSelection();
        else if(boxSelecting&&Mouse.isButtonDown(0)) updateBoxSelection(mouseX,mouseY);
        if(dragging&&!boxSelecting) moveSelected(mouseX,mouseY); hovered=controlAt(mouseX,mouseY)?null:hit(mouseX,mouseY,0);
        if(boxSelecting&&boxSelectionStarted) drawSelectionBox();
        if(hovered!=null&&!isSelected(hovered))outline(hovered.bounds,0xFFEEEEEE,0xFF202020);
        for(MCH_HudLayoutElement e:selected.values())if(e.bounds!=null)outline(e.bounds,e==primary?0xFFFFFF00:0xFFFFA000,0xFF202020);
        if(primary!=null&&primary.bounds!=null)drawString(fontRendererObj,name(primary),(int)primary.bounds.left,(int)Math.max(2,primary.bounds.top-10),0xFFFFFF80);
        if(!rendered){drawRect(width/2-145,height/2-14,width/2+145,height/2+14,0xB0000000);drawCenteredString(fontRendererObj,"Enter or control a vehicle to edit its HUD",width/2,height/2-4,0xFFFFFFFF);}
        drawRect(0,height-29,Math.min(width,358),height,0x90000000); ((GuiButton)buttonList.get(2)).displayString=selected.size()>1?"Reset Selected":"Reset Element";
        if(primary!=null){String status=selected.size()==1?name(primary)+"  offset "+primary.offsetX+", "+primary.offsetY+"  scale "+String.format(java.util.Locale.ROOT,"%.2fx",primary.scale):selected.size()+" elements selected  primary "+String.format(java.util.Locale.ROOT,"%.2fx",primary.scale);int w=fontRendererObj.getStringWidth(status)+10;drawRect(width-w,2,width,16,0x90000000);drawString(fontRendererObj,status,width-w+5,5,0xFFFFFFFF);}
        if(message.length()>0){int w=fontRendererObj.getStringWidth(message)+10;drawRect(width/2-w/2,20,width/2+w/2,34,0xC0800000);drawCenteredString(fontRendererObj,message,width/2,23,0xFFFFFFFF);} super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private String key(MCH_HudLayoutElement e){return e.profileId+"\n"+e.id;} private boolean isSelected(MCH_HudLayoutElement e){return selected.containsKey(key(e));}
    private void validateSelection(){java.util.HashSet<String> valid=new java.util.HashSet<String>();for(MCH_HudLayoutProfile p:MCH_HudLayoutManager.workingProfiles())for(Map.Entry<String,MCH_HudLayoutElement> x:p.elements.entrySet())valid.add(p.profileId+"\n"+x.getKey());selected.keySet().retainAll(valid);if((primary==null&&!selected.isEmpty())||(primary!=null&&!isSelected(primary)))choosePrimary();}
    private void clearSelection(){selected.clear();dragOffsets.clear();primary=null;dragging=false;}
    private boolean controlAt(int x,int y){return y>=height-29;}
    protected void mouseClicked(int x,int y,int button){if(controlAt(x,y)){try{super.mouseClicked(x,y,button);}catch(Exception ignored){}return;}List<MCH_HudLayoutElement> hits=hits(x,y);if(button==1){if(hits.isEmpty()){clearSelection();return;}MCH_HudLayoutElement e=cycle(hits,x,y);String k=key(e);if(selected.remove(k)==null){selected.put(k,e);primary=e;}else if(e==primary)choosePrimary();return;}if(button!=0)return;if(hits.isEmpty()){startBoxSelection(x,y);return;}MCH_HudLayoutElement e=cycle(hits,x,y);if(!isSelected(e)){selected.clear();selected.put(key(e),e);primary=e;}startDrag(x,y);}
    private MCH_HudLayoutElement cycle(List<MCH_HudLayoutElement> hits,int x,int y){if(Math.abs(x-lastClickX)<=2&&Math.abs(y-lastClickY)<=2)overlapIndex=(overlapIndex+1)%hits.size();else overlapIndex=0;lastClickX=x;lastClickY=y;return hits.get(overlapIndex);}
    private void startDrag(int x,int y){dragging=true;dragMouseX=x;dragMouseY=y;dragOffsets.clear();dragBounds=null;for(MCH_HudLayoutElement e:selected.values()){dragOffsets.put(key(e),new int[]{e.offsetX,e.offsetY});if(e.bounds!=null){MCH_HudLayoutBounds b=new MCH_HudLayoutBounds(e.bounds.left,e.bounds.top,e.bounds.right,e.bounds.bottom);if(dragBounds==null)dragBounds=b;else dragBounds.include(b);}}}
    protected void mouseClickMove(int x,int y,int button,long heldTime){if(button==0&&boxSelecting)updateBoxSelection(x,y);super.mouseClickMove(x,y,button,heldTime);}
    protected void mouseMovedOrUp(int x,int y,int button){if(button==0){if(boxSelecting){updateBoxSelection(x,y);if(!boxSelectionStarted)clearSelection();clearBoxSelection();}dragging=false;}super.mouseMovedOrUp(x,y,button);}
    private void moveSelected(int x,int y){int dx=x-dragMouseX,dy=y-dragMouseY;if(snap){dx=Math.round(dx/5.0F)*5;dy=Math.round(dy/5.0F)*5;}moveGroup(dx,dy,true);}
    private void moveGroup(int dx,int dy,boolean fromStart){if(dragBounds!=null){dx=Math.min(dx,(int)Math.ceil(width-4-dragBounds.left));dx=Math.max(dx,(int)Math.floor(4-dragBounds.right));dy=Math.min(dy,(int)Math.ceil(height-4-dragBounds.top));dy=Math.max(dy,(int)Math.floor(4-dragBounds.bottom));}for(MCH_HudLayoutElement e:selected.values()){int[] st=fromStart?dragOffsets.get(key(e)):new int[]{e.offsetX,e.offsetY};if(st!=null){e.offsetX=st[0]+dx;e.offsetY=st[1]+dy;}}}
    protected void keyTyped(char c,int key){if(key==Keyboard.KEY_ESCAPE){close(false);return;}if(!selected.isEmpty()){int step=Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)?5:1;int dx=key==Keyboard.KEY_LEFT?-step:key==Keyboard.KEY_RIGHT?step:0,dy=key==Keyboard.KEY_UP?-step:key==Keyboard.KEY_DOWN?step:0;if(dx!=0||dy!=0){startDrag(0,0);moveGroup(dx,dy,true);dragging=false;}}}
    public void handleMouseInput() {super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0||boxSelecting)return;int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;if(controlAt(x,y))return;MCH_HudLayoutElement under=hit(x,y,0);if(under!=null&&!isSelected(under)){selected.clear();selected.put(key(under),under);primary=under;}if(selected.isEmpty())return;int notches=Math.max(1,Math.abs(wheel)/120),direction=wheel>0?1:-1;for(MCH_HudLayoutElement e:selected.values())e.scale=MCH_HudLayoutElement.normalizeScale(e.scale+direction*0.05F*notches);}
    private MCH_HudLayoutElement hit(int x,int y,int index){List<MCH_HudLayoutElement> h=hits(x,y);return h.isEmpty()?null:h.get(Math.min(index,h.size()-1));}
    private List<MCH_HudLayoutElement> hits(final int x,final int y){List<MCH_HudLayoutElement> h=new ArrayList<MCH_HudLayoutElement>();for(MCH_HudLayoutElement e:MCH_HudLayoutManager.frameElements())if(selectable(e)&&x>=e.bounds.left-4&&x<=e.bounds.right+4&&y>=e.bounds.top-4&&y<=e.bounds.bottom+4)h.add(e);Collections.sort(h,new Comparator<MCH_HudLayoutElement>(){public int compare(MCH_HudLayoutElement a,MCH_HudLayoutElement b){double aa=area(a),bb=area(b);return aa<bb?-1:aa>bb?1:0;}});return h;}
    private boolean selectable(MCH_HudLayoutElement e){return e!=null&&e.movable&&e.category==MCH_HudLayoutElement.Category.SCREEN_SPACE&&e.bounds!=null;}
    private void startBoxSelection(int x,int y){dragging=false;boxSelecting=true;boxSelectionStarted=false;boxStartX=boxCurrentX=x;boxStartY=boxCurrentY=y;boxSelectionSnapshot.clear();boxSelectionSnapshot.putAll(selected);boxHits.clear();}
    private void updateBoxSelection(int x,int y){if(!boxSelecting)return;boxCurrentX=x;boxCurrentY=y;int dx=x-boxStartX,dy=y-boxStartY;if(!boxSelectionStarted&&dx*dx+dy*dy<9)return;boxSelectionStarted=true;int left=Math.min(boxStartX,boxCurrentX),top=Math.min(boxStartY,boxCurrentY),right=Math.max(boxStartX,boxCurrentX),bottom=Math.max(boxStartY,boxCurrentY);boxHits.clear();for(MCH_HudLayoutElement e:MCH_HudLayoutManager.frameElements())if(selectable(e)&&e.bounds.right>=left&&e.bounds.left<=right&&e.bounds.bottom>=top&&e.bounds.top<=bottom)boxHits.put(key(e),e);MCH_HudLayoutElement oldPrimary=primary;selected.clear();boolean control=Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)||Keyboard.isKeyDown(Keyboard.KEY_RCONTROL),shift=Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);if(control){selected.putAll(boxSelectionSnapshot);for(Map.Entry<String,MCH_HudLayoutElement> hit:boxHits.entrySet())if(selected.remove(hit.getKey())==null)selected.put(hit.getKey(),hit.getValue());}else{if(shift)selected.putAll(boxSelectionSnapshot);selected.putAll(boxHits);}if(oldPrimary!=null&&selected.containsKey(key(oldPrimary)))primary=oldPrimary;else choosePrimary();}
    private void choosePrimary(){primary=null;double smallest=Double.MAX_VALUE;for(MCH_HudLayoutElement e:MCH_HudLayoutManager.frameElements())if(isSelected(e)){double candidate=area(e);if(candidate<=smallest){smallest=candidate;primary=e;}}if(primary==null&&!selected.isEmpty())primary=selected.values().iterator().next();}
    private double area(MCH_HudLayoutElement e){return e.bounds==null?Double.MAX_VALUE:Math.max(0.0D,e.bounds.right-e.bounds.left)*Math.max(0.0D,e.bounds.bottom-e.bounds.top);}
    private void clearBoxSelection(){boxSelecting=false;boxSelectionStarted=false;boxStartX=boxStartY=boxCurrentX=boxCurrentY=0;boxSelectionSnapshot.clear();boxHits.clear();}
    private void drawSelectionBox(){int left=Math.min(boxStartX,boxCurrentX),top=Math.min(boxStartY,boxCurrentY),right=Math.max(boxStartX,boxCurrentX),bottom=Math.max(boxStartY,boxCurrentY);drawRect(left,top,right+1,bottom+1,0x4050A0FF);drawHorizontalLine(left,right,top,0xFFFFFFFF);drawHorizontalLine(left,right,bottom,0xFFFFFFFF);drawVerticalLine(left,top,bottom,0xFFFFFFFF);drawVerticalLine(right,top,bottom,0xFFFFFFFF);if(right-left>2&&bottom-top>2){drawHorizontalLine(left+1,right-1,top+1,0xFF2060C0);drawHorizontalLine(left+1,right-1,bottom-1,0xFF2060C0);drawVerticalLine(left+1,top+1,bottom-1,0xFF2060C0);drawVerticalLine(right-1,top+1,bottom-1,0xFF2060C0);}}
    private void outline(MCH_HudLayoutBounds b,int bright,int dark){int l=(int)b.left-2,t=(int)b.top-2,r=(int)b.right+2,bot=(int)b.bottom+2;drawHorizontalLine(l,r,t,dark);drawHorizontalLine(l,r,bot,dark);drawVerticalLine(l,t,bot,dark);drawVerticalLine(r,t,bot,dark);drawHorizontalLine(l+1,r-1,t+1,bright);drawHorizontalLine(l+1,r-1,bot-1,bright);drawVerticalLine(l+1,t+1,bot-1,bright);drawVerticalLine(r-1,t+1,bot-1,bright);}
    private String name(MCH_HudLayoutElement e){return e.displayName==null||e.displayName.length()==0?"HUD element":e.displayName;}
    public boolean doesGuiPauseGame(){return false;}
}
