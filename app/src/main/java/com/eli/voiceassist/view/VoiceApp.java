package com.eli.voiceassist.view;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.eli.voiceassist.mode.ContactInfo;
import com.iflytek.cloud.SpeechUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class VoiceApp extends Application {
    private static final String appID = "5ab85a37";

    @Override
    public void onCreate() {
        SpeechUtility.createUtility(this, "appid=" + appID);
        super.onCreate();
    }
}
