package com.aichat.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText input;
    private TextView btnSend;
    private ImageView backgroundImage;
    private View backgroundScrim;
    private TextView headerName;
    private TextView headerStatus;
    private ChatAdapter adapter;
    private Prefs prefs;
    private String systemPrompt;
    private volatile boolean sending = false;
    /** 防抖合并：1 秒内连发重新计时，超时后打包转发。 */
    private final java.util.List<Message> pendingToSend = new java.util.ArrayList<>();
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable flushRunnable = this::flushPending;

    private static final String STATUS_ONLINE = "在线";
    private static final String STATUS_TYPING = "对方正在输入中…";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        systemPrompt = loadBuiltInPrompt();

        backgroundImage = findViewById(R.id.backgroundImage);
        backgroundScrim = findViewById(R.id.backgroundScrim);
        recyclerView = findViewById(R.id.recyclerView);
        input = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        headerName = findViewById(R.id.headerName);
        headerStatus = findViewById(R.id.headerStatus);

        // LINE 顶部栏
        TextView btnBack = findViewById(R.id.btnBack);
        TextView btnMenu = findViewById(R.id.btnMenu);
        ImageView headerAvatar = findViewById(R.id.headerAvatar);
        TextView dateLabel = findViewById(R.id.dateLabel);
        android.widget.ImageButton btnToolSmile = findViewById(R.id.btnToolSmile);

        // 返回：回到系统主屏幕
        btnBack.setOnClickListener(v -> {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        });

        // 菜单：打开设置
        btnMenu.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // 用户名（可在设置里改，默认“鸠”）与状态
        headerName.setText(prefs.getDisplayName());
        headerStatus.setText(STATUS_ONLINE);

        // 日期
        dateLabel.setText(new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date()));

        // 表情按钮：弹出黄脸 emoji 面板
        btnToolSmile.setOnClickListener(v -> showEmojiPicker());

        // 顶栏对方头像（内置 AI 头像，圆形）
        int headerPx = (int) (38 * getResources().getDisplayMetrics().density);
        headerAvatar.setImageBitmap(AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.ai_avatar), headerPx));

        adapter = new ChatAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        applyAvatars();

        // 加载历史聊天记录
        List<Message> history = prefs.loadMessages();
        if (history.isEmpty()) {
            adapter.add(new Message(Message.ROLE_ASSISTANT,
                    "你好！我是 AI 助手。点击右上角「☰」打开设置，填入你的 API Key 即可开始对话。",
                    nowTime()));
            persist();
        } else {
            adapter.replaceAll(history);
        }
        scrollToBottom();

        adapter.setOnMessageLongClickListener((message, position) ->
                showMessageMenu(message, position));

        btnSend.setOnClickListener(v -> send());
        input.setOnEditorActionListener((v, actionId, event) -> {
            send();
            return true;
        });

        applyBackground();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyBackground();
        applyAvatars();
        if (headerName != null) headerName.setText(prefs.getDisplayName());
        if (headerStatus != null && !sending) headerStatus.setText(STATUS_ONLINE);
        if (!sending) {
            List<Message> stored = prefs.loadMessages();
            if (!stored.equals(adapter.all())) {
                adapter.replaceAll(stored);
                scrollToBottom();
            }
        }
    }

    /** 加载并设置头像（AI 内置，我的可自定义）。 */
    private void applyAvatars() {
        int px = (int) (34 * getResources().getDisplayMetrics().density);
        Bitmap ai = AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.ai_avatar), px);
        Bitmap mine = AvatarUtil.toCircular(AvatarUtil.load(prefs.getAvatarPath()), px);
        Bitmap defaultMine = AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar), px);
        adapter.setAvatars(ai, mine, defaultMine);
    }

    /** 读取 assets/prompt.md 作为固定系统提示词（不可修改）。 */
    private String loadBuiltInPrompt() {
        try {
            InputStream is = getAssets().open("prompt.md");
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            r.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "你是一个乐于助人、回答准确的中文AI助手。";
        }
    }

    /** 当前时间 HH:mm:ss。 */
    private String nowTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
    }

    private void applyBackground() {
        String path = prefs.getBackgroundPath();
        Bitmap bmp = BackgroundUtil.load(path);
        if (bmp != null) {
            backgroundImage.setImageBitmap(bmp);
            backgroundImage.setVisibility(View.VISIBLE);
            backgroundScrim.setVisibility(View.VISIBLE);
        } else {
            backgroundImage.setImageDrawable(null);
            backgroundImage.setVisibility(View.GONE);
            backgroundScrim.setVisibility(View.GONE);
        }
    }

    private void scrollToBottom() {
        recyclerView.post(() -> {
            if (adapter.getItemCount() > 0) {
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        if (prefs.getApiKey().trim().isEmpty()) {
            Toast.makeText(this, "请先在设置里填写 API Key", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        input.setText("");

        // 多行输入 => 按行拆成多条独立消息（微信/LINE 发多条一样）
        final List<Message> newMessages = new java.util.ArrayList<>();
        if (text.contains("\n")) {
            for (String part : text.split("\n")) {
                String p = part.trim();
                if (!p.isEmpty()) newMessages.add(new Message(Message.ROLE_USER, p, nowTime()));
            }
        } else {
            newMessages.add(new Message(Message.ROLE_USER, text, nowTime()));
        }
        if (newMessages.isEmpty()) return;

        // 逐条上屏：主线程 Handler 串行，快速连发时顺序不乱，间隔 0.5~1.2s 随机
        Random rand = new Random();
        long[] gaps = new long[newMessages.size() - 1];
        long totalDelay = 0;
        for (int i = 0; i < gaps.length; i++) {
            gaps[i] = 500 + rand.nextInt(701);
            totalDelay += gaps[i];
        }
        long delay = 0;
        for (int i = 0; i < newMessages.size(); i++) {
            final Message m = newMessages.get(i);
            final long d = delay;
            debounceHandler.postDelayed(() -> {
                adapter.add(m);
                persist();
                scrollToBottom();
            }, d);
            if (i < gaps.length) delay += gaps[i];
        }

        // 防抖合并：等最后一条上屏完成后开始计时 1 秒；1 秒内又发则重新计时，
        // 最后一批消息上屏后 1 秒超时，打包一起转发给 AI
        headerStatus.setText(STATUS_TYPING);
        pendingToSend.addAll(newMessages);
        debounceHandler.removeCallbacks(flushRunnable);
        debounceHandler.postDelayed(flushRunnable, totalDelay + 1000);
    }

    /** 主线程：1 秒防抖超时后把待转发的消息打包发给模型。 */
    private void flushPending() {
        if (pendingToSend.isEmpty()) return;
        if (sending) {
            // 上一个请求还在进行：消息保留在待发列表，稍后自动再接上
            debounceHandler.postDelayed(flushRunnable, 1000);
            return;
        }
        pendingToSend.clear();
        callApi();
    }

    /** 用当前全部消息（含刚发的多条）的最近 N 条调用模型。需在主线程调用。 */
    private void callApi() {
        final String baseUrl = prefs.getBaseUrl();
        final String apiKey = prefs.getApiKey();
        final String model = prefs.getModel();
        final double temp = prefs.getTemperature();
        final int maxTok = prefs.getMaxTokens();

        // 取最近 N 条历史发给模型（adapter.all() 是副本，可安全传给子线程）
        int n = prefs.getContextCount();
        List<Message> all = adapter.all();
        List<Message> recent;
        if (n <= 0) {
            recent = all.subList(all.size() - 1, all.size());
        } else {
            int from = Math.max(0, all.size() - n);
            recent = all.subList(from, all.size());
        }

        sending = true;
        headerStatus.setText(STATUS_TYPING);

        new Thread(() -> {
            try {
                final String reply = ApiClient.chat(baseUrl, apiKey, model, systemPrompt, temp, maxTok, recent);
                // 按空行分段，多段则拆成多条独立消息（和发消息一样逐条上屏）
                final List<String> parts = splitParts(reply);
                // 先随机打字延迟（长短随机），期间顶栏一直显示“对方正在输入中…”
                try {
                    Thread.sleep(typingDelay(reply.length()));
                } catch (InterruptedException ignored) {
                }
                Random rand = new Random();
                for (int i = 0; i < parts.size(); i++) {
                    final String part = parts.get(i);
                    runOnUiThread(() -> {
                        adapter.add(new Message(Message.ROLE_ASSISTANT, part, nowTime()));
                        persist();
                        scrollToBottom();
                    });
                    // 两条 AI 消息之间的随机延迟：按本条长度 0.8~2.5s + 0~0.5s 抖动
                    if (i < parts.size() - 1) {
                        long gap = Math.max(800, Math.min(part.length() * 60L, 2500L))
                                + rand.nextInt(501);
                        try {
                            Thread.sleep(gap);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                runOnUiThread(() -> {
                    sending = false;
                    if (pendingToSend.isEmpty()) headerStatus.setText(STATUS_ONLINE);
                    scrollToBottom();
                    // 请求期间用户又发的消息：自动接上再发给模型
                    if (!pendingToSend.isEmpty()) {
                        debounceHandler.postDelayed(flushRunnable, 300);
                    }
                });
            } catch (final Exception e) {
                // 出错不延迟，立即上屏
                runOnUiThread(() -> {
                    adapter.add(new Message(Message.ROLE_ASSISTANT,
                            "出错了：" + e.getMessage(), nowTime()));
                    persist();
                    sending = false;
                    if (pendingToSend.isEmpty()) headerStatus.setText(STATUS_ONLINE);
                    scrollToBottom();
                    if (!pendingToSend.isEmpty()) {
                        debounceHandler.postDelayed(flushRunnable, 300);
                    }
                });
            }
        }).start();
    }

    /** 随机打字延迟（长短随机）：约 70% 短延迟 0.8~3s，少数按字数 1~8s。 */
    private long typingDelay(int chars) {
        Random rand = new Random();
        if (rand.nextInt(100) < 70) {
            return 800 + rand.nextInt(2201);          // 0.8 ~ 3.0 秒（短）
        }
        long perChar = 20 + rand.nextInt(41);         // 每字 20~60ms
        return Math.min((long) chars * perChar + 1000, 8000L);  // 1 ~ 8 秒（长）
    }

    /** 按空行（段落）拆分；多段才拆，单段保持原样（保留内部换行）。 */
    private List<String> splitParts(String text) {
        List<String> parts = new java.util.ArrayList<>();
        if (text == null) return parts;
        String[] blocks = text.split("\\n\\s*\\n");
        for (String b : blocks) {
            String t = b.trim();
            if (!t.isEmpty()) parts.add(t);
        }
        if (parts.isEmpty()) parts.add(text);
        return parts;
    }

    // ===== 长按消息菜单 =====

    private void showMessageMenu(Message message, int position) {
        boolean isUser = Message.ROLE_USER.equals(message.role);
        String[] items;
        if (isUser) {
            items = new String[]{"复制内容", "删除此条", "撤回此条及回复", "回溯到此处（删除之后全部）"};
        } else {
            items = new String[]{"复制内容", "删除此条", "回溯到此处（删除之后全部）"};
        }

        new AlertDialog.Builder(this)
                .setTitle("消息操作")
                .setItems(items, (dialog, which) -> {
                    String label = items[which];
                    if ("复制内容".equals(label)) {
                        copyText(message.content);
                    } else if ("删除此条".equals(label)) {
                        adapter.removeAt(position);
                        persist();
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    } else if ("撤回此条及回复".equals(label)) {
                        adapter.removeAt(position);
                        if (position < adapter.getItemCount()) {
                            Message next = adapter.all().get(position);
                            if (Message.ROLE_ASSISTANT.equals(next.role)) {
                                adapter.removeAt(position);
                            }
                        }
                        persist();
                        Toast.makeText(this, "已撤回", Toast.LENGTH_SHORT).show();
                    } else if (label.startsWith("回溯到此处")) {
                        adapter.truncateAfter(position);
                        persist();
                        Toast.makeText(this, "已回溯到此处", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyText(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("ai_chat", text));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== 表情面板 =====

    private static final String[] EMOJIS = {
            "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊",
            "😋", "😎", "😍", "😘", "😗", "🙂", "🤗", "🤔", "😐", "😑",
            "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫",
            "😴", "😌", "😛", "😜", "😝", "😒", "😓", "😔", "😕", "🙃",
            "😲", "😭", "😖", "😞", "😟", "😤", "😢", "😬", "😰", "😩",
            "😨", "😱", "😳", "😵", "😡", "😠", "😇", "🥳", "🤩", "🥺",
            "👍", "👎", "👏", "🙏", "💪", "🔥", "✨", "❤️", "💔", "🎉"
    };

    /** 弹出黄脸 emoji 面板，点击插入输入框。 */
    private void showEmojiPicker() {
        final android.widget.GridView grid = new android.widget.GridView(this);
        grid.setNumColumns(6);
        grid.setPadding(16, 8, 16, 8);
        grid.setAdapter(new android.widget.ArrayAdapter<>(this,
                R.layout.item_emoji, EMOJIS));
        grid.setOnItemClickListener((parent, view, pos, id) -> {
            insertEmoji(EMOJIS[pos]);
            Object tag = parent.getTag();
            if (tag instanceof AlertDialog) {
                AlertDialog d = (AlertDialog) tag;
                if (d.isShowing()) d.dismiss();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("表情")
                .setView(grid)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(d -> grid.setTag(dialog));
        dialog.show();
    }

    /** 把 emoji 插入到输入框光标位置。 */
    private void insertEmoji(String emoji) {
        int start = Math.max(input.getSelectionStart(), 0);
        int end = Math.max(input.getSelectionEnd(), 0);
        input.getText().replace(Math.min(start, end), Math.max(start, end), emoji);
        input.setSelection(Math.min(start, end) + emoji.length());
    }

    private void persist() {
        prefs.saveMessages(adapter.all());
    }
}
