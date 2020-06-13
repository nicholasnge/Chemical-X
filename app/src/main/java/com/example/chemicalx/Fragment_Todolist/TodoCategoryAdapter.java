package com.example.chemicalx.Fragment_Todolist;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chemicalx.R;

import java.util.ArrayList;
import java.util.List;

public class TodoCategoryAdapter extends RecyclerView.Adapter<TodoCategoryAdapter.CategoryViewHolder> {
    Context context;
    List<TodoCategoryModel> mCategoryList;
    LayoutInflater mLayoutInflater = null;

    public TodoCategoryAdapter(Context context, List<TodoCategoryModel> mCategoryList){
        super();
        this.context = context;
        this.mCategoryList = mCategoryList;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new TodoCategoryAdapter.CategoryViewHolder(mLayoutInflater.inflate(R.layout.category_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        TodoCategoryModel categoryModel = mCategoryList.get(position);
        holder.categoryTitle.setText(categoryModel.title);
        ArrayList<TodoItemModel> todoList = categoryModel.todoList;

        TodoItemAdapter todoItemAdapter = new TodoItemAdapter(context, todoList, categoryModel.backgroundColor, categoryModel.progressColor);
        holder.categoryRecyclerView.setLayoutManager(new LinearLayoutManager(context) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        holder.categoryRecyclerView.setAdapter(todoItemAdapter);
    }

    @Override
    public int getItemCount() {
        return mCategoryList.size();
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        final TextView categoryTitle;
        final RecyclerView categoryRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTextView);
            categoryRecyclerView = itemView.findViewById(R.id.categoryRecyclerView);
        }
    }
}