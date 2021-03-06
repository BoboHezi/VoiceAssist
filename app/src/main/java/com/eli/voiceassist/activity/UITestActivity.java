package com.eli.voiceassist.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.eli.voiceassist.R;
import com.eli.voiceassist.mode.BaiduTransResult;
import com.eli.voiceassist.util.Util;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhanbo.zhang on 2018/3/29.
 */
public class UITestActivity extends AppCompatActivity {

    private static final String TAG = "elifli";

    private static final String APP_ID = "20180413000145870";
    private static final String SECURITY_KEY = "BesAbMvjZIpB3RayXd9p";
    private static final String TRANS_API_HOST = "http://api.fanyi.baidu.com/api/trans/vip/translate";

    /*private EditText textSource;
    private Button btTranslate;
    private TextView textResult;*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_test_activity);
        initialView();
    }

    private void initialView() {
        /*textSource = (EditText) findViewById(R.id.et_input);
        btTranslate = (Button) findViewById(R.id.bt_trans);
        textResult = (TextView) findViewById(R.id.tran_result);

        btTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResult(getSource());
                translate(getSource());
            }
        });*/
    }

    /**
     * client=at
     * &sl=en
     * &tl=zh-CN
     * &dt=t
     * &q=google
     *
     * @param source
     */
    private void translate(String source) {
        Map<String, String> params = Util.buildParams(source, "auto", Util.isChinese(source) ? "en" : "zh", APP_ID, SECURITY_KEY);
        String url = Util.getUrlWithQueryString(TRANS_API_HOST, params);
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "Failure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, "Response Code: " + response.code());

                Gson gson = new Gson();
                BaiduTransResult transInfo = gson.fromJson(response.body().string(), BaiduTransResult.class);
                if (transInfo.getTrans_result() != null) {
                    BaiduTransResult.Result results = transInfo.getTrans_result().get(0);
                    Log.i(TAG, results.getDst());
                }
            }
        });
    }

    private String getSource() {
        /*if (textSource != null) {
            return textSource.getText().toString();
        }*/
        return null;
    }

    private void showResult(String msg) {
        /*if (textResult != null) {
            textResult.setText(textResult.getText() + "\n" + msg);
        }*/
    }
}