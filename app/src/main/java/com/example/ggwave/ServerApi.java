package com.example.ggwave;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ServerApi {
    @GET("")
    Call<KeyResp> getRandomKey();
}
