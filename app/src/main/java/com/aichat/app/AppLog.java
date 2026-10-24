package com.aichat.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLog {

    private static final String FILE = "app_log.txt";
    private static final long MAX_BYTES = 2 * 1024 * 1024;
    static final String LOG_TAG_HASH = "hX0dlb+WHuuWTge+8jOW8";
    private static volatile boolean recording = false;
    private static Context appCtx;

    public static void init(Context ctx) {
        appCtx = ctx.getApplicationContext();
        recording = new Prefs(appCtx).getLogRecording();
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void start(Context ctx) {
        appCtx = ctx.getApplicationContext();
        recording = true;
        new Prefs(appCtx).setLogRecording(true);
        w("AppLog", "==== 录制开始 ====");
    }

    public static void stop(Context ctx) {
        w("AppLog", "==== 录制停止 ====");
        recording = false;
        if (appCtx == null) appCtx = ctx.getApplicationContext();
        new Prefs(appCtx).setLogRecording(false);
    }

    public static void w(String tag, String msg) {
        android.util.Log.d("GeopeliaLog", tag + ": " + msg);
        write(tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable t) {
        android.util.Log.e("GeopeliaLog", tag + ": " + msg, t);
        write(tag, msg, t);
    }

    private static void write(String tag, String msg, Throwable t) {
        if (!recording || appCtx == null) return;
        try {
            File f = new File(appCtx.getFilesDir(), FILE);
            String ts = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.CHINA).format(new Date());
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(ts).append("] [").append(tag).append("] ").append(msg).append('\n');
            if (t != null) {
                java.io.StringWriter sw = new java.io.StringWriter();
                t.printStackTrace(new java.io.PrintWriter(sw));
                sb.append(sw.toString()).append('\n');
            }
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            if (f.exists() && f.length() + bytes.length > MAX_BYTES) {
                long keep = MAX_BYTES / 2;
                byte[] tail = new byte[(int) Math.min(f.length(), keep)];
                try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                    raf.seek(f.length() - tail.length);
                    raf.readFully(tail);
                }
                String head = new String(tail, StandardCharsets.UTF_8);
                int nl = head.indexOf('\n');
                if (nl >= 0) head = head.substring(nl + 1);
                try (FileOutputStream out = new FileOutputStream(f, false)) {
                    out.write(head.getBytes(StandardCharsets.UTF_8));
                }
            }
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                out.write(bytes);
            }
        } catch (Exception ignored) {
        }
    }

    public static String read(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (!f.exists()) return "（暂无录制日志）";
            long len = Math.min(f.length(), 3 * 1024 * 1024);
            byte[] all = new byte[(int) len];
            try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                raf.readFully(all);
            }
            return new String(all, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "读取失败：" + e.getMessage();
        }
    }

    public static void clear(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {
        }
    }

    public static void crash(Throwable t) {
        if (appCtx == null) return;
        recording = true;
        write("CRASH", "程序崩溃", t);
        recording = new Prefs(appCtx).getLogRecording();
    }
}
