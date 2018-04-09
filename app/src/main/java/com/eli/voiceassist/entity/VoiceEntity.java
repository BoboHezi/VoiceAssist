package com.eli.voiceassist.entity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

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

    private boolean speakEnable = false;
    private boolean aiuiEnable = true;
    private boolean isAIUIWakeup = false;
    private boolean isRecording = false;

    private VoiceEntity(Context context) {
        Log.i(TAG, "create voice");
        this.context = context;
        initParams();
        mRecognizer = SpeechRecognizer.createRecognizer(context, this);
        setRecognizerParam();
        if (speakEnable) {
            mSynthesizer = SpeechSynthesizer.createSynthesizer(context, this);
            setSynthesizerParam();
        }
        if (aiuiEnable) {
            mAiuiAgent = AIUIAgent.createAgent(context, Util.getAIUIParams(context), this);
            mAiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null));
            mAiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
        }
    }

    private void initParams() {
        String params = Util.readStorageParams(context);
        if (params == null || TextUtils.isEmpty(params)) {
            this.params = new SettingParams();
            String language = Util.getSystemLanguage(context);
            if (!language.equalsIgnoreCase("en_us") && !language.equalsIgnoreCase("zh_cn")) {
                language = "en_us";
            }
            this.params.setRecognizerLanguage(language);
            this.params.setVoiceVolume(50);
            this.params.setVoiceSpeed(50);
            this.params.setVoiceName("xiaoyan");
            this.params.setSpeakEnable(true);
            this.params.setRecognizerEOS(1000);
            this.params.setRecognizerBOS(4000);
            this.params.setRecognizerAccent("mandarin");
            Util.writeStorageParams(context, this.params.toString());
            return;
        }

        this.params = Util.parseParams(params);
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
            setSynthesizerParam();
        }
    }

    public SettingParams getParams() {
        return this.params;
    }

    public boolean isRecording() {
        return this.isRecording;
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
                if (aiuiEnable && mAiuiAgent != null && !isAIUIWakeup)
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
            String result = Util.parseVoice(recognizerResult.getResultString());
            Log.i(TAG, "Recognizer Result: " + result);
            if (mOnVoiceEventListener != null)
                mOnVoiceEventListener.onRecognizeResult(false, result);
            if (aiuiEnable && isAIUIWakeup && result != null && result.length() > 0)
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
    private void setRecognizerParam() {
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
    }

    /**
     * set params for voice mSynthesizer
     */
    private void setSynthesizerParam() {
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

    public interface OnVoiceEventListener {
        void onVolumeChanged(int volume);

        void onRecognizeResult(boolean wrong, String result);

        void onRecognizerStatusChanged(boolean status);

        void onAiuiAnalyserResult(Echo echo);
    }
}
