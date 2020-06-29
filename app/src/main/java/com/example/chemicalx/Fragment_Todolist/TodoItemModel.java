package com.example.chemicalx.Fragment_Todolist;

import android.widget.ProgressBar;

public class TodoItemModel {
    String title;
    int totalTime;
    int timePassed;
    int progressBar;

    public TodoItemModel(String title, int totalTime, int timePassed) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed / totalTime *100;
    }
}
