package com.amber.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    Home home;
    android.content.SharedPreferences prefs;
    final ArrayList<App> apps = new ArrayList<>();
    final Handler handler = new Handler(Looper.getMainLooper());
    Dialog drawer;
    final Runnable tick = new Runnable() { public void run() { if(home!=null) home.invalidate(); handler.postDelayed(this,1000); } };
    static class App { String label,pkg,cls; Drawable icon; App(String l,String p,String c,Drawable i){label=l;pkg=p;cls=c;icon=i;} }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("layout",MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0x55321c12);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        loadApps(); home=new Home(); setContentView(home);
    }
    @Override protected void onResume(){super.onResume();loadApps();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);if(drawer!=null)drawer.dismiss();home.invalidate();}
    @Override public void onBackPressed(){if(drawer!=null && drawer.isShowing())drawer.dismiss();}
    void loadApps(){
        apps.clear();
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:getPackageManager().queryIntentActivities(i,0)) {
            if(r.activityInfo.packageName.equals(getPackageName()))continue;
            apps.add(new App(r.loadLabel(getPackageManager()).toString(),r.activityInfo.packageName,r.activityInfo.name,r.loadIcon(getPackageManager())));
        }
        Collections.sort(apps,(a,b)->a.label.compareToIgnoreCase(b.label));
    }
    App find(String key,String... candidates){
        String saved=prefs.getString(key,"");
        for(App a:apps)if((a.pkg+"/"+a.cls).equals(saved))return a;
        for(String p:candidates)for(App a:apps)if(a.pkg.equals(p))return a;
        return null;
    }
    void start(Intent i){try{startActivity(i);}catch(Exception e){Toast.makeText(this,"No compatible app installed",Toast.LENGTH_SHORT).show();}}
    void launch(App a){if(a!=null)start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(a.pkg,a.cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    void openSlot(String key,String label,String... pkgs){App a=find(key,pkgs);if(a!=null)launch(a);else showApps("Choose "+label,key,null);}
    void search(){start(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));}
    void showApps(String title,String assign,String group){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,20,24,16);root.setBackgroundColor(0xfff7f2e9);
        EditText filter=new EditText(this);filter.setSingleLine();filter.setHint("Search apps");root.addView(filter);
        ListView list=new ListView(this);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        ArrayList<App> visible=new ArrayList<>();
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
            public View getView(int p,View old,android.view.ViewGroup parent){
                App a=visible.get(p);LinearLayout row=new LinearLayout(MainActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(12,14,12,14);
                ImageView icon=new ImageView(MainActivity.this);icon.setImageDrawable(a.icon);row.addView(icon,new LinearLayout.LayoutParams(52,52));
                TextView name=new TextView(MainActivity.this);name.setText(a.label);name.setTextColor(0xff2d241e);name.setTextSize(17);name.setPadding(20,0,0,0);row.addView(name);return row;
            }
        };
        Runnable refresh=()->{visible.clear();String q=filter.getText().toString().toLowerCase(Locale.ROOT);for(App a:apps)if(a.label.toLowerCase(Locale.ROOT).contains(q)&&(group==null||a.pkg.startsWith(group)))visible.add(a);adapter.notifyDataSetChanged();};
        list.setAdapter(adapter);refresh.run();filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){refresh.run();}public void afterTextChanged(Editable e){}});
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(root).setNegativeButton("Close",null).create();drawer=d;
        list.setOnItemClickListener((p,v,pos,id)->{App a=visible.get(pos);if(assign!=null){prefs.edit().putString(assign,a.pkg+"/"+a.cls).apply();home.invalidate();}else launch(a);d.dismiss();});
        list.setOnItemLongClickListener((p,v,pos,id)->{start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+visible.get(pos).pkg)));return true;});
        d.show();d.getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.78));d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    void settings(){new AlertDialog.Builder(this).setTitle("Amber Launcher").setItems(new String[]{"Set as default home","Set weather text (manual)","Choose wallpaper","Reset home layout","All apps"},(d,n)->{
        if(n==0){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm.isRoleAvailable(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),8);return;}}start(new Intent(Settings.ACTION_HOME_SETTINGS));}
        if(n==1){EditText t=new EditText(this);t.setHint("Example: 30° ☀");t.setText(prefs.getString("weather",""));new AlertDialog.Builder(this).setTitle("Manual weather display").setMessage("Enter your weather text. This is not a live weather feed.").setView(t).setPositiveButton("Save",(x,y)->{prefs.edit().putString("weather",t.getText().toString()).apply();home.invalidate();}).setNegativeButton("Cancel",null).show();}
        if(n==2){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,9);}catch(Exception e){Toast.makeText(this,"File picker unavailable",0).show();}}
        if(n==3){prefs.edit().clear().apply();home.loadWallpaper();home.invalidate();}
        if(n==4)showApps("All apps",null,null);
    }).show();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==9&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);prefs.edit().putString("wallpaper",u.toString()).apply();home.loadWallpaper();home.invalidate();}catch(Exception e){Toast.makeText(this,"Unable to open image",0).show();}}}

    class Home extends View {
        Paint p=new Paint(3);Bitmap wallpaper;float sx,sy,downX,downY;boolean held;
        ArrayList<Target> targets=new ArrayList<>();
        class Target {RectF r;Runnable click,longClick;Target(RectF r,Runnable c,Runnable l){this.r=r;click=c;longClick=l;}}
        Runnable hold=()->{held=true;for(int i=targets.size()-1;i>=0;i--){Target t=targets.get(i);if(t.r.contains(downX,downY)&&t.longClick!=null){performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);t.longClick.run();return;}}settings();};
        Home(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription("Amber home. Swipe up for all apps. Long press for launcher settings.");loadWallpaper();}
        void loadWallpaper(){try{String u=prefs.getString("wallpaper","");BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;try(java.io.InputStream in=u.isEmpty()?getAssets().open("wallpaper.jpg"):getContentResolver().openInputStream(Uri.parse(u))){wallpaper=BitmapFactory.decodeStream(in,null,o);}}catch(Exception e){try(java.io.InputStream in=getAssets().open("wallpaper.jpg")){wallpaper=BitmapFactory.decodeStream(in);}catch(Exception ignored){}}}
        void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setShader(null);p.clearShadowLayer();}
        void round(Canvas c,float x,float y,float w,float h,float r,int col){color(col);c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);}
        void text(Canvas c,String s,float x,float y,float size,int col,boolean center){color(col);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);p.setShadowLayer(2,0,1,0x44000000);c.drawText(s,x,y,p);p.clearShadowLayer();}
        void glass(Canvas c,float x,float y,float w,float h,float r){color(Color.WHITE);p.setShader(new LinearGradient(x,y,x,y+h,new int[]{0x70ffffff,0x22ffffff,0x48ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0x44ffffff);c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);p.setStyle(Paint.Style.FILL);}
        void hit(float x,float y,float w,float h,Runnable c,Runnable l){targets.add(new Target(new RectF(x,y,x+w,y+h),c,l));}
        String date(String pattern){return new SimpleDateFormat(pattern,Locale.ENGLISH).format(new Date());}
        @Override protected void onDraw(Canvas real){
            super.onDraw(real);sx=getWidth()/691f;sy=getHeight()/1536f;Canvas c=real;c.save();c.scale(sx,sy);targets.clear();
            if(wallpaper!=null){float scale=Math.max(691f/wallpaper.getWidth(),1536f/wallpaper.getHeight());float w=wallpaper.getWidth()*scale,h=wallpaper.getHeight()*scale;color(-1);c.drawBitmap(wallpaper,null,new RectF((691-w)/2,(1536-h)/2,(691+w)/2,(1536+h)/2),p);}else c.drawColor(0xffb37b42);
            text(c,date("hh"),56,279,139,Color.WHITE,false);text(c,date("mm"),230,232,76,Color.WHITE,false);
            // Right aligned date uses its own anchor, independent of the clock.
            color(Color.WHITE);p.setTextSize(30);p.setTextAlign(Paint.Align.RIGHT);c.drawText(date("EEE, MMM d"),632,196,p);
            String weather=prefs.getString("weather","");text(c,weather.isEmpty()?"Weather  +":weather,559,256,weather.isEmpty()?23:44,Color.WHITE,true);
            hit(445,151,195,128,()->settings(),()->settings());hit(55,144,280,150,()->start(new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)),null);
            round(c,56,477,579,94,47,0xfff3f3f0);google(c,79,500,45);text(c,"",160,536,25,0xff777777,false);
            color(0xff202020);c.drawRoundRect(new RectF(491,504,503,528),6,6,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawArc(new RectF(483,508,511,537),0,180,false,p);c.drawLine(497,537,497,544,p);p.setStyle(Paint.Style.FILL);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);c.drawRoundRect(new RectF(575,510,601,536),8,8,p);p.setStyle(Paint.Style.FILL);c.drawCircle(588,523,5,p);
            hit(56,477,402,94,()->search(),null);hit(458,477,78,94,()->start(new Intent("android.speech.action.WEB_SEARCH")),null);hit(536,477,99,94,()->start(new Intent(Intent.ACTION_VIEW,Uri.parse("https://lens.google"))),null);
            slot(c,"calc","Calculator",56,780,100,true,"calculator",0xfff8f8f5,"com.google.android.calculator","com.transsion.calculator","com.android.calculator2");
            glass(c,56,939,260,261,36);
            slot(c,"whatsapp","WhatsApp",74,956,102,false,"phone",0xff18d965,"com.whatsapp");
            slot(c,"tiktok","TikTok",196,956,102,false,"music",0xff10151d,"com.zhiliaoapp.musically","com.ss.android.ugc.trill");
            slot(c,"facebook","Facebook",74,1077,102,false,"facebook",0xff0866ff,"com.facebook.katana","com.facebook.lite");
            slot(c,"play","Play Store",196,1077,102,false,"play",Color.WHITE,"com.android.vending");
            text(c,"AI Suggestions",186,1228,23,Color.WHITE,true);
            slot(c,"gallery","AI Gallery",375,940,100,true,"gallery",0xfffafafa,"com.transsion.gallery","com.google.android.apps.photos","com.android.gallery3d");
            glass(c,375,1099,100,100,27);String[] mini={"G","M","●","▶","◎","▣","▧","31","✳"};int[] cols={0xff4285f4,0xffe94235,0xff34a853};for(int i=0;i<9;i++){float x=387+(i%3)*26,y=1112+(i/3)*25;round(c,x,y,20,20,5,Color.WHITE);text(c,mini[i],x+10,y+15,15,cols[i%3],true);}text(c,"Google",425,1228,23,Color.WHITE,true);hit(369,1090,114,145,()->showApps("Google",null,"com.google"),()->showApps("All apps",null,null));
            glass(c,534,939,100,261,35);
            slot(c,"productivity-play","Play Store",552,955,64,false,"play",Color.WHITE,"com.android.vending");
            slot(c,"settings","Settings",552,1037,64,false,"settings",0xff91969b,"com.android.settings");
            slot(c,"files","Files",552,1116,64,false,"files",0xff228dff,"com.google.android.documentsui","com.android.documentsui","com.google.android.apps.nbu.files");
            text(c,"Productivity",584,1228,22,Color.WHITE,true);
            color(Color.WHITE);c.drawCircle(330,1292,6,p);color(0x88ffffff);c.drawCircle(361,1292,6,p);hit(300,1260,91,60,()->showApps("All apps",null,null),null);
            glass(c,30,1343,631,161,51);
            slot(c,"phone","Phone",59,1378,92,false,"phone",0xff20d66b,"com.google.android.dialer","com.android.dialer","com.android.contacts");
            slot(c,"contacts","Contacts",180,1378,92,false,"contacts",0xff25d273,"com.google.android.contacts","com.android.contacts");
            slot(c,"camera","Camera",300,1378,92,false,"camera",0xffb5b8bd,"com.transsion.camera","com.android.camera","com.android.camera2","com.google.android.GoogleCamera");
            slot(c,"messages","Messages",420,1378,92,false,"messages",Color.WHITE,"com.google.android.apps.messaging","com.android.mms");
            slot(c,"chrome","Chrome",540,1378,92,false,"chrome",Color.WHITE,"com.android.chrome","com.sec.android.app.sbrowser");
            c.restore();
        }
        void slot(Canvas c,String key,String label,float x,float y,float size,boolean caption,String glyph,int bg,String... pkgs){
            App a=find(key,pkgs);round(c,x,y,size,size,size*.25f,bg);
            if(a!=null){c.save();Path clip=new Path();clip.addRoundRect(new RectF(x,y,x+size,y+size),size*.24f,size*.24f,Path.Direction.CW);c.clipPath(clip);a.icon.setBounds((int)x,(int)y,(int)(x+size),(int)(y+size));a.icon.draw(c);c.restore();}else icon(c,glyph,x,y,size);
            if(caption)text(c,label,x+size/2,y+size+30,22,Color.WHITE,true);
            hit(x-4,y-4,size+8,size+(caption?42:8),()->openSlot(key,label,pkgs),()->showApps("Choose "+label,key,null));
        }
        void google(Canvas c,float x,float y,float size){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.2f);RectF r=new RectF(x+5,y+5,x+size-5,y+size-5);int[] cs={0xff4285f4,0xff34a853,0xfffbbc05,0xffea4335};float[] starts={-40,45,135,205};float[] sweeps={85,90,70,90};for(int i=0;i<4;i++){p.setColor(cs[i]);c.drawArc(r,starts[i],sweeps[i],false,p);}p.setColor(cs[0]);c.drawLine(x+size*.53f,y+size*.52f,x+size-1,y+size*.52f,p);p.setStyle(Paint.Style.FILL);}
        void icon(Canvas c,String type,float x,float y,float s){c.save();c.translate(x,y);c.scale(s/100,s/100);color(Color.WHITE);
            switch(type){
                case "phone":p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(new RectF(24,14,87,76),75,110,false,p);c.drawLine(26,26,32,16,p);c.drawLine(64,72,78,62,p);p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);break;
                case "contacts":c.drawCircle(50,32,15,p);c.drawRoundRect(new RectF(22,55,78,83),15,15,p);break;
                case "music":text(c,"♪",51,77,82,0xff29e9e9,true);text(c,"♪",55,74,82,0xffff3159,true);text(c,"♪",52,72,82,Color.WHITE,true);break;
                case "facebook":p.setTypeface(Typeface.DEFAULT_BOLD);text(c,"f",51,100,112,Color.WHITE,true);break;
                case "play":Path a=new Path();a.moveTo(29,19);a.lineTo(29,81);a.lineTo(58,50);a.close();p.setColor(0xff22c9e3);c.drawPath(a,p);tri(c,29,19,58,50,72,38,0xff20c56c);tri(c,29,81,58,50,72,62,0xfff4444d);tri(c,58,50,72,38,88,50,0xffffc637);tri(c,58,50,88,50,72,62,0xffffc637);break;
                case "gallery":int[] cs={0xff0bc4d6,0xff47d682,0xffbce341,0xffffd834,0xffff9435,0xffff5964,0xff6e83ed};for(int i=0;i<7;i++){color(cs[i]);double t=i*Math.PI*2/7;c.drawCircle(50+(float)Math.sin(t)*22,50+(float)Math.cos(t)*22,18,p);}break;
                case "camera":color(0xff24262d);c.drawCircle(50,50,37,p);color(0xff686f7b);c.drawCircle(50,50,29,p);color(0xff101c30);c.drawCircle(50,50,23,p);color(0xff3c749a);c.drawCircle(44,43,10,p);color(0xffc7e5ff);c.drawCircle(40,38,4,p);break;
                case "messages":round(c,14,25,73,53,23,0xff76baff);round(c,19,32,62,43,20,0xff0872ee);tri(c,25,66,25,84,42,71,0xff0872ee);break;
                case "chrome":int[] colors={0xffeb453b,0xfffbc537,0xff32a65a};for(int i=0;i<3;i++){color(colors[i]);c.drawArc(new RectF(12,12,88,88),-90+i*120,120,true,p);}color(Color.WHITE);c.drawCircle(50,50,22,p);color(0xff3286ec);c.drawCircle(50,50,17,p);break;
                case "settings":color(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawCircle(50,50,30,p);c.drawCircle(50,50,19,p);for(int i=0;i<16;i++){double t=i*Math.PI/8;c.drawLine(50+(float)Math.sin(t)*32,50+(float)Math.cos(t)*32,50+(float)Math.sin(t)*40,50+(float)Math.cos(t)*40,p);}p.setStyle(Paint.Style.FILL);break;
                case "files":round(c,12,24,42,24,7,0xffbbddff);round(c,10,34,80,50,8,0xff58adff);round(c,10,45,80,39,8,0xff1478ec);break;
                case "calculator":String[] chars={"+","−","×","="};for(int i=0;i<4;i++){float cx=29+(i%2)*42,cy=28+(i/2)*42;color(i==3?0xffff8765:0xffededeb);c.drawCircle(cx,cy,18,p);text(c,chars[i],cx,cy+9,30,i==3?Color.WHITE:0xff555555,true);}break;
            }c.restore();
        }
        void tri(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,int col){color(col);Path t=new Path();t.moveTo(x1,y1);t.lineTo(x2,y2);t.lineTo(x3,y3);t.close();c.drawPath(t,p);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX()/sx,y=e.getY()/sy;switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=x;downY=y;held=false;handler.postDelayed(hold,550);return true;
            case MotionEvent.ACTION_MOVE:if(Math.abs(x-downX)>18||Math.abs(y-downY)>18)handler.removeCallbacks(hold);return true;
            case MotionEvent.ACTION_UP:handler.removeCallbacks(hold);if(held)return true;if(downY-y>70||Math.abs(x-downX)>90){showApps("All apps",null,null);return true;}if(Math.abs(x-downX)<24&&Math.abs(y-downY)<24){performClick();for(int i=targets.size()-1;i>=0;i--){Target t=targets.get(i);if(t.r.contains(x,y)){t.click.run();return true;}}}return true;
            case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(hold);return true;
        }return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
