package com.example.ggwave;

public class Attendance {
    private String id;
    private String key;

    public Attendance(String id, String key) {
        this.key = key;
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Attendance attendance = (Attendance) o;
        return id.equals(attendance.id) && key.equals(attendance.key);
    }
}
