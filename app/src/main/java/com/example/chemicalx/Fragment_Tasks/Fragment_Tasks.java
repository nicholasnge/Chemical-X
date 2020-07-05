package com.example.chemicalx.Fragment_Tasks;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Fragment_Tasks extends Fragment {
    public final static String TAG = "Fragment_Todolist";
    FirebaseFirestore db;
    private ArrayList<TaskCategoryModel> mDataList = new ArrayList<>();
    private TaskCategoryAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    FloatingActionButton fab;

    //for selected task
    TaskItemModel selectedTask;
    TaskItemAdapter.TodoViewHolder selectedTaskViewholder;
    Timer timer;
    int previousProgressColorOfSelected;
    int previousBackgroundColorOfSelected;

    //colors
    public static final int workProgressColor = R.color.MaterialBlue100;
    public static final int workBackgroundColor = R.color.MaterialBlue50;
    public static final int recreationProgressColor = R.color.MaterialGreen100;
    public static final int recreationBackgroundColor = R.color.MaterialGreen50;
    public static final int hobbyProgressColor = R.color.MaterialRed100;
    public static final int hobbyBackgroundColor = R.color.MaterialRed50;
    public static final int selectedProgressColor = R.color.colorSecondary;
    public static final int selectedBackgroundColor = R.color.colorSecondaryLight;

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
        button.setOnClickListener(new View.OnClickListener() {
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

        //format the array into a taskcategorymodel
        mDataList.clear();
        mDataList.add(new TaskCategoryModel("Work", tasksWork, workBackgroundColor, workProgressColor));
        mDataList.add(new TaskCategoryModel("Recreation", tasksRecreation, recreationBackgroundColor, recreationProgressColor));
        mDataList.add(new TaskCategoryModel("Hobby", tasksHobby, hobbyBackgroundColor, hobbyProgressColor));

        //initialise the recycler view
        initRecyclerView();

        db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document("testuser")
                .collection("tasks")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "FirebaseFirestoreException");
                            return;
                        }
                        if (queryDocumentSnapshots != null) {
                            for (DocumentChange docChange : queryDocumentSnapshots.getDocumentChanges()) {
                                if (docChange.getType() == DocumentChange.Type.ADDED) {
                                    DocumentSnapshot snapshot = docChange.getDocument();

                                    TaskItemModel task = new TaskItemModel(snapshot.getString("title"),
                                            snapshot.getLong("timePassed").intValue(), //timePassed and totalTime are in seconds
                                            snapshot.getLong("totalTime").intValue());

                                    switch (snapshot.getString("category")) {
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
                                            Log.e(TAG, "snapshot: no such category");
                                    }
                                }

                                //update the recyclerview that holds the data
                                updateRecyclerView();
                            }
                        }
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
    }

    private void updateRecyclerView() {
        mAdapter = new TaskCategoryAdapter(this, mDataList);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
    }

    public void selectTask(final TaskItemAdapter.TodoViewHolder holder, final TaskItemModel todoItemModel, int progressColor, int backgroundColor){
        // if item was already selected, deselect
        if (todoItemModel == selectedTask){
            deselectCurrentTask();
        }
        // else handle select
        else {
            if (selectedTask != null){
                //deselect current task
                deselectCurrentTask();
            }
            selectedTask = todoItemModel;
            selectedTaskViewholder = holder;
            previousBackgroundColorOfSelected = backgroundColor;
            previousProgressColorOfSelected = progressColor;

            //format card to look like its selected
            holder.cardView.setCardElevation(20);
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(selectedProgressColor)));
            holder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(selectedBackgroundColor)));

            //timer object to increase timePassed every second
            timer = new Timer();
            TimerTask updateProgress = new TimerTask(){
                @Override
                public void run() {
                    todoItemModel.incrementProgress();
                    holder.progressBar.setProgress(todoItemModel.progressBar);
                }
            };
            timer.schedule(updateProgress, 0, 1000);
        }
    }

    public void deselectCurrentTask(){
        //make it look normal again
        selectedTaskViewholder.cardView.setCardElevation(1);
        selectedTaskViewholder.progressBar.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousProgressColorOfSelected)));
        selectedTaskViewholder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousBackgroundColorOfSelected)));

        //deselect
        selectedTask = null;
        timer.cancel();
    }
}
