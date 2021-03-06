package org.project.adam.alert;
/**
 * Adam project
 * Copyright (C) 2017 Orange
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.ReceiverAction;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.WakeLock;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.support.content.AbstractBroadcastReceiver;
import org.project.adam.MainActivity_;
import org.project.adam.Preferences_;
import org.project.adam.R;

import java.util.Random;

import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@EReceiver
public class AlertReceiver extends AbstractBroadcastReceiver {

    private static final String GROUP = "Reminder";

    public static final String RECEIVER_ACTION = "org.project.adam.ALARM";

    @StringRes(R.string.pref_alert_type_value_none)
    protected String alertTypeNone;

    @StringRes(R.string.pref_alert_type_value_notif)
    protected String alertTypeNotif;

    @StringRes(R.string.pref_alert_type_value_alarm)
    protected String alertTypeAlarm;

    @SystemService
    protected NotificationManager notificationManager;

    @Bean
    protected AlertScheduler alertScheduler;

    @Pref
    protected Preferences_ prefs;

    @ReceiverAction(actions = Intent.ACTION_BOOT_COMPLETED)
    public void resetAlarms() {
        Timber.d("Boot detected, setting up alerts");
        alertScheduler.schedule();
    }

    @ReceiverAction(actions = RECEIVER_ACTION)
    void wakeUpAlert(Intent intent, @ReceiverAction.Extra String time, @ReceiverAction.Extra String content, Context context) {
        Timber.i("ALARM RECEIVED for meal %s", time);
        String selectedAlertType = prefs.alertType().getOr(alertTypeAlarm);
        if (alertTypeNone.equals(selectedAlertType)) {
            Timber.d("There is an alert, but apparently no one cares");
        } else if (alertTypeNotif.equals(selectedAlertType)) {
            showNotification(time, content, context);
        } else if (alertTypeAlarm.equals(selectedAlertType)) {
            showAlertActivity(time, content, context);
        } else {
            Timber.w("How did we ended up here? alert type = %d", prefs.alertType().get());
        }

        alertScheduler.schedule();
    }

    @SystemService
    protected KeyguardManager keyguardManager;

    @WakeLock(tag = "MyTag", level = WakeLock.Level.FULL_WAKE_LOCK, flags = WakeLock.Flag.ACQUIRE_CAUSES_WAKEUP)
    protected void showAlertActivity(String time, String content, Context context) {
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
        AlertActivity_.intent(context).mealContent(content).mealTime(time).flags(FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_CLEAR_TASK).start();
    }

    private void showNotification(String time, String content, Context context) {
        PendingIntent pi = PendingIntent.getActivity(context, 0, MainActivity_.intent(context).get(), PendingIntent.FLAG_UPDATE_CURRENT);

        String body = String.format(context.getResources().getString(R.string.notif_content), time);

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_reminder_notification)
                .setContentTitle(context.getString(R.string.notif_title))
                .setAutoCancel(true)
                .setSound(notificationSoundUri())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentText(body)
                .setGroup(GROUP)
                .setContentIntent(pi);

        if (!TextUtils.isEmpty(content)) {
            mBuilder = mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(String.format("%s\n%s", body, content)));
        }

        Random r = new Random();
        notificationManager.notify(r.nextInt(), mBuilder.build());
    }

    private Uri notificationSoundUri() {
        String ringtoneUri = prefs.notifRingtone().getOr(null);

        if (ringtoneUri != null) {
            return Uri.parse(ringtoneUri);
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }


}
