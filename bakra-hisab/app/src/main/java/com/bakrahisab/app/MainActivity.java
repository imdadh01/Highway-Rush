package com.bakrahisab.app;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.graphics.*;
import android.provider.MediaStore;
import android.print.PrintManager;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private WebView web; private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private AtomicFile dataFile; private android.content.SharedPreferences prefs;
 private String pendingText="",pendingName="",photoKey=""; private volatile boolean backupBusy=false;
 private final Runnable autoBackup=()->runBackup();
 private static final int EXPORT=10,LINK=11,RESTORE=12,PHOTO=13,CAMERA=14;
 @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("backup",MODE_PRIVATE);dataFile=new AtomicFile(new File(getFilesDir(),"hisab-v1.json"));
  web=new WebView(this);web.setBackgroundColor(Color.rgb(246,248,245));web.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView w,String u){return true;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest request){
    Uri u=request.getUrl();String path=u.getPath();
    if("https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&path!=null&&path.matches("/assets/(index\\.html|style\\.css|engine\\.js|app\\.js)")){
     try{String mime=path.endsWith(".html")?"text/html":path.endsWith(".css")?"text/css":"application/javascript";return new WebResourceResponse(mime,"UTF-8",getAssets().open(path.substring(8)));}catch(IOException ignored){}
    }
    return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
   }
  });
  web.setWebChromeClient(new WebChromeClient());web.addJavascriptInterface(new Bridge(),"Native");setContentView(web);web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 private void emit(String type,String payload){runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.nativeEvent&&window.nativeEvent("+JSONObject.quote(type)+","+JSONObject.quote(payload)+")",null);});}
 private byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n,total=0;while((n=x.read(buf))!=-1){total+=n;if(total>60*1024*1024)throw new IOException("Backup 60 MB se bara hai");b.write(buf,0,n);}return b.toByteArray();}}
 private synchronized String readData(){try{return new String(readAll(dataFile.openRead()),StandardCharsets.UTF_8);}catch(FileNotFoundException e){return "";}catch(Exception e){return "!ERROR!";}}
 private synchronized boolean writeData(String value){FileOutputStream out=null;try{JSONObject j=new JSONObject(value);if(j.getInt("version")!=1)throw new Exception();if(value.length()>55*1024*1024)throw new Exception();out=dataFile.startWrite();out.write(value.getBytes(StandardCharsets.UTF_8));dataFile.finishWrite(out);return true;}catch(Exception e){if(out!=null)dataFile.failWrite(out);return false;}}
 private void chooseCreate(int code,String name,String mime){runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,code);}catch(Exception e){emit("error","File picker available nahi.");}});}
 private String backupEnvelope()throws Exception{String raw=readData();if(raw.isEmpty()||raw.equals("!ERROR!"))throw new Exception("No data");JSONObject j=new JSONObject();j.put("app","BakraHisab");j.put("backupVersion",1);j.put("createdAt",System.currentTimeMillis());j.put("data",new JSONObject(raw));return j.toString();}
 private void writeUri(Uri uri,String text)throws Exception{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(text.getBytes(StandardCharsets.UTF_8));out.flush();}}
 private void runBackup(){if(backupBusy||!prefs.getBoolean("auto",false)||prefs.getString("uri","").isEmpty())return;backupBusy=true;emit("backup","Backup file update ho rahi hai...");io.execute(()->{try{String value=backupEnvelope();long revision=new JSONObject(value).getJSONObject("data").getLong("revision");writeUri(Uri.parse(prefs.getString("uri","")),value);prefs.edit().putLong("last",System.currentTimeMillis()).putLong("revision",revision).putString("error","").apply();emit("backup","Backup file save ho gayi. Cloud sync ka status Drive app mein check karein.");}catch(Exception e){prefs.edit().putString("error","Backup pending: internet, storage ya file access check karein.").apply();emit("backup","Backup pending. Phone ka data mehfooz hai.");}finally{backupBusy=false;}});}
 public class Bridge {
  @JavascriptInterface public boolean snapshotBeforeRestore(){try{String old=readData();if(old.isEmpty()||old.equals("!ERROR!"))return true;AtomicFile snapshot=new AtomicFile(new File(getFilesDir(),"pre-restore.json"));FileOutputStream out=snapshot.startWrite();try{out.write(old.getBytes(StandardCharsets.UTF_8));snapshot.finishWrite(out);return true;}catch(Exception e){snapshot.failWrite(out);return false;}}catch(Exception e){return false;}}
  @JavascriptInterface public String load(){return readData();}
  @JavascriptInterface public boolean save(String value){boolean ok=writeData(value);if(ok)handler.post(()->{handler.removeCallbacks(autoBackup);handler.postDelayed(autoBackup,8000);});return ok;}
  @JavascriptInterface public String backupStatus(){JSONObject j=new JSONObject();try{j.put("linked",!prefs.getString("uri","").isEmpty());j.put("auto",prefs.getBoolean("auto",false));j.put("last",prefs.getLong("last",0));j.put("revision",prefs.getLong("revision",-1));j.put("error",prefs.getString("error",""));}catch(Exception e){}return j.toString();}
  @JavascriptInterface public void linkBackup(){chooseCreate(LINK,"Bakra-Hisab-auto-backup.json","application/json");}
  @JavascriptInterface public void setAuto(boolean on){prefs.edit().putBoolean("auto",on).apply();if(on)handler.post(autoBackup);}
  @JavascriptInterface public void backupNow(){if(prefs.getString("uri","").isEmpty()){linkBackup();return;}prefs.edit().putBoolean("auto",true).apply();handler.post(autoBackup);}
  @JavascriptInterface public void exportBackup(){try{pendingText=backupEnvelope();chooseCreate(EXPORT,"Bakra-Hisab-backup-"+System.currentTimeMillis()+".json","application/json");}catch(Exception e){emit("error","Backup tayyar nahi ho saka.");}}
  @JavascriptInterface public void exportText(String text,String name,String mime){pendingText=text;chooseCreate(EXPORT,name,mime);}
  @JavascriptInterface public void restore(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,RESTORE);});}
  @JavascriptInterface public void photo(String key,boolean camera){photoKey=key;runOnUiThread(()->{try{Intent i=camera?new Intent(MediaStore.ACTION_IMAGE_CAPTURE):new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,camera?CAMERA:PHOTO);}catch(Exception e){emit("error","Camera / gallery available nahi.");}});}
  @JavascriptInterface public void printReport(){runOnUiThread(()->{try{PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);pm.print("Bakra-Hisab-report",web.createPrintDocumentAdapter("Bakra-Hisab"),null);}catch(Exception e){emit("error","Print service available nahi.");}});}
  @JavascriptInterface public boolean hasPin(){return prefs.contains("pin");}
  @JavascriptInterface public boolean verifyPin(String pin){long until=prefs.getLong("pinUntil",0);if(System.currentTimeMillis()<until)return false;try{byte[] salt=Base64.decode(prefs.getString("salt",""),Base64.NO_WRAP);String hash=derive(pin,salt);boolean ok=MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8),prefs.getString("pin","").getBytes(StandardCharsets.UTF_8));int fail=ok?0:prefs.getInt("pinFail",0)+1;prefs.edit().putInt("pinFail",fail).putLong("pinUntil",fail>=5?System.currentTimeMillis()+30000:0).apply();return ok;}catch(Exception e){return false;}}
  @JavascriptInterface public boolean setPin(String old,String pin){if(hasPin()&&!verifyPin(old))return false;if(pin.isEmpty()){prefs.edit().remove("pin").remove("salt").apply();return true;}if(!pin.matches("[0-9]{4,8}"))return false;try{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);prefs.edit().putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("pin",derive(pin,salt)).apply();return true;}catch(Exception e){return false;}}
 }
 private String derive(String pin,byte[] salt)throws Exception{return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(pin.toCharArray(),salt,120000,256)).getEncoded(),Base64.NO_WRAP);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null){emit("cancel","");return;}
  if(request==CAMERA){Object b=data.getExtras()==null?null:data.getExtras().get("data");if(b instanceof Bitmap)sendPhoto((Bitmap)b,photoKey);else emit("error","Camera photo nahi mili; Gallery use karein.");return;}
  Uri uri=data.getData();if(uri==null)return;
  if(request==PHOTO){String key=photoKey;io.execute(()->{try{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}o.inSampleSize=Math.max(1,Math.max(o.outWidth,o.outHeight)/1200);o.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,o);}if(bitmap==null)throw new IOException();try(InputStream in=getContentResolver().openInputStream(uri)){android.media.ExifInterface exif=new android.media.ExifInterface(in);int orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();if(orientation==3)m.postRotate(180);if(orientation==6)m.postRotate(90);if(orientation==8)m.postRotate(270);bitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);}catch(Exception ignored){}sendPhoto(bitmap,key);}catch(Exception e){emit("error","Photo load nahi hui.");}});return;}
  if(request==RESTORE){io.execute(()->{try{String txt=new String(readAll(getContentResolver().openInputStream(uri)),StandardCharsets.UTF_8);emit("restore",txt);}catch(Exception e){emit("error","Backup file read nahi hui.");}});return;}
  if(request==LINK){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception e){emit("error","Is location par permanent access nahi. Doosri location select karein.");return;}prefs.edit().putString("uri",uri.toString()).putBoolean("auto",true).apply();runBackup();return;}
  if(request==EXPORT){String txt=pendingText;io.execute(()->{try{writeUri(uri,txt);emit("success","File save ho gayi.");}catch(Exception e){emit("error","File save nahi hui. Dobara try karein.");}});}
 }
 private void sendPhoto(Bitmap b,String key){int max=1000;float ratio=Math.min(1f,(float)max/Math.max(b.getWidth(),b.getHeight()));Bitmap scaled=Bitmap.createScaledBitmap(b,Math.max(1,(int)(b.getWidth()*ratio)),Math.max(1,(int)(b.getHeight()*ratio)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,78,out);try{JSONObject j=new JSONObject();j.put("key",key);j.put("data","data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP));emit("photo",j.toString());}catch(Exception ignored){}}
 @Override public void onBackPressed(){web.evaluateJavascript("window.goBack&&window.goBack()",null);}
 @Override protected void onResume(){super.onResume();if(web!=null){web.onResume();handler.postDelayed(autoBackup,2000);}}
 @Override protected void onPause(){if(web!=null)web.evaluateJavascript("window.dispatchEvent(new Event('appPause'))",null);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdown();if(web!=null){web.removeJavascriptInterface("Native");web.destroy();web=null;}super.onDestroy();}
}
