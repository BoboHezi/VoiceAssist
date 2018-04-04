package com.eli.voiceassist.view;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.eli.voiceassist.R;
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.widget.MyRecyclerAdapter;
import com.eli.voiceassist.widget.RecordButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanbo.zhang on 2018/3/29.
 */
public class UITestActivity extends AppCompatActivity {

    private static final String TAG = "elifli";

    private RecordButton recordButton;
    private RecyclerView recyclerView;
    private MyRecyclerAdapter adapter;
    private List<String> messages;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_test_activity);
        initData();
        initialView();
    }

    private void initialView() {
        recordButton = (RecordButton) findViewById(R.id.record_button);
        recordButton.setOnBreathListener(new RecordButton.OnBreathListener() {
            @Override
            public void onBreathStateChanged(int state) {
                messages.add(Util.randomString(10));
                adapter.notifyDataSetChanged();
                Log.i(TAG, (state == RecordButton.OnBreathListener.BREATH_STATE_START) ? "breath start" : "breath stop");
            }
        });
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, OrientationHelper.VERTICAL, false));
        adapter = new MyRecyclerAdapter(this, messages);
        recyclerView.setAdapter(adapter);
    }

    private void initData() {
        messages = new ArrayList<>();
        /*for (int i = 0; i < 20; i ++) {
            messages.add("message " + i);
        }*/
    }
}