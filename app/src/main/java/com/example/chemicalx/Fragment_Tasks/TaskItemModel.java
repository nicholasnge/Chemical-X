package com.example.chemicalx.Fragment_Tasks;

public class TaskItemModel {
    String title;
    int totalTime;
    int timePassed;
    int progressBar;

    public TaskItemModel(String title, int totalTime, int timePassed) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed / totalTime *100;
    }

    // temporary fix
    public TaskItemModel(String title, int totalTime) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = 0;
        this.progressBar = timePassed / totalTime *100;
    }
}
