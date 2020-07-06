package com.example.chemicalx.Fragment_Tasks;

public class TaskItemModel {
    String title;
    int totalTime;// in seconds
    int timePassed;// in seconds
    int progressBar;
    String category;
    String docID;

    public TaskItemModel(String title, int timePassed, int totalTime, String category, String docID) {
        this.title = title;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed*100 / totalTime;
        this.category = category;
        this.docID = docID;
    }

    public void incrementProgress(){
        this.timePassed +=1;
        this.progressBar = timePassed*100 / totalTime ;
    }
}
