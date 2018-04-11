package com.eli.voiceassist.entity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.content.res.Resources;

import com.eli.voiceassist.R;
import com.eli.voiceassist.mode.Echo;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.util.Util;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import org.json.JSONObject;

/**
 * Created by zhanbo.zhang on 2018/4/3.
 */
public final class VoiceEntity implements VoiceInitialListener {
    private static final String TAG = "VoiceEntity";

    private Context context;

    private SpeechRecognizer mRecognizer;
    private SpeechSynthesizer mSynthesizer;
    private AIUIAgent mAiuiAgent;

    private SettingParams params;

    private OnVoiceEventListener mOnVoiceEventListener;

    private static VoiceEntity instance;

    private boolean speakEnable = true;
    private boolean isAIUIWakeup = false;
    private boolean isRecording = false;

    private static String settingTitles[];
    private static String selectAccent[];
    private static String selectAccentDisplay[];
    private static String selectSpeaker[];
    private static String selectSpeakerDisplay[];
    public static String systemLanguage = "en_us";
    public static String positive;
    public static String negative;
    public static String answerUnKnown;
    public static String answerCalling;
    public static String answerOpen;
    public static String answerFound;
    public static String answerNotFound;

    private VoiceEntity(Context context) {
        this.context = context;

        initData();
        initParams();
        mRecognizer = SpeechRecognizer.createRecognizer(context, this);
        setRecognizerParam(params);
        mSynthesizer = SpeechSynthesizer.createSynthesizer(context, this);
        setSynthesizerParam(params);
        mAiuiAgent = AIUIAgent.createAgent(context, Util.getAIUIParams(context), this);
        mAiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null));
        mAiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
    }

    /**
     * init data: setting info, values, system language
     */
    private void initData() {
        Resources resources = context.getResources();
        settingTitles = resources.getStringArray(R.array.setting_array);
        selectAccent = resources.getStringArray(R.array.accent_array);
        selectAccentDisplay = resources.getStringArray(R.array.accent_display_array);
        selectSpeaker = resources.getStringArray(R.array.speaker_array);
        selectSpeakerDisplay = resources.getStringArray(R.array.speaker_display_array);

        positive = resources.getString(R.string.positive);
        negative = resources.getString(R.string.negative);
        answerUnKnown = resources.getString(R.string.unknown);
        answerCalling = resources.getString(R.string.calling);
        answerOpen = resources.getString(R.string.opening);
        answerFound = resources.getString(R.string.founded);
        answerNotFound = resources.getString(R.string.not_found);

        systemLanguage = Util.getSystemLanguage(context).toLowerCase();
        if (!systemLanguage.equalsIgnoreCase("en_us") && !systemLanguage.equalsIgnoreCase("zh_cn")) {
            systemLanguage = "en_us";
        }
    }

    /**
     * init param
     */
    private void initParams() {
        String params = Util.readStorageParams(context);
        if (params == null || TextUtils.isEmpty(params)) {
            this.params = new SettingParams();
            this.params.setRecognizerLanguage(systemLanguage);
            this.params.setVoiceVolume(50);
            this.params.setVoiceSpeed(50);
            this.params.setVoiceName("xiaoyan");
            this.params.setSpeakEnable(true);
            this.params.setRecognizerEOS(1000);
            this.params.setRecognizerBOS(4000);
            this.params.setRecognizerAccent("mandarin");
        } else {
            this.params = Util.parseSettingParams(params);
            this.params.setRecognizerLanguage(systemLanguage);
        }
        this.params.setVoiceName(this.params.getVoiceName());
        this.speakEnable = this.params.isSpeakEnable();
        Util.writeStorageParams(context, this.params.toString());
    }

    public static VoiceEntity getInstance(Context context) {
        if (instance == null)
            instance = new VoiceEntity(context);
        return instance;
    }

    /**
     * set mRecognizer listener
     *
     * @param listener
     */
    public void setOnVoiceEventListener(OnVoiceEventListener listener) {
        this.mOnVoiceEventListener = listener;
    }

    /**
     * start mRecognizer process
     */
    public void startListen() {
        if (mRecognizer == null)
            return;

        int ret = mRecognizer.startListening(recognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.i(TAG, "Listen Error.");
        }
    }

    /**
     * stop mRecognizer process
     */
    public void stopListen() {
        if (mRecognizer == null)
            return;
        mRecognizer.stopListening();
        isRecording = false;
    }

    /**
     * stop the speak process
     */
    public void stopSpeak() {
        if (mSynthesizer != null && mSynthesizer.isSpeaking()) {
            mSynthesizer.stopSpeaking();
        }
    }

    /**
     * set do we need speak loud
     *
     * @param enable
     */
    public void setSpeakEnable(boolean enable) {
        this.speakEnable = enable;
        if (speakEnable && mSynthesizer == null) {
            mSynthesizer = SpeechSynthesizer.createSynthesizer(context, this);
            setSynthesizerParam(params);
        }
    }

    public SettingParams getParams() {
        return this.params;
    }

    public boolean isRecording() {
        return this.isRecording;
    }

    public void setParams(SettingParams params) {
        this.params = params;
        this.speakEnable = params.isSpeakEnable();
        setRecognizerParam(params);
        if (speakEnable)
            setSynthesizerParam(params);
    }

    /**
     * update use Lexicon
     *
     * @param type
     * @param lexicon
     * @param listener
     */
    public void updateLexicon(String type, String lexicon, LexiconListener listener) {
        mRecognizer.updateLexicon(type, lexicon, listener);
    }

    /**
     * speak loud message
     *
     * @param message
     */
    public void speakMessage(String message) {
        if (!speakEnable || mSynthesizer == null)
            return;
        int ret = mSynthesizer.startSpeaking(message, synthesizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.i(TAG, "Synthesizer Error: " + ret);
        }
    }

    /**
     * send aiui message start analyse
     *
     * @param message
     */
    private void sendAIUIMessage(String message) {
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_WRITE, 0, 0, "data_type=text", message.getBytes());
        mAiuiAgent.sendMessage(msg);
    }


    /**
     * aiui agent listener
     *
     * @param aiuiEvent aiui result
     */
    @Override
    public void onEvent(AIUIEvent aiuiEvent) {
        switch (aiuiEvent.eventType) {
            case AIUIConstant.EVENT_WAKEUP:
                isAIUIWakeup = true;
                break;

            case AIUIConstant.EVENT_SLEEP:
                isAIUIWakeup = false;
                break;

            case AIUIConstant.EVENT_ERROR:
                break;

            case AIUIConstant.EVENT_RESULT:
                JSONObject semanticResult = Util.getSemanticResult(aiuiEvent);
                if (semanticResult != null && semanticResult.length() != 0) {
                    Log.i(TAG, "Analyse Result: " + semanticResult.toString());
                    Echo echo = Util.parseEchoMessage(semanticResult);
                    if (mOnVoiceEventListener != null)
                        mOnVoiceEventListener.onAiuiAnalyserResult(echo);
                }
                break;
        }
    }

    /**
     * speech mRecognizer initial listener
     *
     * @param i initial result
     */
    @Override
    public void onInit(int i) {
        if (i != ErrorCode.SUCCESS) {
            Log.i(TAG, "Initial Error: " + i);
        }
    }

    /**
     * mRecognizer result listener
     */
    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            if (mOnVoiceEventListener != null)
                mOnVoiceEventListener.onVolumeChanged(i);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "Begin Of Speech");
            if (mOnVoiceEventListener != null) {
                mOnVoiceEventListener.onRecognizerStatusChanged(true);
                isRecording = true;
                //当aiui设置可用并且处于sleep状态, 唤醒aiui
                if (mAiuiAgent != null && !isAIUIWakeup)
                    mAiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
            }
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(TAG, "End Of Speech");
            if (mOnVoiceEventListener != null) {
                mOnVoiceEventListener.onRecognizerStatusChanged(false);
                isRecording = false;
            }
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            if (b)
                return;
            Log.i(TAG, "Recognizer Result: " + recognizerResult.getResultString());
            String result = Util.parseRecognizerResult(recognizerResult.getResultString());
            if (mOnVoiceEventListener != null)
                mOnVoiceEventListener.onRecognizeResult(false, result);
            if (isAIUIWakeup && result != null && result.length() > 0)
                sendAIUIMessage(result);
        }

        @Override
        public void onError(SpeechError speechError) {
            String errorMessage = speechError.getPlainDescription(false);
            Log.i(TAG, "Speech Recognizer Error: " + errorMessage);
            if (mOnVoiceEventListener != null)
                mOnVoiceEventListener.onRecognizeResult(true, errorMessage);
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
        }
    };

    /**
     * voice mSynthesizer listener
     */
    private SynthesizerListener synthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {
        }

        @Override
        public void onSpeakPaused() {
        }

        @Override
        public void onSpeakResumed() {
        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {
        }

        @Override
        public void onCompleted(SpeechError speechError) {
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
        }
    };

    /**
     * set params for voice mRecognizer
     */
    private void setRecognizerParam(SettingParams params) {
        if (mRecognizer == null)
            return;
        //清空所以参数
        mRecognizer.setParameter(SpeechConstant.PARAMS, null);
        //设置听写引擎
        mRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置返回结果格式
        mRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //设置标点符号 0:无标点 1:有标点
        mRecognizer.setParameter(SpeechConstant.ASR_PTT, "0");
        //设置语言
        mRecognizer.setParameter(SpeechConstant.LANGUAGE, params.getRecognizerLanguage());
        //设置发音
        mRecognizer.setParameter(SpeechConstant.ACCENT, params.getRecognizerAccent());
        //设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mRecognizer.setParameter(SpeechConstant.VAD_BOS, params.getRecognizerBOS() + "");
        //设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mRecognizer.setParameter(SpeechConstant.VAD_EOS, params.getRecognizerEOS() + "");

        //open translate in english mode
        if (systemLanguage.equalsIgnoreCase("en_us")) {
            mRecognizer.setParameter(SpeechConstant.ASR_SCH, "0");
            mRecognizer.setParameter(SpeechConstant.ADD_CAP, "translate");
            mRecognizer.setParameter(SpeechConstant.TRS_SRC, "its");
            mRecognizer.setParameter(SpeechConstant.ORI_LANG, "en");
            mRecognizer.setParameter(SpeechConstant.TRANS_LANG, "cn");
        }
    }

    /**
     * set params for voice mSynthesizer
     */
    private void setSynthesizerParam(SettingParams params) {
        if (mSynthesizer == null)
            return;
        //清除所有参数
        mSynthesizer.setParameter(SpeechConstant.PARAMS, null);
        //设置合成引擎
        mSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置音调
        mSynthesizer.setParameter(SpeechConstant.PITCH, "50");
        //设置播放器音频流类型
        mSynthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
        //设置播放合成音频打断音乐播放，默认为true
        mSynthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
        //设置合成发言人
        mSynthesizer.setParameter(SpeechConstant.VOICE_NAME, params.getVoiceName());
        //设置语速
        mSynthesizer.setParameter(SpeechConstant.SPEED, params.getVoiceSpeed() + "");
        //设置音量
        mSynthesizer.setParameter(SpeechConstant.VOLUME, params.getVoiceVolume() + "");
    }

    public static String[] getSettingTitles() {
        return settingTitles;
    }

    public static String[] getSelectAccent() {
        return selectAccent;
    }

    public static String[] getSelectAccentDisplay() {
        return selectAccentDisplay;
    }

    public static String[] getSelectSpeaker() {
        return selectSpeaker;
    }

    public static String[] getSelectSpeakerDisplay() {
        return selectSpeakerDisplay;
    }

    public interface OnVoiceEventListener {
        void onVolumeChanged(int volume);

        void onRecognizeResult(boolean wrong, String result);

        void onRecognizerStatusChanged(boolean status);

        void onAiuiAnalyserResult(Echo echo);
    }
}
