package no.bouvet.androidskolen.nearbycontacts.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.messages.BleSignal;
import com.google.android.gms.nearby.messages.Distance;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.nearby.messages.devices.NearbyDevice;

import java.util.ArrayList;
import java.util.List;

import no.bouvet.androidskolen.nearbycontacts.ContactDatabase;
import no.bouvet.androidskolen.nearbycontacts.ContactDetectedListener;
import no.bouvet.androidskolen.nearbycontacts.NearbyActivity;
import no.bouvet.androidskolen.nearbycontacts.OwnContactActivity;
import no.bouvet.androidskolen.nearbycontacts.R;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.ContactLogListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;


/**
 * Tjeneste som kan brukes for å publisere og abonnere på kontakter.
 */

public class NearbyService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = NearbyService.class.getSimpleName();
    private final static int REQUEST_RESOLVE_ERROR = 1;

    private final IBinder mBinder = new NearbyBinder();

    private List<ContactDetectedListener> contactDetectedListeners = new ArrayList<>();
    private NearbyNotificationReceiver nearbyNotificationReceiver;
    private NearbyNotifications nearbyNotifications;
    private ContactDatabase contactDatabase;
    private MessageListener messageListener;
    private GoogleApiClient googleApiClient;
    private Message activeMessage;
    private boolean connected;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.i(TAG, "onStartCommand called with intent == null. Indicates restart because of START_STICKY");
            nearbyNotifications.removeNotification();
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        googleApiClient.connect();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "NearbyService bound");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        Log.i(TAG, "[onUnbind]");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        Log.i(TAG, "[onRebind]");
    }

    // Public metoder utgjør interfacet vi tilbyr ut fra tjenesten.

    public void addContactDetectedListener(ContactDetectedListener listener) {
        contactDetectedListeners.add(listener);
    }

    public void publishContact() {
        Log.i(TAG, "Publishing contact");
        publishContactInternally();
    }

    public void unPublishContact() {
        Log.i(TAG, "Unpublishing contact");
        unpublish();
    }

    private void setupNearbyMessageListener() {
        messageListener = new com.google.android.gms.nearby.messages.MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.i(TAG, "[onFound]");
                NearbyDevice[] nearbyDevices = message.zzbxl();

                for (NearbyDevice nearbyDevice : nearbyDevices) {
                    Log.i(TAG, nearbyDevice.zzbxr());
                }

                String messageAsJson = new String(message.getContent());
                Contact contact = Contact.fromJson(messageAsJson);

                String msg = "Found contact: " + contact.getName();

                contactDatabase.insertContact(contact);

                Toast toast = Toast.makeText(NearbyService.this, msg, Toast.LENGTH_LONG);
                toast.show();

                fireContactDetected(contact);
            }

            @Override
            public void onDistanceChanged(Message message, Distance distance) {
                Log.i(TAG, "Distance changed, message: " + message + ", new distance: " + distance);
            }

            @Override
            public void onBleSignalChanged(Message message, BleSignal bleSignal) {
                Log.i(TAG, "Message: " + message + " has new BLE signal information: " + bleSignal);
            }

            @Override
            public void onLost(Message message) {
                Log.i(TAG, "[onLost]");
                String messageAsJson = new String(message.getContent());

                Contact contact = Contact.fromJson(messageAsJson);
                String msg = "Lost contact: " + contact.getName();
                Toast toast = Toast.makeText(NearbyService.this, msg, Toast.LENGTH_LONG);
                toast.show();

                fireContactLost(contact);
            }
        };
    }

    private void setupNearbyMessagesApi() {
        googleApiClient = new GoogleApiClient.Builder(getBaseContext())
                .addApi(com.google.android.gms.nearby.Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void publish(Contact contact) {
        Log.i(TAG, "[publish] Publishing information about contact: " + contact.getName());
        String json = contact.toJson();
        activeMessage = new Message(json.getBytes());
        com.google.android.gms.nearby.Nearby.Messages.publish(googleApiClient, activeMessage).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Published successfully.");
                } else {
                    Log.i(TAG, "Could not publish, status = " + status);
                }
            }
        });
    }

    private void unpublish() {
        Log.i(TAG, "[unpublish] ");
        if (activeMessage != null) {
            com.google.android.gms.nearby.Nearby.Messages.unpublish(googleApiClient, activeMessage);
            activeMessage = null;
        }
    }

    private void subscribe() {
        Log.i(TAG, "Subscribing.");

        Strategy strategy = new Strategy.Builder().setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT).setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT).setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT).build();

        SubscribeCallback callback = new SubscribeCallback() {
            @Override
            public void onExpired() {
                super.onExpired();
                Log.i(TAG, "[onExpired] subscribing anew...");
                subscribe();
            }
        };
        SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(strategy).setCallback(callback).build();

        com.google.android.gms.nearby.Nearby.Messages.subscribe(googleApiClient, messageListener, options);
    }

    private void unsubscribe() {
        Log.i(TAG, "[unsubscribe].");
        com.google.android.gms.nearby.Nearby.Messages.unsubscribe(googleApiClient, messageListener);
    }


    private void publishContactInternally() {
        Log.i(TAG, "[publishContactInternally]");
        if (googleApiClient.isConnected()) {
            if (OwnContactViewModel.INSTANCE.getContact() != null &&
                    OwnContactViewModel.INSTANCE.getContact().isPublish()) {
                publish(OwnContactViewModel.INSTANCE.getContact());
            }
        } else {
            Log.i(TAG, "googleApiClient not connected, can not publish");
        }
    }

