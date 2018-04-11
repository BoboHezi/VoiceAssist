package com.eli.voiceassist.activity;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class VoiceApp extends Application {
    private static final String appID = "5ab85a37";

    @Override
    public void onCreate() {
        //initial speech
        SpeechUtility.createUtility(this, "appid=" + appID);
        super.onCreate();
    }
}
