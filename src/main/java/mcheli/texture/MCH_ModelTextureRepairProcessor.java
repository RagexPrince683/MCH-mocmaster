package mcheli.texture;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Deterministic, OpenGL-free image algorithms used by the client repair cache. */
public final class MCH_ModelTextureRepairProcessor {
   private static final int OPAQUE = 250;
   private MCH_ModelTextureRepairProcessor() {}

   /* MQO/OpenGL UV (0,0) addresses the first PNG texel.  ImageIO also stores that texel at y=0. */
   public static int textureX(float u,int width){return Math.min(width-1,Math.max(0,(int)(u*width)));}
   public static int textureY(float v,int height){return Math.min(height-1,Math.max(0,(int)(v*height)));}

   public static void rasterizeTriangle(boolean[] mask,int width,int height,float[] uv){
      for(float value:uv)if(value<0.0F||value>1.0F)return;
      float ax=uv[0]*width,ay=uv[1]*height,bx=uv[2]*width,by=uv[3]*height,cx=uv[4]*width,cy=uv[5]*height;
      int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
      int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
      if(edge(ax,ay,bx,by,cx,cy)==0.0F)return;
      for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){float px=x+.5F,py=y+.5F,a=edge(bx,by,cx,cy,px,py),b=edge(cx,cy,ax,ay,px,py),c=edge(ax,ay,bx,by,px,py);if((a>=0&&b>=0&&c>=0)||(a<=0&&b<=0&&c<=0))mask[y*width+x]=true;}
   }
   private static float edge(float ax,float ay,float bx,float by,float x,float y){return(x-ax)*(by-ay)-(y-ay)*(bx-ax);}
   public static void rasterizeFace(boolean[] mask,int w,int h,float[] uv){if(uv.length==6)rasterizeTriangle(mask,w,h,uv);else if(uv.length==8){rasterizeTriangle(mask,w,h,new float[]{uv[0],uv[1],uv[2],uv[3],uv[4],uv[5]});rasterizeTriangle(mask,w,h,new float[]{uv[0],uv[1],uv[4],uv[5],uv[6],uv[7]});}}

   /** Backwards-compatible entry point. */
   public static Result repair(BufferedImage source,boolean[] coverage,int maxArea,int maxThickness,int bleedRadius,int expandRadius){
      return repair(source,coverage,maxArea,6,maxThickness,expandRadius,bleedRadius);
   }

   public static Result repair(BufferedImage source,boolean[] coverage,int maxHoleArea,int thinMinLength,int thinMaxThickness,int thinRadius,int bleedRadius){
      int w=source.getWidth(),h=source.getHeight();BufferedImage out=copy(source);boolean[] holes=new boolean[w*h],thin=new boolean[w*h];int filled=0,expanded=0,bleed=0;
      boolean[] seen=new boolean[w*h];
      for(int start=0;start<w*h;start++)if(coverage[start]&&!seen[start]&&alpha(out,start,w)==0){
         ArrayDeque<Integer> q=new ArrayDeque<Integer>(),pixels=new ArrayDeque<Integer>();q.add(start);seen[start]=true;int minX=w,maxX=-1,minY=h,maxY=-1;boolean enclosed=true;
         while(!q.isEmpty()){int p=q.remove(),x=p%w,y=p/w;pixels.add(p);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);int[] ns={p-1,p+1,p-w,p+w};for(int n:ns){int nx=n%w,ny=n/w;if(n<0||n>=w*h||Math.abs(nx-x)+Math.abs(ny-y)!=1||!coverage[n]){enclosed=false;continue;}if(!seen[n]&&alpha(out,n,w)==0){seen[n]=true;q.add(n);}}}
         int thickness=Math.min(maxX-minX+1,maxY-minY+1);if(enclosed&&pixels.size()<=maxHoleArea&&thickness<=Math.max(1,thinMaxThickness))while(!pixels.isEmpty()){int p=pixels.remove(),color=nearestOpaque(out,p%w,p/w,Math.max(1,thinRadius),OPAQUE);if(color!=0){out.setRGB(p%w,p/w,color|0xFF000000);holes[p]=true;filled++;}}
      }
      // Expand only long, binary-opaque, one/thin-texel components. Expansion is perpendicular to
      // the long axis, so gaps along railings remain gaps and large panels are never selected.
      seen=new boolean[w*h];int[] snapshot=out.getRGB(0,0,w,h,null,0,w);
      for(int start=0;start<w*h;start++)if(coverage[start]&&!seen[start]&&a(snapshot[start])>=OPAQUE){
         ArrayDeque<Integer> q=new ArrayDeque<Integer>(),pixels=new ArrayDeque<Integer>();q.add(start);seen[start]=true;int minX=w,maxX=-1,minY=h,maxY=-1;
         while(!q.isEmpty()){int p=q.remove(),x=p%w,y=p/w;pixels.add(p);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);int[] ns={p-1,p+1,p-w,p+w};for(int n:ns)if(n>=0&&n<w*h&&Math.abs(n%w-x)+Math.abs(n/w-y)==1&&coverage[n]&&!seen[n]&&a(snapshot[n])>=OPAQUE){seen[n]=true;q.add(n);}}
         int cw=maxX-minX+1,ch=maxY-minY+1,thickness=Math.min(cw,ch),length=Math.max(cw,ch);if(length>=thinMinLength&&thickness<=thinMaxThickness&&pixels.size()<=length*Math.max(1,thinMaxThickness)*2){
            boolean horizontal=cw>=ch;for(Integer boxed:pixels){int p=boxed,x=p%w,y=p/w;for(int d=1;d<=thinRadius;d++)for(int sign=-1;sign<=1;sign+=2){int nx=x+(horizontal?0:d*sign),ny=y+(horizontal?d*sign:0);if(nx>=0&&ny>=0&&nx<w&&ny<h&&coverage[ny*w+nx]&&a(snapshot[ny*w+nx])==0&&!thin[ny*w+nx]){out.setRGB(nx,ny,snapshot[p]);thin[ny*w+nx]=true;expanded++;}}}
         }
      }
      for(int pass=0;pass<bleedRadius;pass++){int[] before=out.getRGB(0,0,w,h,null,0,w);for(int p=0;p<w*h;p++)if(coverage[p]&&a(before[p])==0){int c=nearestOpaque(before,p%w,p/w,w,h,1,OPAQUE);if(c!=0&&(out.getRGB(p%w,p/w)&0xFFFFFF)!=(c&0xFFFFFF)){out.setRGB(p%w,p/w,c&0xFFFFFF);bleed++;}}}
      return new Result(out,holes,thin,filled,expanded,bleed,0,0,0,filled+expanded+bleed>0);
   }
   private static BufferedImage copy(BufferedImage i){BufferedImage o=new BufferedImage(i.getWidth(),i.getHeight(),BufferedImage.TYPE_INT_ARGB);o.setRGB(0,0,i.getWidth(),i.getHeight(),i.getRGB(0,0,i.getWidth(),i.getHeight(),null,0,i.getWidth()),0,i.getWidth());return o;}
   private static int a(int c){return(c>>>24)&255;}private static int alpha(BufferedImage i,int p,int w){return a(i.getRGB(p%w,p/w));}

   /** Visible for deterministic radius regression tests. Radius zero never searches. */
   public static int nearestOpaque(BufferedImage image,int x,int y,int radius,int alphaThreshold){return nearestOpaque(image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()),x,y,image.getWidth(),image.getHeight(),radius,alphaThreshold);}
   private static int nearestOpaque(int[] pixels,int x,int y,int w,int h,int radius,int threshold){for(int r=0;r<=radius;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)if(Math.max(Math.abs(dx),Math.abs(dy))==r){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<w&&ny<h&&a(pixels[ny*w+nx])>=threshold)return pixels[ny*w+nx];}return 0;}
   public static float[] correctUV(float u,float v,BufferedImage image,int radius){if(u<0||u>1||v<0||v>1)return new float[]{u,v};int w=image.getWidth(),h=image.getHeight(),x=textureX(u,w),y=textureY(v,h);if(a(image.getRGB(x,y))>0)return new float[]{u,v};for(int r=1;r<=radius;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++)if(dx*dx+dy*dy<=r*r){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<w&&ny<h&&a(image.getRGB(nx,ny))>=OPAQUE)return new float[]{(nx+.5F)/w,(ny+.5F)/h};}return new float[]{u,v};}

   public static final class Result{
      public final BufferedImage image;public final boolean[] components,thinComponents;public final int alphaFilledPixels,alphaExpandedPixels,rgbBleedPixels,correctedUVVertices,correctedUVFaces,correctedUVIslands;public final boolean changed;
      Result(BufferedImage i,boolean[] c,boolean[] thin,int fill,int expand,int bleed,int vertices,int faces,int islands,boolean changed){image=i;components=c;thinComponents=thin;alphaFilledPixels=fill;alphaExpandedPixels=expand;rgbBleedPixels=bleed;correctedUVVertices=vertices;correctedUVFaces=faces;correctedUVIslands=islands;this.changed=changed;}
      public Result withUV(int vertices,int faces,int islands){return new Result(image,components,thinComponents,alphaFilledPixels,alphaExpandedPixels,rgbBleedPixels,vertices,faces,islands,changed||vertices>0);}
      public boolean textureChanged(){return alphaFilledPixels+alphaExpandedPixels+rgbBleedPixels>0;}
   }
}
