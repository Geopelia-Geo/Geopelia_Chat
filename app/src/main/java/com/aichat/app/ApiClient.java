package com.aichat.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 调用 OpenAI 兼容的 /chat/completions 接口。
 * 使用 Android 自带 HttpURLConnection + org.json，无任何第三方依赖，兼容 32 位设备。
 */
public class ApiClient {

    public static String chat(String baseUrl, String apiKey, String model,
                              String systemPrompt, double temperature, int maxTokens,
                              List<Message> history) throws Exception {

        // 归一化 base URL
        baseUrl = baseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String endpoint = baseUrl.toLowerCase().endsWith("/chat/completions")
                ? baseUrl
                : baseUrl + "/chat/completions";

        // 组装 messages
        JSONArray msgs = new JSONArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            msgs.put(new JSONObject()
                    .put("role", Message.ROLE_SYSTEM)
                    .put("content", systemPrompt.trim()));
        }
        for (Message m : history) {
            msgs.put(new JSONObject().put("role", m.role).put("content", m.content));
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("temperature", temperature);
        if (maxTokens > 0) body.put("max_tokens", maxTokens);
        body.put("stream", false);

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            }

            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
                os.flush();
            }

            int code = conn.getResponseCode();
            String respBody = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());

            if (code >= 400) {
                throw new Exception("HTTP " + code + ": " + safeError(respBody));
            }

            JSONObject root = new JSONObject(respBody);
            JSONArray choices = root.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject first = choices.getJSONObject(0);
                JSONObject msg = first.optJSONObject("message");
                if (msg != null) {
                    String c = msg.optString("content", "");
                    if (c.isEmpty() && first.has("text")) c = first.optString("text", "");
                    if (!c.isEmpty()) return c;
                }
                String text = first.optString("text", "");
                if (!text.isEmpty()) return text;
            }
            throw new Exception("接口返回中没有可用的回复内容");
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static String safeError(String body) {
        if (body == null || body.isEmpty()) return "空响应";
        try {
            JSONObject o = new JSONObject(body);
            JSONObject err = o.optJSONObject("error");
            if (err != null) {
                String msg = err.optString("message", "");
                String type = err.optString("type", "");
                return type.isEmpty() ? msg : type + ": " + msg;
            }
            return o.toString();
        } catch (Exception e) {
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
    }
}
