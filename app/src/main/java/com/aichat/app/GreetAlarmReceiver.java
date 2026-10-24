package com.aichat.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GreetAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        AppLog.w("Greet", "闹钟触发");
        try {
            Intent svc = new Intent(ctx, GreetService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                ctx.startForegroundService(svc);
            } else {
                ctx.startService(svc);
            }
        } catch (Exception e) {
            GreetService.checkAndSchedule(ctx);
        }
    }
}
