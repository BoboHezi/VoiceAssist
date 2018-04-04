package com.eli.voiceassist.mode;

/**
 * Created by zhanbo.zhang on 2018/4/3.
 */

public class AppInfo {

    private String appName;
    private String packageName;

    public AppInfo(String appName, String packageName) {
        this.appName = appName;
        this.packageName = packageName;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getPackageName() {
        return this.packageName;
    }
}
