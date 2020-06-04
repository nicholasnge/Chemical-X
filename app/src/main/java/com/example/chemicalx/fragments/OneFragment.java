package com.example.chemicalx.fragments;

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
        mDataList.add(new TimeLineModel("Item successfully delivered", "", OrderStatus.INACTIVE));
        mDataList.add(new TimeLineModel("Courier is out to delivery your order", "2017-02-12 08:00", OrderStatus.ACTIVE));
        mDataList.add(new TimeLineModel("Item has reached courier facility at New Delhi", "2017-02-11 21:00", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Item has been given to the courier", "2017-02-11 18:00", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Item is packed and will dispatch soon", "2017-02-11 09:30", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Order is being readied for dispatch", "2017-02-11 08:00", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Order processing initiated", "2017-02-10 15:00", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Order confirmed by seller", "2017-02-10 14:30", OrderStatus.COMPLETED));
        mDataList.add(new TimeLineModel("Order placed successfully", "2017-02-10 14:00", OrderStatus.COMPLETED));
    }
    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        Log.d("TAG", mLayoutManager.toString());
        Log.d("TAG", recyclerView.toString());
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TimeLineAdapter(mDataList);
        recyclerView.setAdapter(mAdapter);
    }
}