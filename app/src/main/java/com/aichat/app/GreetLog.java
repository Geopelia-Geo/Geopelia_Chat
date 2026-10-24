package com.aichat.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GreetLog {

    private static final long MAX_BYTES = 300 * 1024;

    public static void write(Context ctx, String msg) {
        try {
            File f = new File(ctx.getFilesDir(), "greet_log.txt");
            String ts = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
            String line = "[" + ts + "] " + msg + "\n";
            if (f.exists() && f.length() + line.length() > MAX_BYTES) {
                byte[] keep = new byte[(int) Math.min(f.length(), MAX_BYTES / 2)];
                try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                    raf.seek(f.length() - keep.length);
                    raf.readFully(keep);
                }
                String head = new String(keep, StandardCharsets.UTF_8);
                int nl = head.indexOf('\n');
                if (nl >= 0) head = head.substring(nl + 1);
                try (FileOutputStream out = new FileOutputStream(f, false)) {
                    out.write(head.getBytes(StandardCharsets.UTF_8));
                }
            }
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    public static String read(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), "greet_log.txt");
            if (!f.exists()) return "（暂无问候日志）";
            byte[] all = new byte[(int) Math.min(f.length(), 400 * 1024)];
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
            File f = new File(ctx.getFilesDir(), "greet_log.txt");
            if (f.exists()) f.delete();
        } catch (Exception ignored) {
        }
    }
}
