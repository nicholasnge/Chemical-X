package com.example.chemicalx.Fragment_Schedule;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.vipulasri.timelineview.TimelineView;
import com.example.chemicalx.R;
import com.example.chemicalx.VectorDrawableUtils;
import com.google.android.material.card.MaterialCardView;


import java.util.List;

class TimeLineAdapter extends RecyclerView.Adapter<TimeLineAdapter.TimeLineViewHolder> {
    ViewGroup parent;
    List<TimeLineModel> mFeedList;
    LayoutInflater mLayoutInflater = null;

    public TimeLineAdapter(List<TimeLineModel> mFeedList){
        super();
        this.mFeedList = mFeedList;
    }

    @Override
    public int getItemViewType(int position){
        return TimelineView.getTimeLineViewType(position, getItemCount());
    }

    @NonNull
    @Override
    public TimeLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        this.parent = parent;
        if(mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new TimeLineViewHolder(mLayoutInflater.inflate(R.layout.item_timeline, parent, false), viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeLineViewHolder holder, int position) {
        TimeLineModel timeLineModel = mFeedList.get(position);

        switch(timeLineModel.status){
            case INACTIVE:
                setMarker(holder, R.drawable.ic_marker_inactive, R.color.Grey);
                break;
            case ACTIVE:
                setMarker(holder, R.drawable.ic_marker_active, R.color.DarkGrey);
                break;
            default:
                setMarker(holder, R.drawable.ic_marker, R.color.Grey);
        }

        if(!timeLineModel.getDate().isEmpty()){
            holder.date.setVisibility(View.VISIBLE);
            // original version formats this string
            holder.date.setText(timeLineModel.getDate());
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.message.setText(timeLineModel.getMessage());

        if(timeLineModel.isFromTasks){
            holder.timelineCard.setCardBackgroundColor(parent.getResources().getColor(R.color.MaterialGreen100));
        }
    }

    private void setMarker(TimeLineViewHolder holder, int drawableRestId, int colorFilter){
        holder.timeline.setMarker(VectorDrawableUtils.INSTANCE.getDrawable(holder.itemView.getContext(), drawableRestId, ContextCompat.getColor(holder.itemView.getContext(), colorFilter)));
    }

    @Override
    public int getItemCount() {
        return mFeedList.size();
    }

    public class TimeLineViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView message;
        final TimelineView timeline;
        final MaterialCardView timelineCard;

        public TimeLineViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            date = itemView.findViewById(R.id.text_timeline_date);
            message = itemView.findViewById(R.id.text_timeline_title);
            timeline = itemView.findViewById(R.id.timeline);
            timeline.initLine(viewType);
            timelineCard = itemView.findViewById(R.id.timelineCard);
        }
    }
}
