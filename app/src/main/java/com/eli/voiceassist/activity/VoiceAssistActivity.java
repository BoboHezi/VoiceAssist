package com.eli.voiceassist.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.adapter.DialogListAdapter;
import com.eli.voiceassist.entity.VoiceEntity;
import com.eli.voiceassist.mode.AppInfo;
import com.eli.voiceassist.mode.ContactInfo;
import com.eli.voiceassist.mode.Echo;
import com.eli.voiceassist.mode.WebSearchResult;
import com.eli.voiceassist.util.Util;
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
public class VoiceAssistActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "elifli";
    private static final int HANDLER_LOAD_HINT = 1;
    private static final int HANDLER_UNLOAD_HINT = 2;
    private RecordButton mVoiceRecode;
    private ListView mDialogListView;
    private LinearLayout headerLayout;

    private boolean inRecognition = false;

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

        startRecognizer();
        inRecognition = true;
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
                mApps = Util.readAppList(VoiceAssistActivity.this);
                mContacts = Util.readContacts(VoiceAssistActivity.this);
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
        headerLayout = (LinearLayout) LayoutInflater.from(VoiceAssistActivity.this).inflate(R.layout.hint_layout, null);

        View footView = new View(this);
        footView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.dip2px(this, 100)));
        mDialogListView.addFooterView(footView);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        hintHandler.sendEmptyMessage(HANDLER_LOAD_HINT);
                    }
                }, 2000
        );
    }

    /**
     * handler: load and unload hint layout
     */
    private Handler hintHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            FrameLayout mainFrame = (FrameLayout) findViewById(R.id.main_frame);
            if (msg.what == HANDLER_LOAD_HINT) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.topMargin = Util.dip2px(VoiceAssistActivity.this, 40);
                layoutParams.leftMargin = Util.dip2px(VoiceAssistActivity.this, 20);
                layoutParams.rightMargin = Util.dip2px(VoiceAssistActivity.this, 20);

                headerLayout.setLayoutParams(layoutParams);
                mainFrame.addView(headerLayout, 0);
                headerLayout.startAnimation(AnimationUtils.loadAnimation(VoiceAssistActivity.this, R.anim.dialog_header_enter));

                ListView hintList = headerLayout.findViewById(R.id.hint_list);
                String hints[] = Util.randomHints(VoiceAssistActivity.this, 5);
                ArrayAdapter<String> hintAdapter = new ArrayAdapter<>(VoiceAssistActivity.this, R.layout.hint_list_item, hints);
                hintList.setAdapter(hintAdapter);
            } else if (msg.what == HANDLER_UNLOAD_HINT) {
                headerLayout.startAnimation(AnimationUtils.loadAnimation(VoiceAssistActivity.this, R.anim.dialog_header_exit));
                ((FrameLayout) findViewById(R.id.main_frame)).removeView(headerLayout);
            }
            return false;
        }
    });

    /**
     * dialog list scroll listener
     */
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
        if (!inRecognition) {
            startRecognizer();
        } else {
            stopRecognizer();
        }
        inRecognition = !inRecognition;
    }

    /**
     * start recognizer
     */
    private void startRecognizer() {
        if (mVoiceEntity != null) {
            mVoiceEntity.startListen();
            mVoiceRecode.startBreath();
        }
    }

    /**
     * stop recognizer
     */
    private void stopRecognizer() {
        if (mVoiceEntity != null) {
            mVoiceEntity.stopListen();
            mVoiceRecode.stopBreath();
        }
    }

    /**
     * long click listener for record button
     *
     * @param v
     * @return
     */
    @Override
    public boolean onLongClick(View v) {
        Intent settingIntent = new Intent(VoiceAssistActivity.this, SettingActivity.class);
        startActivity(settingIntent);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVoiceEntity.stopListen();
        mVoiceEntity.stopSpeak();
    }

    /**
     * handle permission request result
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int index = 0;
        for (String s : permissions) {
            if (grantResults[index] == 0) {
                if (Manifest.permission.READ_CONTACTS.equals(s)) {
                    //read contact info(sync)
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mContacts = Util.readContacts(VoiceAssistActivity.this);
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
            if (inRecognition) {
                mVoiceRecode.stopBreath();
                inRecognition = false;
            }
        }

        @Override
        public void onRecognizerStatusChanged(boolean status) {
            if (!status) {
                mVoiceRecode.stopBreath();
                inRecognition = false;
            }
        }

        @Override
        public void onAiuiAnalyserResult(Echo echo) {
            if (echo == null || echo.getEchoType() == Echo.TYPE_UNKNOWN) {
                showMessage(VoiceEntity.answerUnKnown, false);
                mVoiceEntity.speakMessage(VoiceEntity.answerUnKnown);
            } else {
                Log.i(TAG, "service type: " + echo.getEchoType() +
                        "\nservice: " + echo.getService() +
                        "\ninput text: " + echo.getInputMessage() +
                        "\necho text: " + echo.getEcho() +
                        "\nintent: " + echo.getIntent() +
                        "\nparams: " + echo.getParams().toString() +
                        "\nquery: " + echo.getResults().size()
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
            mVoiceEntity.speakMessage(echoMessage);
        } else {
            switch (echo.getIntent()) {
                case Echo.INTENT_DIAL:
                    handleDial(echo);
                    break;

                case Echo.INTENT_SET:
                    handleDial(echo);
                    break;

                case Echo.INTENT_QUERY:
                    if (echo.getService().equalsIgnoreCase("websearch")) {
                        if (echo.getResults().size() > 0) {
                            showSearchResult((WebSearchResult) echo.getResults().get(0));
                        }
                    } else {
                        String echoMessage = echo.getEcho();
                        showMessage(echoMessage, false);
                        mVoiceEntity.speakMessage(echoMessage);
                    }
                    break;

                case Echo.INTENT_RANDOM_SONG:
                    Util.playMusic(this, null, null);
                    break;

                case Echo.INTENT_SEARCH_SONG:
                    Util.playMusic(this, null, null);
                    break;

                case Echo.INTENT_OPEN_APP:
                    handleOpen(echo);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * echo: dial someone/number
     *
     * @param echo
     */
    private void handleDial(Echo echo) {
        if (echo.getParams().size() > 0) {
            String name = echo.getParams().get(0);
            String number = null;
            if (Util.isNumber(name)) {
                //number
                number = name;
            } else {
                //someone
                if (mContacts != null) {
                    for (ContactInfo contact : mContacts) {
                        if (contact.getName().equals(name))
                            number = contact.getNumber();
                    }
                }
            }
            if (number != null) {
                showMessage(VoiceEntity.answerCalling + name, false);
                startDial(number);
            } else {
                showMessage(echo.getEcho(), false);
                mVoiceEntity.speakMessage(echo.getEcho());
            }
        }
    }

    /**
     * dial number
     *
     * @param number
     */
    private void startDial(String number) {
        if (number == null || number.length() < 3)
            return;
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(dialIntent);
    }

    /**
     * echo: open app/device
     *
     * @param echo
     */
    private void handleOpen(Echo echo) {
        String appName = echo.getParams().get(0);
        //device
        if (appName.contains("蓝牙") || appName.equalsIgnoreCase("bluetooth")) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter adapter = bluetoothManager.getAdapter();
                if (adapter != null) {
                    if (!adapter.isEnabled()) {
                        showMessage(getResources().getString(R.string.opening_bluetooth), false);
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(enableIntent);
                    } else {
                        showMessage(getResources().getString(R.string.opened_bluetooth), false);
                    }
                } else {
                    showMessage(getResources().getString(R.string.not_found_bluetooth), false);
                }
            }
            return;
        } else if (appName.contains("无线网") || appName.equalsIgnoreCase("wifi")) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int state = wifiManager.getWifiState();
                if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
                    showMessage(getResources().getString(R.string.opening_wifi), false);
                    wifiManager.setWifiEnabled(true);
                } else if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
                    showMessage(getResources().getString(R.string.opened_wifi), false);
                }
            } else {
                showMessage(getResources().getString(R.string.not_found_wifi), false);
            }
            return;
        }

        //app
        if (mApps != null && mApps.size() > 0) {
            List<AppInfo> matchedApps = Util.matchApp(appName, mApps);
            if (matchedApps != null && matchedApps.size() > 0) {
                if (matchedApps.size() == 1) {
                    //find only one matched
                    String msg = VoiceEntity.answerOpen + appName;
                    showMessage(msg, false);
                    mVoiceEntity.speakMessage(msg);
                    Util.openAPPWithPackage(this, matchedApps.get(0).getPackageName());
                } else {
                    //find multi-matched
                    StringBuilder sb = new StringBuilder(VoiceEntity.answerFound);
                    for (int index = 0; index < matchedApps.size(); index++) {
                        AppInfo app = matchedApps.get(index);
                        sb.append(app.getAppName());
                        if (index < matchedApps.size() - 1)
                            sb.append(",");
                    }
                    showMessage(sb.toString(), false);
                    mVoiceEntity.speakMessage(sb.toString());
                }
            } else {
                String msg = VoiceEntity.answerNotFound + appName;
                showMessage(msg, false);
                mVoiceEntity.speakMessage(msg);
            }
        }
    }

    /**
     * display message in dialog list
     *
     * @param message
     * @param isUser
     */
    private void showMessage(String message, boolean isUser) {
        hintHandler.sendEmptyMessage(HANDLER_UNLOAD_HINT);
        if (message == null || message.equals("") || message.length() == 0)
            return;
        message = message.replace("\"", "");
        if (message.contains("[")) {
            int startIndex = message.indexOf("]") + 1;
            int endIndex = message.lastIndexOf("[");
            message = message.substring(startIndex, endIndex);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("object", false);
        map.put("message", message);
        map.put("role", isUser);
        mDialogs.add(map);
        if (mDialogs.size() > 50) {
            mDialogs.remove(0);
        }
        mDialogListAdapter.notifyDataSetChanged();
    }

    /**
     * display search result in dialog list
     *
     * @param result
     */
    private void showSearchResult(WebSearchResult result) {
        if (result == null)
            return;
        Map<String, Object> map = new HashMap<>();
        map.put("object", true);
        map.put("message", result);
        map.put("role", false);
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
        if (mVoiceEntity != null && !contactLexicon.equals(Util.readStorageContacts(VoiceAssistActivity.this))) {
            mVoiceEntity.updateLexicon("contact", contactLexicon, new LexiconListener() {
                @Override
                public void onLexiconUpdated(String s, SpeechError speechError) {
                    if (speechError == null) {
                        Util.writeStorageContacts(VoiceAssistActivity.this, contactLexicon);
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
