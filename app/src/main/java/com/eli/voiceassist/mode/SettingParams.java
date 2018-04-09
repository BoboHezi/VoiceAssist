package com.eli.voiceassist.mode;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 */

public final class SettingParams {

    private static Map<String, String> keys = new HashMap<>();

    static {
        keys.put("普通话", "mandarin");
        keys.put("粤语", "cantonese");
        keys.put("四川话", "lmz");
        keys.put("小燕(中文女声)", "xiaoyan");
        keys.put("小宇(中文男声)", "xiaoyu");
        keys.put("凯瑟琳(英文女声)", "catherine");
        keys.put("亨利(英文男声)", "henry");
        keys.put("小梅(粤语女声)", "vixm");
        keys.put("晓琳(台湾话女声)", "xiaolin");
    }

    private String recognizerLanguage;
    private String recognizerAccent;
    private String accentDisplay;
    private int recognizerBOS;
    private int recognizerEOS;

    private boolean speakEnable;
    private String voiceName;
    private String nameDisplay;
    private int voiceSpeed;
    private int voiceVolume;

    @Override
    public String toString() {
        Gson gson = new Gson();
        String value = gson.toJson(this);
        return value;
    }

    private void parseJson(String value) {
        try {
            Gson gson = new Gson();
            gson.fromJson(value, SettingParams.class);
        } catch (Exception e) {
        }
    }

    public String getRecognizerAccent() {
        return recognizerAccent;
    }

    public void setRecognizerAccent(String recognizerAccent) {
        this.recognizerAccent = recognizerAccent;
        this.accentDisplay = findKey(recognizerAccent);
    }

    public int getRecognizerBOS() {
        return recognizerBOS;
    }

    public void setRecognizerBOS(int recognizerBOS) {
        this.recognizerBOS = recognizerBOS;
    }

    public int getRecognizerEOS() {
        return recognizerEOS;
    }

    public void setRecognizerEOS(int recognizerEOS) {
        this.recognizerEOS = recognizerEOS;
    }

    public boolean isSpeakEnable() {
        return speakEnable;
    }

    public void setSpeakEnable(boolean speakEnable) {
        this.speakEnable = speakEnable;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
        this.nameDisplay = findKey(voiceName);
    }

    public int getVoiceSpeed() {
        return voiceSpeed;
    }

    public void setVoiceSpeed(int voiceSpeed) {
        this.voiceSpeed = voiceSpeed;
    }

    public int getVoiceVolume() {
        return voiceVolume;
    }

    public void setVoiceVolume(int voiceVolume) {
        this.voiceVolume = voiceVolume;
    }

    public String getRecognizerLanguage() {
        return recognizerLanguage;
    }

    public void setRecognizerLanguage(String recognizerLanguage) {
        this.recognizerLanguage = recognizerLanguage;
    }

    public String getAccentDisplay() {
        return accentDisplay;
    }

    public void setAccentDisplay(String accentDisplay) {
        this.accentDisplay = accentDisplay;
        this.recognizerAccent = keys.get(accentDisplay);
    }

    public String getNameDisplay() {
        return nameDisplay;
    }

    public void setNameDisplay(String nameDisplay) {
        this.nameDisplay = nameDisplay;
        this.voiceName = keys.get(nameDisplay);
    }

    private String findKey(String value) {
        String key = "";
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            if (entry.getValue().equals(value)) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }
}
