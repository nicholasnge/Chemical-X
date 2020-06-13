package com.example.chemicalx.Fragment_Todolist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class Fragment_Todolist extends Fragment {
    private ArrayList<TodoCategoryModel> mDataList = new ArrayList<>();
    private TodoCategoryAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    FloatingActionButton fab;

    public Fragment_Todolist() {
        //required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_todolist, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        setDataListItems();
        initRecyclerView();

        //set floating action button to open addTodo
        FloatingActionButton button = getActivity().findViewById(R.id.floatingActionButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                DialogFragment addTodo = new AddTodo();
                addTodo.show(getChildFragmentManager(), "tag");
            }
        });

        return view;
    }

    private void setDataListItems() {
        ArrayList<TodoItemModel> todoList = new ArrayList<>();
        todoList.add(new TodoItemModel("Finish Orbital Project", 30));
        todoList.add(new TodoItemModel("Revise for CFA exam before December", 70));
        todoList.add(new TodoItemModel("Study for CS3230", 45));

        ArrayList<TodoItemModel> todoList2 = new ArrayList<>();
        todoList2.add(new TodoItemModel("Not play TFT for the rest of my life please", 30));
        todoList2.add(new TodoItemModel("Play Android Studio", 70));
        todoList2.add(new TodoItemModel("Reach Masters in 2v2 Starcraft", 45));

        ArrayList<TodoItemModel> todoList3 = new ArrayList<>();
        todoList3.add(new TodoItemModel("Practice piano for Diya's piano piece", 30));
        todoList3.add(new TodoItemModel("Edit video for Summer Wind", 70));
        todoList3.add(new TodoItemModel("Learn how to paint with Bob Ross", 45));

        mDataList.clear();
        mDataList.add(new TodoCategoryModel("Work", todoList, R.color.MaterialBlue50, R.color.MaterialBlue100));
        mDataList.add(new TodoCategoryModel("Recreation", todoList2, R.color.MaterialGreen50, R.color.MaterialGreen100));
        mDataList.add(new TodoCategoryModel("Hobby", todoList3, R.color.MaterialRed50, R.color.MaterialRed100));
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TodoCategoryAdapter(getActivity(), mDataList);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
    }
}