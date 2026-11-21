package vn.hieumedia.bidvmoneyspeaker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status, volumeLabel;
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("speaker", MODE_PRIVATE);
        buildUi();
    }

    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(30,40,35));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, 10, 0, 10);
        return t;
    }

    private void addToggle(LinearLayout root, String label, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(label); sw.setTextSize(15); sw.setPadding(0, 7, 0, 7);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(key, checked).apply());
        root.addView(sw);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(247,250,248));

        TextView title = text("BIDV MONEY SPEAKER", 23, true);
        title.setTextColor(Color.rgb(13,91,53)); root.addView(title);
        root.addView(text("Loa thông báo biến động số dư · Bản hoàn chỉnh", 14, false));
        status = text("Đang kiểm tra quyền đọc thông báo…", 14, true); root.addView(status);

        addToggle(root, "Bật đọc giao dịch tự động (tiền vào và tiền ra)", "enabled", true);
        root.addView(text("NỘI DUNG CẦN ĐỌC", 17, true));
        root.addView(text("Bạn có thể bật/tắt riêng từng thông tin:", 13, false));
        addToggle(root, "Biến động số dư – số tiền giao dịch", "read_amount", true);
        addToggle(root, "Tên người chuyển / người nhận", "read_name", true);
        addToggle(root, "Số dư tài khoản sau giao dịch", "read_balance", true);
        addToggle(root, "Nội dung chuyển khoản", "read_content", true);

        root.addView(text("ÂM LƯỢNG GIỌNG ĐỌC", 16, true));
        SeekBar seek = new SeekBar(this); seek.setMax(100);
        seek.setProgress(prefs.getInt("volume", 90)); root.addView(seek);
        volumeLabel = text("Âm lượng: " + seek.getProgress() + "%", 13, false); root.addView(volumeLabel);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                volumeLabel.setText("Âm lượng: " + p + "%");
                if (user) prefs.edit().putInt("volume", p).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        Button permission = new Button(this); permission.setText("CẤP QUYỀN ĐỌC THÔNG BÁO");
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(permission);
        Button test = new Button(this); test.setText("NGHE THỬ GIỌNG ĐỌC NỮ");
        test.setOnClickListener(v -> BIDVNotificationListener.speakTest(this)); root.addView(test);
        Button sample = new Button(this); sample.setText("THỬ ĐỌC ĐỦ 4 TRƯỜNG");
        sample.setOnClickListener(v -> BIDVNotificationListener.speakSample(this)); root.addView(sample);

        root.addView(text("Lưu ý: Giọng nữ phụ thuộc giọng tiếng Việt được cài trên điện thoại. Tên người chuyển, số dư và nội dung chỉ đọc chính xác khi thông báo BIDV có cung cấp các trường này. Không cần mật khẩu, PIN hoặc OTP.", 12, false));
        setContentView(root);
    }

    @Override protected void onResume() { super.onResume(); updateStatus(); }
    private void updateStatus() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean ok = enabledListeners != null && enabledListeners.contains(getPackageName());
        if (status != null) status.setText(ok ? "● Đã có quyền đọc thông báo" : "● Chưa cấp quyền đọc thông báo");
    }
}
