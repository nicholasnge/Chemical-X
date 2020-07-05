package com.example.chemicalx.Fragment_Tasks;

import android.content.Context;
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

public class TaskCategoryAdapter extends RecyclerView.Adapter<TaskCategoryAdapter.CategoryViewHolder> {
    Context context;
    List<TaskCategoryModel> mCategoryList;
    LayoutInflater mLayoutInflater = null;

    public TaskCategoryAdapter(Context context, List<TaskCategoryModel> mCategoryList){
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
        return new TaskCategoryAdapter.CategoryViewHolder(mLayoutInflater.inflate(R.layout.category_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        TaskCategoryModel categoryModel = mCategoryList.get(position);
        holder.categoryTitle.setText(categoryModel.title);
        ArrayList<TaskItemModel> todoList = categoryModel.todoList;

        TaskItemAdapter todoItemAdapter = new TaskItemAdapter(context, todoList, categoryModel.backgroundColor, categoryModel.progressColor);
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