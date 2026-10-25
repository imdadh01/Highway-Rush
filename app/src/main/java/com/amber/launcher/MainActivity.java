package com.amber.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    android.content.SharedPreferences prefs;
    Workspace board;
    HomeView home;
    FrameLayout root;
    AppDrawer drawer;
    Bitmap wallpaper;
    final ArrayList<App> apps=new ArrayList<>();
    final Handler handler=new Handler(Looper.getMainLooper());
    final java.util.concurrent.ExecutorService loader=java.util.concurrent.Executors.newSingleThreadExecutor();
    boolean loadingApps,dragging;
    int generation;
    final Runnable tick=new Runnable(){public void run(){home.invalidate();handler.postDelayed(this,1000);}};
    final BroadcastReceiver changes=new BroadcastReceiver(){public void onReceive(Context c,Intent i){loadApps();}};
    static class App {String label,pkg,cls;Bitmap icon;App(String l,String p,String c,Bitmap i){label=l;pkg=p;cls=c;icon=i;}}
    static class DragData {String app,itemId,sourceFolder;DragData(String app,String item,String folder){this.app=app;itemId=item;sourceFolder=folder;}}
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("layout",MODE_PRIVATE);board=new Workspace(prefs);loadWallpaper();root=new FrameLayout(this);home=new HomeView(this);root.addView(home,new FrameLayout.LayoutParams(-1,-1));setContentView(root);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);homeBars();
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_PACKAGE_ADDED);f.addAction(Intent.ACTION_PACKAGE_REMOVED);f.addAction(Intent.ACTION_PACKAGE_CHANGED);f.addDataScheme("package");if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changes,f);
        root.setOnDragListener((v,e)->home.drag(e));
    }
    void homeBars(){getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    void panelBars(){int flags=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE;if(ink()!=Color.WHITE&&opacity()>50)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);}
    @Override protected void onResume(){super.onResume();loadApps();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);}
    @Override protected void onDestroy(){super.onDestroy();unregisterReceiver(changes);loader.shutdownNow();handler.removeCallbacksAndMessages(null);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);closeDrawer();home.page=0;home.invalidate();}
    @Override public void onBackPressed(){if(drawer!=null)closeDrawer();else{home.page=0;home.invalidate();}}
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    String appId(App a){return a.pkg+"/"+a.cls;}
    App byId(String id){for(App a:apps)if(appId(a).equals(id))return a;return null;}
    void loadApps(){int g=++generation;loadingApps=true;loader.execute(()->{ArrayList<App> found=new ArrayList<>();HashSet<String> seen=new HashSet<>();Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);for(ResolveInfo r:getPackageManager().queryIntentActivities(i,0)){if(r.activityInfo.packageName.equals(getPackageName()))continue;String id=r.activityInfo.packageName+"/"+r.activityInfo.name;if(!seen.add(id))continue;try{Drawable d=r.loadIcon(getPackageManager()).mutate();int size=dp(64);Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);d.setBounds(0,0,size,size);d.draw(new Canvas(b));found.add(new App(r.loadLabel(getPackageManager()).toString(),r.activityInfo.packageName,r.activityInfo.name,b));}catch(Exception ignored){}}
        java.text.Collator collator=java.text.Collator.getInstance();Collections.sort(found,(a,b)->collator.compare(a.label,b.label));handler.post(()->{if(isDestroyed()||g!=generation)return;apps.clear();apps.addAll(found);loadingApps=false;home.invalidate();if(drawer!=null)drawer.refresh();});});}
    void start(Intent i){try{startActivity(i);}catch(Exception e){toast("No compatible app available");}}
    void launch(App a){if(a==null){toast("App is no longer installed");return;}String key="use:"+appId(a);prefs.edit().putInt(key,prefs.getInt(key,0)+1).apply();start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(a.pkg,a.cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    void openDrawer(){openFolder(null);}
    void openFolder(Workspace.Item folder){closeDrawer();drawer=new AppDrawer(this,folder);panelBars();root.addView(drawer,new FrameLayout.LayoutParams(-1,-1));drawer.setAlpha(0);drawer.animate().alpha(1).setDuration(140).start();}
    void closeDrawer(){if(drawer!=null){((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawer.getWindowToken(),0);root.removeView(drawer);drawer=null;}homeBars();home.invalidate();}
    void startDrag(View source,DragData data){
        App a=byId(data.app);View.DragShadowBuilder shadow;
        if(a!=null){Bitmap b=a.icon;shadow=new View.DragShadowBuilder(source){public void onProvideShadowMetrics(Point size,Point touch){size.set(dp(68),dp(68));touch.set(dp(34),dp(34));}public void onDrawShadow(Canvas c){Paint p=new Paint(3);c.drawBitmap(b,null,new Rect(0,0,dp(68),dp(68)),p);}};}
        else shadow=new View.DragShadowBuilder(source){public void onProvideShadowMetrics(Point size,Point touch){size.set(dp(68),dp(68));touch.set(dp(34),dp(34));}public void onDrawShadow(Canvas c){Paint p=new Paint(3);p.setColor(0x99ffffff);c.drawRoundRect(0,0,dp(68),dp(68),dp(18),dp(18),p);}};
        if(source.startDragAndDrop(ClipData.newPlainText("Amber app",data.app==null?"folder":data.app),shadow,data,0)){dragging=true;((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);source.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);if(drawer!=null)drawer.setVisibility(View.GONE);homeBars();home.invalidate();}
    }
    void finishDrag(){dragging=false;closeDrawer();}
    String label(Workspace.Item i){if(i.folder)return i.name;App a=byId(i.app);return a==null?"Unavailable":a.label;}
    void addHome(App a,String sourceFolder){Workspace.Item item=board.add(appId(a),home.page);if(sourceFolder!=null)board.removeMember(sourceFolder,appId(a));home.page=item.page;closeDrawer();home.invalidate();}
    void openItem(Workspace.Item i){if(i.folder)openFolder(i);else launch(byId(i.app));}
    void toggleSet(String key,String id){Set<String> s=new HashSet<>(prefs.getStringSet(key,Collections.emptySet()));if(!s.add(id))s.remove(id);prefs.edit().putStringSet(key,s).apply();if(drawer!=null)drawer.refresh();}
    int surface(){return prefs.getInt("surface",0xff252326);}
    int opacity(){return prefs.getInt("surfaceOpacity",88);}
    int ink(){int c=surface();return (.2126*Color.red(c)+.7152*Color.green(c)+.0722*Color.blue(c))>150?0xff242226:Color.WHITE;}
    int secondary(){return (ink()&0xffffff)|0xb3000000;}
    GradientDrawable rounded(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    TextView button(String label){TextView t=text(label,14,ink());t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(10),dp(16),dp(10));t.setBackground(rounded((ink()&0xffffff)|0x18000000,18));t.setMinHeight(dp(44));return t;}
    void sheet(String title,String[] options,java.util.function.IntConsumer action){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(22),dp(12),dp(22),dp(20));box.setBackground(rounded(surface(),28));TextView handle=text("━",20,secondary());handle.setGravity(Gravity.CENTER);box.addView(handle,new LinearLayout.LayoutParams(-1,dp(18)));TextView heading=text(title,21,ink());heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(heading,new LinearLayout.LayoutParams(-1,dp(54)));
        for(int n=0;n<options.length;n++){final int index=n;TextView row=text(options[n],16,ink());row.setPadding(dp(4),0,0,0);box.addView(row,new LinearLayout.LayoutParams(-1,dp(49)));row.setOnClickListener(v->{d.dismiss();action.accept(index);});}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(box);d.setContentView(scroll);Window w=d.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.3f);w.setGravity(Gravity.BOTTOM);d.show();w.setLayout(getResources().getDisplayMetrics().widthPixels,Math.min(dp(95+49*options.length),getResources().getDisplayMetrics().heightPixels-dp(100)));
    }
    void itemMenu(Workspace.Item item){sheet(label(item),item.folder?new String[]{"Open folder","Rename","Folder style","Edit apps","Remove folder"}:new String[]{"Open","Add to favorites","App info","Remove from home"},n->{if(item.folder){if(n==0)openFolder(item);if(n==1)rename(item);if(n==2)folderStyles(item);if(n==3)editFolder(item);if(n==4){board.remove(item);home.invalidate();}}else{if(n==0)launch(byId(item.app));if(n==1)toggleSet("favorites",item.app);if(n==2)appInfo(byId(item.app));if(n==3){board.remove(item);home.invalidate();}}});}
    void folderStyles(Workspace.Item item){sheet("Folder style",new String[]{"Small · 1 × 1","Big · 2 × 2","Vertical · 1 × 2"},n->{board.style(item,new String[]{"small","big","vertical"}[n]);home.page=item.page;home.invalidate();if(drawer!=null)drawer.refresh();});}
    void rename(Workspace.Item item){editText("Folder name",item.name,value->{if(!value.trim().isEmpty()){item.name=value.trim();board.save();home.invalidate();if(drawer!=null)drawer.refresh();}});}
    void editText(String title,String value,java.util.function.Consumer<String> save){Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(24),dp(24),dp(24),dp(24));box.setBackground(rounded(surface(),24));box.addView(text(title,21,ink()));EditText input=new EditText(this);input.setTextColor(ink());input.setSingleLine();input.setText(value);box.addView(input,new LinearLayout.LayoutParams(-1,dp(60)));TextView done=button("Save");box.addView(done);done.setOnClickListener(v->{save.accept(input.getText().toString());d.dismiss();});d.setContentView(box);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.show();d.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(36),-2);}
    void editFolder(Workspace.Item item){pickApps("Apps in "+item.name,new HashSet<>(item.members),ids->{item.members.clear();item.members.addAll(ids);board.save();home.invalidate();if(drawer!=null)drawer.refresh();});}
    void pickApps(String title,Set<String> chosen,java.util.function.Consumer<Set<String>> save){
        Dialog d=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(20),dp(18),dp(12));box.setBackgroundColor(surface());box.addView(text(title,24,ink()),new LinearLayout.LayoutParams(-1,dp(54)));ArrayList<App> snapshot=new ArrayList<>(apps);ListView list=new ListView(this);list.setDivider(null);list.setAdapter(new BaseAdapter(){public int getCount(){return snapshot.size();}public Object getItem(int p){return snapshot.get(p);}public long getItemId(int p){return p;}public View getView(int p,View v,ViewGroup parent){App a=snapshot.get(p);LinearLayout row=new LinearLayout(MainActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(10),dp(4),dp(10));ImageView icon=new ImageView(MainActivity.this);icon.setImageBitmap(a.icon);row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));TextView label=text(a.label,16,ink());label.setPadding(dp(16),0,0,0);row.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));TextView mark=text(chosen.contains(appId(a))?"✓":"○",22,ink());row.addView(mark);return row;}});list.setOnItemClickListener((p,v,pos,id)->{String key=appId(snapshot.get(pos));if(!chosen.add(key))chosen.remove(key);((BaseAdapter)list.getAdapter()).notifyDataSetChanged();});box.addView(list,new LinearLayout.LayoutParams(-1,0,1));TextView done=button("Done");box.addView(done);done.setOnClickListener(v->{save.accept(chosen);d.dismiss();});d.setContentView(box);d.getWindow().setStatusBarColor(surface());d.getWindow().setNavigationBarColor(surface());d.show();d.getWindow().setLayout(-1,-1);}
    void appInfo(App a){if(a!=null)start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));}
    void appMenu(App a,Workspace.Item folder){ArrayList<String> opts=new ArrayList<>(Arrays.asList("Add to home","Favorite / unfavorite","App info","Uninstall","Hide from drawer"));if(folder!=null)opts.add("Remove from folder");sheet(a.label,opts.toArray(new String[0]),n->{if(n==0)addHome(a,null);if(n==1)toggleSet("favorites",appId(a));if(n==2)appInfo(a);if(n==3)start(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+a.pkg)));if(n==4)toggleSet("hidden",appId(a));if(n==5){folder.members.remove(appId(a));board.save();if(drawer!=null)drawer.refresh();home.invalidate();}});}
    void settings(){sheet("Home settings",new String[]{"Set as default home","Wallpaper","Background & opacity","Add home screen","Remove this empty screen","12 / 24 hour clock","Hidden apps","Clear home layout"},n->{
        if(n==0){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm.isRoleAvailable(RoleManager.ROLE_HOME)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),8);return;}}start(new Intent(Settings.ACTION_HOME_SETTINGS));}
        if(n==1){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),9);}catch(Exception e){toast("File picker unavailable");}}
        if(n==2)appearance();if(n==3){home.page=board.pages();board.extraPages=home.page+1;board.save();home.invalidate();}
        if(n==4){if(!board.deletePage(home.page))toast("Only an empty additional screen can be removed");home.page=Math.min(home.page,board.pages()-1);home.invalidate();}
        if(n==5){prefs.edit().putBoolean("clock24",!prefs.getBoolean("clock24",false)).apply();home.invalidate();}
        if(n==6)pickApps("Hidden apps",new HashSet<>(prefs.getStringSet("hidden",Collections.emptySet())),ids->{prefs.edit().putStringSet("hidden",ids).apply();if(drawer!=null)drawer.refresh();});
        if(n==7)sheet("Clear apps and folders?",new String[]{"Clear home layout","Cancel"},x->{if(x==0){board.clear();home.page=0;home.invalidate();}});
    });}
    void appearance(){
        Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(24),dp(24),dp(24),dp(24));box.setBackground(rounded(surface(),26));box.addView(text("Background",23,ink()));TextView hint=text("Drawer, folders & menus",13,secondary());box.addView(hint,new LinearLayout.LayoutParams(-1,dp(34)));
        LinearLayout palette=new LinearLayout(this);int[] colors={0xff252326,0xff0b1421,0xff553b2c,0xff3a344b,0xffeee9df,0xfffafafa};for(int color:colors){View swatch=new View(this);GradientDrawable bg=rounded(color,16);bg.setStroke(dp(2),0x66888888);swatch.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1);lp.setMargins(dp(3),0,dp(3),0);palette.addView(swatch,lp);swatch.setOnClickListener(v->{prefs.edit().putInt("surface",color).apply();refreshAppearance();d.dismiss();appearance();});}box.addView(palette);
        TextView custom=button("Custom color · #"+String.format(Locale.US,"%06X",surface()&0xffffff));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(46));cp.topMargin=dp(14);box.addView(custom,cp);custom.setOnClickListener(v->editText("Hex color",String.format(Locale.US,"#%06X",surface()&0xffffff),value->{try{int c=Color.parseColor(value.trim());prefs.edit().putInt("surface",c|0xff000000).apply();refreshAppearance();d.dismiss();appearance();}catch(Exception e){toast("Use a color such as #252326");}}));
        addSlider(box,"Drawer & folder opacity","surfaceOpacity",88);
        addSlider(box,"Home wallpaper tint","homeTint",0);
        TextView done=button("Done");box.addView(done,new LinearLayout.LayoutParams(-1,dp(48)));done.setOnClickListener(v->d.dismiss());d.setContentView(box);d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setGravity(Gravity.BOTTOM);d.show();d.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels,-2);
    }
    void addSlider(LinearLayout box,String title,String key,int defaultValue){TextView label=text(title+" · "+prefs.getInt(key,defaultValue)+"%",14,ink());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));lp.topMargin=dp(10);box.addView(label,lp);SeekBar slider=new SeekBar(this);slider.setMax(100);slider.setProgress(prefs.getInt(key,defaultValue));slider.setProgressTintList(android.content.res.ColorStateList.valueOf(ink()));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(ink()));box.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){if(user){prefs.edit().putInt(key,value).apply();label.setText(title+" · "+value+"%");refreshAppearance();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}
    void refreshAppearance(){home.invalidate();if(drawer!=null){panelBars();drawer.refreshTheme();}}
    void loadWallpaper(){String uri=prefs.getString("wallpaper","");try(java.io.InputStream in=uri.isEmpty()?getAssets().open("wallpaper.jpg"):getContentResolver().openInputStream(Uri.parse(uri))){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;Bitmap b=BitmapFactory.decodeStream(in,null,o);if(b!=null)wallpaper=b;}catch(Exception e){try(java.io.InputStream in=getAssets().open("wallpaper.jpg")){wallpaper=BitmapFactory.decodeStream(in);}catch(Exception ignored){}}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==9&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);prefs.edit().putString("wallpaper",uri.toString()).apply();loadWallpaper();home.invalidate();}catch(Exception e){toast("Unable to open wallpaper");}}}
}
