package com.aichat.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppLogActivity extends AppCompatActivity {

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1C1B1F);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(18, 18, 18, 8);

        Button btnRefresh = new Button(this);
        btnRefresh.setText("刷新");
        btnRefresh.setOnClickListener(v -> load());
        bar.addView(btnRefresh, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button btnClear = new Button(this);
        btnClear.setText("清空");
        btnClear.setOnClickListener(v -> {
            AppLog.clear(this);
            load();
            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(16, 0, 0, 0);
        bar.addView(btnClear, lp);

        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        tv = new TextView(this);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(18, 10, 18, 30);
        tv.setTextColor(0xFFCFD8DC);
        sv.addView(tv);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        tv.setText(AppLog.read(this));
    }
}
