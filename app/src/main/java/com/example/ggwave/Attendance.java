package com.example.ggwave;

public class Attendance {
    private String id;
    private String name;
    private boolean isAttended;

    public Attendance(String id, String name, boolean isAttended) {
        this.id = id;
        this.name = name;
        this.isAttended = isAttended;
    }

    public String getName() {
        return name;
    }
    public boolean getAttended() {
        return isAttended;
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
        return id.equals(attendance.id) &&
                name.equals(attendance.name) &&
                    isAttended == attendance.isAttended;
    }
}
