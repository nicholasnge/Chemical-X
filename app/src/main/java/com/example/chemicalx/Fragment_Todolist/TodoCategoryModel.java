package com.example.chemicalx.Fragment_Todolist;

import java.util.ArrayList;

public class TodoCategoryModel {
    String title;
    ArrayList<TodoItemModel> todoList;
    int backgroundColor;
    int progressColor;

    public TodoCategoryModel(String title, ArrayList<TodoItemModel> todoList, int backgroundColor, int progressColor) {
        this.title = title;
        this.todoList = todoList;
        this.backgroundColor = backgroundColor;
        this.progressColor = progressColor;
    }
}
