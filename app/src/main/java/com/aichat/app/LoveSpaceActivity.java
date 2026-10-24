package com.aichat.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LoveSpaceActivity extends AppCompatActivity {

    private static final int[] LOVE_LEVELS = {0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000, 5200};

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_love_space);
        prefs = new Prefs(this);

        if (prefs.getLoveStartDate() == null) {
            prefs.setLoveStartDate(today());
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ImageView myAvatar = findViewById(R.id.myAvatar);
        ImageView aiAvatar = findViewById(R.id.aiAvatar);
        int px = (int) (58 * getResources().getDisplayMetrics().density);
        Bitmap aiBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ai_avatar);
        aiAvatar.setImageBitmap(AvatarUtil.toCircular(aiBmp, px));
        String avatarPath = prefs.getAvatarPath();
        Bitmap mine = null;
        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                mine = BitmapFactory.decodeFile(avatarPath);
            } catch (Exception ignored) {
            }
        }
        if (mine == null) {
            mine = BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar);
        }
        myAvatar.setImageBitmap(AvatarUtil.toCircular(mine, px));

        TextView myName = findViewById(R.id.myName);
        TextView aiName = findViewById(R.id.aiName);
        myName.setText("我");
        aiName.setText(prefs.getDisplayName());

        TextView loveDays = findViewById(R.id.loveDays);
        loveDays.setText("在一起 第 " + daysTogether() + " 天");

        refreshLoveCard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLoveCard();
    }

    private void refreshLoveCard() {
        int points = prefs.getLovePoints();
        int level = levelFor(points);
        int floor = LOVE_LEVELS[level];
        int next = level + 1 < LOVE_LEVELS.length ? LOVE_LEVELS[level + 1] : Integer.MAX_VALUE;

        TextView levelLabel = findViewById(R.id.levelLabel);
        TextView pointsLabel = findViewById(R.id.pointsLabel);
        ProgressBar progress = findViewById(R.id.loveProgress);
        TextView nextHint = findViewById(R.id.nextLevelHint);
        TextView moodLabel = findViewById(R.id.moodLabel);

        levelLabel.setText("LV." + (level + 1));
        if (next == Integer.MAX_VALUE) {
            pointsLabel.setText(points + " 点");
            progress.setProgress(100);
            nextHint.setText("已达最高等级");
        } else {
            pointsLabel.setText(points + " / " + next);
            progress.setMax(next - floor);
            progress.setProgress(Math.max(0, points - floor));
            nextHint.setText("再获得 " + (next - points) + " 点进入 LV." + (level + 2));
        }

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
        moodLabel.setText("鸠当前心情：" + moodText);

        if (prefs.getRealismMode()) {
            int day = prefs.effectiveMenstrualDay();
            String phaseName = prefs.menstrualPhaseName(day);
            String ovr = prefs.getMenstrualOverride();
            findViewById(R.id.periodPanel).setVisibility(android.view.View.VISIBLE);
            ((TextView) findViewById(R.id.periodLabel)).setText(
                    "第 " + day + " 天 · " + phaseName + (ovr != null && !ovr.isEmpty() ? "（手动）" : ""));
            ((TextView) findViewById(R.id.periodMoodLabel)).setText("心情：" + prefs.menstrualMood(day));
            ((TextView) findViewById(R.id.periodTalkLabel)).setText("说话：" + prefs.menstrualTalk(day));

            // 阶段倒计时：本阶段剩余天数 + 距下次经期开始天数
            int endDay;
            if (day <= 5) endDay = 5;
            else if (day <= 12) endDay = 12;
            else if (day <= 15) endDay = 15;
            else if (day <= 19) endDay = 19;
            else if (day <= 24) endDay = 24;
            else endDay = 28;
            int stageLeft = Math.max(1, endDay - day + 1);
            int nextPeriod = 29 - day;   // day=28 → 明天就来；day=1 → 28 天后
            ((TextView) findViewById(R.id.periodNextLabel)).setText(
                    "本阶段还剩约 " + stageLeft + " 天 · 距下次经期开始约 " + nextPeriod + " 天");
        }

        int rating = prefs.getUserRating();
        String ratingText;
        String ratingHint;
        if (rating >= 90) { ratingText = "满分男友"; ratingHint = "她觉得你无可挑剔"; }
        else if (rating >= 75) { ratingText = "非常体贴"; ratingHint = "她很满意你的表现"; }
        else if (rating >= 60) { ratingText = "还算合格"; ratingHint = "继续保持，多关心她"; }
        else if (rating >= 40) { ratingText = "需要加油"; ratingHint = "她觉得你可以做得更好"; }
        else if (rating >= 20) { ratingText = "有点过分"; ratingHint = "她最近对你有些失望"; }
        else { ratingText = "不及格"; ratingHint = "她觉得你需要好好反省"; }
        findViewById(R.id.ratingPanel).setVisibility(android.view.View.VISIBLE);
        ((TextView) findViewById(R.id.ratingLabel)).setText(rating + " / 100（" + ratingText + "）");
        ((TextView) findViewById(R.id.ratingHintLabel)).setText(ratingHint);
    }

    private int levelFor(int points) {
        int lv = 0;
        for (int i = 0; i < LOVE_LEVELS.length; i++) {
            if (points >= LOVE_LEVELS[i]) lv = i;
        }
        return lv;
    }

    private int daysTogether() {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            Date start = f.parse(prefs.getLoveStartDate());
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Date today = c.getTime();
            long diff = today.getTime() - start.getTime();
            return (int) (diff / 86400000L) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }
}
