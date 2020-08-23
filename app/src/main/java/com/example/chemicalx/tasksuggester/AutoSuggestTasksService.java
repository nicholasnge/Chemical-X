package com.example.chemicalx.tasksuggester;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;

import com.example.chemicalx.Fragment_Tasks.TaskItemModel;
import com.example.chemicalx.TextClassificationClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class AutoSuggestTasksService extends IntentService
        implements TaskSuggester.TaskSuggestionResponseListener {

    private static final String TAG = "AutoSuggestTasksService";
    int durationInMinutes = 60;
    ArrayList<TaskItemModel> tasks;

    public AutoSuggestTasksService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Service method was fired.");
        if (isBusyNow(intent)) {
            return;
        }

        TextClassificationClient tf_classifytasks = new TextClassificationClient(this);
        long nowMillis = Calendar.getInstance().getTimeInMillis();
        tasks = intent.getBundleExtra("bundle").getParcelableArrayList("tasks");
        if (tasks == null) {
            Log.e(TAG, "tasks is null");
            return;
        }

        TaskSuggester.suggestTask(this, this, tf_classifytasks,
                tasks, durationInMinutes, nowMillis);
    }

    @Override
    public void onTaskSuggestionResponse(int taskIndex, int durationInMinutes) {
        Log.i(TAG, "taskIndex: " + taskIndex + ", durationInMinutes: " + durationInMinutes);
        // if time now is too late (after 9pm), stop the alarms
        if (Calendar.getInstance().HOUR_OF_DAY < 21) {
            // else, set next alarm an hour from now
            setAlarmForNextSuggestion(System.currentTimeMillis() + 60 * 60 * 1000);
        }
        if (taskIndex >= 0) {
            triggerNotification(tasks.get(taskIndex), durationInMinutes);
        }
    }

    void setAlarmForNextSuggestion(long time) {
        Date date = new Date(time); // for debugging
        Log.d(TAG, "setting next alarm at: " + date.toString());

        Intent intent = new Intent(this, AutoSuggestTasksBroadcastReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("tasks", tasks);
        intent.putExtra("bundle", bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }


    private void triggerNotification(TaskItemModel task, long time) {
        Intent intent = new Intent(this, NotificationsBroadcastReceiver.class);
        intent.putExtra("task", task.getTitle());
        intent.putExtra("duration", time);
        sendBroadcast(intent);
    }

    boolean isBusyNow(Intent intent) {
        //projection variables
        String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
        };
        // The indices for the projection array above.
        // we only care about end time and title
        int PROJECTION_TITLE_INDEX = 0;
        int PROJECTION_END_INDEX = 2;

        // the exact moment right now
        Calendar now = Calendar.getInstance();

        // Run query
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Instances.CONTENT_URI;

        // Construct the query with the desired date range.
        Uri.Builder builder = uri.buildUpon();
        // begin time = now
        ContentUris.appendId(builder, now.getTimeInMillis());
        // end time = 5mins from now
        ContentUris.appendId(builder, now.getTimeInMillis() + 5 * 60 * 1000);

        // Submit the query and get a Cursor object back.
        // method called only after READ_CALENDAR permission obtained so can suppress
        @SuppressLint("MissingPermission")
        Cursor cur = cr.query(builder.build(), INSTANCE_PROJECTION, null, null, null);

        // if any event is in the cur, use the end time of that event as the
        // time for the next alarm
        Log.d(TAG, "isBusyNow: " + cur.getCount() + "event(s)");
        if (cur.getCount() <= 0) {
            return false;
        } else {
            //cur starts at index -1, need to move to index 0.
            cur.moveToNext();

            Log.d(TAG, "isBusyNow - currently busy with: " + cur.getString(PROJECTION_TITLE_INDEX));
            // set alarm for end time of the current activity
            setAlarmForNextSuggestion(cur.getLong(PROJECTION_END_INDEX));
            return true;
        }
    }
}