package com.eli.voiceassist.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Switch;

import com.eli.voiceassist.R;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.adapter.SettingListAdapter;

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

    private SettingParams params;

    private String titles[] = new String[]{"口音", "是否发声", "发音人", "语速", "音量"};
    private String summarise[] = new String[]{"识别语音口音", "是", "小燕", "50", "50"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);

        data = new ArrayList<>();
        adapter = new SettingListAdapter(this, data);
        adapter.setListener(itemClickListener);
        settingList = (ListView) findViewById(R.id.setting_list);
        settingList.setAdapter(adapter);

        params = getParams();
        resetLayout(params);
    }

    SettingListAdapter.OnItemClickListener itemClickListener = new SettingListAdapter.OnItemClickListener() {
        @Override
        public void onItemClicked(View parent, int position) {
            if (position == 1) {
                Switch switchButton = parent.findViewById(R.id.item_switch);
                Log.i(TAG, switchButton.isChecked() + "");
            }
            switch (position) {
                case 0:
                    break;

                case 1:
                    Switch switchButton = parent.findViewById(R.id.item_switch);
                    params.setSpeakEnable(switchButton.isChecked());
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
        for (int index = 0; index < titles.length; index++) {
            Map<String, Object> map = new HashMap<>();
            map.put("image", 0);
            map.put("title", titles[index]);

            switch (index) {
                case 0:
                    String accent = params.getRecognizerAccent();
                    if (accent.equals("mandarin")) {
                        map.put("summary", "普通话");
                    } else if (accent.equals("cantonese")) {
                        map.put("summary", "粤  语");
                    } else if (accent.equals("lmz")) {
                        map.put("summary", "四川话");
                    }
                    break;

                case 1:
                    map.put("summary", params.isSpeakEnable() ? "是" : "否");
                    break;

                case 2:
                    map.put("summary", "发音人  " + params.getVoiceName());
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

    /**
     *******Recognizer********
     * Accent       口音
     * VAD_BOS      前端点
     * VAD_EOS      后端点
     *
     ******Synthesizer********
     * Speak        是否发声
     * VOICE_NAME   发音人
     * SPEED        语速
     * VOLUME       音量
     */
}
