package no.bouvet.androidskolen.nearbycontacts;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import no.bouvet.androidskolen.nearbycontacts.adapers.ContactLogAdapter;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.ContactLogListViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.ModelUpdateListener;

public class ContactLogActivity extends AppCompatActivity implements ModelUpdateListener, CompoundButton.OnCheckedChangeListener {

    private ContactDatabase contactDatabase;
    private ContactLogAdapter contactsLogAdapter;
    private Switch showAllSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_log);

        contactDatabase = new ContactDatabase(getApplicationContext());
        List<Contact> contactLog = contactDatabase.getAllContactsEntries();

        showAllSwitch = (Switch) findViewById(R.id.show_all_toggle);
        showAllSwitch.setOnCheckedChangeListener(this);

        TextView logCount = (TextView) findViewById(R.id.log_count);
        logCount.setText("Number of entries in db " + contactLog.size());

        ListView listView = (ListView) findViewById(R.id.contact_history_list_view);
        contactsLogAdapter = new ContactLogAdapter(getApplicationContext());
        listView.setAdapter(contactsLogAdapter);

        updateAdapterModel();
    }

    @Override
    public void onResume() {
        super.onResume();

        ContactLogListViewModel.INSTANCE.setModelUpdateListener(this);
        updateAdapterModel();
    }

    @Override
    public void onPause() {
        super.onPause();

        ContactLogListViewModel.INSTANCE.removeModelUpdateListener(this);
    }

    @Override
    public void onModelChanged() {
        updateAdapterModel();
    }

    private void updateAdapterModel() {
        List<Contact> contactLog;
        if (showAllSwitch.isChecked()) {
            contactLog =  contactDatabase.getAllContactsEntries();
        } else {
            contactLog = contactDatabase.getLastEntryForAllContacts();
        }

        if (contactLog != null) {
            contactsLogAdapter.clear();
            contactsLogAdapter.addAll(contactLog);
            contactsLogAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateAdapterModel();
    }
}
