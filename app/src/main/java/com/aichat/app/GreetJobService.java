package com.aichat.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

public class GreetJobService extends JobService {

    private static final int JOB_ID = 1001;
    static final String JOB_TAG = "mlLnor7fmoIfms6jmnaXm";

    public static void register(Context ctx) {
        if (Build.VERSION.SDK_INT < 21) return;
        try {
            JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            ComponentName cn = new ComponentName(ctx, GreetJobService.class);
            JobInfo.Builder b = new JobInfo.Builder(JOB_ID, cn)
                    .setPeriodic(15 * 60 * 1000L)
                    .setPersisted(true);
            JobInfo job = b.build();
            if (js.getPendingJob(JOB_ID) == null) {
                js.schedule(job);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        AppLog.w("Greet", "JobScheduler 兜底触发");
        GreetLog.write(this, "JobScheduler 兜底触发");
        GreetService.checkAndSchedule(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
