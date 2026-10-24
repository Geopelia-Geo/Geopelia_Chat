package com.aichat.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int REQUEST_PICK_AVATAR = 1002;
    private static final int REQUEST_IMPORT_CHAT = 1003;
    private static final int REQUEST_EXPORT_PERMISSION = 1004;

    private EditText etName, etBaseUrl, etApiKey, etModel, etTemperature, etMaxTokens, etContextCount;
    private com.google.android.material.switchmaterial.SwitchMaterial swPinning;
    private com.google.android.material.switchmaterial.SwitchMaterial swMemory;
    private com.google.android.material.switchmaterial.SwitchMaterial swTrustAll;
    private com.google.android.material.switchmaterial.SwitchMaterial swRealism;
    private android.widget.Spinner spinnerProvider;
    private Prefs prefs;
    
    private String pendingExport = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new Prefs(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etBaseUrl = findViewById(R.id.etBaseUrl);
        etApiKey = findViewById(R.id.etApiKey);
        etModel = findViewById(R.id.etModel);
        etTemperature = findViewById(R.id.etTemperature);
        etMaxTokens = findViewById(R.id.etMaxTokens);
        etContextCount = findViewById(R.id.etContextCount);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnClearChat = findViewById(R.id.btnClearChat);
        MaterialButton btnPickBackground = findViewById(R.id.btnPickBackground);
        MaterialButton btnPickAvatar = findViewById(R.id.btnPickAvatar);
        MaterialButton btnResetAvatar = findViewById(R.id.btnResetAvatar);
        MaterialButton btnResetBackground = findViewById(R.id.btnResetBackground);
        MaterialButton btnExportChat = findViewById(R.id.btnExportChat);
        MaterialButton btnImportChat = findViewById(R.id.btnImportChat);
        android.widget.TextView tvGetKey = findViewById(R.id.tvGetKey);
        android.widget.TextView tvOpenSource = findViewById(R.id.tvOpenSource);
        android.widget.TextView tvContact = findViewById(R.id.tvContact);
        android.widget.TextView tvVersion = findViewById(R.id.tvVersion);
        com.google.android.material.switchmaterial.SwitchMaterial swPinning = findViewById(R.id.swPinning);
        android.widget.Spinner spinnerProvider = findViewById(R.id.spinnerProvider);
        this.swPinning = swPinning;
        swMemory = findViewById(R.id.swMemory);
        swTrustAll = findViewById(R.id.swTrustAll);
        swRealism = findViewById(R.id.swRealism);
        this.spinnerProvider = spinnerProvider;

        com.google.android.material.switchmaterial.SwitchMaterial swGreet = findViewById(R.id.swGreet);
        MaterialButton btnNotifPermission = findViewById(R.id.btnNotifPermission);
        MaterialButton btnBatteryPermission = findViewById(R.id.btnBatteryPermission);
        MaterialButton btnAppDetails = findViewById(R.id.btnAppDetails);
        MaterialButton btnCheat = findViewById(R.id.btnCheat);
        android.widget.Spinner spinnerGreetActivity = findViewById(R.id.spinnerGreetActivity);
        swGreet.setChecked(prefs.isGreetEnabled());
        swGreet.setOnCheckedChangeListener((b, checked) -> {
            prefs.setGreetEnabled(checked);
            if (checked) {
                prefs.setNextGreetTs(0);
                scheduleGreet();
            }
        });
        btnNotifPermission.setOnClickListener(v -> requestNotifPermission());
        btnBatteryPermission.setOnClickListener(v -> requestBatteryPermission());
        btnAppDetails.setOnClickListener(v -> openAppDetails());
        btnCheat.setOnClickListener(v -> openCheatMenu());

        String[] activityNames = {"低调", "中", "高", "自定义"};
        android.widget.ArrayAdapter<String> actAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, activityNames);
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGreetActivity.setAdapter(actAdapter);
        spinnerGreetActivity.setSelection(prefs.getGreetActivity());
        android.widget.EditText etGreetMin = findViewById(R.id.etGreetMin);
        android.widget.EditText etGreetMax = findViewById(R.id.etGreetMax);
        etGreetMin.setText(String.valueOf(prefs.getGreetCustomMin()));
        etGreetMax.setText(String.valueOf(prefs.getGreetCustomMax()));
        final android.view.View llGreetCustom = findViewById(R.id.llGreetCustom);
        llGreetCustom.setVisibility(prefs.getGreetActivity() == 3 ? android.view.View.VISIBLE : android.view.View.GONE);
        spinnerGreetActivity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.setGreetActivity(position);
                prefs.setNextGreetTs(0);
                llGreetCustom.setVisibility(position == 3 ? android.view.View.VISIBLE : android.view.View.GONE);
                scheduleGreet();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        android.widget.ArrayAdapter<String> providerAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ApiClient.PROVIDER_NAMES);
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvider.setAdapter(providerAdapter);
        String savedProvider = prefs.getProvider();
        int pos = 0;
        for (int i = 0; i < ApiClient.PROVIDERS.length; i++) {
            if (ApiClient.PROVIDERS[i].equals(savedProvider)) { pos = i; break; }
        }
        spinnerProvider.setSelection(pos);
        swPinning.setChecked(prefs.getEnablePinning());
        swMemory.setChecked(prefs.getMemEnabled());
        swTrustAll.setChecked(prefs.getTrustAll());
        swRealism.setChecked(prefs.getRealismMode());

        android.widget.Spinner spinnerPromptMode = findViewById(R.id.spinnerPromptMode);
        String[] promptNames = {"Gino皮套模式", "剧情模式"};
        android.widget.ArrayAdapter<String> promptAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, promptNames);
        promptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPromptMode.setAdapter(promptAdapter);
        spinnerPromptMode.setSelection(prefs.getPromptMode());
        spinnerPromptMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.setPromptMode(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        try {
            String ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Geopelia  v" + ver);
        } catch (Exception ignored) {
        }

        tvGetKey.setOnClickListener(v -> openUrl("https://platform.deepseek.com"));
        tvOpenSource.setOnClickListener(v -> openUrl("https://github.com/Geopelia-Geo/Geopelia_Chat"));
        tvContact.setOnClickListener(v -> openUrl("https://space.bilibili.com/5305734"));

        etName.setText(prefs.getDisplayName());
        etBaseUrl.setText(prefs.getBaseUrl());
        etApiKey.setText(prefs.getApiKey());
        etModel.setText(prefs.getModel());
        etTemperature.setText(String.valueOf(prefs.getTemperature()));
        etMaxTokens.setText(prefs.getMaxTokens() > 0 ? String.valueOf(prefs.getMaxTokens()) : "");
        etContextCount.setText(String.valueOf(prefs.getContextCount()));

        btnSave.setOnClickListener(v -> save());
        btnClearChat.setOnClickListener(v -> confirmClearChat());
        btnPickBackground.setOnClickListener(v -> pickBackground());
        btnResetBackground.setOnClickListener(v -> resetBackground());
        btnPickAvatar.setOnClickListener(v -> pickAvatar());
        btnResetAvatar.setOnClickListener(v -> resetAvatar());
        btnExportChat.setOnClickListener(v -> exportChat());
        btnImportChat.setOnClickListener(v -> importChat());

        refreshMemoryStatus();
        findViewById(R.id.btnViewMem).setOnClickListener(v -> showMemoryViewer());

        setupThemeSwatches();
        findViewById(R.id.btnPalette).setOnClickListener(v -> showPaletteDialog());
        applySettingsTheme();
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

    private void setupThemeSwatches() {
        LinearLayout row = (LinearLayout) findViewById(R.id.themeSwatches);
        row.removeAllViews();
        int[] presets = {0xFF06C755, 0xFF2196F3, 0xFF9C27B0, 0xFFFF6B81,
                0xFFFF9800, 0xFF00BCD4, 0xFFF44336};
        int current = prefs.getThemeColor();
        float density = getResources().getDisplayMetrics().density;
        for (int color : presets) {
            android.view.View sw = new android.view.View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (46 * density), (int) (46 * density));
            lp.setMarginEnd((int) (12 * density));
            sw.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(color);
            if (color == current) {
                gd.setStroke((int) (3 * density), 0xFFFFFFFF);
            }
            sw.setBackground(gd);
            sw.setOnClickListener(v -> {
                prefs.setThemeColor(color);
                setupThemeSwatches();
                applySettingsTheme();
                Toast.makeText(this, "主题色已更新", Toast.LENGTH_SHORT).show();
            });
            row.addView(sw);
        }
    }

    private void showPaletteDialog() {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * density), (int) (16 * density), (int) (20 * density), 0);

        android.view.View preview = new android.view.View(this);
        preview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (56 * density)));
        preview.setBackgroundColor(prefs.getThemeColor());

        android.widget.SeekBar hueBar = new android.widget.SeekBar(this);
        hueBar.setMax(359);
        android.widget.SeekBar brightBar = new android.widget.SeekBar(this);
        brightBar.setMax(70);
        brightBar.setProgress(70);

        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(prefs.getThemeColor(), hsv);
        hueBar.setProgress((int) hsv[0]);
        brightBar.setProgress((int) (hsv[2] * 70));

        final java.util.concurrent.atomic.AtomicInteger curColor =
                new java.util.concurrent.atomic.AtomicInteger(prefs.getThemeColor());

        Runnable update = () -> {
            float h = hueBar.getProgress();
            float v = brightBar.getProgress() / 70f;
            if (v < 0.25f) v = 0.25f;
            int c = android.graphics.Color.HSVToColor(new float[]{h, 1f, v});
            curColor.set(c);
            preview.setBackgroundColor(c);
        };
        hueBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                update.run();
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
            }
        });
        brightBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                update.run();
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
            }
        });

        android.widget.TextView hueLabel = new android.widget.TextView(this);
        hueLabel.setText("色相");
        hueLabel.setTextColor(0xFFE4E4E4);
        android.widget.TextView brightLabel = new android.widget.TextView(this);
        brightLabel.setText("亮度");
        brightLabel.setTextColor(0xFFE4E4E4);

        root.addView(preview);
        root.addView(hueLabel);
        root.addView(hueBar);
        root.addView(brightLabel);
        root.addView(brightBar);

        new AlertDialog.Builder(this)
                .setTitle("无极调色盘")
                .setView(root)
                .setPositiveButton("确定", (d, w) -> {
                    prefs.setThemeColor(curColor.get());
                    setupThemeSwatches();
                    applySettingsTheme();
                    Toast.makeText(this, "主题色已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applySettingsTheme() {
        int color = prefs.getThemeColor();
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(color);
        MaterialButton save = findViewById(R.id.btnSave);
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        android.widget.TextView tvGetKey = findViewById(R.id.tvGetKey);
        android.widget.TextView tvOpenSource = findViewById(R.id.tvOpenSource);
        android.widget.TextView tvContact = findViewById(R.id.tvContact);
        tvGetKey.setTextColor(color);
        tvOpenSource.setTextColor(color);
        tvContact.setTextColor(color);
        android.view.ViewGroup root = findViewById(R.id.settingsRoot);
        applyThemeRecursive(root, color);
    }

    private void applyThemeRecursive(android.view.ViewGroup vg, int color) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            android.view.View v = vg.getChildAt(i);
            if (v instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton mb = (com.google.android.material.button.MaterialButton) v;
                if (mb.getId() != R.id.btnSave) {
                    mb.setTextColor(color);
                    mb.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
                }
            } else if (v instanceof com.google.android.material.switchmaterial.SwitchMaterial) {
                com.google.android.material.switchmaterial.SwitchMaterial sw = (com.google.android.material.switchmaterial.SwitchMaterial) v;
                int[][] states = new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}};
                int[] thumbColors = new int[]{color, 0xFF9E9E9E};
                int[] trackColors = new int[]{0x66000000 | (color & 0x00FFFFFF), 0xFF3A3A3A};
                sw.setThumbTintList(new android.content.res.ColorStateList(states, thumbColors));
                sw.setTrackTintList(new android.content.res.ColorStateList(states, trackColors));
            } else if (v instanceof com.google.android.material.textfield.TextInputLayout) {
                com.google.android.material.textfield.TextInputLayout til = (com.google.android.material.textfield.TextInputLayout) v;
                til.setBoxStrokeColor(color);
                til.setBoxStrokeColorStateList(new android.content.res.ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                        new int[]{color, 0xFF555555}));
                til.setHintTextColor(android.content.res.ColorStateList.valueOf(color));
                if (android.os.Build.VERSION.SDK_INT >= 29 && til.getEditText() != null) {
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    gd.setColor(color);
                    float d = getResources().getDisplayMetrics().density;
                    gd.setSize((int) (2 * d), (int) (24 * d));
                    til.getEditText().setTextCursorDrawable(gd);
                }
            }
            if (v instanceof android.view.ViewGroup && !(v instanceof android.widget.EditText)) {
                applyThemeRecursive((android.view.ViewGroup) v, color);
            }
        }
    }

    private void save() {
        String name = etName.getText().toString().trim();
        String baseUrl = etBaseUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String tempStr = etTemperature.getText().toString().trim();
        String maxStr = etMaxTokens.getText().toString().trim();
        String ctxStr = etContextCount.getText().toString().trim();

        if (name.isEmpty()) name = "鸠";
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "请填写接口地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }
        if (model.isEmpty()) model = "deepseek-chat";

        double temp = 1.0;
        try {
            if (!tempStr.isEmpty()) temp = Double.parseDouble(tempStr);
        } catch (NumberFormatException ignored) {
        }
        temp = Math.max(0, Math.min(2, temp));

        int maxTokens = 0;
        try {
            if (!maxStr.isEmpty()) maxTokens = Integer.parseInt(maxStr);
        } catch (NumberFormatException ignored) {
        }
        maxTokens = Math.max(0, Math.min(32768, maxTokens));

        int contextCount = 30;
        try {
            if (!ctxStr.isEmpty()) contextCount = Integer.parseInt(ctxStr);
        } catch (NumberFormatException ignored) {
        }
        contextCount = Math.max(0, Math.min(200, contextCount));

        try {
            int customMin = Integer.parseInt(((android.widget.EditText) findViewById(R.id.etGreetMin)).getText().toString().trim());
            int customMax = Integer.parseInt(((android.widget.EditText) findViewById(R.id.etGreetMax)).getText().toString().trim());
            prefs.setGreetCustomMin(customMin);
            prefs.setGreetCustomMax(Math.max(customMin, customMax));
        } catch (Exception ignored) {
        }

        prefs.setDisplayName(name);
        prefs.save(baseUrl, apiKey, model, temp, maxTokens, contextCount);
        prefs.setEnablePinning(swPinning.isChecked());
        prefs.setMemEnabled(swMemory.isChecked());
        prefs.setTrustAll(swTrustAll.isChecked());
        prefs.setRealismMode(swRealism.isChecked());
        int p = spinnerProvider.getSelectedItemPosition();
        if (p >= 0 && p < ApiClient.PROVIDERS.length) {
            prefs.setProvider(ApiClient.PROVIDERS[p]);
        }
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        prefs.setNextGreetTs(0);
        scheduleGreet();
        finish();
    }

    private void scheduleGreet() {
        try {
            Intent svc = new Intent(this, GreetService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        } catch (Exception ignored) {
        }
    }

    private void showMemoryViewer() {
        Prefs pp = new Prefs(this);
        StringBuilder sb = new StringBuilder();
        sb.append("【待整理轮次】").append(pp.getPendingRounds().size()).append("\n\n");
        sb.append("【短期记忆】").append(pp.getShortMemories().size()).append(" / 7\n");
        for (int i = 0; i < pp.getShortMemories().size(); i++) {
            sb.append(" ").append(i + 1).append(". ").append(pp.getShortMemories().get(i)).append("\n");
        }
        sb.append("\n【长期记忆】").append(pp.getLongMemories().size()).append(" / 15\n");
        for (int i = 0; i < pp.getLongMemories().size(); i++) {
            sb.append(" ").append(i + 1).append(". ").append(pp.getLongMemories().get(i)).append("\n");
        }
        sb.append("\n【世界书】").append(pp.getWorldBook().size()).append(" 条\n");
        for (int i = 0; i < pp.getWorldBook().size(); i++) {
            sb.append(" ").append(i + 1).append(". ").append(pp.getWorldBook().get(i)).append("\n");
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(sb.toString());
        tv.setTextSize(13);
        tv.setLineSpacing(4, 1.2f);
        tv.setTextColor(0xFFCCCCCC);
        tv.setPadding(48, 32, 48, 32);
        sv.addView(tv);
        new android.app.AlertDialog.Builder(this)
                .setTitle("记忆管理（共 " + pp.getShortMemories().size() + " 短期 / "
                        + pp.getLongMemories().size() + " 长期 / "
                        + pp.getWorldBook().size() + " 世界书）")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .setNeutralButton("清空全部记忆", (d, w) -> new android.app.AlertDialog.Builder(this)
                        .setTitle("确认清空")
                        .setMessage("确定要清空所有记忆吗？此操作不可恢复。")
                        .setPositiveButton("确定清空", (d2, w2) -> {
                            pp.clearShortMemories();
                            pp.clearLongMemories();
                            pp.clearWorldBook();
                            pp.clearPendingRounds();
                            Toast.makeText(this, "全部记忆已清空", Toast.LENGTH_SHORT).show();
                            refreshMemoryStatus();
                        })
                        .setNegativeButton("取消", null)
                        .show())
                .show();
    }

    private void refreshMemoryStatus() {
        Prefs pp = new Prefs(this);
        StringBuilder sb = new StringBuilder();
        sb.append("待整理轮次：").append(pp.getPendingRounds().size()).append("\n");
        sb.append("短期记忆：").append(pp.getShortMemories().size()).append(" / 7\n");
        sb.append("长期记忆：").append(pp.getLongMemories().size()).append(" / 15\n");
        sb.append("世界书条目：").append(pp.getWorldBook().size());
        android.widget.TextView tv = findViewById(R.id.tvMemoryStatus);
        if (tv != null) tv.setText(sb.toString());
    }

    private void exportChat() {
        List<Message> msgs = prefs.loadMessages();
        StringBuilder sb = new StringBuilder();
        sb.append("# Geopelia 聊天记录\n\n");
        sb.append("- 导出时间：")
          .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date()))
          .append("\n");
        sb.append("- 消息数：").append(msgs.size()).append("\n\n");
        for (Message m : msgs) {
            String who = Message.ROLE_USER.equals(m.role) ? "我" : "鸠";
            String t = m.time != null ? m.time : "";
            sb.append("## ").append(who).append(" · ").append(t).append("\n");
            if (m.hasImage()) {
                sb.append("[图片]");
                if (m.content != null && !m.content.isEmpty()) sb.append("\n").append(m.content);
            } else {
                sb.append(m.content);
            }
            sb.append("\n\n");
        }
        final String text = sb.toString();

        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Geopelia.md");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Toast.makeText(this, "导出失败：无法创建文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(text.getBytes(StandardCharsets.UTF_8));
                os.close();
                Toast.makeText(this, "已导出到 下载/Geopelia.md", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingExport = text;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXPORT_PERMISSION);
                return;
            }
            writeExportLegacy(text);
        }
    }

    private void writeExportLegacy(String text) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "Geopelia.md");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Toast.makeText(this, "已导出到 下载/Geopelia.md", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXPORT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && pendingExport != null) {
                writeExportLegacy(pendingExport);
                pendingExport = null;
            } else {
                pendingExport = null;
                Toast.makeText(this, "需要存储权限才能导出", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void importChat() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/markdown", "text/plain", "*/*"});
        startActivityForResult(intent, REQUEST_IMPORT_CHAT);
    }

    private List<Message> parseMarkdown(String content) {
        List<Message> list = new ArrayList<>();
        String[] lines = content.split("\n");
        String curRole = null;
        String curTime = null;
        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (curRole != null && buf.length() > 0) {
                    list.add(new Message(curRole, buf.toString().trim(), curTime));
                }
                String head = line.substring(3).trim();
                int idx = head.indexOf('·');
                if (idx > 0) {
                    String who = head.substring(0, idx).trim();
                    curRole = "鸠".equals(who) ? Message.ROLE_ASSISTANT : Message.ROLE_USER;
                    String t = head.substring(idx + 1).trim();
                    curTime = t.isEmpty() ? null : t;
                } else {
                    curRole = Message.ROLE_ASSISTANT;
                    curTime = null;
                }
                buf = new StringBuilder();
            } else {
                buf.append(line).append('\n');
            }
        }
        if (curRole != null && buf.length() > 0) {
            list.add(new Message(curRole, buf.toString().trim(), curTime));
        }
        return list;
    }

    private void doImport(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            r.close();
            final List<Message> parsed = parseMarkdown(sb.toString());
            if (parsed.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "没有解析到聊天记录", Toast.LENGTH_SHORT).show());
                return;
            }
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("导入聊天记录")
                    .setMessage("解析到 " + parsed.size() + " 条消息。导入后将替换当前聊天记录，确定继续？")
                    .setPositiveButton("导入", (d, w) -> {
                        prefs.saveMessages(parsed);
                        Toast.makeText(this, "已导入 " + parsed.size() + " 条消息", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show());
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void pickBackground() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "选择背景图"), REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相册", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickAvatar() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "选择我的头像"), REQUEST_PICK_AVATAR);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相册", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetAvatar() {
        AvatarUtil.delete(this);
        prefs.setAvatarPath(null);
        Toast.makeText(this, "已恢复默认头像", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_IMPORT_CHAT) {
            doImport(uri);
            return;
        }

        final int request = requestCode;
        new Thread(() -> {
            if (request == REQUEST_PICK_IMAGE) {
                final String path = BackgroundUtil.saveFromUri(this, uri);
                runOnUiThread(() -> {
                    if (path != null) {
                        prefs.setBackgroundPath(path);
                        Toast.makeText(this, "背景图已设置", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "背景图设置失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (request == REQUEST_PICK_AVATAR) {
                final String path = AvatarUtil.saveFromUri(this, uri);
                runOnUiThread(() -> {
                    if (path != null) {
                        prefs.setAvatarPath(path);
                        Toast.makeText(this, "我的头像已设置", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "头像设置失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void resetBackground() {
        BackgroundUtil.delete(this);
        prefs.setBackgroundPath(null);
        Toast.makeText(this, "已恢复默认背景", Toast.LENGTH_SHORT).show();
    }

    private void confirmClearChat() {
        new AlertDialog.Builder(this)
                .setTitle("清空聊天记录")
                .setMessage("确定要删除全部聊天记录吗？此操作不可恢复。")
                .setPositiveButton("确定清空", (d, w) -> {
                    prefs.clearMessages();
                    Toast.makeText(this, "聊天记录已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开链接：" + url, Toast.LENGTH_SHORT).show();
        }
    }

    private void openCheatMenu() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("作弊菜单");
        b.setMessage("作弊菜单仅供测试使用。\n\nGeopelia_Geo 非常不建议你使用它——因为我们认为鸠不是一个 AI 工具，而是一个真正的陪伴者，这也是 Geopelia_Geo 一直在坚持做的事。\n\n不过，如果你执意要用，我们也拦不住你。");
        b.setPositiveButton("确定", (d, w) -> confirmCheat(1));
        b.setNegativeButton("取消", null);
        b.show();
    }

    private void confirmCheat(int step) {
        if (step == 1) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("你确定吗");
            b.setMessage("确定要进入作弊菜单吗？");
            b.setPositiveButton("确定", (d, w) -> confirmCheat(2));
            b.setNegativeButton("取消", null);
            b.show();
        } else {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("你真的确定吗");
            b.setMessage("最后一次机会，真的要进去吗？");
            b.setPositiveButton("确定", (d, w) -> startActivity(new Intent(this, CheatActivity.class)));
            b.setNegativeButton("取消", null);
            b.show();
        }
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
            } else {
                Toast.makeText(this, "通知权限已开启", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "当前系统无需申请通知权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestBatteryPermission() {
        try {
            Intent i = new Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e2) {
                Toast.makeText(this, "请到系统设置中手动开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppDetails() {
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "请到系统设置中手动允许自启动", Toast.LENGTH_SHORT).show();
        }
    }
}
