package com.example.ggwave;

import com.google.gson.annotations.SerializedName;

public class KeyResp {
    @SerializedName("str")
    private String key;

    public KeyResp(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
