package com.aichat.app;

public class Message {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    public final String role;
    public String content;
    public String time;
    public String imagePath;
    public long ts;

    public Message(String role, String content) {
        this(role, content, null, null);
    }

    public Message(String role, String content, String time) {
        this(role, content, time, null);
    }

    public Message(String role, String content, String time, String imagePath) {
        this.role = role;
        this.content = content;
        this.time = time;
        this.imagePath = imagePath;
        this.ts = System.currentTimeMillis();
    }

    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }
}
