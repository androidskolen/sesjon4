package no.bouvet.androidskolen.nearbycontacts.services;

import android.app.Service;
import android.content.Intent;
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
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.nearby.messages.devices.NearbyDevice;

import no.bouvet.androidskolen.nearbycontacts.ContactDetectedListener;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;

import static android.app.Activity.RESULT_OK;

/**
 * Tjeneste som kan brukes for å publisere og abonnere på kontakter.
 */

public class NearbyService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private MessageListener messageListener;
    private ContactDetectedListener contactDetectedListener;
    private GoogleApiClient googleApiClient;
    private Message activeMessage;
    private Contact contact;

    private final static String TAG = NearbyService.class.getSimpleName();
    private final static int REQUEST_RESOLVE_ERROR = 1;

    private final IBinder mBinder = new NearbyBinder();

    public NearbyService() {
        super();
    }

    public void setContactDetectedListener(ContactDetectedListener listener) {
        contactDetectedListener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "NearbyService bound");
        setContactDetectedListener(NearbyContactsListViewModel.INSTANCE);
        setupNearbyMessageListener();
        setupNearbyMessagesApi();
        googleApiClient.connect();
        return mBinder;
    }


    // Public metoder utgjør interfacet mot tjenesten.

    public void publishContact() {
        Log.i(TAG, "Publishing contact");
        publishContactInternally();
    }

    public void unPublishContact() {
        Log.i(TAG, "Unpublishing contact");
        unpublish();
    }

    public void unsubscribeToMessages() {
        Log.i(TAG, "Unsubscribing");
        unsubscribe();
    }

    public void subscribeToMessages() {
        Log.i(TAG, "Subscribing");
        subscribe();
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
                String msg = "Lost sight of message: " + messageAsJson;
                Toast toast = Toast.makeText(NearbyService.this, msg, Toast.LENGTH_LONG);
                toast.show();

                Contact contact = Contact.fromJson(messageAsJson);
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
            publish(OwnContactViewModel.INSTANCE.getContact());
            subscribe();
        }
    }

//     GoogleApiClientCallbacks blir håndtert her

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "[onConnected]");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient disconnected with cause: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution()) {
            Log.e(TAG, "GoogleApiClient connection failed");
        } else {
            Log.e(TAG, "GoogleApiClient connection failed");
        }
    }

    private void fireContactDetected(Contact contact) {
        if (contactDetectedListener != null) {
            contactDetectedListener.onContactDetected(contact);
        }
    }

    private void fireContactLost(Contact contact) {
        if (contactDetectedListener != null) {
            contactDetectedListener.onContactLost(contact);
        }
    }

    public class NearbyBinder extends Binder {
        public NearbyService getService() {
            return NearbyService.this;
        }
    }
}
