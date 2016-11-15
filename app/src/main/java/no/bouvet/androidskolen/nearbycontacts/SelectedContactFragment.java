package no.bouvet.androidskolen.nearbycontacts;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.ModelUpdateListener;
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

        new SaveContactTask().execute();
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

    public class SaveContactTask extends AsyncTask<Void, Void, Integer> {

        private static final int SUCCESS = 0;
        private static final int FAILED = 1;

        @Override
        protected Integer doInBackground(Void... voids) {
            Contact contact = SelectedContactViewModel.INSTANCE.getContact();
            if (contact == null) return FAILED;

            String foundContactId = getExistingContactId(contact);
            ArrayList<ContentProviderOperation> ops = createContentProviderOperations(contact, foundContactId);

            try{
                // Executing all the insert operations as a single database transaction
                getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            }catch (RemoteException e) {
                e.printStackTrace();
                return FAILED;
            }catch (OperationApplicationException e) {
                e.printStackTrace();
                return FAILED;
            }

            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == SUCCESS) {
                Toast.makeText(getContext(), "Contact is successfully saved", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Failed to save contact", Toast.LENGTH_LONG).show();
            }
        }
    }

    private ArrayList<ContentProviderOperation> createContentProviderOperations(Contact contact, String foundContactId) {
        if (foundContactId != null) {
            return createUpdateOperations(contact, foundContactId);
        }
        return createInsertOperations(contact);
    }

    private ArrayList<ContentProviderOperation> createInsertOperations(Contact contact) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int rawContactId = 0;

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getTelephone())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, contact.getEmail())
                .build());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = contact.getPicture();
        if (bitmap != null) {    // If an image is selected successfully
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, stream);

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                    .build());

            try {
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ops;
    }

    private ArrayList<ContentProviderOperation> createUpdateOperations(Contact contact, String foundContactId) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?", new String[] {foundContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
                .build());

        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?", new String[] {foundContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getTelephone())
                .build());

        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?", new String[] {foundContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, contact.getEmail())
                .build());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = contact.getPicture();
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, stream);

            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?", new String[] {foundContactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE})
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                    .build());

            try {
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ops;
    }

    @Nullable
    private String getExistingContactId(Contact contact) {
        Uri contactUri = ContactsContract.Contacts.CONTENT_URI;

        String[] projection = {
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID
        };

        String selectionClause = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " = ?";
        String[] selectionArgs = {contact.getName()};

        String foundContactId = null;
        try (Cursor cursor = getContext().getContentResolver().query(contactUri, projection, selectionClause, selectionArgs, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                // Use first hit
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                foundContactId = cursor.getString(idIndex);
            }
        }
        return foundContactId;
    }


}
