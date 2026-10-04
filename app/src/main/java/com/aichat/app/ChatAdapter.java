package com.aichat.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<Message> messages = new ArrayList<>();
    private OnMessageLongClickListener longClickListener;

    private Bitmap aiAvatar;
    private Bitmap userAvatar;
    private Bitmap defaultUserAvatar;

    private final int bubbleRadiusPx;   // 18dp
    private final int cornerPx;         // 5dp
    private final int userBubbleColor;
    private final int userBubbleText;
    private final int assistantBubbleColor;
    private final int assistantBubbleText;

    public ChatAdapter(Context ctx) {
        Resources res = ctx.getResources();
        float density = res.getDisplayMetrics().density;
        bubbleRadiusPx = Math.round(18 * density);
        cornerPx = Math.round(5 * density);
        userBubbleColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.user_bubble);
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

    public static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final LinearLayout row;
        final FrameLayout bubbleWrap;
        final TextView bubble;
        final TextView timeLabel;
        final TextView readLabel;
        final ImageView aiAvatar;
        final ImageView userAvatar;
        public VH(View v) {
            super(v);
            row = v.findViewById(R.id.messageRow);
            bubbleWrap = v.findViewById(R.id.bubbleWrap);
            bubble = v.findViewById(R.id.bubbleText);
            timeLabel = v.findViewById(R.id.timeLabel);
            readLabel = v.findViewById(R.id.readLabel);
            aiAvatar = v.findViewById(R.id.aiAvatar);       // 右布局可能为 null
            userAvatar = v.findViewById(R.id.userAvatar);   // 左布局可能为 null
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
        h.bubble.setText(m.content);

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
