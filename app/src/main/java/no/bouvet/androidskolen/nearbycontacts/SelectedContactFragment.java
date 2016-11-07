package no.bouvet.androidskolen.nearbycontacts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import no.bouvet.androidskolen.nearbycontacts.models.ModelUpdateListener;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.SelectedContactViewModel;

public class SelectedContactFragment extends Fragment implements ModelUpdateListener, View.OnClickListener {

    private static final String TAG = SelectedContactFragment.class.getSimpleName();

    private TextView contactNameTextView;
    private TextView contactEmailTextView;
    private TextView contactTelephoneTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.selected_contact_fragment, container, false);

        Button saveActivityButton = (Button) view.findViewById(R.id.save_to_contacts_button);
        saveActivityButton.setOnClickListener(this);

        contactNameTextView = (TextView) view.findViewById(R.id.contact_name_textView);
        contactEmailTextView = (TextView) view.findViewById(R.id.contact_email_textView);
        contactTelephoneTextView = (TextView) view.findViewById(R.id.contact_telephone_textView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SelectedContactViewModel.INSTANCE.setModelUpdateListener(this);
        updateGui(SelectedContactViewModel.INSTANCE.getContact());
    }

    @Override
    public void onModelChanged() {
        updateGui(SelectedContactViewModel.INSTANCE.getContact());
    }


    private void updateGui(Contact contact) {
        if (contact != null) {
            Log.d(TAG, "Contact selected: " + contact.getName());
            contactNameTextView.setText(contact.getName());
            contactEmailTextView.setText(contact.getEmail());
            contactTelephoneTextView.setText(contact.getTelephone());
        }
    }

    @Override
    public void onClick(View view) {
        Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
        contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        Contact contact = SelectedContactViewModel.INSTANCE.getContact();

        contactIntent.putExtra(ContactsContract.Intents.Insert.NAME, contact.getName());
        contactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, contact.getEmail());
        contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, contact.getTelephone());

        startActivityForResult(contactIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getActivity(), "Added Contact", Toast.LENGTH_SHORT).show();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getActivity(), "Cancelled Added Contact", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
