package com.aichat.app;

/** 单条聊天消息。role: user / assistant；time: HH:mm 显示时间（可为空）。 */
public class Message {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    public final String role;
    public String content;
    public String time;

    public Message(String role, String content) {
        this(role, content, null);
    }

    public Message(String role, String content, String time) {
        this.role = role;
        this.content = content;
        this.time = time;
    }
}
