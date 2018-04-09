package com.eli.voiceassist.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.entity.VoiceEntity;
import com.eli.voiceassist.mode.AppInfo;
import com.eli.voiceassist.mode.ContactInfo;
import com.eli.voiceassist.mode.Echo;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.adapter.DialogListAdapter;
import com.eli.voiceassist.widget.RecordButton;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.SpeechError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public class VoiceAssistDemoActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "elifli";

    private static final int CODE_RESULT_SETTING = 1;

    private RecordButton mVoiceRecode;
    private ListView mDialogListView;

    private boolean isRecording = false;

    private VoiceEntity mVoiceEntity;
    private List<Map<String, Object>> mDialogs;
    private DialogListAdapter mDialogListAdapter;

    private static List<ContactInfo> mContacts;
    private static List<AppInfo> mApps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_assist_activity);
        initialView();
        Util.permissionRequest(this);
        initVoiceAssist();
        readInfo();
    }

    @Override
    protected void onResume() {
        if (mVoiceEntity != null) {
            SettingParams params = Util.parseParams(Util.readStorageParams(this));
            mVoiceEntity.setParams(params);
        }
        super.onResume();
    }

    /**
     * initial the voice entity(content SpeechRecognizer, SpeechSynthesizer, AIUIAgent)
     */
    private void initVoiceAssist() {
        mVoiceEntity = VoiceEntity.getInstance(this);
        mVoiceEntity.setOnVoiceEventListener(voiceEventListener);
    }

    /**
     * get mContacts and installed App info(async)
     */
    private void readInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mApps = Util.readAppList(VoiceAssistDemoActivity.this);
                mContacts = Util.readContacts(VoiceAssistDemoActivity.this);
                if (mContacts != null) {
                    updateContact(mContacts);
                }
            }
        }).start();
    }

    /**
     * initial layout
     */
    private void initialView() {
        mVoiceRecode = (RecordButton) findViewById(R.id.voice_recode);
        mVoiceRecode.setOnClickListener(this);
        mVoiceRecode.setOnLongClickListener(this);
        mDialogListView = (ListView) findViewById(R.id.dialog_list);
        mDialogs = new ArrayList<>();
        mDialogListAdapter = new DialogListAdapter(this, mDialogs);
        mDialogListView.setAdapter(mDialogListAdapter);
        mDialogListView.setOnScrollListener(scrollListener);
    }

    AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE)
                mDialogListAdapter.setAnimation(true);
            else
                mDialogListAdapter.setAnimation(false);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //Log.i(TAG, (firstVisibleItem + visibleItemCount) + "");
            if (firstVisibleItem != 0)
                mDialogListAdapter.setAllAnimation();
        }
    };

    @Override
    public void onClick(View v) {
        /*showMessage(Util.randomString(20), isRecording);
        isRecording = !isRecording;*/
        Log.i(TAG, "isRecording: " + isRecording);
        if (!isRecording) {
            mVoiceEntity.startListen();
            mVoiceRecode.startBreath();
        } else {
            mVoiceEntity.stopListen();
            mVoiceRecode.stopBreath();
        }
        isRecording = !isRecording;
    }

    @Override
    public boolean onLongClick(View v) {
        Intent settingIntent = new Intent(VoiceAssistDemoActivity.this, SettingActivity.class);
        startActivity(settingIntent);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int index = 0;
        for (String s : permissions) {
            if (grantResults[index] == 0) {
                if (Manifest.permission.READ_CONTACTS.equals(s)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mContacts = Util.readContacts(VoiceAssistDemoActivity.this);
                            if (mContacts != null) {
                                updateContact(mContacts);
                            }
                        }
                    }).start();
                    //Log.i(TAG, "READ_CONTACTS granted");
                } else if (Manifest.permission.RECORD_AUDIO.equals(s)) {
                    //Log.i(TAG, "RECORD_AUDIO granted");
                } else if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(s)) {
                    //Log.i(TAG, "READ_EXTERNAL_STORAGE granted");
                } else if (Manifest.permission.READ_PHONE_STATE.equals(s)) {
                    //Log.i(TAG, "READ_PHONE_STATE granted");
                } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(s)) {
                    //Log.i(TAG, "ACCESS_FINE_LOCATION granted");
                } else if (Manifest.permission.CAMERA.equals(s)) {
                    //Log.i(TAG, "CAMERA granted");
                }
            }
            index++;
        }
    }

    /**
     * voice assist listener(content voice recognizer, synthesizer, aiui listener)
     */
    private VoiceEntity.OnVoiceEventListener voiceEventListener = new VoiceEntity.OnVoiceEventListener() {
        @Override
        public void onVolumeChanged(int volume) {
        }

        @Override
        public void onRecognizeResult(boolean wrong, String result) {
            Log.i(TAG, (wrong ? "Error: " : "") + result);
            showMessage(result, !wrong);
            if (isRecording) {
                mVoiceRecode.stopBreath();
                isRecording = false;
            }
        }

        @Override
        public void onRecognizerStatusChanged(boolean status) {
            Log.i(TAG, (status ? "Begin" : "End") + " Recognizer");
            if (!status) {
                mVoiceRecode.stopBreath();
                isRecording = false;
            }
        }

        @Override
        public void onAiuiAnalyserResult(Echo echo) {
            if (echo == null) {
                showMessage("不好意思，我好像没听懂。。。", false);
            } else if (echo.getEchoType() == Echo.TYPE_UNKNOWN) {
                showMessage("不好意思，我好像没听懂。。。", false);
            } else {
                Log.i(TAG, "service type: " + echo.getEchoType() +
                        "\ninput text: " + echo.getInputMessage() +
                        "\necho text: " + echo.getEcho() +
                        "\nintent: " + echo.getIntent() +
                        "\nparams: " + echo.getParams().toString() +
                        "\nhashCode: " + echo.hashCode()
                );
                handleEcho(echo);
            }
        }
    };

    /**
     * handle AIUI echo
     *
     * @param echo
     */
    private void handleEcho(Echo echo) {
        if (echo.getEchoType() == Echo.TYPE_OPEN_QA) {
            String echoMessage = echo.getEcho();
            showMessage(echoMessage, false);
        } else {
            switch (echo.getIntent()) {
                case Echo.INTENT_DIAL:
                    if (echo.getParams().size() > 0) {
                        String name = echo.getParams().get(0);
                        String number = null;
                        Log.i(TAG, "call " + name);
                        if (Util.isNumber(name)) {
                            //输入为号码
                            number = name;
                        } else {
                            //输入为联系人
                            if (mContacts != null) {
                                for (ContactInfo contact : mContacts) {
                                    if (contact.getName().equals(name))
                                        number = contact.getNumber();
                                }
                            }
                        }
                        if (number != null) {
                            showMessage("正在为您呼叫" + name, false);
                            Util.startDial(this, number);
                        } else {
                            showMessage(echo.getEcho(), false);
                        }
                    }
                    break;

                case Echo.INTENT_QUERY:
                    String echoMessage = echo.getEcho();
                    showMessage(echoMessage, false);
                    break;

                case Echo.INTENT_RANDOM_SONG:
                    Util.playMusic(this, null, null);
                    break;

                case Echo.INTENT_SEARCH_SONG:
                    Util.playMusic(this, null, null);
                    break;

                case Echo.INTENT_OPEN_APP:
                    String appName = echo.getParams().get(0);
                    Log.i(TAG, "open " + appName);
                    if (mApps != null && mApps.size() > 0) {
                        List<AppInfo> matchedApps = Util.matchApp(appName, mApps);
                        if (matchedApps != null && matchedApps.size() > 0) {
                            if (matchedApps.size() == 1) {
                                //find only one matched
                                showMessage("正在打开" + appName, false);
                                Util.openAPPWithPackage(this, matchedApps.get(0).getPackageName());
                            } else {
                                //find multi-matched
                                StringBuilder sb = new StringBuilder("为您找到");
                                for (int index = 0; index < matchedApps.size(); index++) {
                                    AppInfo app = matchedApps.get(index);
                                    sb.append(app.getAppName());
                                    if (index < matchedApps.size() - 1)
                                        sb.append(",");
                                    Log.i(TAG, "Name: " + app.getAppName() + "\tPackage: " + app.getPackageName());
                                }
                                sb.append(matchedApps.size());
                                sb.append("个应用。");
                                showMessage(sb.toString(), false);
                            }
                        } else {
                            showMessage("您的手机未安装" + appName, false);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * display message in dialog
     *
     * @param message
     * @param isUser
     */
    private void showMessage(String message, boolean isUser) {
        if (message == null || message.equals("") || message.length() == 0)
            return;
        message = message.replace("\"", "");
        if (message.contains("[")) {
            int startIndex = message.indexOf("]") + 1;
            int endIndex = message.lastIndexOf("[");
            message = message.substring(startIndex, endIndex);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("role", isUser);
        mDialogs.add(map);
        if (mDialogs.size() > 50) {
            mDialogs.remove(0);
        }
        mDialogListAdapter.notifyDataSetChanged();
    }

    /**
     * upload user mContacts information
     *
     * @param contacts
     */
    private void updateContact(List<ContactInfo> contacts) {
        final StringBuilder sb = new StringBuilder();
        for (ContactInfo contact : contacts) {
            sb.append(contact.getName());
            sb.append("\n");
        }
        final String contactLexicon = sb.toString();
        if (mVoiceEntity != null && !contactLexicon.equals(Util.readStorageContacts(VoiceAssistDemoActivity.this))) {
            Log.i(TAG, "update contact");
            mVoiceEntity.updateLexicon("contact", contactLexicon, new LexiconListener() {
                @Override
                public void onLexiconUpdated(String s, SpeechError speechError) {
                    if (speechError == null) {
                        Log.i(TAG, "update success");
                        Util.writeStorageContacts(VoiceAssistDemoActivity.this, contactLexicon);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
    }
}
