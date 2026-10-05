package com.imdadh.merahisaab;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.util.AtomicFile;
import android.webkit.*;
import android.view.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingBackup;
    private boolean unlocking=false, skipNextUnlock=false;
    private final int EXPORT=51, IMPORT=52, UNLOCK=53;
    private AtomicFile ledger;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ledger=new AtomicFile(new File(getFilesDir(),"ledger-v1.json"));
        web=new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.setPadding(0,0,0,0);
        web.setOnApplyWindowInsetsListener((v,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());
                v.setPadding(b.left,b.top,b.right,b.bottom);
            }
            return insets;
        });
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"Android");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"https://app.local/index.html".equals(r.getUrl().toString());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                Uri u=r.getUrl();
                if(!"app.local".equals(u.getHost())) return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                String p=u.getPath();
                if(p==null||p.contains("..")) return null;
                try { String n=p.substring(1); String mime=n.endsWith(".js")?"application/javascript":n.endsWith(".css")?"text/css":"text/html";
                    return new WebResourceResponse(mime,"UTF-8",getAssets().open(n));
                } catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsConfirm(WebView v,String url,String msg,JsResult result){new AlertDialog.Builder(MainActivity.this).setMessage(msg).setPositiveButton(android.R.string.ok,(d,w)->result.confirm()).setNegativeButton(android.R.string.cancel,(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
        });
        setContentView(web); web.loadUrl("https://app.local/index.html");
    }
    private boolean locked(){return getPreferences(MODE_PRIVATE).getBoolean("lock",false);}
    @Override public void onResume(){super.onResume(); if(skipNextUnlock){skipNextUnlock=false;web.setVisibility(View.VISIBLE);}else if(locked()&&!unlocking){web.setVisibility(View.INVISIBLE);authenticate();}}
    @Override public void onPause(){super.onPause(); if(locked())web.setVisibility(View.INVISIBLE);}
    private void authenticate(){
        KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        Intent intent=km.createConfirmDeviceCredentialIntent("Mera Hisaab","Unlock your financial records");
        if(intent==null){getPreferences(MODE_PRIVATE).edit().putBoolean("lock",false).apply();web.setVisibility(View.VISIBLE);return;}
        unlocking=true; startActivityForResult(intent,UNLOCK);
    }
    private synchronized String read(){try{return new String(ledger.readFully(),StandardCharsets.UTF_8);}catch(FileNotFoundException e){return "";}catch(Exception e){return "__READ_ERROR__";}}
    private synchronized boolean write(String data){FileOutputStream out=null;try{
        JSONObject obj=new JSONObject(data); if(obj.optInt("version")!=1||!obj.has("profiles"))return false;
        out=ledger.startWrite();out.write(data.getBytes(StandardCharsets.UTF_8));ledger.finishWrite(out);return true;
    }catch(Exception e){if(out!=null)ledger.failWrite(out);return false;}}
    private void callback(String name,String value){web.evaluateJavascript("window."+name+" && window."+name+"("+JSONObject.quote(value)+")",null);}
    public class Bridge {
        @JavascriptInterface public String load(){return read();}
        @JavascriptInterface public boolean save(String data){return write(data);}
        @JavascriptInterface public boolean isLocked(){return locked();}
        @JavascriptInterface public String setLock(boolean enable){
            KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            if(enable&&!km.isDeviceSecure())return "Set a phone screen lock first / Pehle phone ka screen lock lagayein";
            getPreferences(MODE_PRIVATE).edit().putBoolean("lock",enable).apply();return "";
        }
        @JavascriptInterface public void exportBackup(){runOnUiThread(()->{
            pendingBackup=read(); if(pendingBackup.isEmpty()||pendingBackup.equals("__READ_ERROR__")){callback("nativeError","No readable data to back up");return;}
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE,"Mera-Hisaab-"+new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm",java.util.Locale.US).format(new java.util.Date())+".json");
            try{startActivityForResult(i,EXPORT);}catch(Exception e){callback("nativeError","No document provider available");}
        });}
        @JavascriptInterface public void importBackup(){runOnUiThread(()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            try{startActivityForResult(i,IMPORT);}catch(Exception e){callback("nativeError","No document provider available");}
        });}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==UNLOCK){unlocking=false;if(result==RESULT_OK){skipNextUnlock=true;web.setVisibility(View.VISIBLE);}else finish();return;}
        if(result!=RESULT_OK||data==null||data.getData()==null){pendingBackup=null;return;}
        Uri uri=data.getData();
        try {
            if(request==EXPORT){if(pendingBackup==null)throw new IOException("Export interrupted; please retry");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));}pendingBackup=null;callback("backupSaved","ok");}
            if(request==IMPORT){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>20*1024*1024)throw new IOException("Backup exceeds 20 MB");out.write(b,0,n);}callback("restoreCandidate",out.toString("UTF-8"));}}
        }catch(Exception e){callback("nativeError","File operation failed / File save ya read nahi hui. Dobara koshish karein.");}
    }
    @Override public void onBackPressed(){web.evaluateJavascript("window.back && window.back()",null);}
    @Override public void onDestroy(){if(web!=null){web.removeJavascriptInterface("Android");web.destroy();}super.onDestroy();}
}
