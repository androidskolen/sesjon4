package no.bouvet.androidskolen.nearbycontacts;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import no.bouvet.androidskolen.nearbycontacts.adapers.ContactAdapter;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.ModelUpdateListener;
import no.bouvet.androidskolen.nearbycontacts.models.NearbyContactsListViewModel;

public class NearbyContactsFragment extends Fragment implements AdapterView.OnItemClickListener, ModelUpdateListener {


    private ListView listView;
    private ContactSelectedListener contactSelectedListener;
    private ContactAdapter<Contact> contactsArrayAdapter;

    @Override
    public void onModelChanged() {
        updateAdapterModel();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.nearby_contacts_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.nearby_contacts_listView);
        contactsArrayAdapter = new ContactAdapter(getContext());
        listView.setAdapter(contactsArrayAdapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            ContactSelectedListener listener = (ContactSelectedListener) context;
            contactSelectedListener = listener;

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement ContactSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        NearbyContactsListViewModel.INSTANCE.setModelUpdateListener(this);
        updateAdapterModel();
    }

    @Override
    public void onPause() {
        super.onPause();

        NearbyContactsListViewModel.INSTANCE.removeModelUpdateListener(this);
    }

    private void updateAdapterModel() {
        List<Contact> contactList = NearbyContactsListViewModel.INSTANCE.getNearbyContacts();

        if (contactsArrayAdapter != null) {
            contactsArrayAdapter.clear();
            contactsArrayAdapter.addAll(contactList);
            contactsArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Contact contact = contactsArrayAdapter.getItem(i);
        if (contactSelectedListener != null) {
            contactSelectedListener.onContactSelected(contact);
        }
    }
}
