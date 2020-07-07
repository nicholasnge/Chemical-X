package com.example.chemicalx.Fragment_Tasks;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCategoryAdapter extends RecyclerView.Adapter<TaskCategoryAdapter.CategoryViewHolder> {
    Fragment_Tasks fragment_tasks;
    List<TaskCategoryModel> mCategoryList;
    LayoutInflater mLayoutInflater = null;
    Map<String, TaskItemAdapter> taskItemAdapters = new HashMap<>();
    Map<TaskItemAdapter, CategoryViewHolder> viewholders = new HashMap<>();

    public TaskCategoryAdapter(Fragment_Tasks fragment_tasks, List<TaskCategoryModel> mCategoryList) {
        super();
        this.fragment_tasks = fragment_tasks;
        this.mCategoryList = mCategoryList;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new TaskCategoryAdapter.CategoryViewHolder(mLayoutInflater.inflate(R.layout.category_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        TaskCategoryModel categoryModel = mCategoryList.get(position);
        holder.categoryTitle.setText(categoryModel.title);
        ArrayList<TaskItemModel> taskList = categoryModel.taskList;

        TaskItemAdapter t = new TaskItemAdapter(fragment_tasks, taskList, categoryModel.backgroundColor, categoryModel.progressColor);
        taskItemAdapters.put(categoryModel.title, t);
        viewholders.put(t, holder);
        holder.categoryRecyclerView.setLayoutManager(new LinearLayoutManager(fragment_tasks.getContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        holder.categoryRecyclerView.setAdapter(t);
        holder.categoryTitle.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mCategoryList.size();
    }

    public void notifyChange(String category) {
        TaskItemAdapter t = taskItemAdapters.get(category);
        if (t == null) {
            return;
        }
        if (t.taskList.size() != 0) {
            viewholders.get(t).categoryTitle.setVisibility(View.VISIBLE);
            return;
        }
        viewholders.get(t).categoryTitle.setVisibility(View.GONE);
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