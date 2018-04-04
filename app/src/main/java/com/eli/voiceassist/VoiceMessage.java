package com.eli.voiceassist;

import java.util.ArrayList;

/**
 * Created by zhanbo.zhang on 2018/3/27.
 */
public class VoiceMessage {
    public ArrayList<WSBean> ws;

    public String getWord() {
        StringBuffer sb = new StringBuffer();
        for (WSBean wsBean : ws) {
            String word = wsBean.cw.get(0).w;
            sb.append(word);
        }

        return sb.toString();
    }

    class WSBean {
        public ArrayList<CWBean> cw;
    }

    class CWBean {
        public String w;
    }
}
