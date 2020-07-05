package com.example.chemicalx.Fragment_Tasks;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TodoViewHolder> {
    Fragment_Tasks fragment_tasks;
    ArrayList<TaskItemModel> todoList;
    int backgroundColor;
    int progressColor;
    LayoutInflater mLayoutInflater = null;

    public TaskItemAdapter(Fragment_Tasks fragment_tasks, ArrayList<TaskItemModel> todoList, int backgroundColor, int progressColor){
        this.fragment_tasks = fragment_tasks;
        this.todoList = todoList;
        this.backgroundColor = backgroundColor;
        this.progressColor = progressColor;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new TaskItemAdapter.TodoViewHolder(mLayoutInflater.inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final TodoViewHolder holder, int position) {
        final TaskItemModel todoItemModel = todoList.get(position);
        holder.todoTitle.setText(todoItemModel.title);
        holder.progressBar.setProgress(todoItemModel.progressBar);
        holder.progressBar.setProgressTintList(ColorStateList.valueOf(fragment_tasks.getResources().getColor(progressColor)));
        holder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(fragment_tasks.getResources().getColor(backgroundColor)));
        holder.cardView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                fragment_tasks.selectTask(holder, todoItemModel, progressColor, backgroundColor);
            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public class TodoViewHolder extends RecyclerView.ViewHolder{
        ProgressBar progressBar;
        TextView todoTitle;
        CardView cardView;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.todoCardView);
            progressBar = itemView.findViewById(R.id.todoProgressBar);
            todoTitle = itemView.findViewById(R.id.todoTitle);
        }
    }
}