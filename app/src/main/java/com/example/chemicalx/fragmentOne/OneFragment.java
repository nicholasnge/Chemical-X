package com.example.chemicalx.fragmentOne;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;

import java.util.ArrayList;


public class OneFragment extends Fragment {
    private ArrayList<TimeLineModel> mDataList = new ArrayList<>();
    private TimeLineAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;

    public OneFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_one, container, false);
        if (view == null){
            Log.d("TAG", "VIEW IS NULL");
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        setDataListItems();
        initRecyclerView();
        return view;
    }

    private void setDataListItems() {
        mDataList.clear();
        mDataList.add(new TimeLineModel("CS2030 Lecture", "10:00 AM", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("CS2040 Tutorial", "12:00 PM", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Lunch", "1:30 PM", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Work - Recreation remaining: -4 min", "2:00 PM", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Dinner Break", "6:30 PM", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("UTW1702B Recitation", "8:00 PM", OrderStatus.ACTIVE));
        mDataList.add(new TimeLineModel("Work - Recreation remaining: 90 min", "9:30 PM", OrderStatus.INACTIVE));
        mDataList.add(new TimeLineModel("Sleep - Overdue by: 0 min", "12:30 AM", OrderStatus.INACTIVE));
    }
    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TimeLineAdapter(mDataList);
        recyclerView.setAdapter(mAdapter);
    }
}