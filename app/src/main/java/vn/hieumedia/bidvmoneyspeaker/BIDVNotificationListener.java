package vn.hieumedia.bidvmoneyspeaker;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BIDVNotificationListener extends NotificationListenerService {
    private static TextToSpeech tts;
    private static volatile boolean ttsReady = false;
    private static Context appContext;
    private static final Pattern MONEY = Pattern.compile("(?i)([+\\-]?\\s*[0-9][0-9., ]*)\\s*(đ|đồng|vnd|vnđ)");
    private static final Pattern BALANCE = Pattern.compile("(?i)(?:so du|số dư|balance)\\s*[:：]?\\s*([0-9][0-9., ]*)\\s*(?:đ|đồng|vnd|vnđ)?");
    private static final Pattern NAME = Pattern.compile("(?i)(?:nguoi chuyen|người chuyển|nguoi gui|người gửi|nguoi nhan|người nhận|from|to)\\s*[:：]?\\s*([^.;,\\n]+)");
    private static final Pattern CONTENT = Pattern.compile("(?i)(?:noi dung|nội dung|loi nhan|lời nhắn|content|message)\\s*[:：]?\\s*([^\\n]+)");
    private static String lastFingerprint = "";
    private static long lastAt = 0;

    @Override public void onCreate() { super.onCreate(); appContext = getApplicationContext(); initTts(appContext); }

    private static synchronized void initTts(Context c) {
        if (tts != null) return;
        tts = new TextToSpeech(c, result -> {
            if (result != TextToSpeech.SUCCESS) return;
            int lang = tts.setLanguage(new Locale("vi", "VN"));
            tts.setSpeechRate(0.92f);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                Set<Voice> voices = tts.getVoices();
                if (voices != null) {
                    Voice fallback = null;
                    for (Voice v : voices) {
                        if (v.getLocale() != null && "vi".equalsIgnoreCase(v.getLocale().getLanguage())) {
                            if (fallback == null) fallback = v;
                            String name = v.getName().toLowerCase(Locale.ROOT);
                            if (name.contains("female") || name.contains("woman") || name.contains("mai") || name.contains("linh")) { fallback = v; break; }
                        }
                    }
                    if (fallback != null) tts.setVoice(fallback);
                }
            }
            ttsReady = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED;
        });
    }

    public static void speakTest(Context c) {
        initTts(c.getApplicationContext());
        speak("Xin chào. Đây là loa thông báo giao dịch BIDV. Hệ thống đã sẵn sàng.", c.getApplicationContext());
    }
    public static void speakSample(Context c) {
        SharedPreferences p = c.getSharedPreferences("speaker", Context.MODE_PRIVATE);
        String sample = buildSpeech("Tiền vào cộng 500.000 đồng. Người chuyển: Nguyễn Văn A. Số dư: 12.500.000 đồng. Nội dung: Thanh toán đơn hàng.", p);
        speak(sample, c.getApplicationContext());
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        SharedPreferences prefs = getSharedPreferences("speaker", MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", true)) return;
        String pkg = sbn.getPackageName() == null ? "" : sbn.getPackageName().toLowerCase(Locale.ROOT);
        String app = "";
        try { app = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString().toLowerCase(Locale.ROOT); } catch (Exception ignored) {}
        if (!(pkg.contains("bidv") || app.contains("bidv"))) return;

        Notification n = sbn.getNotification(); Bundle e = n.extras;
        StringBuilder raw = new StringBuilder();
        append(raw, e.getCharSequence(Notification.EXTRA_TITLE));
        append(raw, e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        append(raw, e.getCharSequence(Notification.EXTRA_TEXT));
        CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) for (CharSequence line : lines) append(raw, line);
        String message = raw.toString().trim();
        if (message.length() < 5 || !looksLikeTransaction(message)) return;

        long now = System.currentTimeMillis();
        String fp = pkg + "|" + message;
        synchronized (BIDVNotificationListener.class) {
            if (fp.equals(lastFingerprint) && now - lastAt < 90000) return;
            lastFingerprint = fp; lastAt = now;
        }
        String spoken = buildSpeech(message, prefs);
        if (!spoken.trim().isEmpty()) speak(spoken, this);
    }

    private static void append(StringBuilder b, CharSequence s) {
        if (s != null && s.length() > 0) { if (b.length() > 0) b.append(". "); b.append(s); }
    }
    private static boolean looksLikeTransaction(String s) {
        String x = normalize(s);
        return MONEY.matcher(s).find() || x.contains("ghi no") || x.contains("ghi co") || x.contains("chuyen tien") || x.contains("nhan tien") || x.contains("bien dong so du") || x.contains("thanh toan") || x.contains("transaction");
    }
    private static String buildSpeech(String s, SharedPreferences p) {
        String x = normalize(s);
        boolean incoming = x.contains("nhan tien") || x.contains("ghi co") || x.contains("tien vao") || x.contains("credit") || x.contains("cong ");
        boolean outgoing = x.contains("ghi no") || x.contains("tien ra") || x.contains("thanh toan") || x.contains("debit") || x.contains("tru ");
        StringBuilder out = new StringBuilder();
        if (incoming) out.append("Giao dịch tiền vào. ");
        else if (outgoing) out.append("Giao dịch tiền ra. ");
        else out.append("Thông báo giao dịch BIDV. ");

        if (p.getBoolean("read_amount", true)) {
            Matcher m = MONEY.matcher(s);
            if (m.find()) out.append("Biến động số dư: ").append(normalizeAmount(m.group(1))).append(" đồng. ");
        }
        if (p.getBoolean("read_name", true)) {
            String name = capture(NAME, s);
            if (!name.isEmpty()) out.append("Người chuyển hoặc người nhận: ").append(clean(name)).append(". ");
        }
        if (p.getBoolean("read_balance", true)) {
            Matcher b = BALANCE.matcher(s);
            if (b.find()) out.append("Số dư sau giao dịch: ").append(normalizeAmount(b.group(1))).append(" đồng. ");
        }
        if (p.getBoolean("read_content", true)) {
            String content = capture(CONTENT, s);
            if (!content.isEmpty()) out.append("Nội dung chuyển khoản: ").append(clean(content)).append(". ");
        }
        // If the selected fields are not explicitly labelled in BIDV's notification format,
        // retain the original message so useful transaction details are not silently lost.
        if (out.toString().trim().equals("Giao dịch tiền vào.") || out.toString().trim().equals("Giao dịch tiền ra.") || out.toString().trim().equals("Thông báo giao dịch BIDV.")) {
            String fallback = clean(s);
            if (!fallback.isEmpty()) out.append(fallback);
        }
        return out.toString().trim();
    }
    private static String capture(Pattern pattern, String source) {
        Matcher m = pattern.matcher(source);
        return m.find() ? m.group(1).trim() : "";
    }
    private static String clean(String s) {
        return s.replaceAll("(?i)https?://\\S+", " ").replaceAll("\\s+", " ").trim();
    }
    private static String normalizeAmount(String s) {
        return s.trim().replaceAll("\\s+", "");
    }
    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
    private static void speak(String message, Context c) {
        initTts(c);
        if (!ttsReady || tts == null) {
            android.widget.Toast.makeText(c, "Chưa có dữ liệu giọng đọc tiếng Việt. Hãy cài giọng tiếng Việt trong cài đặt Text-to-Speech.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences p = c.getSharedPreferences("speaker", Context.MODE_PRIVATE);
        float vol = Math.max(0f, Math.min(1f, p.getInt("volume", 90) / 100f));
        Bundle params = new Bundle(); params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol);
        if (android.os.Build.VERSION.SDK_INT >= 21) tts.speak(message, TextToSpeech.QUEUE_ADD, params, "bidv-" + System.currentTimeMillis());
        else tts.speak(message, TextToSpeech.QUEUE_ADD, null);
    }
    @Override public void onDestroy() { super.onDestroy(); }
}
