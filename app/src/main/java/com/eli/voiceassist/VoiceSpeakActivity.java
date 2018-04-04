package com.eli.voiceassist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class VoiceSpeakActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "elifli";

    private SpeechSynthesizer speechSynthesizer;
    private EditText wordInsert;
    private Button speakButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_speak_activity);

        speechSynthesizer = SpeechSynthesizer.createSynthesizer(this, initListener);
        setParam();
        initView();
    }

    private void initView() {
        wordInsert = (EditText) findViewById(R.id.speak_word);
        speakButton = (Button) findViewById(R.id.speak);
        speakButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String word = wordInsert.getText().toString();
        if (TextUtils.isEmpty(word)) {
            return;
        }
        Log.i(TAG, word);
        int ret = speechSynthesizer.startSpeaking(word, synthesizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.i(TAG, "ERROR: " + ret);
        }
    }

    private InitListener initListener = new InitListener() {
        @Override
        public void onInit(int i) {
        }
    };

    private SynthesizerListener synthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            Log.i(TAG, "onSpeakBegin");
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {
            Log.i(TAG, "Buffer: " + i);
        }

        @Override
        public void onSpeakPaused() {
            Log.i(TAG, "onSpeakPaused");
        }

        @Override
        public void onSpeakResumed() {
            Log.i(TAG, "onSpeakResumed");
        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {
            Log.i(TAG, "SpeakProgress: " + i);
        }

        @Override
        public void onCompleted(SpeechError speechError) {
            Log.i(TAG, "onCompleted");
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            Log.i(TAG, "");
        }
    };

    private void setParam() {
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
}
