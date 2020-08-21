package com.example.chemicalx.Fragment_Tasks;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chemicalx.MainActivity;
import com.example.chemicalx.R;
import com.example.chemicalx.TextClassificationClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class Fragment_Tasks extends Fragment implements FeedbackDialog.FeedbackDialogListener {
    public static final String TAG = "FragmentTasks";
    private static final String PAST_EVENTS_DATA_FILENAME = "data/ml_training/past_events_data.json";
    private static final String DATA_POINTS_FILENAME = "data/ml_training/data_points.json";
    private static final double PRODUCTIVITY_FACTOR = 2;
    private static final int PAST_EVENTS_QUEUE_MAX_SIZE = 168;

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
    Date startTime;
    int previousProgressColorOfSelected;
    int previousBackgroundColorOfSelected;
    private long sessionStartTime;

    //for tf model
    public TextClassificationClient tf_classifytasks;

    // Projection arrays. Creating indices for these arrays instead of doing
    // dynamic lookups improves performance.
    // instances
    private static final String[] INSTANCE_PROJECTION = new String[]{
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    // The indices for the projection arrays above.
    // instances
    private static final int PROJECTION_TITLE_INDEX = 0;
    private static final int PROJECTION_BEGIN_INDEX = 1;
    private static final int PROJECTION_END_INDEX = 2;

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

    // past events queue
    private List<PastEvent> pastEventsList;

    // mapping from each incomplete task reference to its respective list of data points
    private Map<String, List<Map<String, Object>>> dataPointsMap;

    public Fragment_Tasks(TextClassificationClient tf_classifytasks, ArrayList<TaskItemModel> tasks) {
        this.tf_classifytasks = tf_classifytasks;
        this.raw_tasks = tasks;
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
        recyclerView = view.findViewById(R.id.todolist_tasks_recycler);

        //set floating action button to open addTodo
        fab = getActivity().findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment addTodo = new AddTask(tf_classifytasks);
                addTodo.show(getChildFragmentManager(), "tag");
            }
        });

        this.pastEventsList = initialisePastEventsList();
        this.dataPointsMap = initialiseDataPointsMap();

        // initialise recycler view holding tasks
        initRecyclerView();

        return view;
    }

    @Override
    public void onPause() {
        saveData();
        super.onPause();
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
            startTime = new Date();

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
        DialogFragment feedbackDialog = new FeedbackDialog(this, currentTask);
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
        Date now = new Date();
        long startTimeMillis = startTime.getTime();
        long endTimeMillis = now.getTime();
        long sessionDuration = now.getTime() - startTimeMillis;
        long expectedTimeToComplete = ((long) (selectedTask.totalTime - selectedTask.timePassed)) * 1000 + sessionDuration;
        Timestamp deadlineTimestamp = selectedTask.dueDate;
        Date deadlineDate = deadlineTimestamp.toDate();
        long deadlineTimeMillis = deadlineDate.getTime();
        long timeUntilDeadline = deadlineTimeMillis - startTimeMillis;

        currentTask.put("docID", selectedTask.docID);
        currentTask.put("timeStart", new Timestamp(startTime));
        currentTask.put("timeEnd", new Timestamp(now));
        currentTask.put("dueDate", deadlineTimestamp);

        currentTask.put("startTime", startTimeMillis);
        currentTask.put("endTime", endTimeMillis);
        currentTask.put("category", selectedTask.category);
        currentTask.put("sessionDuration", sessionDuration);
        currentTask.put("expectedTimeToComplete", expectedTimeToComplete);
        currentTask.put("timeUntilDeadline", timeUntilDeadline);

        return currentTask;
    }

    public void completeTask(TaskItemAdapter.TodoViewHolder holder, TaskItemModel task) {
        Timestamp submissionTime = new Timestamp(new Date());
        double timePassed = task.timePassed;
        double totalTime = task.totalTime;
        double productivityMultiplier = totalTime / timePassed;
        List<Map<String, Object>> taskDataPointsList = dataPointsMap.remove(task.docID);
        if (taskDataPointsList == null) {
            taskDataPointsList = new ArrayList<>();
        }
        for (Map<String, Object> taskDataPoint : taskDataPointsList) {
            double productivity = ((Number) taskDataPoint.get("productivity")).doubleValue();
            productivity *= productivityMultiplier;
            taskDataPoint.put("productivity", productivity);
            taskDataPoint.put("submissionTime", submissionTime);
            // to user's own dataset
            FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("unusedTrainData")
                    .add(taskDataPoint);
            // to base user's dataset
            FirebaseFirestore.getInstance().collection("users")
                    .document("base_user")
                    .collection("unusedTrainData")
                    .add(taskDataPoint);
        }

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

    private List<PastEvent> initialisePastEventsList() {
        List<PastEvent> peList;
        try {
            JSONArray jsonArray = getPastEventsJSONFromFile();
            peList = constructPastEventsListFromJSON(jsonArray);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Past events could not be loaded.\n" + e.getMessage());
            peList = constructPastEventsListFromCalendar();
        }
        return peList;
    }

    private List<PastEvent> constructPastEventsListFromJSON(JSONArray jsonArray) throws JSONException {
        List<PastEvent> peList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject pastEventJSON = jsonArray.getJSONObject(i);
            peList.add(PastEvent.constructFromJSONObject(pastEventJSON));
        }
        return peList;
    }

    private JSONArray getPastEventsJSONFromFile() throws IOException, JSONException {
        File pastEventsFile = new File(getContext().getFilesDir(), PAST_EVENTS_DATA_FILENAME);
        StringBuilder stringBuilder = new StringBuilder();
        FileReader fileReader = new FileReader(pastEventsFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        while (line != null){
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        String response = stringBuilder.toString();
        return new JSONArray(response);
    }

    private List<PastEvent> constructPastEventsListFromCalendar() {
        List<PastEvent> peList = new ArrayList<>();

        // one week ago from today
        Calendar oneWeekAgo = Calendar.getInstance();
        oneWeekAgo.add(Calendar.DATE, -7);

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Instances.CONTENT_URI;

        // Construct the query with the desired date range.
        Uri.Builder builder = uri.buildUpon();
        ContentUris.appendId(builder, oneWeekAgo.getTimeInMillis());
        ContentUris.appendId(builder, now.getTimeInMillis());

        // Submit the query and get a Cursor object back.
        // method called only after READ_CALENDAR permission obtained so can suppress
        @SuppressLint("MissingPermission")
        Cursor cur = cr.query(builder.build(), INSTANCE_PROJECTION, null, null, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            String title;
            long dtstart; // UTC ms since the oneWeekAgo of the epoch
            long dtend;

            String eventCategory;
            int eventCategoryIndex;
            long eventDuration;

            // Get the field values
            title = cur.getString(PROJECTION_TITLE_INDEX);
            dtstart = cur.getLong(PROJECTION_BEGIN_INDEX);
            dtend = cur.getLong(PROJECTION_END_INDEX);

            // processing of values
            eventCategory = tf_classifytasks.classify(title);

            switch(eventCategory) {
                case "Work":
                    eventCategoryIndex = 1;
                    break;
                case "Hobbies":
                    eventCategoryIndex = 2;
                    break;
                case "School":
                    eventCategoryIndex = 3;
                    break;
                case "Chores":
                    eventCategoryIndex = 4;
                    break;
                default:
                    eventCategoryIndex = 0;
            }

            eventDuration = dtend - dtstart;

            // add to data list
            peList.add(new PastEvent(eventCategoryIndex, eventDuration, dtend));
        }

        peList.sort(null);

        return peList;
    }

    private void updatePastEventsList() {
        int size = pastEventsList.size();
        PastEvent lastPastEvent = pastEventsList.get(size - 1);
        long lastPastEventEndTimeMillis = lastPastEvent.getEndTime();

        // lastPastEventEndTime
        Calendar lastPastEventEndTime = Calendar.getInstance();
        lastPastEventEndTime.setTimeInMillis(lastPastEventEndTimeMillis);

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Instances.CONTENT_URI;

        // Construct the query with the desired date range.
        Uri.Builder builder = uri.buildUpon();
        ContentUris.appendId(builder, lastPastEventEndTime.getTimeInMillis());
        ContentUris.appendId(builder, now.getTimeInMillis());

        // Submit the query and get a Cursor object back.
        // method called only after READ_CALENDAR permission obtained so can suppress
        @SuppressLint("MissingPermission")
        Cursor cur = cr.query(builder.build(), INSTANCE_PROJECTION, null, null, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            String title;
            long dtstart; // UTC ms since the lastPastEventEndTime of the epoch
            long dtend;

            String eventCategory;
            int eventCategoryIndex;
            long eventDuration;

            // Get the field values
            title = cur.getString(PROJECTION_TITLE_INDEX);
            dtstart = cur.getLong(PROJECTION_BEGIN_INDEX);
            dtend = cur.getLong(PROJECTION_END_INDEX);

            // processing of values
            eventCategory = tf_classifytasks.classify(title);

            switch(eventCategory) {
                case "Work":
                    eventCategoryIndex = 1;
                    break;
                case "Hobbies":
                    eventCategoryIndex = 2;
                    break;
                case "School":
                    eventCategoryIndex = 3;
                    break;
                case "Chores":
                    eventCategoryIndex = 4;
                    break;
                default:
                    eventCategoryIndex = 0;
            }

            eventDuration = dtend - dtstart;

            // add to data list
            addPastEventToList(new PastEvent(eventCategoryIndex, eventDuration, dtend));
        }

        pastEventsList.sort(null);
        savePastEventsList();
    }

    private void addPastEventToList(PastEvent pastEvent) {
        int size = pastEventsList.size();
        if (size >= PAST_EVENTS_QUEUE_MAX_SIZE) {
            int diff = size - PAST_EVENTS_QUEUE_MAX_SIZE + 1;
            pastEventsList = pastEventsList.subList(diff, size);
        }
        pastEventsList.add(pastEvent);
        savePastEventsList();
    }

    private void addCompletedTaskSessionAsDataPoint(HashMap<String, Object> completedTaskSession,
                                                    double productivity) {
        int taskCategoryIndex;
        long startTime = (long) completedTaskSession.get("startTime");
        Calendar startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.setTimeInMillis(startTime);
        int hourOfDay = startTimeCalendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = startTimeCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        long desiredDuration = (long) completedTaskSession.get("sessionDuration");
        double normalisedDesiredDuration = Math.tanh(Math.log(((double) desiredDuration) / 1000 / 60 / 60));
        String taskCategory = (String) completedTaskSession.get("category");
        switch(taskCategory) {
            case "Work":
                taskCategoryIndex = 1;
                break;
            case "Hobbies":
                taskCategoryIndex = 2;
                break;
            case "School":
                taskCategoryIndex = 3;
                break;
            case "Chores":
                taskCategoryIndex = 4;
                break;
            default:
                taskCategoryIndex = 0;
        }
        long expectedDuration = (long) completedTaskSession.get("expectedTimeToComplete");
        double normalisedExpectedDuration = Math.tanh(((double) expectedDuration) / 1000 / 60 / 60 / 6);
        long timeUntilDeadline = (long) completedTaskSession.get("timeUntilDeadline");
        double normalisedTimeUntilDeadline = Math.tanh(((double) timeUntilDeadline) / 1000 / 60 / 60 / 24 / 7);
        updatePastEventsList();
        List<Map<String, Object>> peList = new ArrayList<>();
        for (PastEvent pastEvent : pastEventsList) {
            Map<String, Object> pastEventMap = pastEvent.getPastEventMap(startTime);
            peList.add(pastEventMap);
        }

        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("productivity", productivity);
        dataPoint.put("hourOfDay", hourOfDay);
        dataPoint.put("dayOfWeek", dayOfWeek);
        dataPoint.put("desiredDuration", normalisedDesiredDuration);
        dataPoint.put("taskCategory", taskCategoryIndex);
        dataPoint.put("expectedDuration", normalisedExpectedDuration);
        dataPoint.put("timeUntilDeadline", normalisedTimeUntilDeadline);
        dataPoint.put("pastEvents", peList);

        String docID = (String) completedTaskSession.get("docID");
        List<Map<String, Object>> taskDataPoints = dataPointsMap.get(docID);
        if (taskDataPoints == null) {
            taskDataPoints = new ArrayList<>();
            dataPointsMap.put(docID, taskDataPoints);
        }
        taskDataPoints.add(dataPoint);
        Log.d(TAG + " Task Data Points", "Data point added.");

        long endTime = (long) completedTaskSession.get("endTime");
        PastEvent completedTaskSessionPE = new PastEvent(taskCategoryIndex, desiredDuration, endTime);
        addPastEventToList(completedTaskSessionPE);
    }

    @Override
    public void onFeedbackClick(DialogFragment dialog, int which, HashMap<String, Object> currentTask) {
        double productivity;
        switch(which) {
            case 0:
                productivity = PRODUCTIVITY_FACTOR;
                break;
            case 2:
                productivity = 1.0 / PRODUCTIVITY_FACTOR;
                break;
            case 1:
            default:
                productivity = 1.0;
        }

        addCompletedTaskSessionAsDataPoint(currentTask, productivity);
        saveDataPointsMap();

//        if (scheduleList == null){
//            Toast.makeText(this, "error: feedback not saved", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        ArrayList<HashMap<String,Object>> schedule = new ArrayList<>();
//        for (TimeLineModel tlm : scheduleList){
//            HashMap<String,Object> tlm_ = new HashMap<>();
//            tlm_.put("category", tlm.category);
//            tlm_.put("timeStart", new Timestamp(new Date(tlm.dtstart)));
//            tlm_.put("timeEnd", new Timestamp(new Date(tlm.dtend)));
//            schedule.add(tlm_);
//        }
//        currentTask.put("schedule", schedule);
//        // 0,1,2 corresponding to productive, average, and unproductive
//        currentTask.put("productivity", which);
//
//        FirebaseFirestore.getInstance().collection("users")
//                .document(FirebaseAuth.getInstance().getUid())
//                .collection("history")
//                .add(currentTask);
    }

    @Override
    public void onCancel(DialogFragment dialog, HashMap<String, Object> currentTask) {
        double productivity = 1.0;

        addCompletedTaskSessionAsDataPoint(currentTask, productivity);
        saveDataPointsMap();
    }

    private Map<String, List<Map<String, Object>>> initialiseDataPointsMap() {
        Map<String, List<Map<String, Object>>> dataMap;
        try {
            JSONObject dataPointsJSONObject = getDataPointsJSONFromFile();
            dataMap = constructDataPointsMapFromJSON(dataPointsJSONObject);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Data points could not be loaded.\n" + e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                Log.e(TAG, stackTraceElement.toString());
            }
            dataMap = new HashMap<>();
        }
        return dataMap;
    }

    private Map<String, List<Map<String, Object>>> constructDataPointsMapFromJSON(
            JSONObject dataPointsJSONObject) throws JSONException {
        Map<String, List<Map<String, Object>>> dataMap = new HashMap<>();
        Iterator<String> taskKeysIterator = dataPointsJSONObject.keys();
        String taskKey;
        while (taskKeysIterator.hasNext()) {
            taskKey = taskKeysIterator.next();
            List<Map<String, Object>> taskDataPointsList = new ArrayList<>();
            JSONArray taskDataPointsJSONArray = dataPointsJSONObject.getJSONArray(taskKey);
            int numOfTaskDataPoints = taskDataPointsJSONArray.length();
            for (int i = 0; i < numOfTaskDataPoints; i++) {
                Map<String, Object> taskDataPointMap = new HashMap<>();
                JSONObject taskDataPointJSONObject = taskDataPointsJSONArray.getJSONObject(i);
                Iterator<String> dataKeysIterator = taskDataPointJSONObject.keys();
                String dataKey;
                while (dataKeysIterator.hasNext()) {
                    dataKey = dataKeysIterator.next();
                    Object data;
                    if (dataKey.equals("pastEvents")) {
                        List<Map<String, Object>> peList = new ArrayList<>();
                        JSONArray pastEventsJSONArray = taskDataPointJSONObject
                                .getJSONArray(dataKey);
                        int numOfPastEvents = pastEventsJSONArray.length();
                        for (int j = 0; j < numOfPastEvents; j++) {
                            Map<String, Object> pastEventMap = new HashMap<>();
                            JSONObject pastEventJSONObject = pastEventsJSONArray.getJSONObject(j);
                            Iterator<String> pastEventDataKeysIterator = pastEventJSONObject.keys();
                            String pastEventDataKey;
                            while (pastEventDataKeysIterator.hasNext()) {
                                pastEventDataKey = pastEventDataKeysIterator.next();
                                Object pastEventData = pastEventJSONObject.get(pastEventDataKey);
                                pastEventMap.put(pastEventDataKey, pastEventData);
                            }
                            peList.add(pastEventMap);
                        }
                        data = peList;
                    } else {
                        data = taskDataPointJSONObject.get(dataKey);
                    }
                    taskDataPointMap.put(dataKey, data);
                }
                taskDataPointsList.add(taskDataPointMap);
            }
            dataMap.put(taskKey, taskDataPointsList);
        }
        return dataMap;
    }

    private JSONObject getDataPointsJSONFromFile() throws IOException, JSONException {
        File dataPointsFile = new File(getContext().getFilesDir(), DATA_POINTS_FILENAME);
        StringBuilder stringBuilder = new StringBuilder();
        FileReader fileReader = new FileReader(dataPointsFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        while (line != null){
            stringBuilder.append(line).append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        String response = stringBuilder.toString();
        return new JSONObject(response);
    }

    private void saveData() {
        saveDataPointsMap();
        savePastEventsList();
    }

    private void saveDataPointsMap() {
        try {
            JSONObject dataPointsJSONObject = constructDataPointsJSONFromMap(dataPointsMap);
            writeDataPointsJSONToFile(dataPointsJSONObject);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Data points could not be saved.\n" + e.getMessage());
        }
    }

    private JSONObject constructDataPointsJSONFromMap(
            Map<String, List<Map<String, Object>>> dataMap) throws JSONException {
        JSONObject dataPointsJSONObject = new JSONObject();
        Set<String> taskKeysSet = dataMap.keySet();
        for (String taskKey : taskKeysSet) {
            JSONArray taskDataPointsJSONArray = new JSONArray();
            List<Map<String, Object>> taskDataPointsList = dataMap.get(taskKey);
            for (Map<String, Object> taskDataPointMap : taskDataPointsList) {
                JSONObject taskDataPointJSONObject = new JSONObject();
                Set<String> dataKeysSet = taskDataPointMap.keySet();
                for (String dataKey : dataKeysSet) {
                    Object dataValue = taskDataPointMap.get(dataKey);
                    taskDataPointJSONObject.put(dataKey, dataValue);
                }
                taskDataPointsJSONArray.put(taskDataPointJSONObject);
            }
            dataPointsJSONObject.put(taskKey, taskDataPointsJSONArray);
        }
        return dataPointsJSONObject;
    }

    private void writeDataPointsJSONToFile(JSONObject dataPointsJSONObject) throws IOException {
        // Convert JsonObject to String Format
        String userString = dataPointsJSONObject.toString();
        // Define the File Path and its Name
        File file = new File(getContext().getFilesDir(), DATA_POINTS_FILENAME);
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(userString);
        bufferedWriter.close();
    }

    private void savePastEventsList() {
        try {
            JSONArray pastEventsJSONArray = constructPastEventsJSONFromList(pastEventsList);
            writePastEventsJSONToFile(pastEventsJSONArray);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Past events could not be saved.\n" + e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                Log.e(TAG, stackTraceElement.toString());
            }
        }
    }

    private JSONArray constructPastEventsJSONFromList(List<PastEvent> peList) throws JSONException {
        JSONArray pastEventsJSONArray = new JSONArray();
        for (PastEvent pastEvent : peList) {
            JSONObject pastEventJSONObject = pastEvent.getJSONObject();
            pastEventsJSONArray.put(pastEventJSONObject);
        }
        return pastEventsJSONArray;
    }

    private void writePastEventsJSONToFile(JSONArray pastEventsJSONArray) throws IOException {
        // Convert JsonObject to String Format
        String userString = pastEventsJSONArray.toString();
        // Define the File Path and its Name
        File file = new File(getContext().getFilesDir(), PAST_EVENTS_DATA_FILENAME);
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(userString);
        bufferedWriter.close();
    }
}