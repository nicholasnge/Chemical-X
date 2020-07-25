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

import com.example.chemicalx.Category;
import com.example.chemicalx.Fragment_Schedule.Fragment_Schedule;
import com.example.chemicalx.R;
import com.example.chemicalx.TextClassificationClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;


public class Fragment_Tasks extends Fragment {
    Fragment_Schedule fragment_schedule;
    public final static String TAG = "FragmentTasks";
    FirebaseFirestore db;
    private ArrayList<TaskCategoryModel> mDataList = new ArrayList<>();
    PriorityQueue<TaskItemModel> taskItemQueue;
    private TaskCategoryAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    FloatingActionButton fab;

    //for selected task
    TaskItemModel selectedTask;
    TaskItemAdapter.TodoViewHolder selectedTaskViewholder;
    Timer timer;
    Date startTimestamp;
    int previousProgressColorOfSelected;
    int previousBackgroundColorOfSelected;

    //for tf model
    public TextClassificationClient tf_classifytasks;

    //colors
    public static final int workProgressColor = R.color.MaterialBlue100;
    public static final int workBackgroundColor = R.color.MaterialBlue50;
    public static final int hobbiesProgressColor = R.color.MaterialGreen100;
    public static final int hobbiesBackgroundColor = R.color.MaterialGreen50;
    public static final int schoolProgressColor = R.color.MaterialRed100;
    public static final int schoolBackgroundColor = R.color.MaterialRed50;
    public static final int choresProgressColor = R.color.MaterialYellow100;
    public static final int choresBackgroundColor = R.color.MaterialYellow50;
    public static final int selectedProgressColor = R.color.colorSecondary;
    public static final int selectedBackgroundColor = R.color.colorSecondaryLight;

