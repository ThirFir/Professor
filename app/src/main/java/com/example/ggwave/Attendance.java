package com.example.ggwave;

public class Attendance {
    private String name;
    private String time;
    private String id;

    public Attendance(String name, String time, String id) {
        this.name = name;
        this.time = time;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
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
        return name.equals(attendance.name) &&
                time.equals(attendance.time) &&
                id.equals(attendance.id);
    }
}
