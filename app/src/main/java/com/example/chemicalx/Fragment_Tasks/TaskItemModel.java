package com.example.chemicalx.Fragment_Tasks;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.chemicalx.Category;
import com.google.firebase.Timestamp;

public class TaskItemModel implements Parcelable {
    String docID;
    String title;
    String category;
    int totalTime;// in seconds
    int timePassed;// in seconds
    int progressBar;
    public Timestamp dueDate;


    public TaskItemModel(String docID, String title, String category, int totalTime, int timePassed, Timestamp dueDate) {
        this.docID = docID;
        this.title = title;
        this.category = category;
        this.totalTime = totalTime;
        this.timePassed = timePassed;
        this.progressBar = timePassed * 100 / totalTime;
        this.dueDate = dueDate;
    }

    protected TaskItemModel(Parcel in) {
        docID = in.readString();
        title = in.readString();
        category = in.readString();
        totalTime = in.readInt();
        timePassed = in.readInt();
        progressBar = in.readInt();
        dueDate = in.readParcelable(Timestamp.class.getClassLoader());
    }

    public static final Creator<TaskItemModel> CREATOR = new Creator<TaskItemModel>() {
        @Override
        public TaskItemModel createFromParcel(Parcel in) {
            return new TaskItemModel(in);
        }

        @Override
        public TaskItemModel[] newArray(int size) {
            return new TaskItemModel[size];
        }
    };

    public void incrementProgress() {
        this.timePassed += 1;
        this.progressBar = timePassed * 100 / totalTime;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public int getTimePassed() {
        return timePassed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(docID);
        dest.writeString(title);
        dest.writeString(category);
        dest.writeInt(totalTime);
        dest.writeInt(timePassed);
        dest.writeInt(progressBar);
        dest.writeParcelable(dueDate, flags);
    }
}
