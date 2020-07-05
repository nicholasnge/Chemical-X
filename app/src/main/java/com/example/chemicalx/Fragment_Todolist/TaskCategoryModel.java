package com.example.chemicalx.Fragment_Todolist;

import java.util.ArrayList;

public class TaskCategoryModel {
    String title;
    ArrayList<TaskItemModel> todoList;
    int backgroundColor;
    int progressColor;

    public TaskCategoryModel(String title, ArrayList<TaskItemModel> todoList, int backgroundColor, int progressColor) {
        this.title = title;
        this.todoList = todoList;
        this.backgroundColor = backgroundColor;
        this.progressColor = progressColor;
    }
}
