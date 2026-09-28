package com.bakrahisab.app;
import android.content.*;
public class HealthAlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){if("com.bakrahisab.app.HEALTH_ALARM".equals(intent.getAction()))HealthReminders.deliver(context);else HealthReminders.schedule(context);}
}
