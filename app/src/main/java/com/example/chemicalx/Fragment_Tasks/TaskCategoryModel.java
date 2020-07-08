package com.example.chemicalx.Fragment_Tasks;

import java.util.ArrayList;

public class TaskCategoryModel {
    String title;
    ArrayList<TaskItemModel> taskList;
    int backgroundColor;
    int progressColor;

    public TaskCategoryModel(String title, ArrayList<TaskItemModel> todoList, int backgroundColor, int progressColor) {
        this.title = title;
        this.taskList = todoList;
        this.backgroundColor = backgroundColor;
        this.progressColor = progressColor;
    }
}
