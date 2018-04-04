package com.eli.voiceassist;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;


import com.google.gson.Gson;
import com.iflytek.aiui.AIUIEvent;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Random;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class Util {

    private static final String TAG = "elifli";

    public static String getAIUIParams(Context context) {
        String value = "";
        AssetManager manager = context.getResources().getAssets();
        try {
            InputStream ins = manager.open("cfg/aiui_phone.cfg");
            byte buffer[] = new byte[ins.available()];
            ins.read(buffer);
            value = new String(buffer);
        } catch (Exception e) {
        }
        return value;
    }

    public static String parseVoice(String voiceResult) {
        Gson gson = new Gson();
        VoiceMessage message = gson.fromJson(voiceResult, VoiceMessage.class);

        return message.getWord();
    }

    public static JSONObject getSemanticResult(AIUIEvent event) {
        try {
            JSONObject data = new JSONObject(event.info).getJSONArray("data").getJSONObject(0);
            JSONObject params = data.getJSONObject("params");
            JSONObject content = data.getJSONArray("content").getJSONObject(0);

            if (content.has("cnt_id")) {
                String cnt_id = content.getString("cnt_id");
                JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));
                String sub = params.optString("sub");
                if (sub.equals("nlp")) {
                    return cntJson.optJSONObject("intent");
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String randomString(int length) {
        if (length <= 0)
            return "";

        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i ++) {
            char c = (char) (random.nextInt(92) + 33);
            sb.append(c);
        }
        return sb.toString();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static void playSound(Context context, int rawID) {
        final SoundPool pool;
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            AudioAttributes.Builder attrBuild = new AudioAttributes.Builder();
            attrBuild.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuild.build());
            pool = builder.build();
        } else {
            pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        }

        pool.load(context, rawID, 1);
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                pool.play(1, 1, 1, 0, 0, 1);
            }
        });
    }
}
