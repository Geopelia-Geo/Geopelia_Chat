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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ApiClient {

    public static final String[] PROVIDERS = {
            "deepseek", "tongyi", "zhipu", "kimi", "doubao", "groq", "anthropic", "other"
    };

    public static final String[] PROVIDER_NAMES = {
            "DeepSeek", "通义千问", "智谱清言", "Kimi", "豆包(火山引擎)", "Groq", "Anthropic", "其他(仅HTTPS)"
    };

    private static final Map<String, String[]> PIN_MAP = new HashMap<>();

    static {
        PIN_MAP.put("deepseek", new String[]{
                "KslVCVLNj5t/e67M06FA5NSudsCnIaTlIxl19lKhqDY=",
                "eLVG2Nq6lNlY482AlhlwwHqvL3TsvXMFJx2ycA8gZpQ="
        });
        PIN_MAP.put("tongyi", new String[]{
                "WZVJFj4+3elgfAAI/zW+L9mKCgh+6gck7f6zYoUC0Yg=",
                "nZ4QsWxivBcuuFkI8dXgfa0Pb2o1sjZ8hKx6h+729xw="
        });
        PIN_MAP.put("zhipu", new String[]{
                "efpviN4CHX6YeOqbLWsBTvnJqjULfZE/j9OAUrm/qH0=",
                "X4AGLwdqSLcL//rYNWWFtfT1CnCt94N7jSHB6cabFBU="
        });
        PIN_MAP.put("kimi", new String[]{
                "kxpiB7utdmL6V1bldpRAPJ8VLFeg4KIcMGm5c/Dy19c=",
                "E3tYcwo9CiqATmKtpMLW5V+pzIq+ZoDmpXSiJlXGmTo="
        });
        PIN_MAP.put("doubao", new String[]{
                "+03hVORN71gtdlp2tS7cTQ7+eFDH5a04h6/ocAF8k0c=",
                "E3tYcwo9CiqATmKtpMLW5V+pzIq+ZoDmpXSiJlXGmTo="
        });
        PIN_MAP.put("groq", new String[]{
                "i/Hiu2xyyGkPJRNjaeu1PZ46XYes2Dx4EbJd+HD06gw=",
                "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4="
        });
        PIN_MAP.put("anthropic", new String[]{
                "n+OFHb16YVI8KEux43Lk4jjFsK76xLolavlcACQEXq4=",
                "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4="
        });
    }

    private static final Map<String, SSLContext> contextCache = new HashMap<>();

    public static String chat(String baseUrl, String apiKey, String model,
                              String systemPrompt, double temperature, int maxTokens,
                              List<Message> history, boolean enablePinning, String provider)
            throws Exception {

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
            if (enablePinning && provider != null && !"other".equals(provider)
                    && PIN_MAP.containsKey(provider)
                    && endpoint.toLowerCase().startsWith("https://")) {
                SSLContext sc = getPinnedContext(provider);
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

    private static SSLContext getPinnedContext(String provider) throws Exception {
        synchronized (contextCache) {
            SSLContext sc = contextCache.get(provider);
            if (sc == null) {
                sc = buildPinnedContext(PIN_MAP.get(provider));
                contextCache.put(provider, sc);
            }
            return sc;
        }
    }

    private static SSLContext buildPinnedContext(final String[] hashes) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        final X509TrustManager systemTm = (X509TrustManager) tmf.getTrustManagers()[0];

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
                        for (String pin : hashes) {
                            if (pin.equals(fp)) return;
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException(e);
                }
                throw new CertificateException(
                        "证书指纹校验失败：连接可能被中间人拦截，或所选服务商证书已更换");
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
