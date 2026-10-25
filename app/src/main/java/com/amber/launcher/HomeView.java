package com.amber.launcher;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class HomeView extends View {
    final MainActivity host;
    final Paint paint=new Paint(3);
    int page=0,bottom=0;
    float sx=1,sy=1,downX,downY,hoverX=-1,hoverY=-1;
    boolean held;
    String pressedItem;
    int edge=0;
    final Runnable edgeTurn=new Runnable(){public void run(){if(!host.dragging||edge==0)return;int next=Math.max(0,page+edge);if(next!=page){page=next;host.board.extraPages=Math.max(host.board.pages(),page+1);host.board.save();invalidate();}host.handler.postDelayed(this,950);}};
    final Runnable hold=this::longPress;
    void longPress(){held=true;Workspace.Item item=hitItem(downX,downY);if(item!=null){host.startDrag(this,new MainActivity.DragData(item.folder?null:item.app,item.id,null));}else host.settings();}
    HomeView(MainActivity h){super(h);host=h;setContentDescription("Home. Swipe up for apps, sideways for pages. Hold an app to drag. Drop onto another app to create a folder.");setOnApplyWindowInsetsListener((v,insets)->{bottom=insets.getSystemWindowInsetBottom();invalidate();return insets;});}
    void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.clearShadowLayer();}
    void text(Canvas c,String value,float x,float y,float size,int color,boolean center){fill(color);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(size);paint.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);paint.setShadowLayer(2,0,1,0x77000000);c.drawText(value,x,y,paint);paint.clearShadowLayer();}
    void round(Canvas c,RectF r,int color,float radius){fill(color);c.drawRoundRect(r,radius,radius,paint);}
    void glass(Canvas c,RectF r,float radius){fill(Color.WHITE);paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0x65ffffff,0x25ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(0x50ffffff);c.drawRoundRect(r,radius,radius,paint);paint.setStyle(Paint.Style.FILL);}
    void wallpaper(Canvas c,int w,int h){if(host.wallpaper==null){c.drawColor(0xff59412d);return;}float s=Math.max(w/(float)host.wallpaper.getWidth(),h/(float)host.wallpaper.getHeight());float bw=s*host.wallpaper.getWidth(),bh=s*host.wallpaper.getHeight();fill(-1);c.drawBitmap(host.wallpaper,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);}
    float gridTop(int p){return p==0?365:95;}
    RectF rect(Workspace.Item i){if(i.dock>=0)return new RectF(24+i.dock*73,771,84+i.dock*73,831);float x=14+i.col*94,y=gridTop(i.page)+i.row*90;return new RectF(x+6,y+3,x+i.width()*94-6,y+i.height()*90-17);}
    Workspace.Item hitItem(float x,float y){for(Workspace.Item i:host.board.items)if((i.dock>=0||i.page==page)&&rect(i).contains(x,y))return i;return null;}
    void drawIcon(Canvas c,String id,RectF r){MainActivity.App a=host.byId(id);if(a!=null){fill(-1);c.drawBitmap(a.icon,null,r,paint);}else{glass(c,r,15);text(c,"?",r.centerX(),r.centerY()+10,28,Color.WHITE,true);}}
    void drawItem(Canvas c,Workspace.Item item){RectF r=rect(item);
        if(item.folder){glass(c,r,item.style.equals("small")?17:21);int cols=item.style.equals("big")?2:item.style.equals("vertical")?1:3;int rows=item.style.equals("big")?2:item.style.equals("vertical")?3:3;int count=Math.min(cols*rows,item.members.size());float cellW=(r.width()-12)/cols,cellH=(r.height()-12)/rows,icon=Math.min(cellW,cellH)*.83f;
            for(int n=0;n<count;n++){float cx=r.left+6+(n%cols+.5f)*cellW,cy=r.top+6+(n/cols+.5f)*cellH;drawIcon(c,item.members.get(n),new RectF(cx-icon/2,cy-icon/2,cx+icon/2,cy+icon/2));}
            if(count==0)text(c,"+",r.centerX(),r.centerY()+10,28,Color.WHITE,true);
        }else{float size=item.dock>=0?56:58;RectF icon=new RectF(r.centerX()-size/2,r.top+2,r.centerX()+size/2,r.top+2+size);drawIcon(c,item.app,icon);}
        if(item.dock<0){String label=host.label(item);paint.setTextSize(12);label=android.text.TextUtils.ellipsize(label,new android.text.TextPaint(paint),r.width()+8,android.text.TextUtils.TruncateAt.END).toString();text(c,label,r.centerX(),r.bottom+14,12,Color.WHITE,true);}
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);wallpaper(canvas,getWidth(),getHeight());sx=getWidth()/400f;sy=(getHeight()-bottom)/850f;Canvas c=canvas;c.save();c.scale(sx,sy);int tint=host.prefs.getInt("homeTint",0);if(tint>0){fill((Math.round(tint*2.55f)<<24)|(host.surface()&0xffffff));c.drawRect(0,0,400,850,paint);}
        if(page==0){String hour=new SimpleDateFormat(host.prefs.getBoolean("clock24",false)?"HH":"hh",Locale.ENGLISH).format(new Date());String minute=new SimpleDateFormat("mm",Locale.ENGLISH).format(new Date());text(c,hour,29,161,78,-1,false);text(c,minute,134,132,42,-1,false);text(c,new SimpleDateFormat("EEE, MMM d",Locale.ENGLISH).format(new Date()),275,113,17,-1,false);
            round(c,new RectF(30,260,370,309),0xeffaf9f5,25);text(c,"G",58,294,30,0xff4285f4,true);text(c,"Search",96,291,15,0xff646464,false);text(c,"⌕",342,295,27,0xff333333,true);
        }
        glass(c,new RectF(17,758,383,839),26);
        if(host.board.items.isEmpty()&&!host.dragging){text(c,"Swipe up for apps",200,470,18,0xeeffffff,true);text(c,"Hold an app and drag it home",200,497,13,0xcfffffff,true);}
        for(Workspace.Item i:host.board.items)if(i.dock>=0||i.page==page)drawItem(c,i);
        int pages=host.board.pages();if(pages<=9){for(int n=0;n<pages;n++){fill(n==page?Color.WHITE:0x66ffffff);c.drawCircle(200+(n-(pages-1)/2f)*15,740,n==page?3.5f:2.5f,paint);}}else text(c,(page+1)+" / "+pages,200,744,12,0xddffffff,true);
        if(host.dragging){round(c,new RectF(15,38,193,77),0xcc242326,19);round(c,new RectF(207,38,385,77),0xcc242326,19);text(c,"Options",104,63,14,Color.WHITE,true);text(c,"Remove",296,63,14,Color.WHITE,true);
            if(hoverY>=gridTop(page)&&hoverY<gridTop(page)+host.board.rows(page)*90){int col=Math.max(0,Math.min(3,(int)((hoverX-14)/94))),row=Math.max(0,(int)((hoverY-gridTop(page))/90));round(c,new RectF(18+col*94,gridTop(page)+row*90,104+col*94,gridTop(page)+row*90+86),0x32ffffff,20);}
            if(hoverX<25||hoverX>375){fill(0x44ffffff);c.drawRect(hoverX<25?0:385,95,hoverX<25?15:400,725,paint);}
        }
        c.restore();
    }
    void updateEdge(float x,float y){int wanted=y>95&&y<730?(x<25?-1:x>375?1:0):0;if(wanted!=edge){host.handler.removeCallbacks(edgeTurn);edge=wanted;if(edge!=0)host.handler.postDelayed(edgeTurn,750);}}
    boolean drag(DragEvent event){if(!(event.getLocalState() instanceof MainActivity.DragData))return false;MainActivity.DragData data=(MainActivity.DragData)event.getLocalState();float x=event.getX()/sx,y=event.getY()/sy;switch(event.getAction()){
        case DragEvent.ACTION_DRAG_STARTED:host.dragging=true;invalidate();return true;
        case DragEvent.ACTION_DRAG_LOCATION:hoverX=x;hoverY=y;updateEdge(x,y);invalidate();return true;
        case DragEvent.ACTION_DRAG_EXITED:edge=0;host.handler.removeCallbacks(edgeTurn);return true;
        case DragEvent.ACTION_DROP:host.handler.removeCallbacks(edgeTurn);edge=0;boolean ok=drop(data,x,y);invalidate();return ok;
        case DragEvent.ACTION_DRAG_ENDED:host.handler.removeCallbacks(edgeTurn);edge=0;hoverX=hoverY=-1;host.finishDrag();invalidate();return true;
        default:return true;
    }}
    boolean drop(MainActivity.DragData data,float x,float y){Workspace.Item source=data.itemId==null?null:host.board.get(data.itemId);
        if(y>=38&&y<=77){if(x<200){if(source!=null)host.itemMenu(source);else{MainActivity.App a=host.byId(data.app);if(a!=null)host.appMenu(a,data.sourceFolder==null?null:host.board.get(data.sourceFolder));}}else if(source!=null)host.board.remove(source);else if(data.sourceFolder!=null)host.board.removeMember(data.sourceFolder,data.app);return true;}
        boolean inDock=y>=758&&y<=839;int dock=Math.max(0,Math.min(4,(int)((x-17)/73)));int col=(int)((x-14)/94),row=(int)((y-gridTop(page))/90);
        if(!inDock&&(x<14||col<0||col>3||y<gridTop(page)||row<0||row>=host.board.rows(page)))return false;
        Workspace.Item target=inDock?host.board.dock(dock,source==null?"":source.id):host.board.at(page,col,row,source==null?"":source.id);
        if(target!=null){
            if(target.id.equals(data.sourceFolder))return true;
            if(source!=null&&source.folder){host.toast("Drop a folder into an empty space");return false;}
            if(data.app==null)return false;
            if(target.folder){if(!target.members.contains(data.app))target.members.add(data.app);}
            else{if(target.app.equals(data.app))return false;target.folder=true;target.name="Folder";target.style="small";target.members.add(target.app);target.members.add(data.app);target.app="";}
            if(source!=null)host.board.items.remove(source);
        }else{
            Workspace.Item move=source==null?new Workspace.Item():source;if(source==null)move.app=data.app;
            if(inDock){move.dock=dock;if(move.folder)move.style="small";}
            else{if(!host.board.fits(move,page,col,row)){host.toast("Not enough space here");return false;}move.dock=-1;move.page=page;move.col=col;move.row=row;}
            if(source==null)host.board.items.add(move);
        }
        if(data.sourceFolder!=null)host.board.removeMember(data.sourceFolder,data.app);host.board.extraPages=Math.max(host.board.extraPages,page+1);host.board.save();return true;
    }
    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/sx,y=e.getY()/sy;switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:downX=x;downY=y;held=false;host.handler.postDelayed(hold,450);return true;
        case MotionEvent.ACTION_MOVE:if(Math.abs(x-downX)>12||Math.abs(y-downY)>12)host.handler.removeCallbacks(hold);return true;
        case MotionEvent.ACTION_UP:host.handler.removeCallbacks(hold);if(held||host.dragging)return true;if(downY-y>55&&Math.abs(y-downY)>Math.abs(x-downX)){host.openDrawer();return true;}if(Math.abs(x-downX)>55){page=Math.max(0,Math.min(host.board.pages()-1,page+(x<downX?1:-1)));invalidate();return true;}if(Math.abs(x-downX)<16&&Math.abs(y-downY)<16){performClick();Workspace.Item item=hitItem(x,y);if(item!=null){host.openItem(item);return true;}if(page==0&&y>=260&&y<=309){host.start(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));return true;}if(page==0&&y>65&&y<180){host.start(new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS));return true;}if(y>=758&&y<=839)host.openDrawer();else if(host.board.items.isEmpty())host.openDrawer();}return true;
        case MotionEvent.ACTION_CANCEL:host.handler.removeCallbacks(hold);return true;
    }return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
