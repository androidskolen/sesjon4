package no.bouvet.androidskolen.nearbycontacts;

import android.app.ActivityManager;
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
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nearby);
        addNearbyContactsFragmentIfNotExists();
        preferences = new Preferences();

        Contact contact = preferences.createContactFromPreferences(getApplicationContext());

        OwnContactViewModel.INSTANCE.setContact(contact);

        intent = new Intent(getApplicationContext(), NearbyService.class);
        if (!isServiceRunning(NearbyService.class)) {
            startService(intent);
        }

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "[onStop]");
        if (!isServiceRunning(NearbyService.class)) {
            stopService(intent);
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
            case R.id.action_goto_contact_log:
                gotoContactLogActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void gotoContactLogActivity() {
        Intent intent = new Intent(this, ContactLogActivity.class);
        startActivity(intent);
    }

    private void gotoOwnContactActivity() {
        Intent intent = new Intent(this, OwnContactActivity.class);
        startActivity(intent);
    }

    private void resetModels() {
        NearbyContactsListViewModel.INSTANCE.reset();
        SelectedContactViewModel.INSTANCE.reset();
    }
}
