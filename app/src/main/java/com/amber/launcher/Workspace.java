package com.amber.launcher;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Persistent cell-based pages. App and folder items share the same placement rules. */
final class Workspace {
    static class Item {
        String id=UUID.randomUUID().toString(),app="",name="Folder",style="small";
        boolean folder=false;
        int page=0,col=0,row=0,dock=-1;
        ArrayList<String> members=new ArrayList<>();
        int width(){return folder&&style.equals("big")?2:1;}
        int height(){return folder&&!style.equals("small")?2:1;}
        JSONObject json() throws JSONException {JSONObject o=new JSONObject();o.put("id",id);o.put("app",app);o.put("name",name);o.put("style",style);o.put("folder",folder);o.put("page",page);o.put("col",col);o.put("row",row);o.put("dock",dock);o.put("members",new JSONArray(members));return o;}
        static Item from(JSONObject o){Item i=new Item();i.id=o.optString("id",i.id);i.app=o.optString("app","");i.name=o.optString("name","Folder");i.style=o.optString("style","small");i.folder=o.optBoolean("folder");i.page=o.optInt("page");i.col=o.optInt("col");i.row=o.optInt("row");i.dock=o.optInt("dock",-1);JSONArray a=o.optJSONArray("members");if(a!=null)for(int n=0;n<a.length();n++)i.members.add(a.optString(n));return i;}
    }
    final ArrayList<Item> items=new ArrayList<>();
    final SharedPreferences prefs;
    int extraPages=1;
    Workspace(SharedPreferences p){prefs=p;try{JSONArray a=new JSONArray(p.getString("workspace_v3","[]"));for(int n=0;n<a.length();n++)items.add(Item.from(a.getJSONObject(n)));}catch(Exception ignored){}extraPages=Math.max(1,p.getInt("workspacePages",1));
        if(!p.contains("workspace_v3"))migrate();
    }
    void migrate(){
        // Import only the user's explicitly added v2 shortcuts/folders. No suggested or dock apps.
        try{JSONArray old=new JSONArray(prefs.getString("homeItems","[]"));for(int n=0;n<old.length();n++){String id=old.getString(n);Item i=new Item();if(id.startsWith("folder:")){i.folder=true;i.name=prefs.getString("name:"+id,"Folder");i.members.addAll(prefs.getStringSet("members:"+id,Collections.emptySet()));}else i.app=id;placeFree(i,0);items.add(i);}}catch(Exception ignored){}save();
    }
    int rows(int page){return page==0?4:7;}
    int pages(){int n=extraPages;for(Item i:items)if(i.dock<0)n=Math.max(n,i.page+1);return Math.max(1,n);}
    Item get(String id){for(Item i:items)if(i.id.equals(id))return i;return null;}
    Item at(int page,int col,int row,String excluding){for(Item i:items)if(i.dock<0&&!i.id.equals(excluding)&&i.page==page&&col>=i.col&&col<i.col+i.width()&&row>=i.row&&row<i.row+i.height())return i;return null;}
    Item dock(int slot,String excluding){for(Item i:items)if(i.dock==slot&&!i.id.equals(excluding))return i;return null;}
    boolean fits(Item item,int page,int col,int row){if(col<0||row<0||col+item.width()>4||row+item.height()>rows(page))return false;for(int y=row;y<row+item.height();y++)for(int x=col;x<col+item.width();x++)if(at(page,x,y,item.id)!=null)return false;return true;}
    void placeFree(Item item,int start){item.dock=-1;for(int p=Math.max(0,start);;p++)for(int r=0;r<=rows(p)-item.height();r++)for(int c=0;c<=4-item.width();c++)if(fits(item,p,c,r)){item.page=p;item.col=c;item.row=r;extraPages=Math.max(extraPages,p+1);return;}}
    Item add(String app,int page){Item i=new Item();i.app=app;placeFree(i,page);items.add(i);save();return i;}
    void style(Item item,String style){item.style=style;if(item.dock>=0){if(!style.equals("small"))placeFree(item,item.page);}else if(!fits(item,item.page,item.col,item.row))placeFree(item,item.page);save();}
    void remove(Item item){items.remove(item);save();}
    void save(){JSONArray a=new JSONArray();for(Item i:items)try{a.put(i.json());}catch(Exception ignored){}prefs.edit().putString("workspace_v3",a.toString()).putInt("workspacePages",extraPages).apply();}
    void clear(){items.clear();extraPages=1;save();}
    void removeMember(String folder,String app){Item i=get(folder);if(i!=null&&i.folder){i.members.remove(app);save();}}
    boolean emptyPage(int p){for(Item i:items)if(i.dock<0&&i.page==p)return false;return true;}
    boolean deletePage(int p){if(p==0||!emptyPage(p))return false;for(Item i:items)if(i.dock<0&&i.page>p)i.page--;extraPages=Math.max(1,pages()-1);save();return true;}
}
