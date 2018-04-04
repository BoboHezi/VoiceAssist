package com.eli.voiceassist;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public class VoiceAssistDemoActivity extends AppCompatActivity implements View.OnClickListener, InitListener {

    private static final String TAG = "elifli";
    private static final int RECORDING_COLOR = 0xff1a562e;
    private static final int SILENT_COLOR = 0xff0a5a64;

    private RecordButton voiceRecode;
    private ListView dialogList;

    private boolean isRecording = false;
    private boolean isAIUIWakeup = false;

    private List<Map<String, Object>> dialogs;
    private DialogListAdapter dialogListAdapter;

    private SpeechRecognizer speechRecognizer;
    private AIUIAgent aiuiAgent;
    private SpeechSynthesizer speechSynthesizer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_assist_activity);

        initVoiceAssist();
        initView();
    }

    private void initVoiceAssist() {
        speechRecognizer = SpeechRecognizer.createRecognizer(this, this);
        setRecognizerParam();
        aiuiAgent = AIUIAgent.createAgent(this, Util.getAIUIParams(this), aiuiListener);
        aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null));
        aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
        speechSynthesizer = SpeechSynthesizer.createSynthesizer(this, this);
        setSynthesizerParam();
    }

    private void initView() {
        voiceRecode = (RecordButton) findViewById(R.id.voice_recode);
        voiceRecode.setOnClickListener(this);
        dialogList = (ListView) findViewById(R.id.dialog_list);
        dialogs = new ArrayList<>();
        dialogListAdapter = new DialogListAdapter(this, dialogs);
        dialogList.setAdapter(dialogListAdapter);
        dialogList.setOnScrollListener(scrollListener);
    }

    AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE)
                dialogListAdapter.setAnimation(true);
            else
                dialogListAdapter.setAnimation(false);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //Log.i(TAG, (firstVisibleItem + visibleItemCount) + "");
            if (firstVisibleItem != 0)
                dialogListAdapter.setAllAnimation();
        }
    };

    @Override
    public void onClick(View v) {
        /*showMessage(Util.randomString(10), isRecording);
        isRecording = !isRecording;*/
        if (!isRecording) {
            if (speechRecognizer.startListening(recognizerListener) != ErrorCode.SUCCESS) {
                Log.i(TAG, "Listen Error.");
            } else {
                isRecording = true;
            }
        } else {
            speechRecognizer.stopListening();
            isRecording = false;
        }
        voiceRecode.setBackgroundTintList(ColorStateList.valueOf(isRecording ? RECORDING_COLOR : SILENT_COLOR));

        if (speechSynthesizer.isSpeaking()) {
            speechSynthesizer.stopSpeaking();
            speechSynthesizer.pauseSpeaking();
        }
    }

    @Override
    public void onInit(int i) {
        if (i != ErrorCode.SUCCESS) {
            Log.i(TAG, "Initial Error: " + i);
        }
    }

    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {
            if (!isAIUIWakeup) {
                aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
            }
        }

        @Override
        public void onEndOfSpeech() {
            voiceRecode.performClick();
            voiceRecode.stopBreath();
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String result = Util.parseVoice(recognizerResult.getResultString());
            Log.i(TAG, result);
            showMessage(result, true);
            if (isAIUIWakeup) {
                sendAIUIMessage(result);
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            String errorMessage = speechError.getPlainDescription(false);
            Log.i(TAG, "Speech Recognizer Error: " + errorMessage);
            showMessage(errorMessage, false);
            //speakMessage(errorMessage);
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private AIUIListener aiuiListener = new AIUIListener() {
        @Override
        public void onEvent(AIUIEvent aiuiEvent) {
            switch (aiuiEvent.eventType) {
                case AIUIConstant.EVENT_WAKEUP:
                    Log.i(TAG, "EVENT_WAKEUP");
                    isAIUIWakeup = true;
                    break;

                case AIUIConstant.EVENT_RESULT:
                    Log.i(TAG, "EVENT_RESULT");
                    JSONObject semanticResult = Util.getSemanticResult(aiuiEvent);
                    if (semanticResult != null && semanticResult.length() != 0) {
                        AIUIEcho echo = ParseAIUI.parseMessage(semanticResult.toString());
                        if (echo != null) {
                            String message = echo.getEcho();
                            Log.i(TAG, message);
                            showMessage(message, false);
                            //speakMessage(message);
                        }
                    }
                    break;

                case AIUIConstant.EVENT_SLEEP:
                    Log.i(TAG, "EVENT_SLEEP");
                    isAIUIWakeup = false;
                    break;

                case AIUIConstant.EVENT_ERROR:
                    break;

                default:break;
            }
        }
    };

    private void setRecognizerParam() {
        if (speechRecognizer == null)
            return;
        //清空所以参数
        speechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        //设置听写引擎
        speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置返回结果格式
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //设置语言
        speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        //设置发音
        speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin");

        //设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "4000");
        //设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "1000");
        //设置标点符号 0:无标点 1:有标点
        speechRecognizer.setParameter(SpeechConstant.ASR_PTT, "0");
    }

    private void setSynthesizerParam() {
        if (speechSynthesizer == null)
            return;
        //清除所有参数
        speechSynthesizer.setParameter(SpeechConstant.PARAMS, null);
        //设置合成引擎
        speechSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置合成发言人
        speechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //设置语速
        speechSynthesizer.setParameter(SpeechConstant.SPEED, "50");
        //设置音调
        speechSynthesizer.setParameter(SpeechConstant.PITCH, "50");
        //设置音量
        speechSynthesizer.setParameter(SpeechConstant.VOLUME, "50");
        //设置播放器音频流类型
        speechSynthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
        //设置播放合成音频打断音乐播放，默认为true
        speechSynthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    private void sendAIUIMessage(String message) {
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_WRITE, 0, 0, "data_type=text", message.getBytes());
        aiuiAgent.sendMessage(msg);
    }

    private void showMessage(String message, boolean isUser) {
        if (message == null || message.equals("") || message.length() == 0)
            return;
        Map<String, Object> map = new HashMap<>();
        message = message.replace("\"", "");
        map.put("message", message);
        map.put("role", isUser);
        dialogs.add(map);
        if (dialogs.size() > 50) {
            dialogs.remove(0);
        }
        dialogListAdapter.notifyDataSetChanged();
    }

    private void speakMessage(String message) {
        speechSynthesizer.startSpeaking(message, new SynthesizerListener() {
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
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
        speechRecognizer.destroy();
        aiuiAgent.destroy();
        speechSynthesizer.stopSpeaking();
        speechSynthesizer.pauseSpeaking();
        speechSynthesizer.destroy();
    }
}
