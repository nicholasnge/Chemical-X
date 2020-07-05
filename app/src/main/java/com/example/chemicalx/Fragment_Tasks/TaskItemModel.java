package com.example.chemicalx.Fragment_Tasks;

public class TaskItemModel {
    String title;
    int totalTime;// in seconds
    int timePassed;// in seconds
    int progressBar;

    public TaskItemModel(String title, int timePassed, int totalTime) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed*100 / totalTime ;
    }

    // temporary fix
    public TaskItemModel(String title, int totalTime) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = 0;
        this.progressBar = timePassed*100 / totalTime ;
    }

    public void incrementProgress(){
        this.timePassed +=1;
        this.progressBar = timePassed*100 / totalTime ;
    }
}
