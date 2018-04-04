package com.eli.voiceassist.entity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.eli.voiceassist.mode.Echo;
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

    private SpeechRecognizer recognizer;
    private SpeechSynthesizer synthesizer;
    private AIUIAgent aiuiAgent;

    private OnVoiceEventListener onVoiceEventListener;

    private static VoiceEntity instance;

    private boolean aiuiEnable = true;
    private boolean isAIUIWakeup = false;
    private boolean isRecording = false;

    private VoiceEntity(Context context) {
        Log.i(TAG, "create voice");
        recognizer = SpeechRecognizer.createRecognizer(context, this);
        setRecognizerParam();
        synthesizer = SpeechSynthesizer.createSynthesizer(context, this);
        setSynthesizerParam();
        if (aiuiEnable) {
            aiuiAgent = AIUIAgent.createAgent(context, Util.getAIUIParams(context), this);
            aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null));
            aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
        }
    }

    public static VoiceEntity getInstance(Context context) {
        if (instance == null)
            instance = new VoiceEntity(context);
        return instance;
    }

    /**
     * set recognizer listener
     *
     * @param listener
     */
    public void setOnVoiceEventListener(OnVoiceEventListener listener) {
        this.onVoiceEventListener = listener;
    }

    /**
     * start recognizer process
     */
    public void startListen() {
        if (recognizer == null)
            return;

        int ret = recognizer.startListening(recognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.i(TAG, "Listen Error.");
        }
    }

    /**
     * stop recognizer process
     */
    public void stopListen() {
        if (recognizer == null)
            return;
        recognizer.stopListening();
        isRecording = false;
    }

    /**
     * stop the speak process
     */
    public void stopSpeak() {

    }

    public boolean isRecording() {
        return this.isRecording;
    }

    public void updateLexicon(String type, String lexicon, LexiconListener listener) {
        recognizer.updateLexicon(type, lexicon, listener);
    }

    /**
     * speak loud message
     *
     * @param message
     */
    public void speakMessage(String message) {
        int ret = synthesizer.startSpeaking(message, synthesizerListener);
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
        aiuiAgent.sendMessage(msg);
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
                    if (onVoiceEventListener != null)
                        onVoiceEventListener.onAiuiAnalyserResult(echo);
                    /*if (echo != null) {
                        Log.i(TAG, "service type: " + echo.getEchoType() +
                                "\ninput text: " + echo.getInputMessage() +
                                "\necho text: " + echo.getEcho() +
                                "\nintent: " + echo.getIntent() +
                                "\nparams: " + echo.getParams().toString()
                        );
                    }*/
                }
                break;
        }
    }

    /**
     * speech recognizer initial listener
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
     * recognizer result listener
     */
    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            if (onVoiceEventListener != null)
                onVoiceEventListener.onVolumeChanged(i);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "Begin Of Speech");
            if (onVoiceEventListener != null) {
                onVoiceEventListener.onRecognizerStatusChanged(true);
                isRecording = true;
                //当aiui设置可用并且处于sleep状态, 唤醒aiui
                if (aiuiEnable && aiuiAgent != null && !isAIUIWakeup)
                    aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
            }
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(TAG, "End Of Speech");
            if (onVoiceEventListener != null) {
                onVoiceEventListener.onRecognizerStatusChanged(false);
                isRecording = false;
            }
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String result = Util.parseVoice(recognizerResult.getResultString());
            Log.i(TAG, "Recognizer Result: " + result);
            if (onVoiceEventListener != null)
                onVoiceEventListener.onRecognizeResult(false, result);
            if (aiuiEnable && isAIUIWakeup && result != null && result.length() > 0)
                sendAIUIMessage(result);
        }

        @Override
        public void onError(SpeechError speechError) {
            String errorMessage = speechError.getPlainDescription(false);
            Log.i(TAG, "Speech Recognizer Error: " + errorMessage);
            if (onVoiceEventListener != null)
                onVoiceEventListener.onRecognizeResult(true, errorMessage);
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
        }
    };

    /**
     * voice synthesizer listener
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
     * set params for voice recognizer
     */
    private void setRecognizerParam() {
        if (recognizer == null)
            return;
        //清空所以参数
        recognizer.setParameter(SpeechConstant.PARAMS, null);
        //设置听写引擎
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置返回结果格式
        recognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //设置语言
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        //设置发音
        recognizer.setParameter(SpeechConstant.ACCENT, "mandarin");
        //设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        recognizer.setParameter(SpeechConstant.VAD_BOS, "4000");
        //设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        recognizer.setParameter(SpeechConstant.VAD_EOS, "1000");
        //设置标点符号 0:无标点 1:有标点
        recognizer.setParameter(SpeechConstant.ASR_PTT, "0");
    }

    /**
     * set params for voice synthesizer
     */
    private void setSynthesizerParam() {
        if (synthesizer == null)
            return;
        //清除所有参数
        synthesizer.setParameter(SpeechConstant.PARAMS, null);
        //设置合成引擎
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置合成发言人
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //设置语速
        synthesizer.setParameter(SpeechConstant.SPEED, "50");
        //设置音调
        synthesizer.setParameter(SpeechConstant.PITCH, "50");
        //设置音量
        synthesizer.setParameter(SpeechConstant.VOLUME, "50");
        //设置播放器音频流类型
        synthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
        //设置播放合成音频打断音乐播放，默认为true
        synthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    public interface OnVoiceEventListener {
        void onVolumeChanged(int volume);

        void onRecognizeResult(boolean wrong, String result);

        void onRecognizerStatusChanged(boolean status);

        void onAiuiAnalyserResult(Echo echo);
    }
}
