package no.bouvet.androidskolen.nearbycontacts;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.SelectedContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.services.NearbyService;
import no.bouvet.androidskolen.nearbycontacts.views.AboutView;

public class NearbyActivity extends AppCompatActivity implements ContactSelectedListener {

    private final static String TAG = NearbyActivity.class.getSimpleName();
    private Preferences preferences;
    NearbyService mService;
    boolean mBound = false;
    private Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nearby);
        addNearbyContactsFragmentIfNotExists();

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

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "[onStart]");

        contact = preferences.createContactFromPreferences(getApplicationContext());
        if (contact == null) {
            // Vi dialog om at man må gå og fylle ut informasjon om seg selv.
            return;
        }

        OwnContactViewModel.INSTANCE.setContact(contact);
        Intent intent = new Intent(this, NearbyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "[onStop]");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
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

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG, "Bound to service");
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.publishContact();
            if (OwnContactViewModel.INSTANCE.getContact().isPublish()) {
                Log.i(TAG, "Bound to NearbyService.... publishing contact information");
                mService.publishContact();
            } else {
                Log.i(TAG, "Bound to NearbyService.... keeping contact information secret");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
