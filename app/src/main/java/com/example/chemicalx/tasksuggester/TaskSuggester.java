package com.example.chemicalx.tasksuggester;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chemicalx.Fragment_Tasks.PastEvent;
import com.example.chemicalx.Fragment_Tasks.TaskItemModel;
import com.example.chemicalx.TextClassificationClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskSuggester {
    private static final String TAG = "TaskSuggester";
    private static final String PAST_EVENTS_DATA_FILENAME = "data/ml_training/past_events_data.json";
    private static final String TRAIN_MODEL_URL = "https://us-central1-chemical-x-d86cc.cloudfunctions.net/train_model";
    private static final String TASK_SUGGESTER_PREDICT_URL = "https://us-central1-chemical-x-d86cc.cloudfunctions.net/task_suggester_predict";
    private static final String BASE_USER_ID = "base_user";
    private static final long TIME_UNTIL_SWITCH_MODEL = 30L * 24 * 60 * 60 * 1000;
    private static final int FAILED_TASK_SUGGESTION_TASK_INDEX = -1;
    private static final int PAST_EVENTS_LIST_MAX_SIZE = 168;

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

    public interface TaskSuggestionResponseListener {
        public void onTaskSuggestionResponse(int taskIndex, int durationInMinutes);
    }

    public static void trainModel(Context context) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG + ".trainModel", "User ID: " + userID);
        JSONObject requestJSONObject = new JSONObject();
        try {
            requestJSONObject.put("userID", userID);
        } catch (JSONException e) {
            Log.e(TAG + ".trainModel", "Could not create the request JSON.\n" + e.getMessage());
            return;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, TRAIN_MODEL_URL, requestJSONObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, "Model successfully trained.", Toast.LENGTH_SHORT);
                        try {
                            String message = (String) response.get("message");
                            Log.d(TAG + ".trainModel", "Response message: " + message);
                            String modelUserID = (String) response.get("modelUserID");
                            Log.d(TAG + ".trainModel", "Model User ID: " + modelUserID);
                        } catch (JSONException e) {
                            Log.e(TAG + ".trainModel", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Model could not be trained; there was an error.", Toast.LENGTH_LONG);
                        String volleyErrorMessage = error.getMessage();
                        if (volleyErrorMessage != null) {
                            Log.e(TAG + ".trainModel", volleyErrorMessage);
                        }
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    public static void suggestTask(Context context, TaskSuggestionResponseListener listener, TextClassificationClient tf_classifytasks, List<TaskItemModel> tasks, int durationInMinutes, long taskIntendedStartTime) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject requestJSONObject = new JSONObject();
        Calendar now = Calendar.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        long timeOfCreation = currentUser.getMetadata().getCreationTimestamp();
        long timeSinceCreation = now.getTimeInMillis() - timeOfCreation;
        String userID;
        if (timeSinceCreation < TIME_UNTIL_SWITCH_MODEL) {
            userID = BASE_USER_ID;
        } else {
            userID = currentUser.getUid();
        }
        long desiredDuration = durationInMinutes * 60 * 1000;
        try {
            JSONArray inputsJSONArray = getInputsJSON(context, tf_classifytasks, tasks, desiredDuration, taskIntendedStartTime);
            requestJSONObject.put("userID", userID);
            requestJSONObject.put("inputs", inputsJSONArray);
            Log.d(TAG + ".suggestTask", "Finished preparing request json.\n" + requestJSONObject.toString(2));
        } catch (JSONException e) {
            Log.e(TAG + ".suggestTask", "Could not create the request JSON.\n" + e.getMessage());
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, TASK_SUGGESTER_PREDICT_URL, requestJSONObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        int taskIndex = FAILED_TASK_SUGGESTION_TASK_INDEX;
                        try {
                            JSONArray productivitiesJSONArray = response.getJSONArray("outputs");
                            int arraySize = productivitiesJSONArray.length();
                            double maxProductivity = 0;
                            double productivity;
                            for (int i = 0; i < arraySize; i++) {
                                productivity = ((Number) productivitiesJSONArray.get(i)).doubleValue();
                                Log.d(TAG + ".suggestTask", "Productivity for task " + i + ":" + productivity);
                                if (productivity > maxProductivity) {
                                    taskIndex = i;
                                    maxProductivity = productivity;
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG + ".suggestTask", "Could not create the request JSON.\n" + e.getMessage());
                        }
                        listener.onTaskSuggestionResponse(taskIndex, durationInMinutes);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Prediction could not be made; there was an error.", Toast.LENGTH_LONG);
                        String volleyErrorMessage = error.getMessage();
                        if (volleyErrorMessage != null) {
                            Log.e(TAG + ".suggestTask", volleyErrorMessage);
                        } else {
                            Log.e(TAG + ".suggestTask", "Request failed.");
                        }
                        listener.onTaskSuggestionResponse(FAILED_TASK_SUGGESTION_TASK_INDEX, durationInMinutes);
                    }
                });

        requestQueue.add(jsonObjectRequest);
        Log.d(TAG + ".suggestTask", "Request sent.");
    }

    private static JSONArray getInputsJSON(
            Context context, TextClassificationClient tf_classifytasks, List<TaskItemModel> tasks, long desiredDuration, long taskIntendedStartTime) throws JSONException {
        JSONArray inputsJSONArray = new JSONArray();
        Calendar taskIntendedStartCalendar = Calendar.getInstance();
        taskIntendedStartCalendar.setTimeInMillis(taskIntendedStartTime);
        int hourOfDay = taskIntendedStartCalendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = taskIntendedStartCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        double normalisedDesiredDuration = Math.tanh(Math.log(((double) desiredDuration) / 1000 / 60 / 60));
        JSONArray pastEventsJSONArray = initialisePastEventsJSON(context, tf_classifytasks, taskIntendedStartTime);
        for (TaskItemModel task : tasks) {
            JSONObject taskInputJSONObject = getTaskInputDataJSON(task, taskIntendedStartTime, hourOfDay, dayOfWeek,
                    normalisedDesiredDuration, pastEventsJSONArray);
            inputsJSONArray.put(taskInputJSONObject);
        }
        return inputsJSONArray;
    }

    private static JSONObject getTaskInputDataJSON(
            TaskItemModel task, long taskIntendedStartTime, int hourOfDay, int dayOfWeek,
            double normalisedDesiredDuration, JSONArray pastEventsJSONArray) throws JSONException {
        JSONObject taskInputJSONObject = new JSONObject();

        String taskCategory = task.getCategory();
        int taskCategoryIndex;
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
        long expectedDuration = ((long) (task.getTotalTime() - task.getTimePassed())) * 1000;
        double normalisedExpectedDuration = Math.tanh(((double) expectedDuration) / 1000 / 60 / 60 / 6);
        long deadlineTimeMillis = task.dueDate.toDate().getTime();
        long timeUntilDeadline = deadlineTimeMillis - taskIntendedStartTime;
        double normalisedTimeUntilDeadline = Math.tanh(((double) timeUntilDeadline) / 1000 / 60 / 60 / 24 / 7);

        taskInputJSONObject.put("taskCategory", taskCategoryIndex);
        taskInputJSONObject.put("expectedDuration", normalisedExpectedDuration);
        taskInputJSONObject.put("timeUntilDeadline", normalisedTimeUntilDeadline);
        taskInputJSONObject.put("hourOfDay", hourOfDay);
        taskInputJSONObject.put("dayOfWeek", dayOfWeek);
        taskInputJSONObject.put("desiredDuration", normalisedDesiredDuration);
        taskInputJSONObject.put("pastEvents", pastEventsJSONArray);

        return taskInputJSONObject;
    }

    private static JSONArray initialisePastEventsJSON(Context context, TextClassificationClient tf_classifytasks, long taskIntendedStartTime) throws JSONException {
        JSONArray pastEventsJSONArray = new JSONArray();
        List<PastEvent> peList = initialisePastEventsList(context, tf_classifytasks);
        for (PastEvent pastEvent : peList) {
            JSONObject pastEventJSONObject = new JSONObject();
            Map<String, Object> pastEventMap = pastEvent.getPastEventMap(taskIntendedStartTime);
            Set<String> pastEventDataKeys = pastEventMap.keySet();
            for (String pastEventDataKey : pastEventDataKeys) {
                Object pastEventDataValue = pastEventMap.get(pastEventDataKey);
                pastEventJSONObject.put(pastEventDataKey, pastEventDataValue);
            }
            pastEventsJSONArray.put(pastEventJSONObject);
        }
        return pastEventsJSONArray;
    }

    private static List<PastEvent> initialisePastEventsList(Context context, TextClassificationClient tf_classifytasks) {
        List<PastEvent> peList;
        try {
            JSONArray jsonArray = getPastEventsJSONFromFile(context);
            peList = constructPastEventsListFromJSON(jsonArray);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Past events could not be loaded.\n" + e.getMessage());
            peList = constructPastEventsListFromCalendar(context, tf_classifytasks);
        }
        peList = updatePastEventsList(context, tf_classifytasks, peList);
        return peList;
    }

    private static List<PastEvent> constructPastEventsListFromJSON(JSONArray jsonArray) throws JSONException {
        List<PastEvent> peList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject pastEventJSON = jsonArray.getJSONObject(i);
            peList.add(PastEvent.constructFromJSONObject(pastEventJSON));
        }
        return peList;
    }

    private static JSONArray getPastEventsJSONFromFile(Context context) throws IOException, JSONException {
        File pastEventsFile = new File(context.getFilesDir(), PAST_EVENTS_DATA_FILENAME);
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

    private static List<PastEvent> constructPastEventsListFromCalendar(Context context, TextClassificationClient tf_classifytasks) {
        List<PastEvent> peList = new ArrayList<>();

        // one week ago from today
        Calendar oneWeekAgo = Calendar.getInstance();
        oneWeekAgo.add(Calendar.DATE, -7);

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = context.getContentResolver();
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

    private static List<PastEvent> updatePastEventsList(Context context, TextClassificationClient tf_classifytasks, List<PastEvent> pastEventsList) {
        int size = pastEventsList.size();
        PastEvent lastPastEvent = pastEventsList.get(size - 1);
        long lastPastEventEndTimeMillis = lastPastEvent.getEndTime();

        // lastPastEventEndTime
        Calendar lastPastEventEndTime = Calendar.getInstance();
        lastPastEventEndTime.setTimeInMillis(lastPastEventEndTimeMillis);

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = context.getContentResolver();
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
            addPastEventToList(context, pastEventsList, new PastEvent(eventCategoryIndex, eventDuration, dtend));
        }

        pastEventsList.sort(null);
        savePastEventsList(context, pastEventsList);

        return pastEventsList;
    }

    private static void addPastEventToList(Context context, List<PastEvent> pastEventsList, PastEvent pastEvent) {
        int size = pastEventsList.size();
        if (size >= PAST_EVENTS_LIST_MAX_SIZE) {
            int diff = size + 1 - PAST_EVENTS_LIST_MAX_SIZE;
            pastEventsList = pastEventsList.subList(diff, size);
        }
        pastEventsList.add(pastEvent);
        savePastEventsList(context, pastEventsList);
    }

    private static void savePastEventsList(Context context, List<PastEvent> pastEventsList) {
        try {
            JSONArray pastEventsJSONArray = constructPastEventsJSONFromList(pastEventsList);
            writePastEventsJSONToFile(context, pastEventsJSONArray);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Past events could not be saved.\n" + e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                Log.e(TAG, stackTraceElement.toString());
            }
        }
    }

    private static JSONArray constructPastEventsJSONFromList(List<PastEvent> peList) throws JSONException {
        JSONArray pastEventsJSONArray = new JSONArray();
        for (PastEvent pastEvent : peList) {
            JSONObject pastEventJSONObject = pastEvent.getJSONObject();
            pastEventsJSONArray.put(pastEventJSONObject);
        }
        return pastEventsJSONArray;
    }

    private static void writePastEventsJSONToFile(Context context, JSONArray pastEventsJSONArray) throws IOException {
        // Convert JsonObject to String Format
        String userString = pastEventsJSONArray.toString();
        // Define the File Path and its Name
        File file = new File(context.getFilesDir(), PAST_EVENTS_DATA_FILENAME);
        file.getParentFile().mkdirs();
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(userString);
        bufferedWriter.close();
    }
}
