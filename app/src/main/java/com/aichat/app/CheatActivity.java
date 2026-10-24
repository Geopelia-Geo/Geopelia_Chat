package com.aichat.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CheatActivity extends AppCompatActivity {

    private Prefs prefs;
    private TextView curLoveLabel;
    private TextView moodHint;
    private final String[] moodKeys = {"happy", "normal", "sad", "shy", "tired", "angry", "jealous", "upset"};
    private final int[] moodBtnIds = {
            R.id.btnMoodHappy, R.id.btnMoodNormal, R.id.btnMoodSad, R.id.btnMoodShy,
            R.id.btnMoodTired, R.id.btnMoodAngry, R.id.btnMoodJealous, R.id.btnMoodUpset
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheat);
        prefs = new Prefs(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        curLoveLabel = findViewById(R.id.curLoveLabel);
        moodHint = findViewById(R.id.moodHint);

        EditText etLovePoints = findViewById(R.id.etLovePoints);
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

        findViewById(R.id.btnResetLove).setOnClickListener(v -> {
            prefs.setLovePoints(0);
            refresh();
            Toast.makeText(this, "好感度已归零", Toast.LENGTH_SHORT).show();
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

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
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
    }
}