    public Fragment_Tasks(TextClassificationClient tf_classifytasks, Fragment_Schedule schedule) {
        this.tf_classifytasks = tf_classifytasks;
        this.fragment_schedule = schedule;
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
        fab = getActivity().findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment addTodo = new AddTask(tf_classifytasks);
                addTodo.show(getChildFragmentManager(), "tag");
            }
        });

        return view;
    }

    private void getTasks() {
        final ArrayList<TaskItemModel> tasksWork = new ArrayList<>();
        final ArrayList<TaskItemModel> tasksHobbies = new ArrayList<>();
        final ArrayList<TaskItemModel> tasksSchool = new ArrayList<>();
        final ArrayList<TaskItemModel> tasksChores = new ArrayList<>();
        taskItemQueue = new PriorityQueue<>(new Comparator<TaskItemModel>() {
            @Override
            public int compare(TaskItemModel o1, TaskItemModel o2) {
                if (o1.dueDate == null){
                    return -1; // o2 is more urgent
                }
                if (o2.dueDate == null){
                    return 1; // o1 is more urgent
                }
                return -o1.dueDate.compareTo(o2.dueDate);
            }
        });

        //format the array into a taskcategorymodel
        mDataList.clear();
        mDataList.add(new TaskCategoryModel("Work", tasksWork, workBackgroundColor, workProgressColor));
        mDataList.add(new TaskCategoryModel("Hobbies", tasksHobbies, hobbiesBackgroundColor, hobbiesProgressColor));
        mDataList.add(new TaskCategoryModel("School", tasksSchool, schoolBackgroundColor, schoolProgressColor));
        mDataList.add(new TaskCategoryModel("Chores", tasksChores, choresBackgroundColor, choresProgressColor));

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
                            boolean added = false;
                            for (DocumentChange docChange : queryDocumentSnapshots.getDocumentChanges()) {
                                if (docChange.getType() == DocumentChange.Type.ADDED) {
                                    added = true;
                                    DocumentSnapshot snapshot = docChange.getDocument();

                                    TaskItemModel task = new TaskItemModel(
                                            snapshot.getId(),
                                            snapshot.getString("title"),
                                            snapshot.getString("category"),
                                            snapshot.getLong("totalTime").intValue(),
                                            snapshot.getLong("timePassed").intValue(),
                                            snapshot.getTimestamp("dueDate"));

                                    switch (snapshot.getString("category")) {
                                        case "Work":
                                            tasksWork.add(task);
                                            break;
                                        case "Hobbies":
                                            tasksHobbies.add(task);
                                            break;
                                        case "School":
                                            tasksSchool.add(task);
                                            break;
                                        case "Chores":
                                            tasksChores.add(task);
                                            break;
                                        default:
                                            Log.e(TAG, "snapshot: no such category: " + snapshot.getString("category") + ", defaulting to Work");
                                            tasksWork.add(task);
                                    }

                                    //add task to priorityqueue
                                    taskItemQueue.add(task);

                                    // find the taskItemAdapter responsible for updating its recyclerview
                                    TaskItemAdapter adapter = mAdapter.taskItemAdapters.get(snapshot.getString("category"));
                                    //if no such category, default to Work
                                    if (adapter == null){
                                        adapter = mAdapter.taskItemAdapters.get("Work");
                                    }
                                    adapter.notifyItemInserted(adapter.taskList.size());
                                    mAdapter.notifyChange(snapshot.getString("category"));
                                }
                            }
                            fragment_schedule.addTasks(taskItemQueue);
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
        mAdapter = new TaskCategoryAdapter(this, mDataList);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
    }

    public void selectTask(final TaskItemAdapter.TodoViewHolder holder, final TaskItemModel todoItemModel, int progressColor, int backgroundColor) {
        // if item was already selected, deselect
        if (todoItemModel == selectedTask) {
            updateFirebase(); //update current task
            deselectCurrentTask();
        }
        // else handle select
        else {
            if (selectedTask != null) {
                updateFirebase(); //update current task
                deselectCurrentTask(); // deselect current task
            }
            selectedTask = todoItemModel;
            selectedTaskViewholder = holder;
            previousBackgroundColorOfSelected = backgroundColor;
            previousProgressColorOfSelected = progressColor;
            startTimestamp = new Date();

            //format card to look like its selected
            holder.cardView.setCardElevation(20);
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(selectedProgressColor)));
            holder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(selectedBackgroundColor)));

            //timer object to increase timePassed every second
            timer = new Timer();
            TimerTask updateProgress = new TimerTask() {
                @Override
                public void run() {
                    todoItemModel.incrementProgress();
                    holder.progressBar.setProgress(todoItemModel.progressBar);
                }
            };
            timer.schedule(updateProgress, 0, 1000);
        }
    }

    public void updateFirebase() {
        //update task document
        db.collection("users")
                .document("testuser")
                .collection("tasks")
                .document(selectedTask.docID)
                .update("timePassed", selectedTask.timePassed);

        //update user history
        HashMap<String, Object> newhistory = new HashMap<>();
        newhistory.put("docID", selectedTask.docID);
        newhistory.put("category", selectedTask.category);
        newhistory.put("timeStart", new Timestamp(startTimestamp));
        newhistory.put("timeEnd", new Timestamp(new Date()));
        newhistory.put("completion", false);

        db.collection("users")
                .document("testuser")
                .collection("history")
                .add(newhistory);
    }

    public void deselectCurrentTask() {
        //deselect
        selectedTask = null;
        timer.cancel();

        //make it look normal again
        selectedTaskViewholder.cardView.setCardElevation(1);
        selectedTaskViewholder.progressBar.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousProgressColorOfSelected)));
        selectedTaskViewholder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousBackgroundColorOfSelected)));
    }

    public void completeTask(TaskItemAdapter.TodoViewHolder holder, TaskItemModel task) {
        db.collection("users")
                .document("testuser")
                .collection("tasks")
                .document(task.docID)
                .delete();

        //update user history
        HashMap<String, Object> newhistory = new HashMap<>();
        newhistory.put("docID", task.docID);
        newhistory.put("category", task.category);
        newhistory.put("timeFinished", new Timestamp(new Date()));
        newhistory.put("timeRemaining", task.timePassed);
        newhistory.put("totalTime", task.totalTime);
        newhistory.put("completion", true);

        db.collection("users")
                .document("testuser")
                .collection("history")
                .add(newhistory);

        // notify category adapter
        mAdapter.notifyChange(task.category);
    }
}