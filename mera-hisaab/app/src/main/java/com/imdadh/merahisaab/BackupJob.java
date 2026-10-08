package com.imdadh.merahisaab;

import android.app.job.*;
import android.content.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import java.io.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** User-selected document backup; provider owns any subsequent cloud upload. */
public class BackupJob extends JobService {
    public static final Object DATA_LOCK=new Object();
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final java.util.Set<JobParameters> stopped=java.util.Collections.newSetFromMap(new ConcurrentHashMap<JobParameters,Boolean>());
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("auto-backup",Context.MODE_PRIVATE);}
    static void schedule(Context c,boolean periodic){
        JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(!prefs(c).getBoolean("enabled",false))return;
        int id=periodic?701:702;
        if(js.getPendingJob(id)!=null)return;
        JobInfo.Builder b=new JobInfo.Builder(id,new ComponentName(c,BackupJob.class)).setPersisted(true);
        if(periodic)b.setPeriodic(6*60*60*1000L);else b.setMinimumLatency(15000).setBackoffCriteria(60000,JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        js.schedule(b.build());
    }
    static void disable(Context c){prefs(c).edit().putBoolean("enabled",false).apply();JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);js.cancel(701);js.cancel(702);}
    @Override public boolean onStartJob(JobParameters params){
        IO.execute(()->{boolean ok=backup(this);new Handler(Looper.getMainLooper()).post(()->{if(!stopped.remove(params))jobFinished(params,!ok&&params.getJobId()==702);});});return true;
    }
    @Override public boolean onStopJob(JobParameters params){stopped.add(params);return prefs(this).getBoolean("enabled",false);}
    private static boolean backup(Context c){
        SharedPreferences p=prefs(c);if(!p.getBoolean("enabled",false))return true;
        try{
            byte[] bytes;
            synchronized(DATA_LOCK){bytes=new AtomicFile(new File(c.getFilesDir(),"ledger-v1.json")).readFully();}
            JSONObject data=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
            if(data.optInt("version")!=1||!data.has("profiles"))throw new IOException("Invalid local data");
            // Retain a complete local snapshot even if the provider write fails.
            AtomicFile snapshot=new AtomicFile(new File(c.getFilesDir(),"last-auto-backup.json"));
            FileOutputStream local=null;
            try{local=snapshot.startWrite();local.write(bytes);snapshot.finishWrite(local);}catch(Exception e){if(local!=null)snapshot.failWrite(local);throw e;}
            if(!p.getBoolean("enabled",false))return true;
            Uri uri=Uri.parse(p.getString("uri",""));
            try(OutputStream out=c.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No output stream");out.write(bytes);out.flush();}
            p.edit().putLong("last",System.currentTimeMillis()).putBoolean("error",false).apply();return true;
        }catch(Exception e){p.edit().putBoolean("error",true).apply();return false;}
    }
}
