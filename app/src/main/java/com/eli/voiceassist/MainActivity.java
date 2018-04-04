package com.eli.voiceassist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.voice_recognizer).setOnClickListener(this);
        findViewById(R.id.voice_speak).setOnClickListener(this);
        findViewById(R.id.ui_test).setOnClickListener(this);
        findViewById(R.id.voice_assist).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.voice_recognizer:
                intent = new Intent(MainActivity.this, VoiceRecognizerActivity.class);
                break;

            case R.id.voice_speak:
                intent = new Intent(MainActivity.this, VoiceSpeakActivity.class);
                break;

            case R.id.ui_test:
                intent = new Intent(MainActivity.this, UITestActivity.class);
                break;

            case R.id.voice_assist:
                intent = new Intent(MainActivity.this, VoiceAssistDemoActivity.class);

            default:break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}
