package com.aichat.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    static final String BOOT_ACTION_EXT = "kdQTDPljY/orq7vvIznpo";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLog.w("Greet", "开机广播");
        GreetJobService.register(context);
        try {
            Intent i = new Intent(context, GreetService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception ignored) {
        }
    }
}
