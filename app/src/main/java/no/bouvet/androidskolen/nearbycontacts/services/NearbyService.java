package no.bouvet.androidskolen.nearbycontacts.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import no.bouvet.androidskolen.nearbycontacts.NearbyNotifications;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.ContactLogListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;


/**
 * Tjeneste som kan brukes for å publisere og abonnere på kontakter.
 */

public class NearbyService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private MessageListener messageListener;
    private List<ContactDetectedListener> contactDetectedListeners = new ArrayList<>();
    private GoogleApiClient googleApiClient;
    private Message activeMessage;

    private final static String TAG = NearbyService.class.getSimpleName();
    private final static int REQUEST_RESOLVE_ERROR = 1;

    private final IBinder mBinder = new NearbyBinder();
    private ContactDatabase contactDatabase;
    private boolean connected;
    private NearbyNotificationReceiver nearbyNotificationReceiver;

    public NearbyService() {
        super();
        Log.i(TAG, "Created NearbyService");
    }

    public void addContactDetectedListener(ContactDetectedListener listener) {
        contactDetectedListeners.add(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.i(TAG, "[RESTART]");
            NearbyNotifications.INSTANCE.removeNotification(getApplicationContext());
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        addContactDetectedListener(NearbyContactsListViewModel.INSTANCE);
        addContactDetectedListener(ContactLogListViewModel.INSTANCE);
        setupNearbyMessageListener();
        setupNearbyMessagesApi();

        googleApiClient.connect();

        contactDatabase = new ContactDatabase(getApplicationContext());

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

    public void publishContact() {
        Log.i(TAG, "Publishing contact");
        publishContactInternally();
    }

    public void unPublishContact() {
        Log.i(TAG, "Unpublishing contact");
        unpublish();
    }

    public void start() {
        if (googleApiClient != null && !connected) {
            googleApiClient.connect();
        }
    }

    public void stop() {
        if (connected) {
            unsubscribe();
            googleApiClient.disconnect();
            connected = false;
            NearbyNotifications.INSTANCE.updateNotification(getApplicationContext(), false);
        }
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
        NearbyNotifications.INSTANCE.updateNotification(getApplicationContext(), true);
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

            PendingIntent.OnFinished onFinished = new PendingIntent.OnFinished() {
                @Override
                public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String s, Bundle bundle) {
                    googleApiClient.connect();
                }
            };

            PendingIntent resolution = result.getResolution();
            try {
                resolution.send(REQUEST_RESOLVE_ERROR, onFinished, null);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "GoogleApiClient connection not opted in", e);
            }
        } else {
            Log.e(TAG, "GoogleApiClient connection failed");
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

    public class NearbyNotificationReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NearbyNotifications.TURN_OFF_NEARBY)) {
                Log.d("[onReceive]", "should turn off service");
                stop();
            }
            else if (intent.getAction().equals(NearbyNotifications.TURN_ON_NEARBY)) {
                Log.d("[onReceive]", "should turn on service");
                start();
            }
        }
    }
}
