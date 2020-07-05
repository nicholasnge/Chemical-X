package com.example.chemicalx.Fragment_Todolist;

import android.widget.ProgressBar;

public class TaskItemModel {
    String title;
    int totalTime;
    int timePassed;
    int progressBar;

    public TaskItemModel(String title, int timePassed, int totalTime) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed*100 / totalTime ;
    }

    // temporary fix
    public TodoItemModel(String title, int totalTime) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = 0;
        this.progressBar = timePassed / totalTime *100;
    }
}
