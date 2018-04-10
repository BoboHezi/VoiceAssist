package com.eli.voiceassist.mode;

import com.eli.voiceassist.entity.VoiceEntity;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 */

public final class SettingParams {

    private static String selectAccent[] = VoiceEntity.getSelectAccent();
    private static String selectAccentDisplay[] = VoiceEntity.getSelectAccentDisplay();
    private static String selectName[] = VoiceEntity.getSelectSpeaker();
    private static String selectNameDisplay[] = VoiceEntity.getSelectSpeakerDisplay();
    private static Map<String, String> keys = new HashMap<>();

    static {
        for (int index = 0; index < selectAccent.length; index++) {
            keys.put(selectAccent[index], selectAccentDisplay[index]);
        }
        for (int index = 0; index < selectName.length; index++) {
            keys.put(selectName[index], selectNameDisplay[index]);
        }
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
        this.accentDisplay = keys.get(recognizerAccent);
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
        this.nameDisplay = keys.get(voiceName);
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
        this.recognizerAccent = findKey(accentDisplay);
    }

    public String getNameDisplay() {
        return nameDisplay;
    }

    public void setNameDisplay(String nameDisplay) {
        this.nameDisplay = nameDisplay;
        this.voiceName = findKey(nameDisplay);
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
