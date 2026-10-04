package com.aichat.app;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ApiClient {

    
    private static final String[] PINNED_SPKI_HASHES = {
            "KslVCVLNj5t/e67M06FA5NSudsCnIaTlIxl19lKhqDY=",  // api.deepseek.com 叶证书
            "eLVG2Nq6lNlY482AlhlwwHqvL3TsvXMFJx2ycA8gZpQ="   // TrustAsia DV TLS RSA CA 2025
    };

    private static volatile SSLContext pinnedContext; // 懒加载缓存

    public static String chat(String baseUrl, String apiKey, String model,
                              String systemPrompt, double temperature, int maxTokens,
                              List<Message> history) throws Exception {

        baseUrl = baseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.toLowerCase().startsWith("http://") && !isPrivateHost(baseUrl)) {
            baseUrl = "https://" + baseUrl.substring(7);
        }
        String endpoint = baseUrl.toLowerCase().endsWith("/chat/completions")
                ? baseUrl
                : baseUrl + "/chat/completions";

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
            if (endpoint.toLowerCase().startsWith("https://") && isDeepSeekHost(endpoint)) {
                SSLContext sc = getPinnedContext();
                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                }
            }
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

    
    private static boolean isDeepSeekHost(String url) {
        try {
            String host = new URL(url).getHost().toLowerCase();
            return host.equals("api.deepseek.com") || host.endsWith(".deepseek.com");
        } catch (Exception e) {
            return false;
        }
    }

    
    private static boolean isPrivateHost(String url) {
        try {
            String host = new URL(url).getHost().toLowerCase();
            return host.equals("localhost") || host.equals("127.0.0.1")
                    || host.startsWith("192.168.") || host.startsWith("10.")
                    || host.startsWith("172.16.") || host.startsWith("172.31.");
        } catch (Exception e) {
            return false;
        }
    }

    private static SSLContext getPinnedContext() throws Exception {
        SSLContext sc = pinnedContext;
        if (sc == null) {
            synchronized (ApiClient.class) {
                sc = pinnedContext;
                if (sc == null) {
                    sc = buildPinnedContext();
                    pinnedContext = sc;
                }
            }
        }
        return sc;
    }

    private static SSLContext buildPinnedContext() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        X509TrustManager systemTm = (X509TrustManager) tmf.getTrustManagers()[0];

        X509TrustManager pinTm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                if (chain == null || chain.length == 0) {
                    throw new CertificateException("服务器未提供证书");
                }
                try {
                    systemTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    throw e;
                }
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    for (X509Certificate cert : chain) {
                        byte[] spki = cert.getPublicKey().getEncoded();
                        String fp = Base64.encodeToString(md.digest(spki), Base64.NO_WRAP);
                        for (String pin : PINNED_SPKI_HASHES) {
                            if (pin.equals(fp)) return; // 命中即通过
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException(e);
                }
                throw new CertificateException(
                        "证书指纹校验失败：连接可能被中间人拦截，或 DeepSeek 证书已更换");
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{pinTm}, null);
        return sc;
    }
}
