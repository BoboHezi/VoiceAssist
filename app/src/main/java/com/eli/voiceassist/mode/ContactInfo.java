package com.eli.voiceassist.mode;

/**
 * Created by zhanbo.zhang on 2018/4/2.
 */

public class ContactInfo {

    private String name;
    private String number;
    private String sortKey;
    private int id;

    public ContactInfo(String name, String number, String sortKey, int id) {
        this.name = name;
        this.number = number;
        this.sortKey = sortKey;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getNumber() {
        return this.number;
    }

    public String getSortKey() {
        return this.sortKey;
    }

    public int getId() {
        return this.id;
    }
}
