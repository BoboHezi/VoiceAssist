package com.eli.voiceassist.view;

import android.Manifest;
import android.content.res.ColorStateList;
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
import com.eli.voiceassist.util.Util;
import com.eli.voiceassist.widget.DialogListAdapter;
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
public class VoiceAssistDemoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "elifli";
    private static final int RECORDING_COLOR = 0xff1a562e;
    private static final int SILENT_COLOR = 0xff0a5a64;

    private RecordButton voiceRecode;
    private ListView dialogList;

    private boolean isRecording = false;

    private List<Map<String, Object>> dialogs;
    private DialogListAdapter dialogListAdapter;

    private VoiceEntity voiceEntity;

    private static List<ContactInfo> contacts;
    private static List<AppInfo> apps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_assist_activity);
        initialView();
        Util.permissionRequest(this);
        initVoiceAssist();
        readInfo();
    }

    /**
     * initial the voice entity(content SpeechRecognizer, SpeechSynthesizer, AIUIAgent)
     */
    private void initVoiceAssist() {
        voiceEntity = VoiceEntity.getInstance(this);
        voiceEntity.setOnVoiceEventListener(voiceEventListener);
    }

    /**
     * get contacts and installed App info(async)
     */
    private void readInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                apps = Util.readAppList(VoiceAssistDemoActivity.this);
                contacts = Util.readContacts(VoiceAssistDemoActivity.this);
                if (contacts != null) {
                    updateContact(contacts);
                }
            }
        }).start();
    }

    /**
     * initial layout
     */
    private void initialView() {
        voiceRecode = (RecordButton) findViewById(R.id.voice_recode);
        voiceRecode.setOnClickListener(this);
        dialogList = (ListView) findViewById(R.id.dialog_list);
        dialogs = new ArrayList<>();
        dialogListAdapter = new DialogListAdapter(this, dialogs);
        dialogList.setAdapter(dialogListAdapter);
        dialogList.setOnScrollListener(scrollListener);
    }

    AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE)
                dialogListAdapter.setAnimation(true);
            else
                dialogListAdapter.setAnimation(false);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //Log.i(TAG, (firstVisibleItem + visibleItemCount) + "");
            if (firstVisibleItem != 0)
                dialogListAdapter.setAllAnimation();
        }
    };

    @Override
    public void onClick(View v) {
        /*showMessage(Util.randomString(20), isRecording);
        isRecording = !isRecording;*/
        Log.i(TAG, "isRecording: " + isRecording);
        if (!isRecording) {
            voiceEntity.startListen();
        } else {
            voiceEntity.stopListen();
        }
        isRecording = !isRecording;
        voiceRecode.setBackgroundTintList(ColorStateList.valueOf(isRecording ? RECORDING_COLOR : SILENT_COLOR));
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
                            contacts = Util.readContacts(VoiceAssistDemoActivity.this);
                            if (contacts != null) {
                                updateContact(contacts);
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
        }

        @Override
        public void onRecognizerStatusChanged(boolean status) {
            Log.i(TAG, (status ? "Begin" : "End") + " Recognizer");
            if (!status) {
                voiceRecode.stopBreath();
                isRecording = false;
            }
        }

        @Override
        public void onAiuiAnalyserResult(Echo echo) {
            if (echo != null) {
                Log.i(TAG, "service type: " + echo.getEchoType() +
                        "\ninput text: " + echo.getInputMessage() +
                        "\necho text: " + echo.getEcho() +
                        "\nintent: " + echo.getIntent() +
                        "\nparams: " + echo.getParams().toString() +
                        "\nhashCode: " + echo.hashCode()
                );
                handleEcho(echo);
            } else {
                showMessage("不好意思，我好像没听懂。。。", false);
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
                            if (contacts != null) {
                                for (ContactInfo contact : contacts) {
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
                    if (apps != null && apps.size() > 0) {
                        List<AppInfo> matched = Util.matchApp(appName, apps);
                        if (matched != null && matched.size() > 0) {
                            if (matched.size() == 1) {
                                //find only one matched
                                showMessage("正在打开" + appName, false);
                                Util.openAPPWithPackage(this, matched.get(0).getPackageName());
                            } else {
                                //find multi-matched
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
            int startIndex = message.indexOf("]");
            int endIndex = message.lastIndexOf("[");
            message = message.substring(startIndex, endIndex);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("role", isUser);
        dialogs.add(map);
        if (dialogs.size() > 50) {
            dialogs.remove(0);
        }
        dialogListAdapter.notifyDataSetChanged();
    }

    /**
     * upload user contacts information
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
        if (voiceEntity != null && !contactLexicon.equals(Util.readStorageContacts(VoiceAssistDemoActivity.this))) {
            Log.i(TAG, "update contact");
            voiceEntity.updateLexicon("contact", contactLexicon, new LexiconListener() {
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
