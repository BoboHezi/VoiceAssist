package com.eli.voiceassist.mode;

import com.google.gson.Gson;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 */

public final class SettingParams {

    private String recognizerLanguage;
    private String recognizerAccent;
    private int recognizerBOS;
    private int recognizerEOS;

    private boolean speakEnable;
    private String voiceName;
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
}
