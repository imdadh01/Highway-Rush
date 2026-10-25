package com.amber.launcher;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Full-window launcher surface, with density-aware icons and an accessible native grid. */
final class AppDrawer extends Dialog {
    final MainActivity host;
    final String title, assign, group;
    final ArrayList<MainActivity.App> visible=new ArrayList<>();
    GridView grid;
    EditText search;
    TextView count,empty,allTab,favTab;
    BaseAdapter adapter;
    boolean favorites=false,usageSort=false;
    static final int INK=0xff242a28,SECONDARY=0xff7a817c,ACCENT=0xff316651,BG=0xfff5f6f2;
    AppDrawer(MainActivity h,String title,String assign,String group){super(h,android.R.style.Theme_Material_Light_NoActionBar);this.host=h;this.title=title;this.assign=assign;this.group=group;}
    int dp(float n){return host.dp(n);}
    TextView text(String s,int size,int color){TextView t=new TextView(host);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        LinearLayout root=new LinearLayout(host);root.setOrientation(1);root.setBackgroundColor(BG);root.setPadding(dp(18),dp(10),dp(10),0);root.setFocusableInTouchMode(true);
        LinearLayout header=new LinearLayout(host);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=text(assign==null?title:"Choose an app",30,INK);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(heading,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView menu=text("⋮",30,INK);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Drawer options");header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));menu.setOnClickListener(v->options());
        TextView close=text("×",30,INK);close.setGravity(Gravity.CENTER);close.setContentDescription("Close app drawer");header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));close.setOnClickListener(v->dismiss());root.addView(header);
        count=text("Loading apps…",13,SECONDARY);count.setPadding(dp(1),0,0,dp(16));root.addView(count);
        LinearLayout searchBox=new LinearLayout(host);searchBox.setGravity(Gravity.CENTER_VERTICAL);searchBox.setBackground(shape(0xffe8ece6,28));searchBox.setPadding(dp(16),0,dp(8),0);
        TextView mag=text("⌕",28,ACCENT);searchBox.addView(mag,new LinearLayout.LayoutParams(dp(32),dp(48)));
        search=new EditText(host);search.setSingleLine();search.setTextSize(16);search.setTextColor(INK);search.setHintTextColor(SECONDARY);search.setHint("Search apps");search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(0,0,0,0);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);searchBox.addView(search,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView clear=text("×",24,SECONDARY);clear.setGravity(Gravity.CENTER);clear.setContentDescription("Clear search");clear.setOnClickListener(v->search.setText(""));searchBox.addView(clear,new LinearLayout.LayoutParams(dp(40),dp(48)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(54));sp.setMargins(0,0,dp(8),dp(18));root.addView(searchBox,sp);
        LinearLayout tabs=new LinearLayout(host);allTab=tab("All apps",tabs);favTab=tab("Favorites",tabs);allTab.setOnClickListener(v->{favorites=false;refresh();});favTab.setOnClickListener(v->{favorites=true;refresh();});root.addView(tabs);
        FrameLayout body=new FrameLayout(host);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,0,1);bp.topMargin=dp(16);root.addView(body,bp);
        grid=new GridView(host);grid.setNumColumns(host.prefs.getInt("columns",4));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setVerticalSpacing(dp(8));grid.setHorizontalSpacing(dp(3));grid.setClipToPadding(false);grid.setPadding(0,0,0,dp(22));grid.setVerticalScrollBarEnabled(false);grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(-1,-1);gp.rightMargin=dp(24);body.addView(grid,gp);
        empty=text("",16,SECONDARY);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(20),dp(40),dp(20));body.addView(empty,new FrameLayout.LayoutParams(-1,-1));grid.setEmptyView(empty);
        LinearLayout rail=new LinearLayout(host);rail.setOrientation(1);rail.setGravity(Gravity.CENTER);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(24),-1,Gravity.RIGHT);body.addView(rail,rp);
        for(char letter='A';letter<='Z';letter++){final String key=String.valueOf(letter);TextView t=text(key,10,ACCENT);t.setGravity(Gravity.CENTER);t.setContentDescription("Jump to "+key);rail.addView(t,new LinearLayout.LayoutParams(-1,0,1));t.setOnClickListener(v->jump(key));}
        // Dragging down the alphabet rail works without 26 precisely targeted taps.
        rail.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){int i=Math.max(0,Math.min(25,(int)(26*e.getY()/Math.max(1,v.getHeight()))));jump(String.valueOf((char)('A'+i)));return true;}return true;});
        adapter=new BaseAdapter(){public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
            public View getView(int position,View recycled,android.view.ViewGroup parent){
                LinearLayout cell;
                if(recycled instanceof LinearLayout)cell=(LinearLayout)recycled;else{
                    cell=new LinearLayout(host);cell.setOrientation(1);cell.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);cell.setPadding(dp(2),dp(5),dp(2),dp(2));
                    ImageView icon=new ImageView(host);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);cell.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
                    TextView label=text("",12,INK);label.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setIncludeFontPadding(false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(34));lp.topMargin=dp(9);cell.addView(label,lp);
                    cell.setLayoutParams(new AbsListView.LayoutParams(-1,dp(108)));
                }
                MainActivity.App a=visible.get(position);((ImageView)cell.getChildAt(0)).setImageDrawable(host.iconCopy(a));((TextView)cell.getChildAt(1)).setText(a.label);cell.setContentDescription(a.label);return cell;
            }
        };
        grid.setAdapter(adapter);grid.setOnItemClickListener((p,v,pos,id)->{
            MainActivity.App a=visible.get(pos);
            if(assign!=null){host.prefs.edit().putString(assign,host.appId(a)).remove("removed:"+assign).apply();host.home.invalidate();dismiss();}
            else{host.launch(a);dismiss();}
        });
        grid.setOnItemLongClickListener((p,v,pos,id)->{if(assign==null)appMenu(visible.get(pos));else Toast.makeText(host,"Tap an app to select it",0).show();return true;});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh();}public void afterTextChanged(Editable e){}});
        search.setOnEditorActionListener((v,action,event)->{if(!visible.isEmpty()){MainActivity.App a=visible.get(0);if(assign!=null){host.prefs.edit().putString(assign,host.appId(a)).remove("removed:"+assign).apply();host.home.invalidate();}else host.launch(a);dismiss();}return true;});
        setContentView(root);root.requestFocus();host.drawerRefresh=this::refresh;refresh();
        setOnDismissListener(d->{host.drawerRefresh=null;host.home.invalidate();});
    }
    TextView tab(String label,LinearLayout parent){TextView t=text(label,14,INK);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(112),dp(38));lp.rightMargin=dp(10);parent.addView(t,lp);return t;}
    void jump(String key){for(int i=0;i<visible.size();i++)if(visible.get(i).label.toUpperCase(Locale.ROOT).compareTo(key)>=0){grid.setSelection(i);return;}}
    void refresh(){if(adapter==null)return;
        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);Set<String> hidden=host.prefs.getStringSet("hidden",Collections.emptySet()),favs=host.prefs.getStringSet("favorites",Collections.emptySet());
        Set<String> members=group!=null&&group.startsWith("folder:")?host.prefs.getStringSet("members:"+group,Collections.emptySet()):null;
        visible.clear();for(MainActivity.App a:host.apps){String id=host.appId(a);if(!hidden.contains(id)&&(!favorites||favs.contains(id))&&(group==null||(members!=null?members.contains(id):a.pkg.startsWith(group)))&&(a.label.toLowerCase(Locale.ROOT).contains(q)||a.pkg.toLowerCase(Locale.ROOT).contains(q)))visible.add(a);}
        if(usageSort)Collections.sort(visible,(a,b)->Integer.compare(host.prefs.getInt("use:"+host.appId(b),0),host.prefs.getInt("use:"+host.appId(a),0)));
        count.setText(visible.size()+" apps  ·  "+(usageSort?"Most used":"A–Z"));empty.setText(host.loadingApps?"Loading apps…":q.isEmpty()?(favorites?"Your favorites appear here.\nLong-press an app to add one.":"No apps in this folder yet."):"No apps match “"+search.getText()+"”");
        allTab.setBackground(shape(!favorites?ACCENT:0xffe8ece6,20));allTab.setTextColor(!favorites?Color.WHITE:INK);favTab.setBackground(shape(favorites?ACCENT:0xffe8ece6,20));favTab.setTextColor(favorites?Color.WHITE:INK);adapter.notifyDataSetChanged();
    }
    void options(){ArrayList<String> opts=new ArrayList<>(Arrays.asList("Sort A–Z","Sort by most used","4 columns","5 columns","Hidden apps"));if(group!=null&&group.startsWith("folder:"))opts.add("Edit folder apps");new AlertDialog.Builder(host).setTitle("App drawer").setItems(opts.toArray(new String[0]),(d,n)->{if(n==0)usageSort=false;if(n==1)usageSort=true;if(n==2||n==3){int cols=n==2?4:5;host.prefs.edit().putInt("columns",cols).apply();grid.setNumColumns(cols);}if(n==4)host.hiddenApps();if(n==5)host.editFolder(group);refresh();}).show();}
    void appMenu(MainActivity.App a){String id=host.appId(a);boolean favorite=host.prefs.getStringSet("favorites",Collections.emptySet()).contains(id);
        new AlertDialog.Builder(host).setTitle(a.label).setItems(new String[]{"Add to home screen",favorite?"Remove favorite":"Add to favorites","Add to folder","App info","Uninstall","Hide from drawer"},(d,n)->{
            if(n==0){host.addHome(a);dismiss();}
            if(n==1){host.toggleSet("favorites",id);refresh();}
            if(n==2)host.chooseFolder(a);
            if(n==3)host.start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));
            if(n==4)host.start(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+a.pkg)));
            if(n==5){host.toggleSet("hidden",id);refresh();}
        }).show();
    }
}
