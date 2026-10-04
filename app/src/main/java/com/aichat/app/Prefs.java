package com.aichat.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 应用设置与聊天记录存储。系统提示词已内置，不再可配置。 */
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

    /** 顶栏显示的昵称（默认“鸠”），可在设置里修改。 */
    public String getDisplayName() {
        return sp.getString(KEY_NAME, "鸠");
    }

    public void setDisplayName(String name) {
        sp.edit().putString(KEY_NAME, name == null ? "" : name).apply();
    }

    public void save(String baseUrl, String apiKey, String model,
                     double temperature, int maxTokens, int contextCount) {
        sp.edit()
          .putString(KEY_BASE_URL, baseUrl)
          .putString(KEY_API_KEY, apiKey)
          .putString(KEY_MODEL, model)
          .putFloat(KEY_TEMP, (float) temperature)
          .putInt(KEY_MAX_TOK, maxTokens)
          .putInt(KEY_CONTEXT, contextCount)
          .apply();
    }

    // ===== 聊天记录持久化 =====

    public void saveMessages(List<Message> msgs) {
        try {
            JSONArray arr = new JSONArray();
            for (Message m : msgs) {
                JSONObject o = new JSONObject().put("role", m.role).put("content", m.content);
                if (m.time != null) o.put("time", m.time);
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
                list.add(new Message(o.optString("role", Message.ROLE_ASSISTANT),
                                      o.optString("content", ""),
                                      o.isNull("time") ? null : o.optString("time", null)));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void clearMessages() {
        sp.edit().remove(KEY_MESSAGES).apply();
    }
}
