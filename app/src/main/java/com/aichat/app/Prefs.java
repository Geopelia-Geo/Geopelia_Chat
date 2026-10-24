package com.aichat.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Prefs {
    private static final String FILE = "aichat_prefs";

    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_API_KEY  = "api_key";
    public static final String KEY_MODEL    = "model";
    public static final String KEY_TEMP     = "temperature";
    public static final String KEY_MAX_TOK  = "max_tokens";
    public static final String KEY_CONTEXT  = "context_count";
    public static final String KEY_MESSAGES = "messages_json";
    public static final String KEY_BG_PATH  = "background_path";
    public static final String KEY_AVATAR_PATH = "avatar_path";
    public static final String KEY_NAME = "display_name";
    public static final String KEY_ENABLE_PINNING = "enable_pinning";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_LOVE_POINTS = "love_points";
    public static final String KEY_LOVE_START = "love_start_date";
    public static final String KEY_MOOD = "mood";
    public static final String KEY_MOOD_TS = "mood_last_change_ts";
    public static final String KEY_USE_PROMPT = "use_prompt";
    public static final String KEY_GREET_ENABLED = "greet_enabled";
    public static final String KEY_GREET_TEST_MIN = "greet_test_minutes";
    public static final String KEY_GREET_TEST_SET_TS = "greet_test_set_ts";
    public static final String KEY_LAST_GREET_TS = "last_greet_ts";
    public static final String KEY_LAST_USE_TS = "last_use_ts";
    public static final String KEY_GREET_ACTIVITY = "greet_activity";
    public static final String KEY_THEME_COLOR = "theme_color";
    public static final String KEY_NEXT_GREET_TS = "next_greet_ts";
    public static final String KEY_GREET_CUSTOM_MIN = "greet_custom_min";
    public static final String KEY_GREET_CUSTOM_MAX = "greet_custom_max";
    public static final String KEY_LAST_GREET_TEXT = "last_greet_text";
    public static final String KEY_GREET_FOLLOW_UP = "greet_follow_up";
    public static final String KEY_LAST_USER_MSG_TS = "last_user_msg_ts";
    public static final String KEY_PROMPT_MODE = "prompt_mode";
    public static final String KEY_NSFW_WARNED = "nsfw_warned";
    public static final String KEY_GREET_ANYTIME = "greet_anytime";
    public static final String KEY_LOG_RECORDING = "log_recording";
    public static final String KEY_NEXT_EVENT = "next_event";
    public static final String KEY_MEM_ENABLED = "mem_enabled";
    public static final String KEY_MEM_ORGANIZING = "mem_organizing";
    public static final String KEY_MEM_SHORT = "mem_short";
    public static final String KEY_MEM_LONG = "mem_long";
    public static final String KEY_MEM_WORLD = "mem_world";
    public static final String KEY_MEM_PENDING = "mem_pending";
    public static final String KEY_TRUST_ALL = "trust_all";
    public static final String KEY_REALISM_MODE = "realism_mode";
    public static final String KEY_MENSTRUAL_BASE_TS = "menstrual_base_ts";
    public static final String KEY_MENSTRUAL_OVERRIDE = "menstrual_override";
    public static final String KEY_USER_RATING = "user_rating";
    public static final String KEY_LAST_DONATE_VERSION = "last_donate_version";

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getBaseUrl() {
        return sp.getString(KEY_BASE_URL, "https://api.deepseek.com/v1");
    }

    public String getApiKey() {
        return sp.getString(KEY_API_KEY, "");
    }

    public String getModel() {
        return sp.getString(KEY_MODEL, "deepseek-chat");
    }

    public double getTemperature() {
        return sp.getFloat(KEY_TEMP, 1.0f);
    }

    public int getMaxTokens() {
        return sp.getInt(KEY_MAX_TOK, 1024);
    }

    public int getContextCount() {
        return sp.getInt(KEY_CONTEXT, 30);
    }

    public String getBackgroundPath() {
        return sp.getString(KEY_BG_PATH, null);
    }

    public void setBackgroundPath(String path) {
        if (path == null) sp.edit().remove(KEY_BG_PATH).apply();
        else sp.edit().putString(KEY_BG_PATH, path).apply();
    }

    public String getAvatarPath() {
        return sp.getString(KEY_AVATAR_PATH, null);
    }

    public void setAvatarPath(String path) {
        if (path == null) sp.edit().remove(KEY_AVATAR_PATH).apply();
        else sp.edit().putString(KEY_AVATAR_PATH, path).apply();
    }

    
    public String getDisplayName() {
        return sp.getString(KEY_NAME, "鸠");
    }

    public void setDisplayName(String name) {
        sp.edit().putString(KEY_NAME, name == null ? "" : name).apply();
    }

    public boolean getEnablePinning() {
        return sp.getBoolean(KEY_ENABLE_PINNING, true);
    }

    public void setEnablePinning(boolean v) {
        sp.edit().putBoolean(KEY_ENABLE_PINNING, v).apply();
    }

    public boolean getTrustAll() {
        return sp.getBoolean(KEY_TRUST_ALL, false);
    }

    public void setTrustAll(boolean v) {
        sp.edit().putBoolean(KEY_TRUST_ALL, v).apply();
    }

    public boolean getRealismMode() {
        return sp.getBoolean(KEY_REALISM_MODE, false);
    }

    public void setRealismMode(boolean v) {
        sp.edit().putBoolean(KEY_REALISM_MODE, v).apply();
    }

    public long getMenstrualBaseTs() {
        return sp.getLong(KEY_MENSTRUAL_BASE_TS, 0);
    }

    public void setMenstrualBaseTs(long ts) {
        sp.edit().putLong(KEY_MENSTRUAL_BASE_TS, ts).apply();
    }

    public String getMenstrualOverride() {
        return sp.getString(KEY_MENSTRUAL_OVERRIDE, "");
    }

    public void setMenstrualOverride(String v) {
        sp.edit().putString(KEY_MENSTRUAL_OVERRIDE, v == null ? "" : v).apply();
    }

    public int getUserRating() {
        return sp.getInt(KEY_USER_RATING, 50);
    }

    public void setUserRating(int v) {
        int c = Math.max(0, Math.min(100, v));
        sp.edit().putInt(KEY_USER_RATING, c).apply();
    }

    /** 最后一次弹出过捐赠弹窗的 versionCode；0 表示从未弹过 */
    public int getLastDonateVersion() {
        return sp.getInt(KEY_LAST_DONATE_VERSION, 0);
    }

    public void setLastDonateVersion(int v) {
        sp.edit().putInt(KEY_LAST_DONATE_VERSION, v).apply();
    }

    public int currentMenstrualDay() {
        long base = getMenstrualBaseTs();
        if (base <= 0) {
            base = System.currentTimeMillis();
            setMenstrualBaseTs(base);
        }
        long days = (System.currentTimeMillis() - base) / (24L * 3600 * 1000);
        int d = (int) (days % 28);
        if (d < 0) d += 28;
        return d + 1;
    }

    public int effectiveMenstrualDay() {
        int day = currentMenstrualDay();
        String ovr = getMenstrualOverride();
        if (ovr != null && !ovr.isEmpty()) {
            if (ovr.equals("月经期")) return 3;
            if (ovr.equals("卵泡期")) return 9;
            if (ovr.equals("排卵期")) return 14;
            if (ovr.equals("黄体期")) return 21;
        }
        return day;
    }

    public String menstrualPhaseName(int day) {
        if (day <= 5) return "经期";
        if (day <= 12) return "卵泡期";
        if (day == 13) return "排卵前";
        if (day == 14) return "排卵日";
        if (day == 15) return "排卵后";
        if (day <= 19) return "黄体早期";
        if (day <= 24) return "黄体中期";
        return "黄体晚期";
    }

    public String menstrualMood(int day) {
        String[] m = {"",
            "低落、想被照顾","自我怀疑、脆弱","抑郁、提不起劲","烦躁、易炸","恢复、缓和",
            "转晴、轻松","心情好、外向","稳定上升","愉快、自信","状态好","精力足、积极",
            "高涨、自信","情绪顶点","峰值、外放","略回落",
            "平稳","平稳偏累","稳定","轻微烦躁苗头",
            "敏感、易饿","烦躁起","易怒","烦躁重","烦躁回落",
            "情绪下沉","低落、敏感","缓和中","低谷收尾"};
        return day >= 1 && day <= 28 ? m[day] : "";
    }

    public String menstrualTalk(int day) {
        String[] t = {"",
            "话少、句子短、轻微撒娇","消极、反复确认对方在不在乎","回复慢、敷衍、常说没事",
            "冲、带刺、常说你烦不烦","语气软、主动说话、撒娇",
            "话变多、爱开玩笑","活泼、爱调侃","自然、健谈、体贴","主动、热情、爱夸人",
            "开朗、有耐心","话密、爱聊","大方、主动、爱撒娇",
            "热情、话多、主动调情","最主动、最健谈","仍热情、稍收敛",
            "正常、温和","话略少、偶尔走神","正常、略敏感","偶尔不耐烦",
            "爱抱怨小事、提要求","爱顶嘴、语气变短","一点就炸、说话冲",
            "挑刺、已读不回","不耐烦但能忍",
            "话变少、语气低落","易哭、想太多","消极但愿意说话","沉默、需要哄，次日循环"};
        return day >= 1 && day <= 28 ? t[day] : "";
    }

    public String menstrualSuffix() {
        if (!getRealismMode()) return "";
        int day = effectiveMenstrualDay();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【生理期·第").append(day).append("天·").append(menstrualPhaseName(day)).append("】");
        sb.append("你现在的心情：").append(menstrualMood(day));
        sb.append("。说话方式：").append(menstrualTalk(day)).append("。");
        if (day <= 5) {
            sb.append("【规则：经期无论好感度多高，严禁任何色情内容，对方越界就烦躁生气拒绝。】");
        }
        return sb.toString();
    }
    public void setTemperature(double v) { sp.edit().putFloat(KEY_TEMP, (float) v).apply(); }

    public void setMaxTokens(int v) { sp.edit().putInt(KEY_MAX_TOK, v).apply(); }

    public void setContextCount(int v) { sp.edit().putInt(KEY_CONTEXT, v).apply(); }

    public String getMood() { return sp.getString(KEY_MOOD, "normal"); }
    public void setMood(String m) { sp.edit().putString(KEY_MOOD, m).apply(); }

    public long getMoodLastChangeTs() { return sp.getLong(KEY_MOOD_TS, 0); }
    public void setMoodLastChangeTs(long ts) { sp.edit().putLong(KEY_MOOD_TS, ts).apply(); }

    public int getLovePoints() { return sp.getInt(KEY_LOVE_POINTS, 0); }
    public void setLovePoints(int v) {
        sp.edit().putInt(KEY_LOVE_POINTS, Math.max(0, Math.min(5200, v))).apply();
    }

    public String getLoveStartDate() { return sp.getString(KEY_LOVE_START, null); }
    public void setLoveStartDate(String d) { sp.edit().putString(KEY_LOVE_START, d).apply(); }

    public boolean getUsePrompt() { return sp.getBoolean(KEY_USE_PROMPT, true); }
    public void setUsePrompt(boolean v) { sp.edit().putBoolean(KEY_USE_PROMPT, v).apply(); }

    public String getProvider() { return sp.getString(KEY_PROVIDER, "deepseek"); }
    public void setProvider(String v) { sp.edit().putString(KEY_PROVIDER, v).apply(); }

    public int getThemeColor() { return sp.getInt(KEY_THEME_COLOR, 0xFF06C755); }
    public void setThemeColor(int c) { sp.edit().putInt(KEY_THEME_COLOR, c).apply(); }

    public int getPromptMode() { return sp.getInt(KEY_PROMPT_MODE, 0); }
    public void setPromptMode(int v) { sp.edit().putInt(KEY_PROMPT_MODE, v).apply(); }

    public boolean getNsfwWarned() { return sp.getBoolean(KEY_NSFW_WARNED, false); }
    public void setNsfwWarned(boolean v) { sp.edit().putBoolean(KEY_NSFW_WARNED, v).apply(); }

    public boolean getLogRecording() { return sp.getBoolean(KEY_LOG_RECORDING, false); }
    public void setLogRecording(boolean v) { sp.edit().putBoolean(KEY_LOG_RECORDING, v).apply(); }

    public String getNextEvent() { return sp.getString(KEY_NEXT_EVENT, ""); }
    public void setNextEvent(String v) { sp.edit().putString(KEY_NEXT_EVENT, v == null ? "" : v).apply(); }

    public boolean isGreetEnabled() { return sp.getBoolean(KEY_GREET_ENABLED, false); }
    public void setGreetEnabled(boolean v) { sp.edit().putBoolean(KEY_GREET_ENABLED, v).apply(); }

    public int getGreetActivity() { return sp.getInt(KEY_GREET_ACTIVITY, 1); }
    public void setGreetActivity(int v) { sp.edit().putInt(KEY_GREET_ACTIVITY, v).apply(); }

    public boolean getGreetAnytime() { return sp.getBoolean(KEY_GREET_ANYTIME, false); }
    public void setGreetAnytime(boolean v) { sp.edit().putBoolean(KEY_GREET_ANYTIME, v).apply(); }

    public int getGreetCustomMin() { return sp.getInt(KEY_GREET_CUSTOM_MIN, 30); }
    public void setGreetCustomMin(int v) { sp.edit().putInt(KEY_GREET_CUSTOM_MIN, v).apply(); }

    public int getGreetCustomMax() { return sp.getInt(KEY_GREET_CUSTOM_MAX, 60); }
    public void setGreetCustomMax(int v) { sp.edit().putInt(KEY_GREET_CUSTOM_MAX, v).apply(); }

    public int getGreetFollowUp() { return sp.getInt(KEY_GREET_FOLLOW_UP, 0); }
    public void setGreetFollowUp(int v) { sp.edit().putInt(KEY_GREET_FOLLOW_UP, v).apply(); }

    public int getGreetTestMinutes() { return sp.getInt(KEY_GREET_TEST_MIN, -1); }
    public void setGreetTestMinutes(int v) {
        sp.edit().putInt(KEY_GREET_TEST_MIN, v)
          .putLong(KEY_GREET_TEST_SET_TS, System.currentTimeMillis()).apply();
    }
    public long getGreetTestSetTs() { return sp.getLong(KEY_GREET_TEST_SET_TS, 0); }

    public long getLastGreetTs() { return sp.getLong(KEY_LAST_GREET_TS, 0); }
    public void setLastGreetTs(long ts) { sp.edit().putLong(KEY_LAST_GREET_TS, ts).apply(); }

    public long getLastUseTs() { return sp.getLong(KEY_LAST_USE_TS, 0); }
    public void setLastUseTs(long ts) { sp.edit().putLong(KEY_LAST_USE_TS, ts).apply(); }

    public long getLastUserMsgTs() { return sp.getLong(KEY_LAST_USER_MSG_TS, 0); }
    public void setLastUserMsgTs(long ts) { sp.edit().putLong(KEY_LAST_USER_MSG_TS, ts).apply(); }

    public long getNextGreetTs() { return sp.getLong(KEY_NEXT_GREET_TS, 0); }
    public void setNextGreetTs(long ts) { sp.edit().putLong(KEY_NEXT_GREET_TS, ts).apply(); }

    public String getLastGreetText() { return sp.getString(KEY_LAST_GREET_TEXT, ""); }
    public void setLastGreetText(String v) { sp.edit().putString(KEY_LAST_GREET_TEXT, v == null ? "" : v).apply(); }

    public void save(String baseUrl, String apiKey, String model, double temp, int maxTok, int ctx) {
        sp.edit().putString(KEY_BASE_URL, baseUrl)
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_MODEL, model)
                .putFloat(KEY_TEMP, (float) temp)
                .putInt(KEY_MAX_TOK, maxTok)
                .putInt(KEY_CONTEXT, ctx).apply();
    }

    public boolean getMemEnabled() { return sp.getBoolean(KEY_MEM_ENABLED, true); }
    public void setMemEnabled(boolean v) { sp.edit().putBoolean(KEY_MEM_ENABLED, v).apply(); }

    public boolean getMemOrganizing() { return sp.getBoolean(KEY_MEM_ORGANIZING, false); }
    public void setMemOrganizing(boolean v) { sp.edit().putBoolean(KEY_MEM_ORGANIZING, v).apply(); }

    private List<String> loadStrList(String key) {
        List<String> list = new ArrayList<>();
        String raw = sp.getString(key, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (Exception ignored) {
        }
        return list;
    }

    private void saveStrList(String key, List<String> list) {
        JSONArray arr = new JSONArray();
        for (String t : list) arr.put(t);
        sp.edit().putString(key, arr.toString()).apply();
    }

    public List<String> getShortMemories() { return loadStrList(KEY_MEM_SHORT); }
    public void addShortMemory(String t) {
        if (t == null || t.trim().isEmpty()) return;
        List<String> l = loadStrList(KEY_MEM_SHORT);
        l.add(t.trim());
        saveStrList(KEY_MEM_SHORT, l);
    }
    public void clearShortMemories() { sp.edit().remove(KEY_MEM_SHORT).apply(); }

    public List<String> getLongMemories() { return loadStrList(KEY_MEM_LONG); }
    public void addLongMemory(String t) {
        if (t == null || t.trim().isEmpty()) return;
        List<String> l = loadStrList(KEY_MEM_LONG);
        l.add(t.trim());
        saveStrList(KEY_MEM_LONG, l);
    }
    public void clearLongMemories() { sp.edit().remove(KEY_MEM_LONG).apply(); }

    public List<String> getWorldBook() { return loadStrList(KEY_MEM_WORLD); }
    public void addWorldBook(String t) {
        if (t == null || t.trim().isEmpty()) return;
        List<String> l = loadStrList(KEY_MEM_WORLD);
        String tt = t.trim();
        for (String e : l) if (e.equals(tt)) return;
        l.add(tt);
        saveStrList(KEY_MEM_WORLD, l);
    }
    public void clearWorldBook() { sp.edit().remove(KEY_MEM_WORLD).apply(); }

    public static class Round {
        public String user, ai;
        public long ts;
        public Round(String u, String a, long t) { user = u; ai = a; ts = t; }
    }

    public List<Round> getPendingRounds() {
        List<Round> list = new ArrayList<>();
        String raw = sp.getString(KEY_MEM_PENDING, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Round(o.optString("user", ""), o.optString("ai", ""), o.optLong("ts", 0)));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void addPendingRound(String user, String ai) {
        try {
            List<Round> list = getPendingRounds();
            list.add(new Round(user == null ? "" : user, ai == null ? "" : ai, System.currentTimeMillis()));
            if (list.size() > 50) list = list.subList(list.size() - 50, list.size());
            JSONArray arr = new JSONArray();
            for (Round r : list) {
                JSONObject o = new JSONObject();
                o.put("user", r.user);
                o.put("ai", r.ai);
                o.put("ts", r.ts);
                arr.put(o);
            }
            sp.edit().putString(KEY_MEM_PENDING, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void removeFirstPendingRound() {
        try {
            List<Round> list = getPendingRounds();
            if (list.isEmpty()) return;
            list.remove(0);
            JSONArray arr = new JSONArray();
            for (Round r : list) {
                JSONObject o = new JSONObject();
                o.put("user", r.user);
                o.put("ai", r.ai);
                o.put("ts", r.ts);
                arr.put(o);
            }
            sp.edit().putString(KEY_MEM_PENDING, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void clearPendingRounds() { sp.edit().remove(KEY_MEM_PENDING).apply(); }

    public void saveMessages(List<Message> msgs) {
        JSONArray arr = new JSONArray();
        try {
            for (Message m : msgs) {
                JSONObject o = new JSONObject();
                o.put("role", m.role);
                o.put("content", m.content == null ? "" : m.content);
                if (m.time != null) o.put("time", m.time);
                if (m.imagePath != null) o.put("image", m.imagePath);
                o.put("ts", m.ts);
                arr.put(o);
            }
            sp.edit().putString(KEY_MESSAGES, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public List<Message> loadMessages() {
        List<Message> list = new ArrayList<>();
        String raw = sp.getString(KEY_MESSAGES, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String img = o.isNull("image") ? null : o.optString("image", null);
                Message m = new Message(o.optString("role", Message.ROLE_ASSISTANT),
                        o.optString("content", ""),
                        o.isNull("time") ? null : o.optString("time", null), img);
                if (!o.isNull("ts")) m.ts = o.optLong("ts", 0);
                list.add(m);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void clearMessages() { sp.edit().remove(KEY_MESSAGES).apply(); }
}
