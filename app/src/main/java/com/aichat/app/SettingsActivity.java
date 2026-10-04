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

        prefs.setDisplayName(name);
        prefs.save(baseUrl, apiKey, model, temp, maxTokens, contextCount);
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
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
            sb.append(m.content).append("\n\n");
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
}
