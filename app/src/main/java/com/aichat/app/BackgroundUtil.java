package com.aichat.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class BackgroundUtil {

    private static final String BG_FILE = "background.jpg";
    private static final int MAX_DIMEN = 1920;

    
    public static String saveFromUri(Context ctx, Uri uri) {
        InputStream is = null;
        try {
            is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            byte[] header = readHeader(is);
            BitmapFactory.decodeByteArray(header, 0, header.length, opts);
            is.close();

            int sample = 1;
            int w = opts.outWidth, h = opts.outHeight;
            while (w / sample > MAX_DIMEN || h / sample > MAX_DIMEN) sample *= 2;

            is = ctx.getContentResolver().openInputStream(uri);
            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, decodeOpts);
            if (bmp == null) return null;

            File out = new File(ctx.getFilesDir(), BG_FILE);
            FileOutputStream fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
    }

    public static Bitmap load(String path) {
        if (path == null) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        return BitmapFactory.decodeFile(path);
    }

    public static void delete(Context ctx) {
        File f = new File(ctx.getFilesDir(), BG_FILE);
        if (f.exists()) f.delete();
    }

    private static byte[] readHeader(InputStream is) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int total = 0;
        while (total < buf.length) {
            int n = is.read(buf, total, buf.length - total);
            if (n <= 0) break;
            total += n;
        }
        byte[] out = new byte[total];
        System.arraycopy(buf, 0, out, 0, total);
        return out;
    }
}
