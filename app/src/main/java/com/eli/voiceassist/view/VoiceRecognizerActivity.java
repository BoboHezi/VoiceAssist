package com.eli.voiceassist.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.entity.VoiceEntity;
import com.eli.voiceassist.mode.Echo;

public class VoiceRecognizerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "elifli";

    private VoiceEntity voiceEntity;

    private Button startButton;
    private Button stopButton;
    private Button cancelButton;
    private TextView wordMessage;

    private boolean isAIUIWakeup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_recognizer_activity);
        voiceEntity = VoiceEntity.getInstance(this);
        voiceEntity.setOnVoiceEventListener(listener);
        initView();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                voiceEntity.startListen();
                break;

            case R.id.stop:
                voiceEntity.stopListen();
                break;

            case R.id.cancel:
                break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
    }

    VoiceEntity.OnVoiceEventListener listener = new VoiceEntity.OnVoiceEventListener() {
        @Override
        public void onVolumeChanged(int volume) {
        }

        @Override
        public void onRecognizeResult(boolean wrong, String result) {
            Log.i(TAG, (wrong ? "Error: " : "") + result);
        }

        @Override
        public void onRecognizerStatusChanged(boolean status) {
            Log.i(TAG, (status ? "Begin" : "End") + " Recognizer");
        }

        @Override
        public void onAiuiAnalyserResult(Echo echo) {
            if (echo != null) {
                Log.i(TAG, "service type: " + echo.getEchoType() +
                        "\ninput text: " + echo.getInputMessage() +
                        "\necho text: " + echo.getEcho() +
                        "\nintent: " + echo.getIntent() +
                        "\nparams: " + echo.getParams().toString()
                );
            }
        }
    };
}
