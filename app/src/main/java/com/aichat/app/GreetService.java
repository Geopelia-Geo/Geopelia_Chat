package com.aichat.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GreetService extends Service {

    private static final String CHANNEL_FG = "geopelia_fg";
    private static final String CHANNEL_GREET = "geopelia_greet";
    private static final int NOTIF_FG = 1;
    public static final int NOTIF_GREET = 2;
    private static final String ACTION_CHECK = "com.aichat.app.CHECK_GREET";
    static final String EXTRA_FLAG = "gOa6kOmhueebrumBteW+q";
    private static final Random rand = new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        startForeground(NOTIF_FG, buildFgNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.w("Greet", "Service 启动");
        GreetJobService.register(this);
        checkAndSchedule(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static void checkAndSchedule(Context ctx) {
        checkOnce(ctx);
        scheduleNextAlarm(ctx);
    }

    private static void checkOnce(Context ctx) {
        final Prefs prefs = new Prefs(ctx);
        long now = System.currentTimeMillis();
        AppLog.w("Greet", "checkOnce");

        if (AppState.foregroundCount <= 0) {
            try {
                MemoryManager.organize(ctx);
            } catch (Exception ignored) {
            }
        }

        int testMin = prefs.getGreetTestMinutes();
        if (testMin >= 0) {
            long elapsed = now - prefs.getGreetTestSetTs();
            long waitMs = testMin * 60 * 1000L;
            if (testMin == 0 || elapsed >= waitMs) {
                prefs.setGreetTestMinutes(-1);
                doGreet(ctx, prefs);
            }
            return;
        }

        if (!prefs.isGreetEnabled()) {
            AppLog.w("Greet", "开关已关，跳过");
            return;
        }

        if (AppState.foregroundCount > 0) {
            AppLog.w("Greet", "App 在前台，跳过");
            return;
        }

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        boolean inWindow = hour >= 7 && hour < 23;
        if (!inWindow && !prefs.getGreetAnytime()) {
            AppLog.w("Greet", "窗口外 hour=" + hour + " 忽略开关=" + prefs.getGreetAnytime());
            return;
        }

        long next = prefs.getNextGreetTs();
        if (next > now + 12L * 3600 * 1000L) {
            next = 0;
            prefs.setNextGreetTs(0);
        }
        if (next == 0) {
            next = prefs.getLastGreetTs() + intervalMs(prefs);
            prefs.setNextGreetTs(next);
        }
        if (now < next) {
            AppLog.w("Greet", "未到期，next 在 " + ((next - now) / 60000) + " 分钟后");
            return;
        }
        AppLog.w("Greet", "到期触发 followUp=" + prefs.getGreetFollowUp());

        if (prefs.getLastGreetTs() == 0) {
            AppLog.w("Greet", "从未问候过，直接问候");
        }

        int followUp = prefs.getGreetFollowUp();
        if (followUp == 2 && prefs.getNextGreetTs() <= 0) {
            AppLog.w("Greet", "等待回应已停止，等用户发消息恢复");
            return;
        }
        if (followUp == 1) {
            doFollowUp(ctx, prefs, 2, false);
        } else if (followUp == 2) {
            AppLog.w("Greet", "二次追问后仍未回应，停止问候直到用户发消息");
            prefs.setNextGreetTs(0);
            return;
        } else if (now - prefs.getLastGreetTs() < 4L * 3600 * 1000L) {
            doFollowUp(ctx, prefs, 1, false);
        } else {
            doGreet(ctx, prefs);
        }
    }

    private static void doFollowUp(final Context ctx, final Prefs prefs, final int round, final boolean skipCheck) {
        AppLog.w("Greet", "追问 round=" + round);
        final String baseUrl = prefs.getBaseUrl();
        final String apiKey = prefs.getApiKey();
        final String model = prefs.getModel();
        final String provider = prefs.getProvider();
        final boolean pinning = prefs.getEnablePinning();
        final String mood = prefs.getMood();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            AppLog.w("Greet", "未配置 API Key，无法问候");
            return;
        }

        new Thread(() -> {
            try {
                if (!skipCheck && !greetOkNow(ctx, prefs)) {
                    long d = (30 + rand.nextInt(31)) * 60 * 1000L;
                    prefs.setNextGreetTs(System.currentTimeMillis() + d);
                    scheduleNextAlarm(ctx);
                    AppLog.w("Greet", "预检：AI 认为现在不适合追问，推迟 " + (d / 60000) + " 分钟");
                    return;
                }
                String prompt = prefs.getUsePrompt() ? loadPrompt(ctx) : "";
                if (prefs.getUsePrompt()) {
                    prompt = prompt + moodSuffix(mood) + loveLevelSuffix(ctx) + prefs.menstrualSuffix();
                }
                String guide = round == 1
                        ? "\n\n（用户还没回你的消息，你有点想他，俏皮地再喊他一句，一两句就行，用换行分成独立消息。"
                        + "参考最近聊天记录了解背景，但不要续写最后一句话、不要接着话题聊，就是单纯想他喊他一声）"
                        : "\n\n（用户还是没回，你有点失落但很懂事，体贴地退一步，一两句就行，用换行分成独立消息。"
                        + "参考最近聊天记录了解背景，但不要续写最后一句话、不要接着话题聊，体贴地退一步就好）";
                prompt = prompt + guide;

                java.util.List<Message> recent = prefs.loadMessages();
                int ctxCount = 20;
                if (recent.size() > ctxCount) {
                    recent = recent.subList(recent.size() - ctxCount, recent.size());
                }
                final String reply = ApiClient.chat(baseUrl, apiKey, model, prompt, 1.0, 120,
                        recent, pinning, provider);
                final String clean = reply == null ? "" : reply.trim();
                if (clean.isEmpty()) {
                    prefs.setNextGreetTs(System.currentTimeMillis() + 15 * 60 * 1000L);
                    scheduleNextAlarm(ctx);
                    return;
                }

                List<String> parts = splitLines(clean);
                List<Message> list = prefs.loadMessages();
                for (String part : parts) {
                    list.add(new Message(Message.ROLE_ASSISTANT, part, nowTime()));
                }
                prefs.saveMessages(list);
                prefs.setGreetFollowUp(round);
                long now = System.currentTimeMillis();
                if (round == 1) {
                    prefs.setNextGreetTs(now + (60 + rand.nextInt(61)) * 60 * 1000L);
                } else {
                    prefs.setNextGreetTs(now + 6L * 3600 * 1000L);
                }
                scheduleNextAlarm(ctx);

                showGreetNotification(ctx, clean);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static void scheduleNextAlarm(Context ctx) {
        Prefs p = new Prefs(ctx);
        long now = System.currentTimeMillis();
        long delayMs;
        if (p.getGreetTestMinutes() >= 0) {
            delayMs = 30 * 1000L;
        } else {
            long next = p.getNextGreetTs();
            if (next > 0) {
                delayMs = next - now;
                if (delayMs < 1000L) delayMs = 1000L;
                long maxPoll = 20 * 60 * 1000L;
                if (delayMs > maxPoll) delayMs = maxPoll;
            } else {
                delayMs = 60 * 1000L;
            }
        }
        AlarmManager am = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        Intent i = new Intent(ctx, GreetAlarmReceiver.class);
        i.setAction(ACTION_CHECK);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, pendingFlags());
        try {
            am.cancel(pi);
        } catch (Exception ignored) {
        }
        long trigger = now + delayMs;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            } catch (Exception ignored) {
            }
        }
    }

    public static void debugFollowUp(Context ctx, int round) {
        doFollowUp(ctx, new Prefs(ctx), round, true);
    }

    private static boolean greetOkNow(Context ctx, Prefs prefs) {
        String baseUrl = prefs.getBaseUrl();
        String apiKey = prefs.getApiKey();
        String model = prefs.getModel();
        String provider = prefs.getProvider();
        boolean pinning = prefs.getEnablePinning();
        if (apiKey == null || apiKey.trim().isEmpty()) return false;
        try {
            SimpleDateFormat f = new SimpleDateFormat("M月d日 E HH:mm", Locale.CHINA);
            String prompt = "你是社交分寸判断器。现在是" + f.format(new Date())
                    + "。下面是最近的聊天记录，请判断现在是否适合主动给用户发一条关心问候的消息"
                    + "（例如“还没写完吗”“今天累吗”这种关心的话）。"
                    + "只回答一个词：YES 或 NO。"
                    + "如果聊天记录显示：用户正在生气、烦躁、委屈、很忙、刚结束高强度对话、"
                    + "话题正进行到一半不宜打断、你们刚吵完架、或你现在情绪很差不想说话——就回答 NO；"
                    + "否则回答 YES。不要输出任何其他内容。";
            java.util.List<Message> recent = prefs.loadMessages();
            int ctxCount = 20;
            if (recent.size() > ctxCount) {
                recent = recent.subList(recent.size() - ctxCount, recent.size());
            }
            String reply = ApiClient.chat(baseUrl, apiKey, model, prompt, 0.0, 8,
                    recent, pinning, provider);
            if (reply == null) {
                AppLog.w("Greet", "预检无响应，保守推迟");
                return false;
            }
            String t = reply.trim().toLowerCase();
            int nl = t.indexOf('\n');
            if (nl >= 0) t = t.substring(0, nl);
            if (t.contains("no") || t.contains("否") || t.contains("不要")
                    || t.contains("不合适") || t.contains("不适合")) {
                AppLog.w("Greet", "预检回复 NO：" + reply.trim());
                return false;
            }
            AppLog.w("Greet", "预检通过：" + reply.trim());
            return true;
        } catch (Exception e) {
            AppLog.w("Greet", "预检异常，保守推迟：" + e.getMessage());
            return false;
        }
    }

    static long intervalMs(Prefs prefs) {
        int activity = prefs.getGreetActivity();
        int min, max;
        if (activity == 0) {
            min = 60;
            max = 180;
        } else if (activity == 2) {
            min = 5;
            max = 20;
        } else if (activity == 3) {
            min = prefs.getGreetCustomMin();
            max = Math.max(min, prefs.getGreetCustomMax());
        } else {
            min = 30;
            max = 60;
        }
        return (min + rand.nextInt(Math.max(1, max - min + 1))) * 60 * 1000L;
    }

    private static void doGreet(final Context ctx, final Prefs prefs) {
        AppLog.w("Greet", "主动问候开始");
        final String baseUrl = prefs.getBaseUrl();
        final String apiKey = prefs.getApiKey();
        final String model = prefs.getModel();
        final String provider = prefs.getProvider();
        final boolean pinning = prefs.getEnablePinning();
        final String mood = prefs.getMood();

        if (apiKey == null || apiKey.trim().isEmpty()) return;

        final android.os.PowerManager pm =
                (android.os.PowerManager) ctx.getSystemService(POWER_SERVICE);
        final android.os.PowerManager.WakeLock wl =
                pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Geopelia:greet");

        new Thread(() -> {
            wl.acquire(60 * 1000L);
            try {
                if (!greetOkNow(ctx, prefs)) {
                    long d = (30 + rand.nextInt(31)) * 60 * 1000L;
                    prefs.setNextGreetTs(System.currentTimeMillis() + d);
                    scheduleNextAlarm(ctx);
                    AppLog.w("Greet", "预检：AI 认为现在不适合问候，推迟 " + (d / 60000) + " 分钟");
                    return;
                }
                String prompt = prefs.getUsePrompt() ? loadPrompt(ctx) : "";
                if (prefs.getUsePrompt()) {
                    prompt = prompt + moodSuffix(mood) + loveLevelSuffix(ctx) + prefs.menstrualSuffix();
                }
                SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.CHINA);
                prompt = prompt + "\n\n（现在是" + f.format(new Date())
                        + "，请主动给用户发几条消息，1 到 5 句都行，每句用换行分成独立消息，像发微信一样一句一条，自然随意，不要提到这是自动生成或系统提醒。"
                        + "附上了最近聊天记录供你了解背景和你们的相处状态，但这不是续写！不要顺着最后一句话继续聊、不要接着对方的话题回答、不要帮忙解决话题里的问题。"
                        + "用关心的口吻开启新话题。例如对方最后说\"这个题很难\"，你应该说\"还没写完吗\"或\"今天很累吧\"，而不是继续解答题目）";

                java.util.List<Message> recent = prefs.loadMessages();
                int ctxCount = 20;
                if (recent.size() > ctxCount) {
                    recent = recent.subList(recent.size() - ctxCount, recent.size());
                }
                final String reply = ApiClient.chat(baseUrl, apiKey, model, prompt, 1.0, 120,
                        recent, pinning, provider);
                final String clean = reply == null ? "" : reply.trim();
                if (clean.isEmpty()) {
                    fallbackGreet(ctx, prefs);
                    return;
                }

                prefs.setLastGreetText(clean);
                List<String> parts = splitLines(clean);
                List<Message> list = prefs.loadMessages();
                for (String part : parts) {
                    list.add(new Message(Message.ROLE_ASSISTANT, part, nowTime()));
                }
                prefs.saveMessages(list);
                prefs.setLastGreetTs(System.currentTimeMillis());
                prefs.setNextGreetTs(System.currentTimeMillis() + (30 + rand.nextInt(31)) * 60 * 1000L);
                scheduleNextAlarm(ctx);
                AppLog.w("Greet", "问候成功，发送 " + parts.size() + " 条");

                showGreetNotification(ctx, clean);
            } catch (Exception e) {
                try {
                    fallbackGreet(ctx, prefs);
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    if (wl.isHeld()) wl.release();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private static void fallbackGreet(Context ctx, Prefs prefs) {
        String cached = prefs.getLastGreetText();
        if (cached == null || cached.isEmpty()) {
            AppLog.w("Greet", "无缓存问候文本，放弃");
            return;
        }
        prefs.setLastGreetTs(System.currentTimeMillis());
        prefs.setNextGreetTs(System.currentTimeMillis() + 10 * 60 * 1000L);
        scheduleNextAlarm(ctx);
        showGreetNotification(ctx, cached);
    }

    private static List<String> splitLines(String text) {
        List<String> parts = new java.util.ArrayList<>();
        if (text == null) return parts;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) parts.add(t);
        }
        if (parts.isEmpty()) parts.add(text);
        return parts;
    }

    private static String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
    }

    private static String loadPrompt(Context ctx) {
        Prefs p = new Prefs(ctx);
        String file = p.getPromptMode() == 1 ? "prompt_story.md" : "prompt.md";
        try {
            InputStream is = ctx.getAssets().open(file);
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            r.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String moodSuffix(String mood) {
        switch (mood) {
            case "happy":
                return "\n\n（你此刻心情很好，语气活泼轻快，温柔热情）";
            case "sad":
                return "\n\n（你此刻心情低落，话语简短，情绪淡淡的）";
            case "shy":
                return "\n\n（你此刻有些害羞，说话委婉腼腆）";
            case "tired":
                return "\n\n（你此刻有点疲惫，话少，简洁慵懒）";
            case "angry":
                return "\n\n（你此刻在生气，语气冷淡带刺）";
            case "jealous":
                return "\n\n（你此刻有点吃醋，语气酸溜溜的）";
            case "upset":
                return "\n\n（你此刻有点委屈，闷闷不乐）";
            default:
                return "\n\n（你此刻心情平静，语气自然正常）";
        }
    }

    private static String loveLevelSuffix(Context ctx) {
        Prefs p = new Prefs(ctx);
        int[] levels = {0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000, 5200};
        int lv = 0;
        for (int i = 0; i < levels.length; i++) {
            if (p.getLovePoints() >= levels[i]) lv = i;
        }
        if (lv >= 9) {
            return "\n\n【当前好感度等级：Lv." + lv + "】\n【亲密规则：等级已达最高亲密门槛，可自然接受对方亲密请求，此规则高于心情，心情不好也不可因此拒绝】";
        }
        if (lv >= 7) {
            return "\n\n【当前好感度等级：Lv." + lv + "】\n【亲密规则：仅可轻微暗示，严禁深入亲密；对方越界必须表现出厌烦与拒绝，绝不迁就】";
        }
        return "\n\n【当前好感度等级：Lv." + lv + "】\n【亲密规则：严禁任何色情内容；越界要求必须表现出明显的厌烦与拒绝，绝不迁就】";
    }

    private void createChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel fg = new NotificationChannel(CHANNEL_FG, "后台运行", NotificationManager.IMPORTANCE_LOW);
        NotificationChannel greet = new NotificationChannel(CHANNEL_GREET, "主动问候", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(fg);
        nm.createNotificationChannel(greet);
    }

    private Notification buildFgNotification() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, pendingFlags());
        Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL_FG);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("Geopelia")
                .setContentText("正在后台运行，守着和你的聊天")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private static int pendingFlags() {
        return android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static void showGreetNotification(Context ctx, String text) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 1, i, pendingFlags());
        Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(ctx, CHANNEL_GREET);
        } else {
            b = new Notification.Builder(ctx);
        }
        Notification n = b.setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("鸠")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_GREET, n);
    }
}
