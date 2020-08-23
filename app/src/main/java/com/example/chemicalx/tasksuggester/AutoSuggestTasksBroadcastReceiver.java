package com.example.chemicalx.tasksuggester;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.chemicalx.Fragment_Tasks.TaskItemModel;

import java.util.ArrayList;

public class AutoSuggestTasksBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "AutoSuggestTasksBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "intent received");
        Intent i = new Intent(context, AutoSuggestTasksService.class);
        ArrayList<TaskItemModel> tasks =  intent.getBundleExtra("bundle").getParcelableArrayList("tasks");

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("tasks", tasks);
        i.putExtra("bundle",bundle);
        context.startService(i);
    }
}
