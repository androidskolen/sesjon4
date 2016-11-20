package no.bouvet.androidskolen.nearbycontacts;


import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public enum  NearbyNotifications implements Application.ActivityLifecycleCallbacks {

    INSTANCE;

    private final static int NOTIFICATION_ID = 123;
    private final AtomicInteger activityResumedCounter = new AtomicInteger();

    private boolean listening;
    private Context applicationContext;
    private NotificationManager notificationManager;

    public void listenForActivityLifeCycle(Application application) {
        if (!listening) {
            application.registerActivityLifecycleCallbacks(this);
            listening = true;
        }
        applicationContext = application;
        notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void checkIfNotificationShouldBeDisplayed() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(createNotificationTask(), 500);
    }

    private Runnable createNotificationTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (activityResumedCounter.get() == 0) {
                    Log.d("NOTIFICATION", "Showing notification since no activities are running");
                    Notification notification = createNotification();

                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        };
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext)
                .setContentTitle(applicationContext.getResources().getString(R.string.notification_title))
                .setContentText(applicationContext.getResources().getString(R.string.notification_text))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light);

        Intent intent = new Intent(applicationContext, NearbyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(applicationContext);
        stackBuilder.addParentStack(NearbyActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        activityResumedCounter.incrementAndGet();
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activityResumedCounter.decrementAndGet();
        checkIfNotificationShouldBeDisplayed();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
