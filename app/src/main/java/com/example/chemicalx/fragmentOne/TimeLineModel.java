package com.example.chemicalx.fragmentOne;

public class TimeLineModel {
    String message;
    String date;
    OrderStatus status;

    public TimeLineModel (String message, String date, OrderStatus status) {
        this.message = message;
        this.date = date;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