//     GoogleApiClientCallbacks blir håndtert her

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "[onConnected]");
        connected = true;
        nearbyNotifications.updateNotification();
        publishContactInternally();
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient disconnected with cause: " + i);
        connected = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution()) {
            PendingIntent resolution = result.getResolution();
            if (resolution != null) {
                optInForNearby(resolution);
            }
        } else {
            Log.e(TAG, "GoogleApiClient connection failed");
        }
    }

    private void optInForNearby(PendingIntent resolution) {
        try {
            PendingIntent.OnFinished onFinished = new PendingIntent.OnFinished() {
                @Override
                public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String s, Bundle bundle) {
                    googleApiClient.connect();
                }
            };
            resolution.send(REQUEST_RESOLVE_ERROR, onFinished, null);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "GoogleApiClient connection not opted in", e);
        }
    }

    private void fireContactDetected(Contact contact) {
        for (ContactDetectedListener listener : contactDetectedListeners) {
            listener.onContactDetected(contact);
        }
    }

    private void fireContactLost(Contact contact) {
        for (ContactDetectedListener listener : contactDetectedListeners) {
            listener.onContactLost(contact);
        }
    }

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "[onCreate]");
        addContactDetectedListener(NearbyContactsListViewModel.INSTANCE);
        addContactDetectedListener(ContactLogListViewModel.INSTANCE);

        setupNearbyMessageListener();
        setupNearbyMessagesApi();

        contactDatabase = new ContactDatabase(getApplicationContext());

        nearbyNotifications = new NearbyNotifications();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NearbyNotifications.TURN_ON_NEARBY);
        filter.addAction(NearbyNotifications.TURN_OFF_NEARBY);
        nearbyNotificationReceiver = new NearbyNotificationReceiver();
        registerReceiver(nearbyNotificationReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.i(TAG, "[onDestroy]");
        unregisterReceiver(nearbyNotificationReceiver);
    }

    public class NearbyNotifications {

        public final static String TURN_OFF_NEARBY = "no.bouvet.androidskolen.nearbycontacts.TURNOFF";
        public final static String TURN_ON_NEARBY = "no.bouvet.androidskolen.nearbycontacts.TURNON";

        private final static int NOTIFICATION_ID = 123;

        private NotificationManager notificationManager;

        /**
         * Creates a new Notification and either adds a new or updates an old in the Notifications
         * Drawer
         */
        public void updateNotification() {
            if (notificationManager == null) {
                notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            Notification notification = createNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

        /**
         * Removes the Notification from the Notfications Drawer
         */
        public void removeNotification() {
            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        }

        private Notification createNotification() {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(getApplicationContext().getResources().getString(R.string.app_name))
                    .setContentText(getContentText())
                    .setSmallIcon(R.mipmap.ic_launcher);

            // Set intent that is fired if main content is pressed
            Intent intent = new Intent(getApplicationContext(), NearbyActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(NearbyActivity.class);
            stackBuilder.addNextIntent(intent);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // Add an action for turning on and off use of nearby messages
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(getBroadCastActionName());
            PendingIntent broadcast = PendingIntent.getBroadcast(getApplicationContext(), 12, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(getAction(broadcast));

            // Add an action that starts an Activity.
            intent = new Intent(getApplicationContext(), OwnContactActivity.class);
            stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(NearbyActivity.class);
            stackBuilder.addNextIntent(intent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.notification_settings, "Settings", pendingIntent);

            builder.setPriority(NotificationCompat.PRIORITY_HIGH);

            return builder.build();
        }

        private String getContentText() {
            if (connected) return getApplicationContext().getResources().getString(R.string.notification_started_text);
            else return getApplicationContext().getResources().getString(R.string.notification_stopped_text);
        }

        private String getBroadCastActionName() {
            if (connected) return TURN_OFF_NEARBY;
            else return TURN_ON_NEARBY;
        }

        private NotificationCompat.Action getAction(PendingIntent pendingIntent) {
            if (connected) return new NotificationCompat.Action(R.drawable.notification_checked, "Turn off", pendingIntent);
            else return new NotificationCompat.Action(R.drawable.notification_unchecked, "Turn on", pendingIntent);
        }

    }

    public class NearbyNotificationReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NearbyNotifications.TURN_OFF_NEARBY)) {
                Log.d("[onReceive]", "should turn off service");
                unsubscribeAndDisconnect();
            }
            else if (intent.getAction().equals(NearbyNotifications.TURN_ON_NEARBY)) {
                Log.d("[onReceive]", "should turn on service");
                connect();
            }
        }

        private void connect() {
            if (googleApiClient != null && !connected) {
                googleApiClient.connect();
            }
        }

        private void unsubscribeAndDisconnect() {
            if (connected) {
                unsubscribe();
                googleApiClient.disconnect();
                connected = false;
                nearbyNotifications.updateNotification();
            }
        }
    }
}
