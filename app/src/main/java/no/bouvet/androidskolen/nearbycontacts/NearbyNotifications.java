package no.bouvet.androidskolen.nearbycontacts;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public enum  NearbyNotifications {

    INSTANCE;

    public final static String TURN_OFF_NEARBY = "no.bouvet.androidskolen.nearbycontacts.TURNOFF";
    public final static String TURN_ON_NEARBY = "no.bouvet.androidskolen.nearbycontacts.TURNON";

    private final static int NOTIFICATION_ID = 123;

    private NotificationManager notificationManager;


    public void updateNotification(Context context, boolean isConnectedToNearby) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        Notification notification = createNotification(context, isConnectedToNearby);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void removeNotification(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private Notification createNotification(Context context, boolean isConnectedToNearby) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(getContentText(context, isConnectedToNearby))
                .setSmallIcon(R.mipmap.ic_launcher);

        // Set intent that is fired if main content is pressed
        Intent intent = new Intent(context, NearbyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(NearbyActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        // Add an action for turning on and off use of nearby messages
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(getBroadCastActionName(isConnectedToNearby));
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 12, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(getAction(isConnectedToNearby, broadcast));

        // Add an action that starts an Activity.
        Intent activityIntent = new Intent(context, OwnContactActivity.class);
        PendingIntent activity = PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.notification_settings, "Settings", activity);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }

    private String getContentText(Context context, boolean isConnectedToNearby) {
        if (isConnectedToNearby) return context.getResources().getString(R.string.notification_started_text);
        else return context.getResources().getString(R.string.notification_stopped_text);
    }

    private String getBroadCastActionName(boolean isConnectedToNearby) {
        if (isConnectedToNearby) return TURN_OFF_NEARBY;
        else return TURN_ON_NEARBY;
    }

    private NotificationCompat.Action getAction(boolean isConnectedToNearby, PendingIntent pendingIntent) {
        if (isConnectedToNearby) return new NotificationCompat.Action(R.drawable.notification_checked, "Turn off", pendingIntent);
        else return new NotificationCompat.Action(R.drawable.notification_unchecked, "Turn on", pendingIntent);
    }


}
