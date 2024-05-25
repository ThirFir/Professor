package com.example.ggwave;

import com.google.gson.annotations.SerializedName;

public class StudentResp {

    @SerializedName("studentId")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("attendance")
    public boolean attendance;

    public StudentResp(String id, String name, boolean attendance) {
        this.id = id;
        this.name = name;
        this.attendance = attendance;
    }

    public String getName() {
        return name;
    }
    public String getId() {
        return id;
    }
    public boolean getAttendance() {
        return attendance;
    }

}
