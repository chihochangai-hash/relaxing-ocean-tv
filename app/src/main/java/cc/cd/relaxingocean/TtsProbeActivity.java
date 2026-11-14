package cc.cd.relaxingocean;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import java.util.Locale;
import java.util.Set;

/** One-shot check that this TV's built-in speech engine can start a utterance. Not a launcher icon. */
public class TtsProbeActivity extends Activity {
    static final String TAG = "TtsProbe";
    private static final String ENGINE = "com.google.android.tts";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private AudioManager audio;
    private boolean heardStart;
    private boolean finished;
    private int ticks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView label = new TextView(this);
        label.setText("正在試內建語音");
        label.setTextColor(Color.WHITE);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(28);
        setContentView(label);
        audio = getSystemService(AudioManager.class);
        Log.i(TAG, "musicVol=" + audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                + "/" + audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        tts = new TextToSpeech(this, status -> handler.post(() -> onReady(status)), ENGINE);
        handler.postDelayed(this::giveUp, 20000);
    }

    private void onReady(int status) {
        Log.i(TAG, "init status=" + status + " default=" + tts.getDefaultEngine());
        for (TextToSpeech.EngineInfo info : tts.getEngines()) {
            Log.i(TAG, "engine " + info.name);
        }
        if (status != TextToSpeech.SUCCESS) {
            finishSoon();
            return;
        }
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        logVoices();
        Locale locale = pickLocale();
        String line = "zh".equals(locale.getLanguage())
                ? "你好，這是電視的內建語音。"
                : "Hello. This is the television built-in voice.";
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                heardStart = true;
                Log.i(TAG, "onStart " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                Log.i(TAG, "onDone " + utteranceId);
                finishSoon();
            }

            @Override
            public void onError(String utteranceId) {
                onError(utteranceId, TextToSpeech.ERROR);
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                Log.i(TAG, "onError " + utteranceId + " code=" + errorCode);
                finishSoon();
            }
        });
        int speak = tts.speak(line, TextToSpeech.QUEUE_FLUSH, null, "probe");
        Log.i(TAG, "speak result=" + speak + " locale=" + locale + " text=" + line);
        handler.post(this::watch);
    }

    private Locale pickLocale() {
        Locale[] locales = { Locale.TAIWAN, Locale.CHINESE, Locale.US };
        for (Locale locale : locales) {
            int result = tts.setLanguage(locale);
            Log.i(TAG, "setLanguage " + locale.toLanguageTag() + " -> " + result);
            if (result >= TextToSpeech.LANG_AVAILABLE) return locale;
        }
        return Locale.US;
    }

    private void logVoices() {
        if (tts.getVoices() == null) {
            Log.i(TAG, "voices=null");
            return;
        }
        int shown = 0;
        for (Voice voice : tts.getVoices()) {
            String lang = voice.getLocale() == null ? "" : voice.getLocale().getLanguage();
            if (!"zh".equals(lang) && !"en".equals(lang)) continue;
            if (shown >= 30) break;
            shown++;
            Set<String> features = voice.getFeatures();
            boolean installed = features == null
                    || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
            Log.i(TAG, "voice " + voice.getName()
                    + " locale=" + voice.getLocale()
                    + " net=" + voice.isNetworkConnectionRequired()
                    + " installed=" + installed);
        }
        Log.i(TAG, "voicesListed=" + shown);
    }

    private void watch() {
        if (finished) return;
        ticks++;
        Log.i(TAG, "tick " + ticks
                + " musicActive=" + audio.isMusicActive()
                + " started=" + heardStart);
        if (ticks < 20) handler.postDelayed(this::watch, 500);
    }

    private void giveUp() {
        if (finished) return;
        Log.i(TAG, "timeout started=" + heardStart);
        finishSoon();
    }

    private void finishSoon() {
        if (finished) return;
        finished = true;
        handler.postDelayed(this::finish, 400);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
