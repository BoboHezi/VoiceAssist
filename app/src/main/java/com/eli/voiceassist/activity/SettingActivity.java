package com.eli.voiceassist.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;

import com.eli.voiceassist.R;
import com.eli.voiceassist.adapter.SettingListAdapter;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.widget.CustomSeekBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 */

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "elifli";
    private ListView settingList;
    private List<Map<String, Object>> data;
    private SettingListAdapter adapter;
    private Dialog percentSelectDialog;
    private Dialog itemSelectDialog;

    private CustomSeekBar percentBar;
    private ListView selectItemList;

    private SettingParams params;

    private int nowSelectIndex;

    private String titles[] = new String[]{"识别口音(中文)", "是否发声", "发音人", "语速", "音量"};
    private String summarise[] = new String[]{"识别语音口音", "是", "小燕", "50", "50"};
    private String selectAccent[] = new String[]{"普通话", "粤语", "四川话"};
    private String selectName[] = new String[]{"小燕", "小宇", "凯瑟琳", "亨利", "小梅", "晓琳"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);

        initData();
        initView();
    }

    private void initView() {
        settingList = (ListView) findViewById(R.id.setting_list);
        settingList.setAdapter(adapter);
        resetLayout(params);

        WindowManager.LayoutParams lp;
        percentSelectDialog = new Dialog(this);
        percentSelectDialog.setContentView(R.layout.percent_select_dialog);
        lp = percentSelectDialog.getWindow().getAttributes();
        lp.gravity = Gravity.TOP;
        percentSelectDialog.getWindow().setAttributes(lp);

        itemSelectDialog = new Dialog(this);
        itemSelectDialog.setContentView(R.layout.item_select_dialog);
        lp = percentSelectDialog.getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        itemSelectDialog.getWindow().setAttributes(lp);

        selectItemList = itemSelectDialog.findViewById(R.id.item_list);
        selectItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String value = "";
                if (nowSelectIndex == 0) {
                    params.setAccentDisplay(selectAccent[position]);
                } else if (nowSelectIndex == 2) {
                    params.setNameDisplay(selectName[position]);
                }
                itemSelectDialog.dismiss();
                resetLayout(params);
                Util.writeStorageParams(SettingActivity.this, params.toString());
                Log.i(TAG, value);
            }
        });

        percentBar = percentSelectDialog.findViewById(R.id.percent_bar);
        percentBar.setListener(new CustomSeekBar.OnPercentChangedListener() {
            @Override
            public void onPercentChanged(int percent) {
                if (nowSelectIndex == 3 || nowSelectIndex == 4) {
                    if (nowSelectIndex == 3)
                        params.setVoiceSpeed(percent);
                    else if (nowSelectIndex == 4)
                        params.setVoiceVolume(percent);
                    resetLayout(params);
                    Util.writeStorageParams(SettingActivity.this, params.toString());
                }
            }
        });
    }

    private void initData() {
        data = new ArrayList<>();
        adapter = new SettingListAdapter(this, data);
        adapter.setListener(itemClickListener);
        params = getParams();
    }

    SettingListAdapter.OnItemClickListener itemClickListener = new SettingListAdapter.OnItemClickListener() {
        @Override
        public void onItemClicked(View parent, int position) {
            Log.i(TAG, "item clicked: " + position);
            nowSelectIndex = position;
            switch (position) {
                case 0:
                    if (itemSelectDialog != null) {
                        ArrayAdapter<String> selectItemsAdapter = new ArrayAdapter<>(SettingActivity.this, android.R.layout.simple_list_item_1, selectAccent);
                        selectItemList.setAdapter(selectItemsAdapter);
                        itemSelectDialog.show();
                    }
                    break;

                case 1:
                    Switch switchButton = parent.findViewById(R.id.item_switch);
                    params.setSpeakEnable(switchButton.isChecked());
                    resetLayout(params);
                    Util.writeStorageParams(SettingActivity.this, params.toString());
                    break;

                case 2:
                    if (itemSelectDialog != null) {
                        ArrayAdapter<String> selectItemsAdapter = new ArrayAdapter<>(SettingActivity.this, android.R.layout.simple_list_item_1, selectName);
                        selectItemList.setAdapter(selectItemsAdapter);
                        itemSelectDialog.show();
                    }
                    break;

                case 3:
                    if (percentSelectDialog != null) {
                        percentSelectDialog.show();
                        percentBar.setPercent(params.getVoiceSpeed());
                    }
                    break;

                case 4:
                    if (percentSelectDialog != null) {
                        percentSelectDialog.show();
                        percentBar.setPercent(params.getVoiceVolume());
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private SettingParams getParams() {
        return Util.parseParams(Util.readStorageParams(this));
    }

    private void resetLayout(SettingParams params) {
        if (params == null)
            return;
        data.clear();
        for (int index = 0; index < titles.length; index++) {
            Map<String, Object> map = new HashMap<>();
            map.put("image", 0);
            map.put("title", titles[index]);

            switch (index) {
                case 0:
                    map.put("summary", params.getAccentDisplay());
                    break;

                case 1:
                    map.put("summary", params.isSpeakEnable() ? "是" : "否");
                    break;

                case 2:
                    map.put("summary", params.getNameDisplay());
                    break;

                case 3:
                    map.put("summary", params.getVoiceSpeed() + "");
                    break;

                case 4:
                    map.put("summary", params.getVoiceVolume() + "");
                    break;

                default:
                    break;
            }
            if (index != 1)
                map.put("hide", true);
            else
                map.put("hide", false);
            data.add(map);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     *******Recognizer********
     * Accent       口音
     * Speak        是否发声
     * VOICE_NAME   发音人
     * SPEED        语速
     * VOLUME       音量
     */
}
