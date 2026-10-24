package com.aichat.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ChatAdapter.VH> {

    private final Context context;
    private final List<Message> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;

    private Bitmap aiAvatar;
    private Bitmap userAvatar;
    private Bitmap defaultUserAvatar;

    private final int bubbleRadiusPx;
    private final int cornerPx;
    private int userBubbleColor;
    private final int userBubbleText;
    private final int assistantBubbleColor;
    private final int assistantBubbleText;
    private static final long TIME_BAR_GAP = 3L * 60L * 1000L;

    public ChatAdapter(Context ctx) {
        this.context = ctx;
        Resources res = ctx.getResources();
        float density = res.getDisplayMetrics().density;
        bubbleRadiusPx = Math.round(18 * density);
        cornerPx = Math.round(5 * density);
        userBubbleColor = new Prefs(ctx).getThemeColor();
        userBubbleText = androidx.core.content.ContextCompat.getColor(ctx, R.color.user_bubble_text);
        assistantBubbleColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.assistant_bubble);
        assistantBubbleText = androidx.core.content.ContextCompat.getColor(ctx, R.color.assistant_bubble_text);
    }

    public interface OnMessageLongClickListener {
        void onLongClick(Message message, int position);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener l) {
        this.longClickListener = l;
    }

    public void setAvatars(Bitmap ai, Bitmap mine, Bitmap defaultMine) {
        this.aiAvatar = ai;
        this.userAvatar = mine;
        this.defaultUserAvatar = defaultMine;
        notifyDataSetChanged();
    }

    public void setThemeColor(int color) {
        if (color != userBubbleColor) {
            userBubbleColor = color;
            notifyDataSetChanged();
        }
    }

    public static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final LinearLayout row;
        final FrameLayout bubbleWrap;
        final TextView bubble;
        final ImageView bubbleImage;
        final TextView timeLabel;
        final TextView readLabel;
        final ImageView aiAvatar;
        final ImageView userAvatar;
        final TextView timeDivider;
        public VH(View v) {
            super(v);
            row = v.findViewById(R.id.messageRow);
            bubbleWrap = v.findViewById(R.id.bubbleWrap);
            bubble = v.findViewById(R.id.bubbleText);
            bubbleImage = v.findViewById(R.id.bubbleImage);
            timeLabel = v.findViewById(R.id.timeLabel);
            readLabel = v.findViewById(R.id.readLabel);
            aiAvatar = v.findViewById(R.id.aiAvatar);
            userAvatar = v.findViewById(R.id.userAvatar);
            timeDivider = v.findViewById(R.id.timeDivider);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return Message.ROLE_USER.equals(messages.get(position).role) ? 1 : 0;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = viewType == 1 ? R.layout.item_message_right : R.layout.item_message_left;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        Message m = messages.get(position);

        if (h.timeDivider != null) {
            if (shouldShowTimeBar(position)) {
                h.timeDivider.setVisibility(View.VISIBLE);
                h.timeDivider.setText(formatTimeBar(m.ts));
            } else {
                h.timeDivider.setVisibility(View.GONE);
            }
        }

        if (m.hasImage()) {
            h.bubbleImage.setVisibility(View.VISIBLE);
            Bitmap bmp = decodeSampledBitmap(m.imagePath, 480);
            if (bmp != null) h.bubbleImage.setImageBitmap(bmp);
            final String imgPath = m.imagePath;
            h.bubbleImage.setOnClickListener(v -> showImageFull(imgPath));
            if (m.content != null && !m.content.isEmpty()) {
                h.bubble.setVisibility(View.VISIBLE);
                h.bubble.setText(m.content);
            } else {
                h.bubble.setVisibility(View.GONE);
            }
        } else {
            h.bubble.setVisibility(View.VISIBLE);
            h.bubbleImage.setVisibility(View.GONE);
            h.bubbleImage.setOnClickListener(null);
            h.bubble.setText(m.content != null ? m.content : "");
        }

        boolean sameAsPrev = position > 0
                && messages.get(position - 1).role.equals(m.role);
        boolean withCorner = !sameAsPrev;

        if (Message.ROLE_USER.equals(m.role)) {
            h.row.setGravity(android.view.Gravity.END | android.view.Gravity.BOTTOM);
            h.bubble.setTextColor(userBubbleText);
            setBubbleBg(h.bubbleWrap, userBubbleColor, true, withCorner);
            if (h.readLabel != null) h.readLabel.setVisibility(View.VISIBLE);
            if (h.userAvatar != null) {
                h.userAvatar.setVisibility(sameAsPrev ? View.INVISIBLE : View.VISIBLE);
                Bitmap mine = userAvatar != null ? userAvatar : defaultUserAvatar;
                h.userAvatar.setImageBitmap(mine);
            }
        } else {
            h.row.setGravity(android.view.Gravity.START | android.view.Gravity.BOTTOM);
            h.bubble.setTextColor(assistantBubbleText);
            setBubbleBg(h.bubbleWrap, assistantBubbleColor, false, withCorner);
            if (h.aiAvatar != null) {
                h.aiAvatar.setVisibility(sameAsPrev ? View.INVISIBLE : View.VISIBLE);
                h.aiAvatar.setImageBitmap(aiAvatar);
            }
        }

        if (h.timeLabel != null) {
            if (m.time != null && !m.time.isEmpty()) {
                h.timeLabel.setVisibility(View.VISIBLE);
                h.timeLabel.setText(m.time);
            } else {
                h.timeLabel.setVisibility(View.GONE);
            }
        }

        final int pos = position;
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(m, pos);
            return true;
        });
    }

    private boolean shouldShowTimeBar(int position) {
        if (position <= 0) return false;
        Message cur = messages.get(position);
        Message prev = messages.get(position - 1);
        if (cur.ts <= 0 || prev.ts <= 0) return false;
        return cur.ts - prev.ts > TIME_BAR_GAP;
    }

    private String formatTimeBar(long ts) {
        Calendar cur = Calendar.getInstance();
        cur.setTimeInMillis(ts);
        Calendar now = Calendar.getInstance();
        java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm", Locale.CHINA);
        String hm = timeFmt.format(new Date(ts));
        boolean sameYear = cur.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        boolean sameDay = sameYear
                && cur.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        if (sameDay) return hm;
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = sameYear
                && cur.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
        if (isYesterday) return "昨天 " + hm;
        java.text.SimpleDateFormat md = new java.text.SimpleDateFormat(
                sameYear ? "M月d日 HH:mm" : "yyyy年M月d日 HH:mm", Locale.CHINA);
        return md.format(new Date(ts));
    }

    private Bitmap decodeSampledBitmap(String path, int reqWidth) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int scale = 1;
            while (o.outWidth / scale / 2 >= reqWidth) scale *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Exception e) {
            return null;
        }
    }

    private void showImageFull(String path) {
        try {
            Bitmap bmp = decodeSampledBitmap(path, 1024);
            if (bmp == null) return;
            ImageView iv = new ImageView(context);
            iv.setImageBitmap(bmp);
            iv.setAdjustViewBounds(true);
            new AlertDialog.Builder(context)
                    .setView(iv)
                    .setPositiveButton("关闭", null)
                    .show();
        } catch (Exception ignored) {
        }
    }

    private void setBubbleBg(FrameLayout v, int color, boolean isMine, boolean withCorner) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        if (withCorner && isMine) {
            gd.setCornerRadii(new float[]{
                    bubbleRadiusPx, bubbleRadiusPx,
                    bubbleRadiusPx, bubbleRadiusPx,
                    cornerPx, cornerPx,
                    bubbleRadiusPx, bubbleRadiusPx});
        } else if (withCorner) {
            gd.setCornerRadii(new float[]{
                    bubbleRadiusPx, bubbleRadiusPx,
                    bubbleRadiusPx, bubbleRadiusPx,
                    bubbleRadiusPx, bubbleRadiusPx,
                    cornerPx, cornerPx});
        } else {
            gd.setCornerRadius(bubbleRadiusPx);
        }
        v.setBackground(gd);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void add(Message m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeAt(int position) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, messages.size());
        }
    }

    public void truncateAfter(int position) {
        if (position < 0 || position >= messages.size()) return;
        while (messages.size() > position + 1) {
            messages.remove(messages.size() - 1);
        }
        notifyItemRangeRemoved(position + 1, messages.size());
        notifyDataSetChanged();
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    public void replaceAll(List<Message> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
        notifyDataSetChanged();
    }

    public List<Message> all() {
        return new ArrayList<>(messages);
    }
}
