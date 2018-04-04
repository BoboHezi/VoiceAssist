package com.eli.voiceassist;

import org.json.JSONObject;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public class ParseAIUI {

    public static AIUIEcho parseMessage(String message) {
        AIUIEcho result = null;
        if (message.contains("answer")) {
            try {
                JSONObject answer = new JSONObject(message).getJSONObject("answer");
                String echo = answer.optString("text");
                result = new AIUIEcho(AIUIEcho.TYPE_ANSWER, echo);
            } catch (Exception e) {
            }
        }
        return result;
    }
}
