package com.aichat.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugActivity extends AppCompatActivity {

    private Prefs prefs;
    private TextView curLoveLabel;
    private TextView moodHint;
    private TextView debugInfo;
    private final String[] moodKeys = {"happy", "normal", "sad", "shy", "tired", "angry", "jealous", "upset"};
    private final int[] moodBtnIds = {
            R.id.btnMoodHappy, R.id.btnMoodNormal, R.id.btnMoodSad, R.id.btnMoodShy,
            R.id.btnMoodTired, R.id.btnMoodAngry, R.id.btnMoodJealous, R.id.btnMoodUpset
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        prefs = new Prefs(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        curLoveLabel = findViewById(R.id.curLoveLabel);
        moodHint = findViewById(R.id.moodHint);
        debugInfo = findViewById(R.id.debugInfo);

        setupMenstrual();

        EditText etUserRating = findViewById(R.id.etUserRating);
        etUserRating.setText(String.valueOf(prefs.getUserRating()));
        findViewById(R.id.btnSetRating).setOnClickListener(v -> {
            try {
                int r = Integer.parseInt(etUserRating.getText().toString().trim());
                prefs.setUserRating(r);
                Toast.makeText(this, "评分已设为 " + r, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "请输入数字", Toast.LENGTH_SHORT).show();
            }
        });

        EditText etLovePoints = findViewById(R.id.etLovePoints);

        EditText etNextEvent = findViewById(R.id.etNextEvent);
        findViewById(R.id.btnSetEvent).setOnClickListener(v -> {
            String ev = etNextEvent.getText().toString().trim();
            if (ev.isEmpty()) {
                Toast.makeText(this, "请输入事件内容", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.setNextEvent(ev);
            refresh();
            Toast.makeText(this, "已设置下次事件，下次对话触发", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnClearEvent).setOnClickListener(v -> {
            prefs.setNextEvent("");
            etNextEvent.setText("");
            refresh();
            Toast.makeText(this, "已清除事件", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnTestEvent).setOnClickListener(v -> {
            final String eventText = prefs.getNextEvent();
            if (eventText == null || eventText.trim().isEmpty()) {
                Toast.makeText(this, "请先设置事件", Toast.LENGTH_SHORT).show();
                return;
            }
            final String baseUrl = prefs.getBaseUrl();
            final String apiKey = prefs.getApiKey();
            final String model = prefs.getModel();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Toast.makeText(this, "未配置 API Key", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "正在测试事件…", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    java.util.List<Message> all = prefs.loadMessages();
                    java.util.List<Message> recent;
                    if (all.size() <= 20) {
                        recent = all;
                    } else {
                        recent = all.subList(all.size() - 20, all.size());
                    }
                    java.util.List<Message> payload = new java.util.ArrayList<>(recent);
                    payload.add(new Message(Message.ROLE_SYSTEM, "系统场景提示词：" + eventText));
                    String prompt = "用关心的口吻，1 到 2 句就行，每句用换行分成独立消息。";
                    final String reply = ApiClient.chat(baseUrl, apiKey, model, prompt, 1.0, 200,
                            payload, prefs.getEnablePinning(), prefs.getProvider());
                    if (reply == null || reply.trim().isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(DebugActivity.this,
                                "AI 无回复，事件未消费", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    java.util.List<Message> list = prefs.loadMessages();
                    for (String line : reply.split("\n")) {
                        String t = line.trim();
                        if (t.isEmpty()) continue;
                        list.add(new Message(Message.ROLE_ASSISTANT, t,
                                new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date())));
                    }
                    prefs.saveMessages(list);
                    prefs.setNextEvent("");
                    runOnUiThread(() -> {
                        refresh();
                        Toast.makeText(DebugActivity.this,
                                "测试完成：鸠的回复已加入聊天记录，事件已清除", Toast.LENGTH_LONG).show();
                    });
                } catch (final Exception e) {
                    runOnUiThread(() -> Toast.makeText(DebugActivity.this,
                            "测试失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
        findViewById(R.id.btnSetLove).setOnClickListener(v -> {
            String s = etLovePoints.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "请输入数值", Toast.LENGTH_SHORT).show();
                return;
            }
            int val = Math.max(0, Math.min(9999, Integer.parseInt(s)));
            prefs.setLovePoints(val);
            refresh();
            Toast.makeText(this, "好感度已设置为 " + val, Toast.LENGTH_SHORT).show();
        });

        for (int i = 0; i < moodBtnIds.length; i++) {
            final int idx = i;
            findViewById(moodBtnIds[i]).setOnClickListener(v -> {
                prefs.setMood(moodKeys[idx]);
                prefs.setMoodLastChangeTs(System.currentTimeMillis());
                refresh();
                Toast.makeText(this, "心情已切换", Toast.LENGTH_SHORT).show();
            });
        }

        SwitchMaterial swUsePrompt = findViewById(R.id.swUsePrompt);
        swUsePrompt.setChecked(prefs.getUsePrompt());
        swUsePrompt.setOnCheckedChangeListener((b, checked) -> {
            prefs.setUsePrompt(checked);
            Toast.makeText(this, checked ? "已启用内置提示词" : "已禁用内置提示词", Toast.LENGTH_SHORT).show();
        });

        SwitchMaterial swGreetAnytime = findViewById(R.id.swGreetAnytime);
        swGreetAnytime.setChecked(prefs.getGreetAnytime());
        swGreetAnytime.setOnCheckedChangeListener((b, checked) -> {
            prefs.setGreetAnytime(checked);
            refresh();
            Toast.makeText(this, checked ? "已忽略时间窗口" : "已恢复时间窗口（7-23点）", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnGreetLog).setOnClickListener(v ->
                startActivity(new Intent(this, GreetLogActivity.class)));

        findViewById(R.id.btnGreetTest2).setOnClickListener(v -> {
            GreetService.debugFollowUp(this, 2);
            Toast.makeText(this, "已触发第二次追问", Toast.LENGTH_SHORT).show();
        });

        final TextView logRecState = findViewById(R.id.logRecState);
        final Runnable updateLogState = () -> logRecState.setText("状态：" + (AppLog.isRecording() ? "录制中" : "未录制"));
        updateLogState.run();
        findViewById(R.id.btnLogStart).setOnClickListener(v -> {
            AppLog.start(this);
            updateLogState.run();
            Toast.makeText(this, "开始录制，所有日志将写入文件", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnLogStop).setOnClickListener(v -> {
            AppLog.stop(this);
            updateLogState.run();
            Toast.makeText(this, "已停止录制", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnLogView).setOnClickListener(v ->
                startActivity(new Intent(this, AppLogActivity.class)));
        findViewById(R.id.btnLogClear).setOnClickListener(v -> {
            AppLog.clear(this);
            Toast.makeText(this, "录制日志已清空", Toast.LENGTH_SHORT).show();
        });

        EditText etGreetMinutes = findViewById(R.id.etGreetMinutes);
        findViewById(R.id.btnGreetTest).setOnClickListener(v -> {
            String s = etGreetMinutes.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "请输入分钟数", Toast.LENGTH_SHORT).show();
                return;
            }
            int min = Math.max(0, Integer.parseInt(s));
            prefs.setGreetTestMinutes(min);
            Intent svc = new Intent(this, GreetService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            Toast.makeText(this, min == 0 ? "已触发立即问候测试" : "将在 " + min + " 分钟后问候", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnClearChat).setOnClickListener(v -> {
            prefs.clearMessages();
            refresh();
            Toast.makeText(this, "聊天记录已清空", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnResetLove).setOnClickListener(v -> {
            prefs.setLovePoints(0);
            refresh();
            Toast.makeText(this, "好感度已归零", Toast.LENGTH_SHORT).show();
        });

        refresh();
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
        refresh();
    }

    private void refresh() {
        TextView curEvent = findViewById(R.id.curEventLabel);
        if (curEvent != null) {
            String ev = prefs.getNextEvent();
            curEvent.setText(ev == null || ev.isEmpty() ? "当前事件：无" : "当前事件：" + ev);
        }
        int points = prefs.getLovePoints();
        int level = 0;
        int[] levels = {0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000, 5200};
        for (int i = 0; i < levels.length; i++) {
            if (points >= levels[i]) level = i;
        }
        curLoveLabel.setText("当前：" + points + " 点 · LV." + (level + 1));

        String mood = prefs.getMood();
        String moodText;
        switch (mood) {
            case "happy": moodText = "开心"; break;
            case "sad": moodText = "低落"; break;
            case "shy": moodText = "害羞"; break;
            case "tired": moodText = "疲惫"; break;
            case "angry": moodText = "生气"; break;
            case "jealous": moodText = "吃醋"; break;
            case "upset": moodText = "委屈"; break;
            default: moodText = "平静";
        }
        moodHint.setText("当前心情：" + moodText);

        SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        long lastUse = prefs.getLastUseTs();
        long lastGreet = prefs.getLastGreetTs();
        StringBuilder sb = new StringBuilder();
        sb.append("Base URL：").append(prefs.getBaseUrl()).append('\n');
        sb.append("模型：").append(prefs.getModel()).append('\n');
        sb.append("提供商：").append(prefs.getProvider()).append('\n');
        sb.append("上下文条数：").append(prefs.getContextCount()).append('\n');
        sb.append("证书固定：").append(prefs.getEnablePinning() ? "开" : "关").append('\n');
        sb.append("内置提示词：").append(prefs.getUsePrompt() ? "开" : "关").append('\n');
        sb.append("主动问候：").append(prefs.isGreetEnabled() ? "开" : "关").append('\n');
        sb.append("忽略时间窗口：").append(prefs.getGreetAnytime() ? "开" : "关").append('\n');
        sb.append("聊天消息数：").append(prefs.loadMessages().size()).append('\n');
        sb.append("最后使用：").append(lastUse > 0 ? f.format(new Date(lastUse)) : "从未").append('\n');
        sb.append("最后问候：").append(lastGreet > 0 ? f.format(new Date(lastGreet)) : "从未").append('\n');
        long nextGreet = prefs.getNextGreetTs();
        if (nextGreet > 0) {
            sb.append("下次问候：").append(f.format(new Date(nextGreet)))
                    .append("（").append((nextGreet - System.currentTimeMillis()) / 1000 / 60).append("分钟后）").append('\n');
        } else {
            sb.append("下次问候：未设定").append('\n');
        }
        sb.append("心情：").append(moodText).append('\n');
        sb.append("好感度：").append(points).append('\n');
        sb.append("心情切换时间：").append(f.format(new Date(prefs.getMoodLastChangeTs() > 0 ? prefs.getMoodLastChangeTs() : System.currentTimeMillis()))).append('\n');
        sb.append("记忆功能：").append(prefs.getMemEnabled() ? "开" : "关").append('\n');
        sb.append("待整理轮次：").append(prefs.getPendingRounds().size()).append('\n');
        sb.append("短期记忆：").append(prefs.getShortMemories().size()).append(" / 7").append('\n');
        sb.append("长期记忆：").append(prefs.getLongMemories().size()).append(" / 15").append('\n');
        sb.append("世界书条目：").append(prefs.getWorldBook().size());
        debugInfo.setText(sb.toString());
    }

    private void setupMenstrual() {
        TextView phaseHint = findViewById(R.id.phaseHint);
        String[] phases = {"月经期", "卵泡期", "排卵期", "黄体期"};
        int[] ids = {R.id.btnPhasePeriod, R.id.btnPhaseFollicular, R.id.btnPhaseOvulation, R.id.btnPhaseLuteal};
        for (int i = 0; i < ids.length; i++) {
            final String ph = phases[i];
            findViewById(ids[i]).setOnClickListener(v -> {
                prefs.setMenstrualOverride(ph);
                if (prefs.getMenstrualBaseTs() <= 0) {
                    prefs.setMenstrualBaseTs(System.currentTimeMillis());
                }
                refreshPhaseHint(phaseHint);
                Toast.makeText(this, "已设为 " + ph, Toast.LENGTH_SHORT).show();
            });
        }
        findViewById(R.id.btnPhaseAuto).setOnClickListener(v -> {
            prefs.setMenstrualOverride("");
            refreshPhaseHint(phaseHint);
            Toast.makeText(this, "已恢复自动", Toast.LENGTH_SHORT).show();
        });
        refreshPhaseHint(phaseHint);
    }

    private void refreshPhaseHint(TextView phaseHint) {
        if (!prefs.getRealismMode()) {
            phaseHint.setText("当前：真实模式未开启");
            return;
        }
        int day = prefs.effectiveMenstrualDay();
        String phase = prefs.menstrualPhaseName(day);
        String ovr = prefs.getMenstrualOverride();
        String mode = (ovr != null && !ovr.isEmpty()) ? "（手动）" : "（自动）";
        phaseHint.setText("当前：第" + day + "天 · " + phase + mode
                + "\n心情：" + prefs.menstrualMood(day)
                + "\n说话：" + prefs.menstrualTalk(day));
    }
}