package com.example.chemicalx.Fragment_Tasks;

import com.example.chemicalx.Category;
import com.google.firebase.Timestamp;

public class TaskItemModel {
    String docID;
    String title;
    String category;
    int totalTime;// in seconds
    int timePassed;// in seconds
    int progressBar;
    Timestamp dueDate;


    public TaskItemModel(String docID, String title, String category, int totalTime, int timePassed, Timestamp dueDate) {
        this.docID = docID;
        this.title = title;
        this.category = category;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed * 100 / totalTime;
        this.dueDate = dueDate;
    }

    public void incrementProgress() {
        this.timePassed += 1;
        this.progressBar = timePassed * 100 / totalTime;
    }

    public String getTitle() {
        return title;
    }
    public String getCategory(){ return category;}
}
