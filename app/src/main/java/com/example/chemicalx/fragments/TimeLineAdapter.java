package com.example.chemicalx.fragments;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.TimeLineModel;

import java.util.List;

class TimeLineAdapter extends RecyclerView.Adapter<TimeLineAdapter.TimeLineViewHolder> {
    List<TimeLineModel> mFeedModel;

    public TimeLineAdapter(List<TimeLineModel> mFeedModel){
        this.mFeedModel = mFeedModel;
    }

    @NonNull
    @Override
    public TimeLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class TimeLineViewHolder extends RecyclerView.ViewHolder {
        public TimeLineViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
