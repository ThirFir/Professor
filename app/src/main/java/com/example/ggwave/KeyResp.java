package com.example.ggwave;

import com.google.gson.annotations.SerializedName;

public class KeyResp {
    @SerializedName("key")
    private String key;

    public String getKey() {
        return key;
    }
}
