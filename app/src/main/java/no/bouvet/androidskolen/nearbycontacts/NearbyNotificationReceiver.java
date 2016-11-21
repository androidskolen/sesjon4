package no.bouvet.androidskolen.nearbycontacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NearbyNotificationReceiver extends BroadcastReceiver{


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NearbyNotifications.TURN_OFF_NEARBY)) {

            Log.d("[onReceive]", "should turn off service");

        }
    }



}
