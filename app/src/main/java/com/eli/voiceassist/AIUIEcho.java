package com.eli.voiceassist;

import java.util.List;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public class AIUIEcho {

    public static final int TYPE_ANSWER = 1;
    public static final int TYPE_ACTION = 2;

    public static final int INTENT_DIAL = 1;
    public static final int INTENT_SEARCH_BY_SONG = 2;
    public static final int INTENT_RANDOM_SONG = 3;
    public static final int INTENT_SEARCH_BY_ARTIST = 4;

    private int type = 0;
    private String echo;
    private List<String> params;

    public AIUIEcho() {
        this(0);
    }

    public AIUIEcho(int type) {
        this(type, null);
    }

    public AIUIEcho(int type, String echo) {
        this(type, echo, null);
    }

    public AIUIEcho(int type, String echo, List<String> params) {
        this.type = type;
        this.echo = echo;
        this.params = params;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public void addParams(String param) {
        this.params.add(param);
    }
}
