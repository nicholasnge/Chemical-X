package com.example.chemicalx.tasksuggester;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chemicalx.MainActivity;
import com.example.chemicalx.R;

public class NotificationsBroadcastReceiver extends BroadcastReceiver {
    public final static String TAG = "ScheduleBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        String task = intent.getStringExtra("task");
        long duration = intent.getLongExtra("duration", 60);

        // sometimes intents are double sent, dunno why. so this is a plaster to deny the erroneous intents
        if (task == null){
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SCHEDULE_CHANNEL")
                .setSmallIcon(R.drawable.ic_molecular)
                .setContentTitle("Task Suggestion")
                .setContentText(task + " for " + duration + " minutes")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }
}
