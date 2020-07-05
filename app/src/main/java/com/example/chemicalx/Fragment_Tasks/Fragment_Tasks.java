package com.example.chemicalx.Fragment_Tasks;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class Fragment_Tasks extends Fragment {
    public final static String TAG = "Fragment_Todolist";
    FirebaseFirestore db;
    private ArrayList<TaskCategoryModel> mDataList = new ArrayList<>();
    private TaskCategoryAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    FloatingActionButton fab;

    public Fragment_Tasks() {
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
        getTasks();

        //set floating action button to open addTodo
        FloatingActionButton button = getActivity().findViewById(R.id.floatingActionButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                DialogFragment addTodo = new AddTask();
                addTodo.show(getChildFragmentManager(), "tag");
            }
        });

        return view;
    }

    private void getTasks() {
        final ArrayList<TaskItemModel> tasksWork = new ArrayList<>();
        final ArrayList<TaskItemModel> tasksRecreation = new ArrayList<>();
        final ArrayList<TaskItemModel> tasksHobby = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document("testuser")
                .collection("tasks")
                .get()
        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<DocumentSnapshot> snapshotList = queryDocumentSnapshots.getDocuments();
                TaskItemModel task;
                for (DocumentSnapshot snapshot : snapshotList) {
                    Log.d(TAG, snapshot.getString("title"));
                    Log.d(TAG, snapshot.getLong("timePassed").intValue() + "");
                    Log.d(TAG, snapshot.getLong("totalTime").intValue() + "");

                    task = new TaskItemModel(snapshot.getString("title"),
                            snapshot.getLong("timePassed").intValue(),
                            snapshot.getLong("totalTime").intValue());

                    switch(snapshot.getString("category")){
                        case "Work":
                            tasksWork.add(task);
                            break;
                        case "Recreation":
                            tasksRecreation.add(task);
                            break;
                        case "Hobby":
                            tasksHobby.add(task);
                            break;
                        default:
                            Log.e(TAG, "snapshot getCategory: no such category");
                    }
                }

                //format the array into a todocategorymodel
                mDataList.clear();
                mDataList.add(new TaskCategoryModel("Work", tasksWork, R.color.MaterialBlue50, R.color.MaterialBlue100));
                mDataList.add(new TaskCategoryModel("Recreation", tasksRecreation, R.color.MaterialGreen50, R.color.MaterialGreen100));
                mDataList.add(new TaskCategoryModel("Hobby", tasksHobby, R.color.MaterialRed50, R.color.MaterialRed100));

                //create the recyclerview that holds the data
                initRecyclerView();
            }
        });
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TaskCategoryAdapter(getActivity(), mDataList);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
    }
}
