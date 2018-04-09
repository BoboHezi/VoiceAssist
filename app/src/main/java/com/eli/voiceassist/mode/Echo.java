package com.eli.voiceassist.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public final class Echo {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_OPEN_QA = 1;
    public static final int TYPE_SKILL = 2;
    public static final int TYPE_OPEN_SKILL = 3;

    public static final String INTENT_DIAL = "DIAL";
    public static final String INTENT_QUERY = "QUERY";
    public static final String INTENT_DIAL_SOMEONE = "dial_someone";
    public static final String INTENT_CREATE_CONTACT = "create_contact";
    public static final String INTENT_DIAL_NUMBER = "dial_number";
    public static final String INTENT_SEARCH_SONG = "search_by_song";
    public static final String INTENT_RANDOM_SONG = "random_song";
    public static final String INTENT_OPEN_APP = "open_app";

    public static final String AIUI_CUSTOM_MARK = "ELI";

    //回应类型(service type)
    private int echoType;
    //服务
    private String service;
    //输入信息
    private String inputMessage;
    //回应信息
    private String echoMessage;
    //意图
    private String intent;
    //参数
    private List<String> params;

    private Echo() {
    }

    public Echo(int type) {
        this(type, null);
    }

    public Echo(int type, String input) {
        this(type, input, null);
    }

    public Echo(int type, String input, String echo) {
        this(type, input, echo, null);
    }

    public Echo(int type, String input, String echo, String intent) {
        this.echoType = type;
        this.inputMessage = input;
        this.echoMessage = echo;
        this.intent = intent;
        this.params = new ArrayList<>();
    }

    public int getEchoType() {
        return this.echoType;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return this.service;
    }

    public void setInputMessage(String message) {
        this.inputMessage = message;
    }

    public String getInputMessage() {
        return this.inputMessage;
    }

    public void setEchoMessage(String message) {
        this.echoMessage = message;
    }

    public String getEcho() {
        return this.echoMessage;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getIntent() {
        return this.intent;
    }

    public void addParam(String param) {
        this.params.add(param);
    }

    public List<String> getParams() {
        return this.params;
    }
}
