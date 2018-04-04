package com.eli.voiceassist;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;

import org.json.JSONObject;

public class VoiceRecognizerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "elifli";

    private SpeechRecognizer speechRecognizer;
    private AIUIAgent aiuiAgent;

    private Button startButton;
    private Button stopButton;
    private Button cancelButton;
    private TextView wordMessage;

    private boolean isAIUIWakeup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_recognizer_activity);

        initView();
        initVoiceRecognizer();
    }

    private void initView() {
        startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(this);
        stopButton = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(this);
        cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(this);
        wordMessage = (TextView) findViewById(R.id.word);
    }

    private void initVoiceRecognizer() {
        speechRecognizer = SpeechRecognizer.createRecognizer(this, initListener);
        aiuiAgent = AIUIAgent.createAgent(this, Util.getAIUIParams(this), aiuiListener);
        aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_START, 0, 0, null, null));
        aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                setRecognizerParam();
                int ret = speechRecognizer.startListening(recognizerListener);
                if (ret != ErrorCode.SUCCESS) {
                    Log.i(TAG, "ERROR: " + ret);
                } else {
                    toastMessage("start listen", false);
                }
                break;

            case R.id.stop:
                speechRecognizer.stopListening();
                toastMessage("stop listen", false);
                break;

            case R.id.cancel:
                speechRecognizer.cancel();
                break;

            default:break;
        }
    }

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

    private InitListener initListener = new InitListener() {
        @Override
        public void onInit(int i) {
            if (i != ErrorCode.SUCCESS)
                toastMessage("Error: " + i, false);
        }
    };

    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            //Log.i(TAG, "Volume: " + i);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "onBeginOfSpeech");
            if (!isAIUIWakeup)
                aiuiAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(TAG, "onEndOfSpeech");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String result = Util.parseVoice(recognizerResult.getResultString());
            Log.i(TAG, result);
            //toastMessage(result, true);
            wordMessage.setText(wordMessage.getText() + "\n" + result);

            if (isAIUIWakeup)
                sendAIUIMessage(result);
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.i(TAG, speechError.getPlainDescription(true));
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
                        Log.i(TAG, "AIUIListener: " + semanticResult);
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

    private void toastMessage(String message, boolean isLong) {
        Toast toast = Toast.makeText(this, "", isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setText(message);
        toast.show();
    }

    private void sendAIUIMessage(String message) {
        AIUIMessage msg = new AIUIMessage(AIUIConstant.CMD_WRITE, 0, 0, "data_type=text", message.getBytes());
        aiuiAgent.sendMessage(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
        speechRecognizer.destroy();
        aiuiAgent.destroy();
    }
}
