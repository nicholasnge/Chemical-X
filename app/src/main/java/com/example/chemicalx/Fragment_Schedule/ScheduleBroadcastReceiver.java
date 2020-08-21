package com.example.chemicalx.Fragment_Schedule;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chemicalx.MainActivity;
import com.example.chemicalx.R;

public class ScheduleBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SCHEDULE BROADCAST", " HEAR HEAR");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        String task = intent.getStringExtra("task");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SCHEDULE_CHANNEL")
                .setSmallIcon(R.drawable.ic_molecular)
                .setContentTitle(task)
                .setContentText("Try working on this task now if you have the time!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }
}
