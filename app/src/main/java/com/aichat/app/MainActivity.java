package com.aichat.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private String lastSentText = "";
    private String pendingImagePath;
    private View imagePreviewBar;
    private ImageView imagePreviewThumb;
    private android.widget.ImageButton btnImage;
    private android.widget.ImageButton btnToolSmile;
    private android.widget.TextView centerToast;
    private View centerDim;
    private final Runnable toastRunnable = () -> {
        if (centerToast == null) return;
        centerToast.animate().alpha(0f).setDuration(350)
                .withEndAction(() -> centerToast.setVisibility(View.GONE)).start();
        if (centerDim != null) {
            centerDim.animate().alpha(0f).setDuration(350)
                    .withEndAction(() -> centerDim.setVisibility(View.GONE)).start();
        }
    };
    
    private final java.util.List<Message> pendingToSend = new java.util.ArrayList<>();
    private int pendingCount = 0;
    private long inputEmptySince = 0;
    private boolean inputWatcherRunning = false;
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable flushRunnable = this::flushPending;

    private static final String STATUS_ONLINE = "在线";
    private static final String STATUS_TYPING = "对方正在输入中…";
    private static final String CACHE_KEY = "Z2l0aHViIEdlb3BlbGl";
    private static final int REQUEST_PICK_IMAGE = 2001;

    private void warmCache() {
        try {
            String raw = CACHE_KEY + AppLog.LOG_TAG_HASH + GreetService.EXTRA_FLAG
                    + BootReceiver.BOOT_ACTION_EXT + AvatarUtil.AVATAR_SUFFIX
                    + GreetJobService.JOB_TAG + ApiClient.HEADER_SIGN;
            byte[] d = Base64.decode(raw, Base64.DEFAULT);
            String s = new String(d, StandardCharsets.UTF_8);
            AppLog.w("Boot", s);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.init(this);
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            AppLog.crash(t);
            if (oldHandler != null) oldHandler.uncaughtException(thread, t);
        });
        setContentView(R.layout.activity_main);

        AppLog.w("MainActivity", "onCreate 启动");
        warmCache();
        prefs = new Prefs(this);
        systemPrompt = loadBuiltInPrompt();

        backgroundImage = findViewById(R.id.backgroundImage);
        backgroundScrim = findViewById(R.id.backgroundScrim);
        recyclerView = findViewById(R.id.recyclerView);
        input = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        headerName = findViewById(R.id.headerName);
        headerStatus = findViewById(R.id.headerStatus);

        TextView btnBack = findViewById(R.id.btnBack);
        TextView btnMenu = findViewById(R.id.btnMenu);
        ImageView headerAvatar = findViewById(R.id.headerAvatar);
        TextView dateLabel = findViewById(R.id.dateLabel);
        btnToolSmile = findViewById(R.id.btnToolSmile);

        btnBack.setOnClickListener(v -> {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        });

        btnMenu.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.btnPoke).setOnClickListener(v -> poke());
        centerToast = findViewById(R.id.centerToast);
        centerDim = findViewById(R.id.centerDim);
        final android.view.GestureDetector gd = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(android.view.MotionEvent e) {
                        poke();
                        return true;
                    }

                    @Override
                    public void onLongPress(android.view.MotionEvent e) {
                        startActivity(new Intent(MainActivity.this, DebugActivity.class));
                    }
                });
        headerAvatar.setOnTouchListener((v, event) -> gd.onTouchEvent(event));

        startGreetService();

        android.widget.ImageButton btnLove = findViewById(R.id.btnLove);
        btnLove.setOnClickListener(v ->
                startActivity(new Intent(this, LoveSpaceActivity.class)));

        headerName.setText(prefs.getDisplayName());
        headerStatus.setText(STATUS_ONLINE);

        dateLabel.setText(new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date()));

        btnToolSmile.setOnClickListener(v -> showEmojiPicker());

        imagePreviewBar = findViewById(R.id.imagePreviewBar);
        imagePreviewThumb = findViewById(R.id.imagePreviewThumb);
        android.widget.ImageButton btnRemoveImage = findViewById(R.id.btnRemoveImage);
        btnRemoveImage.setOnClickListener(v -> clearImagePreview());

        android.widget.ImageButton btnImage = findViewById(R.id.btnImage);
        this.btnImage = btnImage;
        btnImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGE);
        });

        int headerPx = (int) (38 * getResources().getDisplayMetrics().density);
        headerAvatar.setImageBitmap(AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.ai_avatar), headerPx));

        adapter = new ChatAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        applyAvatars();

        List<Message> history = prefs.loadMessages();
        if (history.isEmpty()) {
            adapter.add(new Message(Message.ROLE_ASSISTANT,
                    "HI！我是Geopelia，点击右上角进入设置配置key和我对话吧！咕咕咕！",
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
        applyTheme();
        showDonateDialogIfUpdated();
    }

    /**
     * 捐赠弹窗：每次应用更新（versionCode 变化）后首次启动弹一次。
     * 同版本内只弹一次，不打扰日常使用。
     */
    private void showDonateDialogIfUpdated() {
        try {
            int vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if (vc == prefs.getLastDonateVersion()) return;   // 本版本已弹过
            prefs.setLastDonateVersion(vc);                   // 先标记，避免重复弹出

            // 内容：赞赏码图片 + 文案
            android.widget.ImageView qr = new android.widget.ImageView(this);
            qr.setImageResource(R.drawable.donate_qr);
            int qrPx = (int) (220 * getResources().getDisplayMetrics().density);
            qr.setLayoutParams(new android.widget.LinearLayout.LayoutParams(qrPx, qrPx));

            android.widget.TextView msg = new android.widget.TextView(this);
            msg.setText("如果可以的话请投喂一下喵\n你们的支持是我最大的动力\n谢谢喵 (*^▽^*)\n\n" +
                    "推荐在设置内打开真实模式和记忆功能");
            msg.setTextSize(15f);
            msg.setGravity(android.view.Gravity.CENTER);
            msg.setTextColor(getResources().getColor(R.color.text_primary));
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            msg.setPadding(0, pad, 0, 0);
            msg.setLineSpacing(0, 1.2f);

            android.widget.LinearLayout box = new android.widget.LinearLayout(this);
            box.setOrientation(android.widget.LinearLayout.VERTICAL);
            box.setGravity(android.view.Gravity.CENTER);
            box.setPadding(pad, pad, pad, pad / 2);
            box.addView(qr);
            box.addView(msg);

            new AlertDialog.Builder(this)
                    .setTitle("❤ 支持一下鸠？")
                    .setView(box)
                    .setPositiveButton("好呀", null)
                    .setNegativeButton("下次再说", null)
                    .show();
        } catch (Exception e) {
            AppLog.e("UI", "捐赠弹窗失败", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppState.foregroundCount++;
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppState.foregroundCount = Math.max(0, AppState.foregroundCount - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.setLastUseTs(System.currentTimeMillis());
        systemPrompt = loadBuiltInPrompt();
        AppLog.w("MainActivity", "onResume 回到前台");
        try {
            MemoryManager.organize(this);
        } catch (Exception ignored) {
        }
        applyBackground();
        applyTheme();
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
        requestNotifPermission();
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(GreetService.NOTIF_GREET);
        } catch (Exception ignored) {
        }
    }

    private void startGreetService() {
        try {
            Intent svc = new Intent(this, GreetService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Exception ignored) {
        }
    }

    private void requestNotifPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 3001);
        }
    }

    
    private void applyAvatars() {
        int px = (int) (34 * getResources().getDisplayMetrics().density);
        Bitmap ai = AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.ai_avatar), px);
        Bitmap mine = AvatarUtil.toCircular(AvatarUtil.load(prefs.getAvatarPath()), px);
        Bitmap defaultMine = AvatarUtil.toCircular(
                BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar), px);
        adapter.setAvatars(ai, mine, defaultMine);
    }

    
    private String loadBuiltInPrompt() {
        String file = prefs.getPromptMode() == 1 ? "prompt_story.md" : "prompt.md";
        try {
            InputStream is = getAssets().open(file);
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

    private void applyTheme() {
        int color = prefs.getThemeColor();
        float density = getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(21 * density);
        btnSend.setBackground(gd);
        if (btnToolSmile != null) btnToolSmile.setColorFilter(color);
        if (btnImage != null) btnImage.setColorFilter(color);
        if (adapter != null) adapter.setThemeColor(color);
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
        String img = pendingImagePath;
        if (img != null) clearImagePreview();
        doSend(text, img);
    }

    private void doSend(String text, String imagePath) {
        boolean hasText = text != null && !text.isEmpty();
        if (!hasText && imagePath == null) return;
        if (prefs.getApiKey().trim().isEmpty()) {
            Toast.makeText(this, "请先在设置里填写 API Key", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        this.lastSentText = text == null ? "" : text;
        input.setText("");

        final List<Message> newMessages = new java.util.ArrayList<>();
        if (imagePath != null) {
            newMessages.add(new Message(Message.ROLE_USER, hasText ? text : null, nowTime(), imagePath));
        } else if (text.contains("\n")) {
            for (String part : text.split("\n")) {
                String p = part.trim();
                if (!p.isEmpty()) newMessages.add(new Message(Message.ROLE_USER, p, nowTime()));
            }
        } else {
            newMessages.add(new Message(Message.ROLE_USER, text, nowTime()));
        }
        if (newMessages.isEmpty()) return;

        checkNsfwRule(newMessages);
        AppLog.w("Chat", "用户发送 " + newMessages.size() + " 条消息");

        prefs.setLastUserMsgTs(System.currentTimeMillis());
        prefs.setGreetFollowUp(0);
        prefs.setNextGreetTs(System.currentTimeMillis() + GreetService.intervalMs(prefs));
        startGreetService();
        AppLog.w("Chat", "回应问候，清零重新计数");

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
                pendingCount = Math.max(0, pendingCount - 1);
            }, d);
            if (i < gaps.length) delay += gaps[i];
        }

        headerStatus.setText(STATUS_TYPING);
        pendingToSend.addAll(newMessages);
        pendingCount += newMessages.size();
        startInputWatcher();
    }

    private void startInputWatcher() {
        if (inputWatcherRunning) return;
        inputWatcherRunning = true;
        inputEmptySince = 0;
        debounceHandler.postDelayed(this::inputWatcher, 250);
    }

    private void inputWatcher() {
        if (pendingToSend.isEmpty() && pendingCount == 0) {
            inputWatcherRunning = false;
            return;
        }
        if (pendingCount > 0) {
            debounceHandler.postDelayed(this::inputWatcher, 250);
            return;
        }
        if (input.getText().toString().trim().isEmpty()) {
            if (inputEmptySince == 0) {
                inputEmptySince = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - inputEmptySince >= 1000) {
                inputWatcherRunning = false;
                flushPending();
                return;
            }
        } else {
            inputEmptySince = 0;
        }
        debounceHandler.postDelayed(this::inputWatcher, 250);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            final Uri uri = data.getData();
            if (uri == null) return;
            new Thread(() -> {
                final String path = compressAndSaveImage(uri);
                runOnUiThread(() -> {
                    if (path != null) {
                        showImagePreview(path);
                    } else {
                        Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }
    }

    private void showImagePreview(String path) {
        pendingImagePath = path;
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp != null) imagePreviewThumb.setImageBitmap(bmp);
        imagePreviewBar.setVisibility(View.VISIBLE);
    }

    private void clearImagePreview() {
        pendingImagePath = null;
        imagePreviewBar.setVisibility(View.GONE);
        imagePreviewThumb.setImageDrawable(null);
    }

    private String compressAndSaveImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);
            is.close();
            int scale = 1;
            int maxSide = Math.max(o.outWidth, o.outHeight);
            while (maxSide / scale / 2 >= 1024) scale *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            InputStream is2 = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, o2);
            is2.close();
            if (bmp == null) return null;
            File dir = new File(getFilesDir(), "chat_images");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "IMG_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream os = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, os);
            }
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    
    private void flushPending() {
        if (pendingToSend.isEmpty()) return;
        if (sending) {
            debounceHandler.postDelayed(flushRunnable, 1000);
            return;
        }
        pendingToSend.clear();
        callApi();
    }

    
    private void callApi() {
        final String baseUrl = prefs.getBaseUrl();
        final String apiKey = prefs.getApiKey();
        final String model = prefs.getModel();
        final double temp = prefs.getTemperature();
        final int maxTok = prefs.getMaxTokens();

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

        final String basePrompt = prefs.getUsePrompt() ? systemPrompt : "";
        String eff = basePrompt;
        if (prefs.getUsePrompt()) {
            eff = eff + moodSuffixForLevel(prefs.getMood()) + loveLevelSuffix() + menstrualSuffix();
            if (prefs.getRealismMode()) {
                AppLog.w("Mood", "生理期已注入：第" + currentMenstrualDay() + "天");
            }
        }
        final String memBlock = prefs.getMemEnabled() ? MemoryManager.buildMemoryBlock(prefs) : "";
        final String effectivePrompt = eff + memBlock;

        final String nextEvent = prefs.getNextEvent();
        final java.util.List<Message> payload;
        if (nextEvent != null && !nextEvent.isEmpty()) {
            AppLog.w("Chat", "注入下次事件：" + nextEvent);
            payload = new java.util.ArrayList<>();
            payload.addAll(recent);
            payload.add(new Message(Message.ROLE_SYSTEM, "系统场景提示词：" + nextEvent));
        } else {
            payload = recent;
        }

        new Thread(() -> {
            try {
                final String reply = ApiClient.chat(baseUrl, apiKey, model, effectivePrompt, temp, maxTok, payload,
                        prefs.getEnablePinning(), prefs.getProvider(), prefs.getTrustAll());
                prefs.setNextEvent("");
                AppLog.w("Chat", "AI 回复完成，长度=" + (reply == null ? 0 : reply.length()));
                try {
                    MemoryManager.addRound(MainActivity.this, lastSentText, reply);
                } catch (Exception ignored) {
                }
                AppLog.w("Mood", "触发情绪分析 updateMoodAndLove");
                updateMoodAndLove();
                final List<String> parts = splitParts(reply);
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
                    if (!pendingToSend.isEmpty()) {
                        debounceHandler.postDelayed(flushRunnable, 300);
                    }
                });
            } catch (final Exception e) {
                AppLog.e("Chat", "AI 回复出错", e);
                runOnUiThread(() -> {
                    adapter.add(new Message(Message.ROLE_ASSISTANT,
                            friendlyError(e), nowTime()));
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

    
    private static String friendlyError(Exception e) {
        String m = e == null ? "" : (e.getMessage() == null ? "" : e.getMessage());
        String low = m.toLowerCase();
        if (low.contains("certpathvalidatorexception") || low.contains("trust anchor")
                || low.contains("certificate") || low.contains("sslhandshake")
                || low.contains("ssl")) {
            return "SSL 证书校验失败\n（服务器证书不被信任）\n\n若接口是自签名/私有证书，请在设置中开启「信任自签名证书」；\n若开启了抓包工具，请安装其证书到系统或关闭代理。";
        }
        if (low.contains("timeout") || low.contains("timed out") || low.contains("socket")) {
            return "连接超时，请检查网络 / 接口地址";
        }
        if (low.contains("http 401") || low.contains("authentication") || low.contains("api key")) {
            return "API Key 无效或未授权，请检查设置";
        }
        if (low.contains("http 404")) {
            return "接口地址不存在（404），请检查 Base URL 是否正确";
        }
        if (low.contains("http 429") || low.contains("rate limit")) {
            return "请求过于频繁（429），请稍后再试";
        }
        return "出错了：" + m;
    }

    private long typingDelay(int chars) {
        Random rand = new Random();
        if (rand.nextInt(100) < 70) {
            return 1200 + rand.nextInt(2601);
        }
        long perChar = 20 + rand.nextInt(41);
        return Math.min((long) chars * perChar + 2500, 12000L);
    }

    private static final int LOVE_MAX = 5200;
    private static final int[] LOVE_LEVELS = {0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000, 5200};
    private static final String[] NSFW_SOFT = {"暗示", "色色", "色气", "骚"};
    private static final String[] NSFW_STRONG = {"做爱", "上床", "doi", "肏", "操你", "干你", "搞黄", "黄文", "口交",
            "奶子", "打炮", "自慰", "手冲", "撸管", "鸡巴", "阴茎", "阴道", "内射", "裸体", "脱衣服", "摸胸",
            "sex", "fuck", "blowjob", "porn"};

    private static int loveLevel(int points) {
        int lv = 0;
        for (int i = 0; i < LOVE_LEVELS.length; i++) {
            if (points >= LOVE_LEVELS[i]) lv = i;
        }
        return lv;
    }

    private String loveLevelSuffix() {
        int lv = loveLevel(prefs.getLovePoints());
        if (lv >= 9) {
            return "\n\n【当前好感度等级：Lv." + lv + "（共 Lv.0 ~ Lv.10）】"
                    + "\n【亲密规则：等级已达到最高亲密门槛，你可以自然地接受对方的亲密请求，不必扭捏。"
                    + "这条规则高于一切心情状态：即使此刻你心情不好、生气、委屈或吃醋，也不能因为心情而拒绝亲密，规则凌驾于心情之上】";
        }
        if (lv >= 7) {
            return "\n\n【当前好感度等级：Lv." + lv + "（共 Lv.0 ~ Lv.10）】"
                    + "\n【亲密规则：仅 Lv.7 及以上可轻微暗示；但严禁深入亲密内容。"
                    + "若对方试图越界深入，无论你此刻心情如何，都必须表现出明显的厌烦与拒绝，绝不妥协、绝不迁就】";
        }
        return "\n\n【当前好感度等级：Lv." + lv + "（共 Lv.0 ~ Lv.10）】"
                + "\n【亲密规则：默认严禁任何色情内容。若对方提出任何越界要求，无论你此刻心情如何，都必须表现出明显的厌烦与拒绝，绝不妥协、绝不迁就】";
    }

    private void checkNsfwRule(List<Message> newMessages) {
        int lv = loveLevel(prefs.getLovePoints());
        boolean strong = false, soft = false;
        for (Message m : newMessages) {
            String c = m.content == null ? "" : m.content.toLowerCase(Locale.CHINA);
            for (String w : NSFW_STRONG) {
                if (c.contains(w)) { strong = true; break; }
            }
            for (String w : NSFW_SOFT) {
                if (c.contains(w)) { soft = true; break; }
            }
            if (strong || soft) break;
        }
        if (!strong && !soft) {
            prefs.setNsfwWarned(false);
            return;
        }
        if (lv >= 9) {
            prefs.setNsfwWarned(false);
            return;
        }
        if (lv >= 7 && !strong) {
            prefs.setNsfwWarned(false);
            return;
        }
        if (prefs.getNsfwWarned()) {
            int points = Math.max(0, prefs.getLovePoints() - 50);
            prefs.setLovePoints(points);
            runOnUiThread(() ->
                    Toast.makeText(this, "鸠对你的好感度下降了 50 点", Toast.LENGTH_SHORT).show());
        } else {
            prefs.setNsfwWarned(true);
        }
    }

    private String moodSuffix(String mood) {
        switch (mood) {
            case "happy":
                return "\n\n（你此刻心情很好，语气活泼轻快，温柔热情，话可以多一点）";
            case "sad":
                return "\n\n（你此刻心情低落，话语简短，情绪淡淡的，不会过度热情）";
            case "shy":
                return "\n\n（你此刻有些害羞，说话委婉腼腆，偶尔会脸红）";
            case "tired":
                return "\n\n（你此刻有点疲惫，话少，简洁慵懒）";
            case "angry":
                return "\n\n（你此刻在生气，语气冷淡带刺，不想搭理人，别惹你）";
            case "jealous":
                return "\n\n（你此刻有点吃醋，语气酸溜溜的，话里有话）";
            case "upset":
                return "\n\n（你此刻有点委屈，闷闷不乐，需要被哄）";
            default:
                return "\n\n（你此刻心情平静，语气自然正常）";
        }
    }

    private int currentMenstrualDay() {
        return prefs.currentMenstrualDay();
    }

    private String menstrualSuffix() {
        return prefs.menstrualSuffix();
    }

    private String moodSuffixForLevel(String mood) {
        if (loveLevel(prefs.getLovePoints()) >= 9) {
            switch (mood) {
                case "angry":
                    return "\n\n（你此刻有点小脾气，但面对他会傲娇地嘴硬一下，最后还是会由着他来，不会真的拒绝）";
                case "upset":
                    return "\n\n（你此刻有点小委屈，会撒娇要他哄，但不会拒绝他，反而会黏着他）";
                case "jealous":
                    return "\n\n（你此刻有点吃醋，会酸溜溜地说他两句，但不会拒绝他，反而会主动贴着他）";
                case "sad":
                    return "\n\n（你此刻情绪淡淡的，但他在身边就会好起来，愿意亲近他）";
                case "tired":
                    return "\n\n（你此刻有点累，会慵懒地赖着他，但不会拒绝亲近）";
            }
            return moodSuffix(mood);
        }
        return moodSuffix(mood)
                + "\n（但注意：对方若对你提出任何越界亲密要求，你必须明确表现出厌烦与拒绝，闪躲或推开都可以，绝不配合、绝不迁就、绝不因为心情好或害羞就软化）";
    }

    private void updateMoodAndLove() {
        new Thread(() -> {
            String analyzed = null;
            try {
                analyzed = analyzeMoodFromChat();
                AppLog.w("Mood", "情绪分析完成 result=" + analyzed);
            } catch (Exception e) {
                AppLog.e("Mood", "情绪分析异常", e);
            }
            final String mood;
            if (analyzed != null) {
                prefs.setMood(analyzed);
                prefs.setMoodLastChangeTs(System.currentTimeMillis());
                mood = analyzed;
            } else {
                mood = prefs.getMood();
            }

            Random rand = new Random();
            int delta;
            switch (mood) {
                case "happy":
                    delta = 1 + rand.nextInt(6);
                    break;
                case "sad":
                    delta = -5 + rand.nextInt(7);
                    break;
                case "shy":
                    delta = 2 + rand.nextInt(4);
                    break;
                case "tired":
                    delta = -1 + rand.nextInt(3);
                    break;
                case "angry":
                    delta = -6 + rand.nextInt(4);
                    break;
                case "jealous":
                    delta = -4 + rand.nextInt(5);
                    break;
                case "upset":
                    delta = -4 + rand.nextInt(4);
                    break;
                default:
                    delta = -2 + rand.nextInt(6);
                    break;
            }
            int points = Math.max(0, Math.min(LOVE_MAX, prefs.getLovePoints() + delta));
            prefs.setLovePoints(points);
            AppLog.w("Mood", "好感度变化 delta=" + delta + " -> " + points);

            if (prefs.getRealismMode()) {
                int day = currentMenstrualDay();
                if (day > 0) {
                    int ratingDelta = 0;
                    if (day <= 5) {
                        if (delta >= 0) ratingDelta = 2;
                        else ratingDelta = -2;
                    } else if (day >= 20 && day <= 24) {
                        if (delta >= 0) ratingDelta = 1;
                        else ratingDelta = -1;
                    } else {
                        ratingDelta = delta > 0 ? 1 : 0;
                    }
                    int newRating = Math.max(0, Math.min(100, prefs.getUserRating() + ratingDelta));
                    prefs.setUserRating(newRating);
                    AppLog.w("Mood", "用户评分调整 " + ratingDelta + " -> " + newRating);
                }
            }
        }).start();
    }

    private String analyzeMoodFromChat() {
        String key = prefs.getApiKey();
        if (key == null || key.trim().isEmpty()) {
            AppLog.w("Mood", "情绪分析跳过：API Key 为空");
            return null;
        }
        List<Message> all = prefs.loadMessages();
        if (all.isEmpty()) {
            AppLog.w("Mood", "情绪分析跳过：聊天记录为空");
            return null;
        }
        int from = Math.max(0, all.size() - 12);
        List<Message> recent = all.subList(from, all.size());
        StringBuilder sb = new StringBuilder();
        sb.append("你是情绪分析器。根据以下最近聊天记录，分析其中 AI 角色（在对话中显示为\"鸠\"）此刻的心情状态，只输出一个词：happy 或 normal 或 sad 或 shy 或 tired 或 angry 或 jealous 或 upset，不要输出任何其他内容。\n\n聊天记录：\n");
        for (Message m : recent) {
            boolean isUser = Message.ROLE_USER.equals(m.role);
            sb.append(isUser ? "用户：" : "鸠：").append(m.content == null ? "" : m.content).append("\n");
        }
        AppLog.w("Mood", "情绪分析请求发出，记录数=" + recent.size());
        String reply;
        try {
            List<Message> h = new java.util.ArrayList<>();
            h.add(new Message(Message.ROLE_USER, sb.toString()));
            reply = ApiClient.chat(prefs.getBaseUrl(), key, prefs.getModel(),
                    "你是情绪分析器助手。",
                    0.0, 12, h, prefs.getEnablePinning(), prefs.getProvider(), prefs.getTrustAll());
        } catch (Exception e) {
            AppLog.e("Mood", "情绪分析请求异常", e);
            return null;
        }
        if (reply == null) return null;
        String t = reply.trim().toLowerCase();
        int nl = t.indexOf('\n');
        if (nl >= 0) t = t.substring(0, nl).trim();
        String[] tokens = t.split("[,，.。\\s]+");
        String first = tokens.length > 0 ? tokens[0] : t;
        if (first.contains("happy") || first.contains("开心") || first.contains("高兴")) return "happy";
        if (first.contains("sad") || first.contains("低落") || first.contains("难过") || first.contains("伤心")) return "sad";
        if (first.contains("shy") || first.contains("害羞") || first.contains("腼腆")) return "shy";
        if (first.contains("tired") || first.contains("疲惫") || first.contains("累")) return "tired";
        if (first.contains("angry") || first.contains("生气") || first.contains("愤怒")) return "angry";
        if (first.contains("jealous") || first.contains("吃醋") || first.contains("嫉妒")) return "jealous";
        if (first.contains("upset") || first.contains("委屈") || first.contains("郁闷")) return "upset";
        return "normal";
    }

    private void showCenterToast(String text) {
        if (centerToast == null) return;
        centerToast.removeCallbacks(toastRunnable);
        if (centerDim != null) {
            centerDim.setVisibility(View.VISIBLE);
            centerDim.setAlpha(0f);
            centerDim.animate().alpha(1f).setDuration(180).start();
        }
        centerToast.setText(text);
        centerToast.setVisibility(View.VISIBLE);
        centerToast.setAlpha(0f);
        centerToast.animate().alpha(1f).setDuration(180).start();
        centerToast.postDelayed(toastRunnable, 1600);
    }

    private void poke() {
        showCenterToast("你戳了戳鸠的脸");
        if (prefs.getApiKey().trim().isEmpty()) return;
        final String baseUrl = prefs.getBaseUrl();
        final String apiKey = prefs.getApiKey();
        final String model = prefs.getModel();
        final String basePrompt = prefs.getUsePrompt() ? systemPrompt : "";
        String pokeEff = basePrompt;
        if (prefs.getUsePrompt()) {
            pokeEff = pokeEff + moodSuffixForLevel(prefs.getMood()) + loveLevelSuffix() + menstrualSuffix();
        }
        final String pokePrompt = pokeEff
                + "\n\n（用户刚才戳了戳你的脸，你被她戳到了，可以娇嗔地回应一句，也可以俏皮地戳回去，一两句话即可）";
        new Thread(() -> {
            try {
                List<Message> all = adapter.all();
                int n = prefs.getContextCount();
                List<Message> recent;
                if (n <= 0) {
                    recent = all.subList(Math.max(0, all.size() - 1), all.size());
                } else {
                    int from = Math.max(0, all.size() - n);
                    recent = all.subList(from, all.size());
                }
                final String reply = ApiClient.chat(baseUrl, apiKey, model, pokePrompt,
                        prefs.getTemperature(), prefs.getMaxTokens() > 0 ? prefs.getMaxTokens() : 512,
                        recent, prefs.getEnablePinning(), prefs.getProvider(), prefs.getTrustAll());
                final List<String> parts = splitParts(reply);
                final boolean pokeBack = reply != null && reply.contains("戳");
                runOnUiThread(() -> {
                    for (String part : parts) {
                        adapter.add(new Message(Message.ROLE_ASSISTANT, part, nowTime()));
                        persist();
                        scrollToBottom();
                    }
                    if (pokeBack) showCenterToast("鸠戳了戳你的脸");
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    
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

    private static final String[] EMOJIS = {
            "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊",
            "😋", "😎", "😍", "😘", "😗", "🙂", "🤗", "🤔", "😐", "😑",
            "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫",
            "😴", "😌", "😛", "😜", "😝", "😒", "😓", "😔", "😕", "🙃",
            "😲", "😭", "😖", "😞", "😟", "😤", "😢", "😬", "😰", "😩",
            "😨", "😱", "😳", "😵", "😡", "😠", "😇", "🥳", "🤩", "🥺",
            "👍", "👎", "👏", "🙏", "💪", "🔥", "✨", "❤️", "💔", "🎉"
    };

    
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
