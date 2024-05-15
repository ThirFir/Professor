package com.example.ggwave;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ServerApi {
    @GET("")
    Call<KeyResp> getRandomKey();

    @GET("lecturelist")
    Call<List<LectureResp>> getLectureList(@Query("professorID") String professorId);
}
