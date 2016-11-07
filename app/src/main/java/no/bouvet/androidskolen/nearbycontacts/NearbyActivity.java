package no.bouvet.androidskolen.nearbycontacts;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.messages.BleSignal;
import com.google.android.gms.nearby.messages.Distance;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.nearby.messages.devices.NearbyDevice;

import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.SelectedContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.views.AboutView;

public class NearbyActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ContactSelectedListener {

    private final static String TAG = NearbyActivity.class.getSimpleName();
    private final static int REQUEST_RESOLVE_ERROR = 1;

    private MessageListener messageListener;
    private ContactDetectedListener contactDetectedListener;
    private GoogleApiClient googleApiClient;
    private Message activeMessage;
    private Preferences preferences;

    public void setContactDetectedListener(ContactDetectedListener listener) {
        contactDetectedListener = listener;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nearby);

        setupNearbyMessageListener();

        setupNearbyMessagesApi();

        addNearbyContactsFragmentIfNotExists();

        setContactDetectedListener(NearbyContactsListViewModel.INSTANCE);

        preferences = new Preferences();

    }

    private void addNearbyContactsFragmentIfNotExists() {

        NearbyContactsFragment nearbyContactsFragment = (NearbyContactsFragment) getFragmentManager().findFragmentById(R.id.nearby_contacts_list_fragment);
        if (nearbyContactsFragment == null || !nearbyContactsFragment.isInLayout()) {
            nearbyContactsFragment = new NearbyContactsFragment();

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_holder, nearbyContactsFragment);
            fragmentTransaction.commit();
        }
    }

    private void setupNearbyMessageListener() {
        messageListener = new com.google.android.gms.nearby.messages.MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, "[onFound]");
                NearbyDevice[] nearbyDevices = message.zzbxl();

                for (NearbyDevice nearbyDevice : nearbyDevices) {
                    Log.d(TAG, nearbyDevice.zzbxr());
                }

                String messageAsJson = new String(message.getContent());
                Contact contact = Contact.fromJson(messageAsJson);

                String msg = "Found contact: " + contact.getName();
                Toast toast = Toast.makeText(NearbyActivity.this, msg, Toast.LENGTH_LONG);
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
                Log.d(TAG, "[onLost]");
                String messageAsJson = new String(message.getContent());
                String msg = "Lost sight of message: " + messageAsJson;
                Toast toast = Toast.makeText(NearbyActivity.this, msg, Toast.LENGTH_LONG);
                toast.show();

                Contact contact = Contact.fromJson(messageAsJson);
                fireContactLost(contact);
            }
        };
    }

    private void setupNearbyMessagesApi() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(com.google.android.gms.nearby.Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "[onStart]");

        Contact contact = preferences.createContactFromPreferences(getApplicationContext());
        if (contact == null) {
            // Vi dialog om at man må gå og fylle ut informasjon om seg selv.
            return;
        }

        OwnContactViewModel.INSTANCE.setContact(contact);
        googleApiClient.connect();
    }



    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "[onStop]");

        if (googleApiClient.isConnected()) {
            unpublish();
            unsubscribe();
            googleApiClient.disconnect();
            resetModels();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nearby_activity_actions, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_goto_own_activity:
                gotoOwnContactActivity();
                return true;
            case R.id.action_show_about:
                showAboutDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(new AboutView(this))
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Just close the dialog
                    }
                })
                .create();

        dialog.show();
    }

    private void gotoOwnContactActivity() {
        Intent intent = new Intent(this, OwnContactActivity.class);
        startActivity(intent);
    }

    private void resetModels() {
        NearbyContactsListViewModel.INSTANCE.reset();
        SelectedContactViewModel.INSTANCE.reset();
    }

    private void publish(Contact contact) {

        Log.i(TAG, "[publish] Publishing information about contact: " + contact.getName());
        String json = contact.toJson();
        activeMessage = new Message(json.getBytes());
        com.google.android.gms.nearby.Nearby.Messages.publish(googleApiClient, activeMessage);
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
                Log.d(TAG, "[onExpired] subscribing anew...");
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
        Log.d(TAG, "[publishContactInternally]");
        if (googleApiClient.isConnected()) {
            publish(OwnContactViewModel.INSTANCE.getContact());
            subscribe();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "[onConnected]");
        publishContactInternally();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient disconnected with cause: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "GoogleApiClient connection failed", e);
            }
        } else {
            Log.e(TAG, "GoogleApiClient connection failed");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            if (resultCode == Activity.RESULT_OK) {
                googleApiClient.connect();
            } else {
                Log.e(TAG, "GoogleApiClient connection failed. Unable to resolve.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void onContactSelected(Contact contact) {
        Log.d(TAG, "Contact selected: " + contact.getName());

        SelectedContactFragment selectedContactFragment = (SelectedContactFragment) getFragmentManager().findFragmentById(R.id.selected_contact_fragment);

        if (selectedContactFragment == null || !selectedContactFragment.isInLayout()) {
            SelectedContactFragment newFragment = new SelectedContactFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_holder, newFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }

        SelectedContactViewModel.INSTANCE.setSelectedContact(contact);

    }
}
