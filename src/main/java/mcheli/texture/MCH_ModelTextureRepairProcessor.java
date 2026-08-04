package mcheli.texture;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Deterministic, OpenGL-free image/UV algorithms used by the client repair cache. */
public final class MCH_ModelTextureRepairProcessor {
   private MCH_ModelTextureRepairProcessor() {}

   public static void rasterizeTriangle(boolean[] mask, int width, int height, float[] uv) {
      for(float value : uv) if(value < 0.0F || value > 1.0F) return; // wrapping must remain wrapping
      float ax=uv[0]*width, ay=(1.0F-uv[1])*height;
      float bx=uv[2]*width, by=(1.0F-uv[3])*height;
      float cx=uv[4]*width, cy=(1.0F-uv[5])*height;
      int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx))));
      int maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
      int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy))));
      int maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
      float area=edge(ax,ay,bx,by,cx,cy); if(area==0.0F) return;
      for(int y=minY;y<=maxY;y++) for(int x=minX;x<=maxX;x++) {
         float px=x+0.5F, py=y+0.5F;
         float a=edge(bx,by,cx,cy,px,py), b=edge(cx,cy,ax,ay,px,py), c=edge(ax,ay,bx,by,px,py);
         if((a>=0&&b>=0&&c>=0)||(a<=0&&b<=0&&c<=0)) mask[y*width+x]=true;
      }
   }
   private static float edge(float ax,float ay,float bx,float by,float x,float y){return (x-ax)*(by-ay)-(y-ay)*(bx-ax);}

   /** MQO quads use the same fan split as GL_QUADS: (0,1,2) and (0,2,3). */
   public static void rasterizeFace(boolean[] mask,int w,int h,float[] uv) {
      if(uv.length==6) rasterizeTriangle(mask,w,h,uv);
      else if(uv.length==8) {
         rasterizeTriangle(mask,w,h,new float[]{uv[0],uv[1],uv[2],uv[3],uv[4],uv[5]});
         rasterizeTriangle(mask,w,h,new float[]{uv[0],uv[1],uv[4],uv[5],uv[6],uv[7]});
      }
   }

   public static Result repair(BufferedImage source, boolean[] coverage, int maxArea, int maxThickness, int bleedRadius, int expandRadius) {
      int w=source.getWidth(),h=source.getHeight(); BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
      out.setRGB(0,0,w,h,source.getRGB(0,0,w,h,null,0,w),0,w);
      boolean[] visited=new boolean[w*h], components=new boolean[w*h]; int repaired=0;
      for(int start=0;start<w*h;start++) if(coverage[start]&&!visited[start]&&alpha(out,start,w)==0) {
         ArrayDeque<Integer> q=new ArrayDeque<Integer>(); ArrayDeque<Integer> pixels=new ArrayDeque<Integer>(); q.add(start);visited[start]=true;
         int minX=w,maxX=-1,minY=h,maxY=-1; boolean enclosed=true;
         while(!q.isEmpty()){int p=q.remove(),x=p%w,y=p/w;pixels.add(p);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            int[] ns={p-1,p+1,p-w,p+w}; for(int n:ns){int nx=n%w,ny=n/w;if(n<0||n>=w*h||Math.abs(nx-x)+Math.abs(ny-y)!=1||!coverage[n]){enclosed=false;continue;}if(!visited[n]&&alpha(out,n,w)==0){visited[n]=true;q.add(n);}}
         }
         int thickness=Math.min(maxX-minX+1,maxY-minY+1);
         if(enclosed&&pixels.size()<=maxArea&&thickness<=maxThickness){while(!pixels.isEmpty()){int p=pixels.remove();components[p]=true;int color=nearestOpaque(out,p,w,h,Math.max(1,expandRadius));if(color!=0){out.setRGB(p%w,p/w,color|0xFF000000);repaired++;}}}
      }
      // Bleed color only; alpha remains byte-for-byte unchanged in this pass.
      for(int pass=0;pass<bleedRadius;pass++){int[] before=out.getRGB(0,0,w,h,null,0,w);for(int p=0;p<w*h;p++)if(coverage[p]&&((before[p]>>>24)&255)==0){int c=nearestOpaque(before,p,w,h);if(c!=0)out.setRGB(p%w,p/w,c&0x00FFFFFF);}}
      return new Result(out,components,repaired);
   }
   private static int alpha(BufferedImage i,int p,int w){return (i.getRGB(p%w,p/w)>>>24)&255;}
   private static int nearestOpaque(BufferedImage i,int p,int w,int h,int r){int[] a=i.getRGB(0,0,w,h,null,0,w);return nearestOpaque(a,p,w,h);}
   private static int nearestOpaque(int[] a,int p,int w,int h){int x=p%w,y=p/w;for(int r=1;r<=2;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<w&&ny<h&&((a[ny*w+nx]>>>24)&255)>=250)return a[ny*w+nx];}return 0;}

   public static float[] correctUV(float u,float v,BufferedImage image,int radius){
      if(u<0||u>1||v<0||v>1)return new float[]{u,v};int w=image.getWidth(),h=image.getHeight(),x=Math.min(w-1,Math.max(0,(int)(u*w))),y=Math.min(h-1,Math.max(0,(int)((1-v)*h)));
      if(((image.getRGB(x,y)>>>24)&255)>0)return new float[]{u,v};
      for(int r=1;r<=radius;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)if(dx*dx+dy*dy<=r*r){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<w&&ny<h&&((image.getRGB(nx,ny)>>>24)&255)>=250)return new float[]{(nx+0.5F)/w,1.0F-(ny+0.5F)/h};}
      return new float[]{u,v};
   }
   public static final class Result { public final BufferedImage image; public final boolean[] components; public final int repairedPixels; Result(BufferedImage i,boolean[] c,int n){image=i;components=c;repairedPixels=n;} }
}
