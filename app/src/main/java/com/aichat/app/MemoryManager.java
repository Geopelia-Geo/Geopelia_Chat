package com.aichat.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MemoryManager {
    private static final int SHORT_LIMIT = 7;
    private static final int LONG_LIMIT = 15;
    private static final int WORLD_LIMIT = 60;

    public static boolean hasApi(Context ctx) {
        Prefs p = new Prefs(ctx);
        String k = p.getApiKey();
        return k != null && !k.trim().isEmpty();
    }

    public static void addRound(Context ctx, String userText, String aiText) {
        Prefs p = new Prefs(ctx);
        if (!p.getMemEnabled()) return;
        p.addPendingRound(userText, aiText);
        organize(ctx);
    }

    public static void organize(Context ctx) {
        Prefs p = new Prefs(ctx);
        if (!p.getMemEnabled()) return;
        if (p.getMemOrganizing()) return;
        if (p.getPendingRounds().isEmpty()) return;
        if (!hasApi(ctx)) return;
        p.setMemOrganizing(true);
        new Thread(() -> {
            try {
                runOrganize(ctx);
            } catch (Exception e) {
                AppLog.e("Mem", "整理出错", e);
            } finally {
                new Prefs(ctx).setMemOrganizing(false);
            }
        }).start();
    }

    private static void runOrganize(Context ctx) {
        Prefs p = new Prefs(ctx);
        List<Prefs.Round> rounds = p.getPendingRounds();
        if (rounds.isEmpty()) return;

        Prefs.Round round = rounds.get(0);
        String result = summarizeRound(ctx, round);
        if (result == null) {
            return;
        }
        p.removeFirstPendingRound();

        String summary = null;
        String entities = null;
        if (result.contains("\n实体：") || result.contains("实体：")) {
            int idx = result.indexOf("实体：");
            summary = result.substring(0, idx).replace("记忆：", "").trim();
            entities = result.substring(idx + "实体：".length()).trim();
        } else {
            summary = result.replace("记忆：", "").trim();
        }
        String dateTag = new java.text.SimpleDateFormat("MM/dd", java.util.Locale.CHINA).format(new java.util.Date());
        if (summary != null && !summary.isEmpty() && !summary.equals("无") && !summary.equals("无记忆")) {
            p.addShortMemory("[" + dateTag + "] " + summary);
        }
        if (entities != null && !entities.isEmpty() && !entities.equals("无") && !entities.equals("无实体")) {
            String[] lines = entities.split("\n");
            for (String line : lines) {
                String t = line.trim();
                if (t.isEmpty() || t.equals("无")) continue;
                p.addWorldBook(t);
            }
        }

        if (p.getShortMemories().size() >= SHORT_LIMIT) {
            String longMem = summarizeLong(ctx);
            if (longMem != null && !longMem.trim().isEmpty()) {
                p.addLongMemory("[" + dateTag + "] " + longMem.trim());
                p.clearShortMemories();
            }
        }

        if (p.getLongMemories().size() >= LONG_LIMIT) {
            String digest = summarizeWorld(ctx);
            if (digest != null && !digest.trim().isEmpty()) {
                p.addWorldBook(digest.trim());
                p.clearLongMemories();
            }
        }
    }

    private static String summarizeRound(Context ctx, Prefs.Round round) {
        Prefs p = new Prefs(ctx);
        StringBuilder ctxInfo = new StringBuilder();
        ctxInfo.append("【整理时状态】AI 当前心情：").append(p.getMood());
        if (p.getRealismMode()) {
            long base = p.getMenstrualBaseTs();
            if (base > 0) {
                long d = (System.currentTimeMillis() - base) / (24L * 3600 * 1000);
                int day = (int) ((d % 28) + 1);
                if (day < 1) day += 28;
                String phase;
                if (day <= 5) phase = "经期";
                else if (day <= 12) phase = "卵泡期";
                else if (day <= 15) phase = "排卵期";
                else phase = "黄体期";
                ctxInfo.append("；生理期：第").append(day).append("天·").append(phase);
                ctxInfo.append("（注意：经期/黄体期的负面情绪是激素导致的临时状态，不代表真实关系，归纳时不要把激素情绪当作关系事实记录）");
            }
        }
        ctxInfo.append("\n\n");
        StringBuilder user = new StringBuilder();
        user.append(ctxInfo)
                .append("以下是最近一轮对话（用户与 AI 的交流）：\n\n用户：")
                .append(round.user == null ? "" : round.user)
                .append("\nAI：")
                .append(round.ai == null ? "" : round.ai)
                .append("\n\n请完成两件事：\n1. 把这段对话归纳成一句简短记忆（不超过60字），保留关键事实、约定、情绪和重要细节。如果 AI 的负面情绪明显是生理期/激素导致的临时烦躁，不要记录为关系问题。\n2. 提取重要信息（人物、地点、物品、重要事件、用户偏好、正在进行的计划），每条一行。\n\n严格按以下格式输出：\n记忆：<简短记忆>\n实体：<无则写「无」，否则每行一条，格式：类型：名称：描述>");

        List<Message> history = new ArrayList<>();
        history.add(new Message(Message.ROLE_USER, user.toString(), null, null));
        try {
            return ApiClient.chat(p.getBaseUrl(), p.getApiKey(), p.getModel(),
                    "你是记忆整理助手，负责把对话内容整理成简洁持久的记忆。不要输出任何多余内容。",
                    0.3, 800, history, p.getEnablePinning(), p.getProvider(), p.getTrustAll());
        } catch (Exception e) {
            AppLog.e("Mem", "短期归纳失败", e);
            return null;
        }
    }

    private static String summarizeLong(Context ctx) {
        Prefs p = new Prefs(ctx);
        StringBuilder sb = new StringBuilder("以下是有序的短期记忆片段：\n\n");
        List<String> shorts = p.getShortMemories();
        for (int i = 0; i < shorts.size(); i++) {
            sb.append(i + 1).append(". ").append(shorts.get(i)).append('\n');
        }
        sb.append("\n请把这些短期记忆归纳成 2 到 3 条长期记忆，每条不超过 100 字，保留重要事实、关系、情感脉络和长期约定。按行输出，每条一行。");

        List<Message> history = new ArrayList<>();
        history.add(new Message(Message.ROLE_USER, sb.toString(), null, null));
        try {
            return ApiClient.chat(p.getBaseUrl(), p.getApiKey(), p.getModel(),
                    "你是记忆整理助手，负责把短期记忆合并为长期记忆。只输出归纳后的记忆条目，每行一条，不要编号以外的多余内容。",
                    0.3, 1000, history, p.getEnablePinning(), p.getProvider(), p.getTrustAll());
        } catch (Exception e) {
            AppLog.e("Mem", "长期归纳失败", e);
            return null;
        }
    }

    private static String summarizeWorld(Context ctx) {
        Prefs p = new Prefs(ctx);
        StringBuilder sb = new StringBuilder("以下是累积的长期记忆：\n\n");
        List<String> longs = p.getLongMemories();
        for (int i = 0; i < longs.size(); i++) {
            sb.append(i + 1).append(". ").append(longs.get(i)).append('\n');
        }
        sb.append("\n请把上面的长期记忆总结成精简的世界书条目（人物/地点/物品/重要事件/长期状态），丢弃过时和琐碎细节，只保留仍然重要且持久的信息。每行一条，格式：类型：名称：描述");

        List<Message> history = new ArrayList<>();
        history.add(new Message(Message.ROLE_USER, sb.toString(), null, null));
        try {
            return ApiClient.chat(p.getBaseUrl(), p.getApiKey(), p.getModel(),
                    "你是世界书整理助手，负责把长期记忆沉淀为精简的世界设定条目。只输出条目，每行一条。",
                    0.3, 1200, history, p.getEnablePinning(), p.getProvider(), p.getTrustAll());
        } catch (Exception e) {
            AppLog.e("Mem", "世界书总结失败", e);
            return null;
        }
    }

    public static String buildMemoryBlock(Prefs p) {
        StringBuilder sb = new StringBuilder();
        List<String> world = p.getWorldBook();
        List<String> longs = p.getLongMemories();
        List<String> shorts = p.getShortMemories();

        if (!world.isEmpty() || !longs.isEmpty() || !shorts.isEmpty()) {
            sb.append("\n\n【记忆】");
        }
        if (!world.isEmpty()) {
            sb.append("\n[世界书]");
            for (String s : world) {
                sb.append('\n').append(s);
            }
        }
        if (!longs.isEmpty()) {
            sb.append("\n[长期记忆]");
            int from = Math.max(0, longs.size() - 8);
            for (int i = from; i < longs.size(); i++) {
                sb.append('\n').append(longs.get(i));
            }
        }
        if (!shorts.isEmpty()) {
            sb.append("\n[近期记忆]");
            int from = Math.max(0, shorts.size() - 3);
            for (int i = from; i < shorts.size(); i++) {
                sb.append('\n').append(shorts.get(i));
            }
        }
        return sb.toString();
    }
}
