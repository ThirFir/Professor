package com.example.ggwave;

import com.google.gson.annotations.SerializedName;

public class LectureResp {
    @SerializedName("lectureCode")
    String lectureCode;

    @SerializedName("lectureName")
    String lectureName;

    @SerializedName("professorId")
    String professorId;

    @SerializedName("startTime")
    String startTime;

    @SerializedName("endTime")
    String endTime;

    public LectureResp(String lectureCode, String lectureName, String professorId, String startTime, String endTime) {
        this.lectureCode = lectureCode;
        this.lectureName = lectureName;
        this.professorId = professorId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getLectureCode() {
        return lectureCode;
    }

    public String getLectureName() {
        return lectureName;
    }

    public String getProfessorId() {
        return professorId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

}
