package no.bouvet.androidskolen.nearbycontacts;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

class NearbyNotifications implements Application.ActivityLifecycleCallbacks {

    private final AtomicInteger _activityResumedCounter = new AtomicInteger();

    NearbyNotifications() {

    }

    private void checkIfNotificationShouldBeDisplayed() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(createNotificationTask(), 500);
    }

    private Runnable createNotificationTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (_activityResumedCounter.get() == 0) {
                    Log.d("NOTIFICATION", "Showing notification since no activities are running");
                }
            }
        };
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        _activityResumedCounter.incrementAndGet();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        _activityResumedCounter.decrementAndGet();
        checkIfNotificationShouldBeDisplayed();
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
