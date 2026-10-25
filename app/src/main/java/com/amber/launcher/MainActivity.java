package com.amber.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import org.json.JSONArray;
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
    Runnable drawerRefresh;
    boolean loadingApps;
    int loadGeneration=0;
    final java.util.concurrent.ExecutorService loader=java.util.concurrent.Executors.newSingleThreadExecutor();
    final BroadcastReceiver packagesChanged=new BroadcastReceiver(){public void onReceive(Context c,Intent i){loadApps();}};

    final Runnable tick = new Runnable() { public void run() { if(home!=null) home.invalidate(); handler.postDelayed(this,1000); } };
    static class App { String label,pkg,cls; Drawable icon; App(String l,String p,String c,Drawable i){label=l;pkg=p;cls=c;icon=i;} }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("layout",MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0x55321c12);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        home=new Home(); setContentView(home);
        IntentFilter packageFilter=new IntentFilter();packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);packageFilter.addDataScheme("package");
        if(Build.VERSION.SDK_INT>=33)registerReceiver(packagesChanged,packageFilter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(packagesChanged,packageFilter);

    }
    @Override protected void onResume(){super.onResume();loadApps();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);if(drawer!=null)drawer.dismiss();home.page=0;home.invalidate();}
    @Override public void onBackPressed(){if(drawer!=null && drawer.isShowing())drawer.dismiss();else{home.page=0;home.invalidate();}}
    @Override protected void onDestroy(){super.onDestroy();unregisterReceiver(packagesChanged);loader.shutdownNow();handler.removeCallbacksAndMessages(null);}
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    String appId(App a){return a.pkg+"/"+a.cls;}
    Drawable iconCopy(App a){Drawable.ConstantState state=a.icon.getConstantState();return state==null?a.icon:state.newDrawable(getResources()).mutate();}
    App byId(String id){for(App a:apps)if(appId(a).equals(id))return a;return null;}
    void loadApps(){
        final int generation=++loadGeneration;loadingApps=true;
        loader.execute(()->{
            ArrayList<App> found=new ArrayList<>();HashSet<String> seen=new HashSet<>();
            Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo r:getPackageManager().queryIntentActivities(intent,0)){
                if(r.activityInfo.packageName.equals(getPackageName()))continue;
                String id=r.activityInfo.packageName+"/"+r.activityInfo.name;if(!seen.add(id))continue;
                try{
                    Drawable original=r.loadIcon(getPackageManager()).mutate();
                    // Each app owns a normalized bitmap; the home canvas and grid never share mutable bounds.
                    int size=dp(60);Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);original.setBounds(0,0,size,size);original.draw(c);
                    found.add(new App(r.loadLabel(getPackageManager()).toString(),r.activityInfo.packageName,r.activityInfo.name,new BitmapDrawable(getResources(),bitmap)));
                }catch(Exception ignored){found.add(new App(r.activityInfo.name,r.activityInfo.packageName,r.activityInfo.name,getPackageManager().getDefaultActivityIcon()));}
            }
            java.text.Collator collator=java.text.Collator.getInstance();Collections.sort(found,(a,b)->collator.compare(a.label,b.label));
            handler.post(()->{if(isDestroyed()||generation!=loadGeneration)return;apps.clear();apps.addAll(found);loadingApps=false;home.invalidate();if(drawerRefresh!=null)drawerRefresh.run();});
        });
    }
    App find(String key,String... candidates){
        String saved=prefs.getString(key,"");
        for(App a:apps)if((a.pkg+"/"+a.cls).equals(saved))return a;
        for(String p:candidates)for(App a:apps)if(a.pkg.equals(p))return a;
        return null;
    }
    void start(Intent i){try{startActivity(i);}catch(Exception e){Toast.makeText(this,"No compatible app installed",Toast.LENGTH_SHORT).show();}}
    void launch(App a){if(a!=null){String key="use:"+appId(a);prefs.edit().putInt(key,prefs.getInt(key,0)+1).apply();start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(a.pkg,a.cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}}
    void openSlot(String key,String label,String... pkgs){App a=find(key,pkgs);if(a!=null)launch(a);else showApps("Choose "+label,key,null);}
    void search(){start(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));}
    void showApps(String title,String assign,String group){
        if(drawer!=null&&drawer.isShowing())drawer.dismiss();
        drawer=new AppDrawer(this,title,assign,group);drawer.show();drawer.getWindow().setLayout(-1,-1);
    }
    ArrayList<String> homeItems(){ArrayList<String> items=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("homeItems","[]"));for(int i=0;i<a.length();i++)items.add(a.getString(i));}catch(Exception ignored){}return items;}
    void saveItems(ArrayList<String> items){prefs.edit().putString("homeItems",new JSONArray(items).toString()).apply();home.invalidate();}
    void toggleSet(String key,String value){Set<String> set=new HashSet<>(prefs.getStringSet(key,Collections.emptySet()));if(!set.add(value))set.remove(value);prefs.edit().putStringSet(key,set).apply();home.invalidate();}
    void addHome(App app){ArrayList<String> items=homeItems();String id=appId(app);if(!items.contains(id)){if(items.size()>=20){Toast.makeText(this,"Home is full. Move apps into a folder first.",0).show();return;}items.add(id);saveItems(items);}home.page=1;home.invalidate();Toast.makeText(this,"Added to home",0).show();}
    String itemLabel(String id){if(id.startsWith("folder:"))return prefs.getString("name:"+id,"Folder");App a=byId(id);return a==null?"App unavailable":a.label;}
    void newFolder(App initial){
        if(homeItems().size()>=20){Toast.makeText(this,"Remove a home shortcut to make space",0).show();return;}
        EditText input=new EditText(this);input.setSingleLine();input.setHint("Folder name");input.setText("My folder");
        new AlertDialog.Builder(this).setTitle("New folder").setView(input).setPositiveButton("Create",(d,w)->{String id="folder:"+UUID.randomUUID();String name=input.getText().toString().trim();prefs.edit().putString("name:"+id,name.isEmpty()?"Folder":name).apply();if(initial!=null)prefs.edit().putStringSet("members:"+id,new HashSet<>(Arrays.asList(appId(initial)))).apply();ArrayList<String> items=homeItems();items.add(id);saveItems(items);home.page=1;if(initial==null)editFolder(id);}).setNegativeButton("Cancel",null).show();
    }
    void chooseFolder(App app){ArrayList<String> ids=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();labels.add("Create new folder");for(String id:homeItems())if(id.startsWith("folder:")){ids.add(id);labels.add(itemLabel(id));}
        new AlertDialog.Builder(this).setTitle("Add "+app.label+" to folder").setItems(labels.toArray(new String[0]),(d,n)->{if(n==0)newFolder(app);else{String id=ids.get(n-1);Set<String> members=new HashSet<>(prefs.getStringSet("members:"+id,Collections.emptySet()));members.add(appId(app));prefs.edit().putStringSet("members:"+id,members).apply();home.invalidate();Toast.makeText(this,"Added to "+itemLabel(id),0).show();}}).show();
    }
    void editFolder(String id){ArrayList<App> snapshot=new ArrayList<>(apps);String[] names=new String[snapshot.size()];boolean[] checked=new boolean[names.length];Set<String> members=new HashSet<>(prefs.getStringSet("members:"+id,Collections.emptySet()));for(int i=0;i<names.length;i++){names[i]=snapshot.get(i).label;checked[i]=members.contains(appId(snapshot.get(i)));}
        new AlertDialog.Builder(this).setTitle("Apps in "+itemLabel(id)).setMultiChoiceItems(names,checked,(d,n,on)->{String key=appId(snapshot.get(n));if(on)members.add(key);else members.remove(key);}).setPositiveButton("Save",(d,n)->{prefs.edit().putStringSet("members:"+id,members).apply();home.invalidate();if(drawerRefresh!=null)drawerRefresh.run();}).setNegativeButton("Cancel",null).show();
    }
    void homeMenu(String id){boolean folder=id.startsWith("folder:");new AlertDialog.Builder(this).setTitle(itemLabel(id)).setItems(folder?new String[]{"Open folder","Edit apps","Rename folder","Remove folder"}:new String[]{"Open app","Add to folder","App info","Remove from home"},(d,n)->{
        App a=byId(id);
        if(n==0){if(folder)showApps(itemLabel(id),null,id);else if(a!=null)launch(a);}
        if(n==1){if(folder)editFolder(id);else if(a!=null)chooseFolder(a);}
        if(n==2){if(folder){EditText input=new EditText(this);input.setText(itemLabel(id));new AlertDialog.Builder(this).setTitle("Rename folder").setView(input).setPositiveButton("Save",(x,y)->{String name=input.getText().toString().trim();if(!name.isEmpty())prefs.edit().putString("name:"+id,name).apply();home.invalidate();}).setNegativeButton("Cancel",null).show();}else if(a!=null)start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));}
        if(n==3){ArrayList<String> items=homeItems();items.remove(id);saveItems(items);}
    }).show();}
    void hiddenApps(){ArrayList<App> snapshot=new ArrayList<>(apps);String[] labels=new String[snapshot.size()];boolean[] checked=new boolean[labels.length];Set<String> hidden=new HashSet<>(prefs.getStringSet("hidden",Collections.emptySet()));for(int i=0;i<labels.length;i++){labels[i]=snapshot.get(i).label;checked[i]=hidden.contains(appId(snapshot.get(i)));}
        new AlertDialog.Builder(this).setTitle("Hide apps from drawer").setMultiChoiceItems(labels,checked,(d,n,on)->{String id=appId(snapshot.get(n));if(on)hidden.add(id);else hidden.remove(id);}).setPositiveButton("Save",(d,n)->{prefs.edit().putStringSet("hidden",hidden).apply();if(drawerRefresh!=null)drawerRefresh.run();}).setNegativeButton("Cancel",null).show();
    }
    void slotMenu(String key,String label,App app){new AlertDialog.Builder(this).setTitle(app==null?label:app.label).setItems(new String[]{"Change app","Remove shortcut","App info"},(d,n)->{if(n==0)showApps("Choose "+label,key,null);if(n==1){prefs.edit().putBoolean("removed:"+key,true).apply();home.invalidate();}if(n==2&&app!=null)start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.pkg)));}).show();}
    void settings(){new AlertDialog.Builder(this).setTitle("Amber Launcher").setItems(new String[]{"Set as default home","Set weather text (manual)","Choose wallpaper","Reset home layout","All apps","Create folder","Hidden apps","Drawer grid","Clock format"},(d,n)->{
        if(n==0){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm.isRoleAvailable(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),8);return;}}start(new Intent(Settings.ACTION_HOME_SETTINGS));}
        if(n==1){EditText t=new EditText(this);t.setHint("Example: 30° ☀");t.setText(prefs.getString("weather",""));new AlertDialog.Builder(this).setTitle("Manual weather display").setMessage("Enter your weather text. This is not a live weather feed.").setView(t).setPositiveButton("Save",(x,y)->{prefs.edit().putString("weather",t.getText().toString()).apply();home.invalidate();}).setNegativeButton("Cancel",null).show();}
        if(n==2){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,9);}catch(Exception e){Toast.makeText(this,"File picker unavailable",0).show();}}
        if(n==3){new AlertDialog.Builder(this).setTitle("Reset launcher?").setMessage("This clears your home layout, folders and launcher preferences.").setPositiveButton("Reset",(x,y)->{prefs.edit().clear().apply();home.loadWallpaper();home.page=0;home.invalidate();}).setNegativeButton("Cancel",null).show();}
        if(n==4)showApps("All apps",null,null);
        if(n==5)newFolder(null);
        if(n==6)hiddenApps();
        if(n==7)new AlertDialog.Builder(this).setTitle("Drawer grid").setItems(new String[]{"4 columns","5 columns"},(x,y)->prefs.edit().putInt("columns",y==0?4:5).apply()).show();
        if(n==8){prefs.edit().putBoolean("clock24",!prefs.getBoolean("clock24",false)).apply();home.invalidate();}

    }).show();}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==9&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);prefs.edit().putString("wallpaper",u.toString()).apply();home.loadWallpaper();home.invalidate();}catch(Exception e){Toast.makeText(this,"Unable to open image",0).show();}}}

    class Home extends View {
        Paint p=new Paint(3);Bitmap wallpaper;float sx,sy,downX,downY;boolean held;int page=0,dragIndex=-1;float dragX,dragY;int bottomInset=0;
        ArrayList<Target> targets=new ArrayList<>();
        class Target {RectF r;Runnable click,longClick;Target(RectF r,Runnable c,Runnable l){this.r=r;click=c;longClick=l;}}
        Runnable hold=()->{held=true;if(page==1){int index=workspaceIndex(downX,downY);if(index>=0&&index<homeItems().size()){dragIndex=index;dragX=downX;dragY=downY;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);invalidate();return;}}for(int i=targets.size()-1;i>=0;i--){Target t=targets.get(i);if(t.r.contains(downX,downY)&&t.longClick!=null){performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);t.longClick.run();return;}}settings();};
        Home(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription("Amber home. Swipe up for all apps. Long press for launcher settings.");setOnApplyWindowInsetsListener((v,insets)->{bottomInset=insets.getSystemWindowInsetBottom();invalidate();return insets;});loadWallpaper();}
        void loadWallpaper(){try{String u=prefs.getString("wallpaper","");BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;try(java.io.InputStream in=u.isEmpty()?getAssets().open("wallpaper.jpg"):getContentResolver().openInputStream(Uri.parse(u))){wallpaper=BitmapFactory.decodeStream(in,null,o);}}catch(Exception e){try(java.io.InputStream in=getAssets().open("wallpaper.jpg")){wallpaper=BitmapFactory.decodeStream(in);}catch(Exception ignored){}}}
        void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setShader(null);p.clearShadowLayer();}
        void round(Canvas c,float x,float y,float w,float h,float r,int col){color(col);c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);}
        void text(Canvas c,String s,float x,float y,float size,int col,boolean center){color(col);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);p.setShadowLayer(2,0,1,0x44000000);c.drawText(s,x,y,p);p.clearShadowLayer();}
        void glass(Canvas c,float x,float y,float w,float h,float r){color(Color.WHITE);p.setShader(new LinearGradient(x,y,x,y+h,new int[]{0x70ffffff,0x22ffffff,0x48ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0x44ffffff);c.drawRoundRect(new RectF(x,y,x+w,y+h),r,r,p);p.setStyle(Paint.Style.FILL);}
        void hit(float x,float y,float w,float h,Runnable c,Runnable l){targets.add(new Target(new RectF(x,y,x+w,y+h),c,l));}
        String date(String pattern){return new SimpleDateFormat(pattern,Locale.ENGLISH).format(new Date());}
        @Override protected void onDraw(Canvas real){
            super.onDraw(real);sx=getWidth()/691f;sy=(getHeight()-bottomInset)/1536f;real.drawColor(0xff382819);Canvas c=real;c.save();c.scale(sx,sy);targets.clear();
            if(wallpaper!=null){float scale=Math.max(691f/wallpaper.getWidth(),1536f/wallpaper.getHeight());float w=wallpaper.getWidth()*scale,h=wallpaper.getHeight()*scale;color(-1);c.drawBitmap(wallpaper,null,new RectF((691-w)/2,(1536-h)/2,(691+w)/2,(1536+h)/2),p);}else c.drawColor(0xffb37b42);
            if(page==0){
            text(c,date(prefs.getBoolean("clock24",false)?"HH":"hh"),56,279,139,Color.WHITE,false);text(c,date("mm"),230,232,76,Color.WHITE,false);
            // Right aligned date uses its own anchor, independent of the clock.
            color(Color.WHITE);p.setTextSize(30);p.setTextAlign(Paint.Align.RIGHT);c.drawText(date("EEE, MMM d"),632,196,p);
            String weather=prefs.getString("weather","");text(c,weather.isEmpty()?"":weather,559,256,weather.isEmpty()?23:44,Color.WHITE,true);
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
            }else drawWorkspace(c);
            color(page==0?Color.WHITE:0x66ffffff);c.drawCircle(330,1292,6,p);color(page==1?Color.WHITE:0x66ffffff);c.drawCircle(361,1292,6,p);hit(300,1260,91,60,()->{page=1-page;invalidate();},null);
            glass(c,30,1343,631,161,51);
            slot(c,"phone","Phone",59,1378,92,false,"phone",0xff20d66b,"com.google.android.dialer","com.android.dialer","com.android.contacts");
            slot(c,"contacts","Contacts",180,1378,92,false,"contacts",0xff25d273,"com.google.android.contacts","com.android.contacts");
            slot(c,"camera","Camera",300,1378,92,false,"camera",0xffb5b8bd,"com.transsion.camera","com.android.camera","com.android.camera2","com.google.android.GoogleCamera");
            slot(c,"messages","Messages",420,1378,92,false,"messages",Color.WHITE,"com.google.android.apps.messaging","com.android.mms");
            slot(c,"chrome","Chrome",540,1378,92,false,"chrome",Color.WHITE,"com.android.chrome","com.sec.android.app.sbrowser");
            c.restore();
        }
        void slot(Canvas c,String key,String label,float x,float y,float size,boolean caption,String glyph,int bg,String... pkgs){
            if(prefs.getBoolean("removed:"+key,false)){hit(x,y,size,size,()->showApps("Choose "+label,key,null),()->showApps("Choose "+label,key,null));return;}
            App a=find(key,pkgs);round(c,x,y,size,size,size*.25f,bg);
            if(a!=null){c.save();Path clip=new Path();clip.addRoundRect(new RectF(x,y,x+size,y+size),size*.24f,size*.24f,Path.Direction.CW);c.clipPath(clip);a.icon.setBounds((int)x,(int)y,(int)(x+size),(int)(y+size));a.icon.draw(c);c.restore();}else icon(c,glyph,x,y,size);
            if(caption)text(c,a==null?label:a.label,x+size/2,y+size+30,22,Color.WHITE,true);
            hit(x-4,y-4,size+8,size+(caption?42:8),()->openSlot(key,label,pkgs),()->slotMenu(key,label,a));
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
        int workspaceIndex(float x,float y){if(x<35||x>657||y<210||y>1215)return -1;int col=Math.max(0,Math.min(3,(int)((x-35)/155)));int row=(int)((y-210)/201);return row*4+col;}
        void drawWorkspace(Canvas c){
            text(c,"Your apps",52,143,44,Color.WHITE,false);text(c,"Hold and drag to arrange · Swipe up for all apps",52,185,20,0xddffffff,false);
            ArrayList<String> items=homeItems();if(items.isEmpty()){glass(c,55,400,581,235,35);text(c,"Make this space yours",346,482,32,Color.WHITE,true);text(c,"Long-press an app in All apps to add it here",346,530,21,Color.WHITE,true);text(c,"Tap to browse apps",346,581,22,Color.WHITE,true);hit(55,400,581,235,()->showApps("All apps",null,null),()->newFolder(null));}
            for(int i=0;i<items.size();i++){if(i==dragIndex)continue;drawItem(c,items.get(i),65+(i%4)*155,235+(i/4)*201);}
            if(dragIndex>=0&&dragIndex<items.size()){glass(c,dragX-57,dragY-57,114,114,29);drawItem(c,items.get(dragIndex),dragX-48,dragY-48);}
        }
        void drawItem(Canvas c,String id,float x,float y){
            if(id.startsWith("folder:")){glass(c,x,y,96,96,24);int i=0;for(String member:prefs.getStringSet("members:"+id,Collections.emptySet())){App app=byId(member);if(app!=null){Drawable icon=iconCopy(app);int left=(int)x+10+(i%3)*26,top=(int)y+10+(i/3)*26;icon.setBounds(left,top,left+22,top+22);icon.draw(c);if(++i==9)break;}}if(i==0)text(c,"+",x+48,y+66,48,Color.WHITE,true);
            }else{App a=byId(id);if(a!=null){Drawable icon=iconCopy(a);icon.setBounds((int)x,(int)y,(int)x+96,(int)y+96);icon.draw(c);}else{glass(c,x,y,96,96,24);text(c,"?",x+48,y+66,48,Color.WHITE,true);}}
            String label=itemLabel(id);if(label.length()>15)label=label.substring(0,14)+"…";text(c,label,x+48,y+126,21,Color.WHITE,true);
            hit(x-8,y-8,112,150,()->{if(id.startsWith("folder:"))showApps(itemLabel(id),null,id);else{App a=byId(id);if(a!=null)launch(a);else homeMenu(id);}},()->homeMenu(id));
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX()/sx,y=e.getY()/sy;switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=x;downY=y;held=false;dragIndex=-1;handler.postDelayed(hold,550);return true;
            case MotionEvent.ACTION_MOVE:if(dragIndex>=0){dragX=x;dragY=y;invalidate();return true;}if(Math.abs(x-downX)>18||Math.abs(y-downY)>18)handler.removeCallbacks(hold);return true;
            case MotionEvent.ACTION_UP:handler.removeCallbacks(hold);
                if(dragIndex>=0){ArrayList<String> items=homeItems();int old=dragIndex;dragIndex=-1;if(old<items.size()){if(Math.abs(x-downX)>24||Math.abs(y-downY)>24){int target=workspaceIndex(x,y);if(target>=0){target=Math.min(items.size()-1,target);String item=items.remove(old);items.add(target,item);saveItems(items);}}else homeMenu(items.get(old));}invalidate();return true;}
                if(held)return true;
                if(downY-y>70&&Math.abs(y-downY)>Math.abs(x-downX)){showApps("All apps",null,null);return true;}
                if(Math.abs(x-downX)>90){page=x<downX?1:0;invalidate();return true;}
                if(Math.abs(x-downX)<24&&Math.abs(y-downY)<24){performClick();for(int i=targets.size()-1;i>=0;i--){Target t=targets.get(i);if(t.r.contains(x,y)){t.click.run();return true;}}}return true;
            case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(hold);dragIndex=-1;invalidate();return true;
        }return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
