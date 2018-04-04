package com.eli.voiceassist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by zhanbo.zhang on 2018/3/29.
 */
public class UITestActivity extends AppCompatActivity {

    private static final String TAG = "elifli";

    private RecordButton recordButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_test_activity);

        recordButton = (RecordButton) findViewById(R.id.record_button);
        recordButton.setOnBreathListener(new RecordButton.OnBreathListener() {
            @Override
            public void onBreathStateChanged(int state) {
                Log.i(TAG, (state == RecordButton.OnBreathListener.BREATH_STATE_START) ? "breath start" : "breath stop");
            }
        });
    }
}
