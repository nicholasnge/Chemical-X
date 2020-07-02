package com.example.chemicalx.Fragment_Schedule;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeLineModel {
    String message;
    String date;
    long dtstart;
    OrderStatus status;

    public TimeLineModel (String message, String date, OrderStatus status) {
        this.message = message;
        this.date = date;
        this.status = status;
    }

    public TimeLineModel (String message, long dtstart, OrderStatus status) {
        this.message = message;
        this.dtstart = dtstart;
        this.status = status;
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

    public void setDate(String date) {
        this.date = date;
    }

    public long getDtstart() {
        return dtstart;
    }

    public void setDtstart(long dtstart) {
        this.dtstart = dtstart;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
