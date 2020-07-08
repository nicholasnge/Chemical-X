package com.example.chemicalx.Fragment_Schedule;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeLineModel implements Comparable<TimeLineModel>{
    String message;
    long dtstart;
    long dtend;
    OrderStatus status;
    boolean isFromTasks = false;

    public TimeLineModel (String message, long dtstart, long dtend, OrderStatus status) {
        this.message = message;
        this.dtstart = dtstart;
        this.dtend = dtend;
        this.status = status;
    }
    public TimeLineModel (String message, long dtstart, long dtend, OrderStatus status, boolean isFromTasks) {
        this.message = message;
        this.dtstart = dtstart;
        this.dtend = dtend;
        this.status = status;
        this.isFromTasks = isFromTasks;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        // get date of the time of the event being represented
        // and format it to something like "14:23 pm"
        Date date = new Date(dtstart);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
        return simpleDateFormat.format(date);
    }

    @Override
    public int compareTo(TimeLineModel o) {
        return (int) (this.dtstart - o.dtstart);
    }
}
