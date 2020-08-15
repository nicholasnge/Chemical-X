package com.example.chemicalx.Fragment_Tasks;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.Category;
import com.example.chemicalx.Fragment_Schedule.Fragment_Schedule;
import com.example.chemicalx.MainActivity;
import com.example.chemicalx.R;
import com.example.chemicalx.TextClassificationClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;


public class Fragment_Tasks extends Fragment {
    Fragment_Schedule fragment_schedule;
    public final static String TAG = "FragmentTasks";
    ArrayList<TaskItemModel> raw_tasks;
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

    public Fragment_Tasks(TextClassificationClient tf_classifytasks, ArrayList<TaskItemModel> tasks) {
        this.tf_classifytasks = tf_classifytasks;
        this.raw_tasks = tasks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_todolist, container, false);
        recyclerView = view.findViewById(R.id.todolist_tasks_recycler);

        //set floating action button to open addTodo
        fab = getActivity().findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment addTodo = new AddTask(tf_classifytasks);
                addTodo.show(getChildFragmentManager(), "tag");
                triggerNotification();
            }
        });

        // initialise recycler view holding tasks
        initRecyclerView();

        return view;
    }

    private void triggerNotification(){
        Intent intent = new Intent(getActivity(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_molecular)
                .setContentTitle("ChemicAL X")
                .setContentText("you have a task that needs to be completed!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "chemical channel";
            String description = "chemical channel desc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public void addTask(TaskItemModel task) {
        switch (task.category) {
            case "Work":
                mDataList.get(0).add(task);
                break;
            case "Hobbies":
                mDataList.get(1).add(task);
                break;
            case "School":
                mDataList.get(2).add(task);
                break;
            case "Chores":
                mDataList.get(3).add(task);
                break;
            default:
                Log.e(TAG, "snapshot: no such category: " + task.category + ", defaulting to Work");
                mDataList.get(0).add(task);
        }

        // find the taskItemAdapter responsible for updating its recyclerview
        TaskItemAdapter adapter = mAdapter.taskItemAdapters.get(task.category);
        //if no such category, default to Work
        if (adapter == null) {
            adapter = mAdapter.taskItemAdapters.get("Work");
        }
        adapter.notifyItemInserted(adapter.taskList.size());
        mAdapter.notifyChange(task.category);
    }

    private void initRecyclerView() {
        //format the array into a taskcategorymodel
        mDataList.clear();
        mDataList.add(new TaskCategoryModel("Work", new ArrayList<>(), workBackgroundColor, workProgressColor));
        mDataList.add(new TaskCategoryModel("Hobbies", new ArrayList<>(), hobbiesBackgroundColor, hobbiesProgressColor));
        mDataList.add(new TaskCategoryModel("School", new ArrayList<>(), schoolBackgroundColor, schoolProgressColor));
        mDataList.add(new TaskCategoryModel("Chores", new ArrayList<>(), choresBackgroundColor, choresProgressColor));

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
            deselectCurrentTask();
        }
        // else handle select
        else {
            if (selectedTask != null) {
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
        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("tasks")
                .document(selectedTask.docID)
                .update("timePassed", selectedTask.timePassed);
    }

    public void deselectCurrentTask() {
        //stop increasing timing
        timer.cancel();

        //get current task details to give to dialog
        HashMap<String, Object> currentTask = getCurrentTaskDetails();

        // Initialise Feedback Dialog
        DialogFragment feedbackDialog = new FeedbackDialog(currentTask);
        // You can get the FragmentManager by calling getSupportFragmentManager()
        // from the FragmentActivity or getFragmentManager() from a Fragment.
        feedbackDialog.show(getChildFragmentManager(), "feedback");

        //update firebase
        updateFirebase();

        //deselect task
        selectedTask = null;

        //make deselected task look normal again
        selectedTaskViewholder.cardView.setCardElevation(1);
        selectedTaskViewholder.progressBar.setProgressTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousProgressColorOfSelected)));
        selectedTaskViewholder.progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getResources().getColor(previousBackgroundColorOfSelected)));
    }

    public HashMap<String, Object> getCurrentTaskDetails() {
        HashMap<String, Object> currentTask = new HashMap<>();
        currentTask.put("docID", selectedTask.docID);
        currentTask.put("category", selectedTask.category);
        currentTask.put("timeStart", new Timestamp(startTimestamp));
        currentTask.put("timeEnd", new Timestamp(new Date()));
        currentTask.put("dueDate", selectedTask.dueDate);
        return currentTask;
    }

    public void completeTask(TaskItemAdapter.TodoViewHolder holder, TaskItemModel task) {
        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
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

        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("history")
                .add(newhistory);

        // notify category adapter
        mAdapter.notifyChange(task.category);
    }
}