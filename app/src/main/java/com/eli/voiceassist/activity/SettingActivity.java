package com.eli.voiceassist.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;

import com.eli.voiceassist.R;
import com.eli.voiceassist.adapter.SettingListAdapter;
import com.eli.voiceassist.entity.VoiceEntity;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.widget.CustomSeekBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 * <p>
 * 设置页面
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

    private String titles[] = VoiceEntity.getSettingTitles();
    private String selectAccent[] = VoiceEntity.getSelectAccentDisplay();
    private String selectName[] = VoiceEntity.getSelectSpeakerDisplay();

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
        itemSelectDialog.setContentView(R.layout.setting_select_dialog);
        lp.gravity = Gravity.CENTER;
        itemSelectDialog.getWindow().setAttributes(lp);

        selectItemList = itemSelectDialog.findViewById(R.id.item_list);
        selectItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemSelectDialog.dismiss();
                if (nowSelectIndex == 0) {
                    params.setAccentDisplay(selectAccent[position]);
                } else if (nowSelectIndex == 2) {
                    params.setNameDisplay(selectName[position]);
                } else {
                    return;
                }
                resetLayout(params);
                VoiceEntity.getInstance(SettingActivity.this).setParams(params);
            }
        });

        percentBar = percentSelectDialog.findViewById(R.id.percent_bar);
        percentBar.setListener(new CustomSeekBar.OnPercentChangedListener() {
            @Override
            public void onPercentChanged(int percent) {
                if (nowSelectIndex == 3) {
                    params.setVoiceSpeed(percent);
                } else if (nowSelectIndex == 4) {
                    params.setVoiceVolume(percent);
                } else {
                    return;
                }
                resetLayout(params);
                VoiceEntity.getInstance(SettingActivity.this).setParams(params);
            }
        });
    }

    /**
     * init param, list adapter
     */
    private void initData() {
        data = new ArrayList<>();
        adapter = new SettingListAdapter(this, data);
        adapter.setListener(itemClickListener);
        params = getParams();
    }

    /**
     * settings item click
     */
    SettingListAdapter.OnItemClickListener itemClickListener = new SettingListAdapter.OnItemClickListener() {
        @Override
        public void onItemClicked(View parent, int position) {
            nowSelectIndex = position;
            switch (position) {
                case 0:
                    //set recognizer accent
                    if (itemSelectDialog != null) {
                        ArrayAdapter<String> selectItemsAdapter = new ArrayAdapter<>(SettingActivity.this, android.R.layout.simple_list_item_1, selectAccent);
                        selectItemList.setAdapter(selectItemsAdapter);
                        itemSelectDialog.show();
                    }
                    break;

                case 1:
                    //toggle speak enable
                    Switch switchButton = parent.findViewById(R.id.item_switch);
                    params.setSpeakEnable(switchButton.isChecked());
                    resetLayout(params);
                    VoiceEntity.getInstance(SettingActivity.this).setParams(params);
                    break;

                case 2:
                    //set speaker
                    if (itemSelectDialog != null) {
                        ArrayAdapter<String> selectItemsAdapter = new ArrayAdapter<>(SettingActivity.this, android.R.layout.simple_list_item_1, selectName);
                        selectItemList.setAdapter(selectItemsAdapter);
                        itemSelectDialog.show();
                    }
                    break;

                case 3:
                    //set voice speed
                    if (percentSelectDialog != null) {
                        percentSelectDialog.show();
                        percentBar.setPercent(params.getVoiceSpeed());
                    }
                    break;

                case 4:
                    //set volume
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

    /**
     * read params from disk
     *
     * @return
     */
    private SettingParams getParams() {
        return Util.parseSettingParams(Util.readStorageParams(this));
    }

    /**
     * set setting layout info by params
     *
     * @param params
     */
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
                    map.put("summary", params.isSpeakEnable() ? VoiceEntity.positive : VoiceEntity.negative);
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
    protected void onStop() {
        //write params to disk
        Util.writeStorageParams(SettingActivity.this, params.toString());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
