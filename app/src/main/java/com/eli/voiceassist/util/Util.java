package com.eli.voiceassist.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;


import com.eli.voiceassist.mode.AppInfo;
import com.eli.voiceassist.mode.ContactInfo;
import com.eli.voiceassist.mode.Echo;
import com.eli.voiceassist.mode.VoiceMessage;
import com.google.gson.Gson;
import com.iflytek.aiui.AIUIEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class Util {

    private static final String TAG = "elifli";

    private static List<String> allPermissions = new ArrayList<>();
    static {
        allPermissions.add(Manifest.permission.READ_CONTACTS);
        allPermissions.add(Manifest.permission.RECORD_AUDIO);
        allPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        allPermissions.add(Manifest.permission.READ_PHONE_STATE);
        allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        allPermissions.add(Manifest.permission.CAMERA);
    }

    public static String getAIUIParams(Context context) {
        String value = "";
        AssetManager manager = context.getResources().getAssets();
        try {
            InputStream ins = manager.open("cfg/aiui_phone.cfg");
            byte buffer[] = new byte[ins.available()];
            ins.read(buffer);
            value = new String(buffer);
        } catch (Exception e) {
        }
        return value;
    }

    public static String parseVoice(String voiceResult) {
        Gson gson = new Gson();
        VoiceMessage message = gson.fromJson(voiceResult, VoiceMessage.class);

        return message.getWord();
    }

    public static JSONObject getSemanticResult(AIUIEvent event) {
        try {
            JSONObject data = new JSONObject(event.info).getJSONArray("data").getJSONObject(0);
            JSONObject params = data.getJSONObject("params");
            JSONObject content = data.getJSONArray("content").getJSONObject(0);

            if (content.has("cnt_id")) {
                String cnt_id = content.getString("cnt_id");
                JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));
                String sub = params.optString("sub");
                if (sub.equals("nlp")) {
                    return cntJson.optJSONObject("intent");
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String randomString(int length) {
        if (length <= 0)
            return "";

        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i ++) {
            char c = (char) (random.nextInt(92) + 33);
            sb.append(c);
        }
        return sb.toString();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static void playSound(Context context, int rawID) {
        final SoundPool pool;
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            AudioAttributes.Builder attrBuild = new AudioAttributes.Builder();
            attrBuild.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuild.build());
            pool = builder.build();
        } else {
            pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        }

        pool.load(context, rawID, 1);
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                pool.play(1, 1, 1, 0, 0, 1);
            }
        });
    }

    public static Echo parseEchoMessage(JSONObject echoObject) {
        if (echoObject.isNull("service")) {
            return null;
        }
        Echo result = null;
        try {
            String service = echoObject.optString("service");
            //设置类型
            if (service.equals("openQA")) {
                //问答
                result = new Echo(Echo.TYPE_OPEN_QA);
            } else if (service.contains(Echo.AIUI_CUSTOM_MARK)) {
                //技能
                result = new Echo(Echo.TYPE_SKILL);
            } else {
                //开放技能
                result = new Echo(Echo.TYPE_OPEN_SKILL);
            }
            result.setService(service);
            //设置输入信息
            result.setInputMessage(echoObject.optString("text"));
            //设置回应信息
            if (!echoObject.isNull("answer")) {
                JSONObject answer = echoObject.getJSONObject("answer");
                if (answer != null) {
                    result.setEchoMessage(answer.optString("text"));
                }
            }
            //设置意图
            if (result.getEchoType() != Echo.TYPE_OPEN_QA && !echoObject.isNull("semantic")) {
                JSONObject semantic = echoObject.getJSONArray("semantic").getJSONObject(0);
                String intent = semantic.optString("intent");
                result.setIntent(intent);

                //设置参数
                if (!semantic.isNull("slots")) {
                    JSONArray slots = semantic.getJSONArray("slots");
                    for (int i = 0; i < slots.length(); i ++) {
                        JSONObject slot = slots.getJSONObject(i);
                        String value = slot.optString("value");
                        result.addParam(value);
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return result;
    }

    public static List<ContactInfo> readContacts(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        List<ContactInfo> contacts = new ArrayList<>();
        try {
            Uri contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(contactUri, new String[]{"display_name", "sort_key", "contact_id", "data1"}, null, null, "sort_key");
            String name;
            String number;
            String sortKey;
            int id;
            while (cursor.moveToNext()) {
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                id = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                sortKey = cursor.getString(0);
                ContactInfo contact = new ContactInfo(name, number, sortKey, id);
                contacts.add(contact);
            }
            cursor.close();
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return contacts;
    }

    public static void permissionRequest(Context context) {
        List<String> requestPermission = new ArrayList<>();
        for (String permission : allPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.add(permission);
            }
        }
        if (requestPermission != null && requestPermission.size() > 0)
            ActivityCompat.requestPermissions((Activity) context, requestPermission.toArray(new String[requestPermission.size()]), 1);
    }

    public static boolean isNumber(String values) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(values).matches();
    }

    public static void startDial(Context context, String number) {
        if (number == null || number.length() < 3)
            return;
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        context.startActivity(dialIntent);
    }

    public static void playMusic(Context context, String song, String artist) {
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        /*if (Build.VERSION.SDK_INT >= 15) {
            intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent("android.intent.action.MUSIC_PLAYER");
        }

        if (song != null) {
            Uri uri = Uri.parse(song);
            intent.setDataAndType(uri, "audio/*");
        } else if (artist != null) {

        } else {

        }*/

        context.startActivity(intent);
    }

    public static List<AppInfo> readAppList(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> infos = packageManager.getInstalledPackages(0);

        List<AppInfo> apps = new ArrayList<>();

        for (PackageInfo packageInfo : infos) {
            String appName = packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
            String packageName = packageInfo.packageName;
            apps.add(new AppInfo(appName, packageName));
        }

        return apps;
    }

    public static void writeStorageContacts(Context context, String contactLexicon) {
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "contacts");
            FileWriter fw = new FileWriter(contactFile);
            fw.write(contactLexicon);
            fw.close();
        } catch (Exception e) {
        }
    }

    public static String readStorageContacts(Context context) {
        String value = null;
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "contacts");
            if (contactFile.exists()) {
                FileReader fr = new FileReader(contactFile);
                char buffer[] = new char[1024];
                StringBuilder sb = new StringBuilder();
                int length;
                while ((length = fr.read(buffer)) > 0) {
                    sb.append(buffer, 0, length);
                }
                value = sb.toString();
            }
        } catch (Exception e) {
        }
        return value;
    }

    public static void openAPPWithPackage(final Context context, final String packageName) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    context.startActivity(intent);
                }
            }
        }, 500);
    }

    public static List<AppInfo> matchApp(String appName, List<AppInfo> apps) {
        List<AppInfo> values = new ArrayList<>();

        for (AppInfo app : apps) {
            if (app.getAppName().equalsIgnoreCase(appName)) {
                values.add(app);
                return values;
            }
        }

        for (AppInfo app : apps) {
            if (app.getAppName().contains(appName)) {
                values.add(app);
            }
        }

        return values;
    }
}
