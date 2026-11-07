package org.godotengine.plugin.android.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import org.godotengine.plugin.android.notification.model.NotificationData;

public class NotificationJobService extends JobService {
    private static final String LOG_TAG = "godot::NotificationJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        if (extras == null || !extras.containsKey(NotificationData.DATA_KEY_ID)) {
            Log.e(LOG_TAG, "onStartJob(): Missing notification extras");
            return false;
        }

        int notificationId = extras.getInt(NotificationData.DATA_KEY_ID, 0);
        Log.i(LOG_TAG, "onStartJob(): Firing scheduled notification via JobScheduler for id=" + notificationId);

        Context context = getApplicationContext();

        // Cancel any corresponding AlarmManager pending intent to avoid duplicate firing
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent alarmIntent = new Intent(context, NotificationReceiver.class);
                alarmIntent.putExtra(NotificationData.DATA_KEY_ID, notificationId);
                alarmManager.cancel(PendingIntent.getBroadcast(context, notificationId, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "onStartJob(): Failed to cancel redundant alarm: " + e.getMessage());
        }

        Intent receiverIntent = new Intent(context, NotificationReceiver.class);
        receiverIntent.putExtra(NotificationData.DATA_KEY_ID, notificationId);
        receiverIntent.putExtra(NotificationData.DATA_KEY_CHANNEL_ID, extras.getString(NotificationData.DATA_KEY_CHANNEL_ID, "shiro_reminders"));
        receiverIntent.putExtra(NotificationData.DATA_KEY_TITLE, extras.getString(NotificationData.DATA_KEY_TITLE, "Shiro"));
        receiverIntent.putExtra(NotificationData.DATA_KEY_CONTENT, extras.getString(NotificationData.DATA_KEY_CONTENT, ""));
        receiverIntent.putExtra(NotificationData.DATA_KEY_SMALL_ICON_NAME, extras.getString(NotificationData.DATA_KEY_SMALL_ICON_NAME, "ic_default_notification"));

        if (extras.containsKey(NotificationData.DATA_KEY_DEEPLINK)) {
            receiverIntent.putExtra(NotificationData.DATA_KEY_DEEPLINK, extras.getString(NotificationData.DATA_KEY_DEEPLINK));
        }
        if (extras.getInt(NotificationData.OPTION_KEY_RESTART_APP, 0) == 1) {
            receiverIntent.putExtra(NotificationData.OPTION_KEY_RESTART_APP, true);
        }
        int badgeCount = extras.getInt(NotificationData.DATA_KEY_BADGE_COUNT, 0);
        if (badgeCount > 0) {
            receiverIntent.putExtra(NotificationData.DATA_KEY_BADGE_COUNT, badgeCount);
        }

        new NotificationReceiver().onReceive(context, receiverIntent);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
