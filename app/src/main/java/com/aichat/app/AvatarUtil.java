package com.aichat.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AvatarUtil {

    private static final String AVATAR_FILE = "my_avatar.jpg";
    private static final int MAX_DIMEN = 512;

    
    public static String saveFromUri(Context ctx, Uri uri) {
        try {
            Bitmap bmp = decodeSampled(uri, ctx, MAX_DIMEN);
            if (bmp == null) return null;
            File out = new File(ctx.getFilesDir(), AVATAR_FILE);
            FileOutputStream fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush();
            fos.close();
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap load(String path) {
        if (path == null) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        return BitmapFactory.decodeFile(path);
    }

    public static void delete(Context ctx) {
        File f = new File(ctx.getFilesDir(), AVATAR_FILE);
        if (f.exists()) f.delete();
    }

    
    public static Bitmap toCircular(Bitmap src, int sizePx) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2, y = (h - side) / 2;
        Bitmap square = Bitmap.createBitmap(src, x, y, side, side);
        Bitmap scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true);
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(scaled, 0, 0, p);
        if (scaled != square && scaled != src) scaled.recycle();
        if (square != src) square.recycle();
        return out;
    }

    private static Bitmap decodeSampled(Uri uri, Context ctx, int maxDimen) throws Exception {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);
        is.close();

        int sample = 1;
        while (opts.outWidth / sample > maxDimen || opts.outHeight / sample > maxDimen) sample *= 2;

        is = ctx.getContentResolver().openInputStream(uri);
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeStream(is, null, decodeOpts);
        is.close();
        return bmp;
    }
}
