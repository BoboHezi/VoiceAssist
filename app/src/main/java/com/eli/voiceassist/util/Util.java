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
import android.text.TextUtils;
import android.util.Log;

import com.eli.voiceassist.R;
import com.eli.voiceassist.mode.AppInfo;
import com.eli.voiceassist.mode.ContactInfo;
import com.eli.voiceassist.mode.Echo;
import com.eli.voiceassist.mode.SettingParams;
import com.eli.voiceassist.mode.VoiceMessage;
import com.eli.voiceassist.mode.WebSearchResult;
import com.google.gson.Gson;
import com.iflytek.aiui.AIUIEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class Util {

    private static final String TAG = "elifli";
    //list of request permission
    private static List<String> allPermissions = new ArrayList<>();
    //map of calculated dp
    private static Map<Float, Integer> calculatedValues = new HashMap<>();

    static {
        allPermissions.add(Manifest.permission.READ_CONTACTS);
        allPermissions.add(Manifest.permission.RECORD_AUDIO);
        allPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        allPermissions.add(Manifest.permission.READ_PHONE_STATE);
        allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        allPermissions.add(Manifest.permission.CAMERA);
    }

    /**
     * read aiui params
     *
     * @param context
     * @return
     */
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

    public static String parseRecognizerResult(String voiceResult) {
        Gson gson = new Gson();
        VoiceMessage message = gson.fromJson(voiceResult, VoiceMessage.class);
        if (message == null)
            return "";
        return message.getWord();
    }

    public static SettingParams parseSettingParams(String value) {
        if (value == null || TextUtils.isEmpty(value))
            return null;

        SettingParams params = new SettingParams();
        try {
            Gson gson = new Gson();
            params = gson.fromJson(value, SettingParams.class);
        } catch (Exception e) {
        }

        return params;
    }

    /**
     * parse aiui result
     *
     * @param event
     * @return
     */
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
        for (int i = 0; i < length; i++) {
            char c = (char) (random.nextInt(92) + 33);
            sb.append(c);
        }
        return sb.toString();
    }

    public static String[] randomHints(Context context, int length) {
        String allHints[] = context.getResources().getStringArray(R.array.hint_messages);
        String hints[] = new String[length];
        hints[0] = allHints[0];

        Random random = new Random();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 1; i < hints.length; i++) {
            while (true) {
                int j = random.nextInt(allHints.length - 1) + 1;
                if (!indexes.contains(j)) {
                    indexes.add(j);
                    break;
                }
            }
            hints[i] = allHints[indexes.get(i - 1)];
        }
        return hints;
    }

    public static int dip2px(Context context, float dpValue) {
        if (calculatedValues != null && calculatedValues.containsKey(dpValue))
            return calculatedValues.get(dpValue);
        int value = (int) (dpValue * context.getResources().getDisplayMetrics().density + 0.5f);
        if (calculatedValues != null) {
            calculatedValues.put(dpValue, value);
        }
        return value;
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
                pool.play(1, 1, 1, 1, 0, 1);
            }
        });
    }

    public static Echo parseEchoMessage(JSONObject echoObject) {
        if (echoObject.isNull("service")) {
            return new Echo(Echo.TYPE_UNKNOWN, echoObject.optString("text"));
        }
        Echo value = null;
        try {
            String service = echoObject.optString("service");
            //设置类型
            if (service.equals("openQA")) {
                //问答
                value = new Echo(Echo.TYPE_OPEN_QA);
            } else if (service.contains(Echo.AIUI_CUSTOM_MARK)) {
                //技能
                value = new Echo(Echo.TYPE_SKILL);
            } else {
                //开放技能
                value = new Echo(Echo.TYPE_OPEN_SKILL);
            }
            value.setService(service);
            //设置输入信息
            value.setInputMessage(echoObject.optString("text"));
            //设置回应信息
            if (!echoObject.isNull("answer")) {
                JSONObject answer = echoObject.getJSONObject("answer");
                if (answer != null) {
                    value.setEchoMessage(answer.optString("text"));
                }
            }
            //设置意图
            if (value.getEchoType() != Echo.TYPE_OPEN_QA && !echoObject.isNull("semantic")) {
                JSONObject semantic = echoObject.getJSONArray("semantic").getJSONObject(0);
                String intent = semantic.optString("intent");
                value.setIntent(intent);

                //设置参数
                if (!semantic.isNull("slots")) {
                    JSONArray slots = semantic.getJSONArray("slots");
                    for (int i = 0; i < slots.length(); i++) {
                        JSONObject slot = slots.getJSONObject(i);
                        String param = slot.optString("value");
                        value.addParam(param);
                    }
                }
            }
            if (service.equalsIgnoreCase("websearch")) {
                JSONArray results = echoObject.getJSONObject("data").getJSONArray("result");
                List<Object> searchResults = new ArrayList<>();
                for (int index = 0; index < results.length(); index++) {
                    JSONObject object = results.getJSONObject(index);
                    Gson gson = new Gson();
                    WebSearchResult result = gson.fromJson(object.toString(), WebSearchResult.class);
                    searchResults.add(result);
                }
                value.setResults(searchResults);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return value;
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

    public static void writeStorageParams(Context context, String value) {
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "params");
            writeStorage(contactFile, value);
        } catch (Exception e) {
        }
    }

    public static void writeStorageContacts(Context context, String contactLexicon) {
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "contacts");
            writeStorage(contactFile, contactLexicon);
        } catch (Exception e) {
        }
    }

    private static void writeStorage(File file, String value) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(value);
            fw.close();
        } catch (Exception e) {
        }
    }

    public static String readStorageParams(Context context) {
        String value = null;
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "params");
            value = readStorage(contactFile);
        } catch (Exception e) {
        }
        return value;
    }

    public static String readStorageContacts(Context context) {
        String value = null;
        try {
            File contactFile = new File(context.getExternalFilesDir(""), "contacts");
            value = readStorage(contactFile);
        } catch (Exception e) {
        }
        return value;
    }

    private static String readStorage(File file) {
        if (file == null || !file.exists())
            return "";
        String value = null;
        try {
            FileReader fr = new FileReader(file);
            char buffer[] = new char[1024];
            StringBuilder sb = new StringBuilder();
            int length;
            while ((length = fr.read(buffer)) > 0) {
                sb.append(buffer, 0, length);
            }
            value = sb.toString();
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
            if (app.getAppName().toLowerCase().contains(appName.toLowerCase())) {
                values.add(app);
            }
        }

        return values;
    }

    public static String getSystemLanguage(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(Locale.getDefault().getLanguage());
        sb.append("_");
        String country = context.getResources().getConfiguration().locale.getCountry();
        sb.append(country);
        return sb.toString();
    }
}
