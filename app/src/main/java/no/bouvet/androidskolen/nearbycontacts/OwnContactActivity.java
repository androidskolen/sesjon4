package no.bouvet.androidskolen.nearbycontacts;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import java.io.ByteArrayOutputStream;

import no.bouvet.androidskolen.nearbycontacts.models.Contact;
import no.bouvet.androidskolen.nearbycontacts.models.OwnContactViewModel;
import no.bouvet.androidskolen.nearbycontacts.services.NearbyService;

public class OwnContactActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private EditText userNameEditText;
    private EditText userEmailEditText;
    private EditText userTelephoneEditText;
    private ImageView userPicture;
    private Preferences preferences;
    NearbyService mService;
    boolean mBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_own_contact);

        Button takePictureButton = (Button) findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(this);

        Button removePictureButton = (Button) findViewById(R.id.remove_picture_button);
        removePictureButton.setOnClickListener(this);

        Switch publishSwitch = (Switch) findViewById(R.id.publish_toggle);
        publishSwitch.setOnCheckedChangeListener(this);

        userNameEditText = (EditText) findViewById(R.id.user_name_editText);
        userEmailEditText = (EditText) findViewById(R.id.user_email_editText);
        userTelephoneEditText = (EditText) findViewById(R.id.user_telephone_editText);
        userPicture = (ImageView) findViewById(R.id.user_picture_imageView);

        preferences = new Preferences();

        Intent intent = new Intent(this, NearbyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Contact contact = preferences.createContactFromPreferences(getApplicationContext());
        if (contact != null) {
            userNameEditText.setText(contact.getName());
            userEmailEditText.setText(contact.getEmail());
            userTelephoneEditText.setText(contact.getTelephone());
            if (contact.getPicture() != null) {
                userPicture.setImageBitmap(contact.getPicture());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        preferences.saveContactToPreferences(createContactFromInput(), getApplicationContext());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture_button:
                startImageCaptureActivityForResult();
                break;
            case R.id.remove_picture_button:
                removePicture();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.publish_toggle:
                if (isChecked) {
                    handlePublish();
                } else {
                    mService.unPublishContact();
                }
                break;
            case R.id.subscribe_toggle:
                if (isChecked) {
                    mService.subscribeToMessages();
                } else {
                    mService.unsubscribeToMessages();
                }
                break;
        }
    }

    private void handlePublish() {
        if (!isUserPictureSet()) {
            startNoPictureDialogFragment();
        } else {
            saveContact();
            mService.publishContact();
        }
    }

    private boolean isUserPictureSet() {
        return userPicture.getDrawable() != null &&
                ((BitmapDrawable) userPicture.getDrawable()).getBitmap() != null;
    }

    private void startNoPictureDialogFragment() {
        DialogFragment dialog = NoPictureDialogFragment.newInstance();
        dialog.show(getFragmentManager(), null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            removePicture();
            Bundle extras = data.getExtras();

            // Siden vi skal sende bilde i en kanal med begrenset plass bruker vi
            // en thumbnail. Et alternativ er å ha to varianter av bilde, et som man viser
            // og et som man sender. Men vi holder det enkelt.
            // Velger også å croppe bildet til kvadratisk form for at det skal se bedre ut.
            userPicture.setImageBitmap(ThumbnailUtils.extractThumbnail((Bitmap) extras.get("data"), 100, 100));
        }
    }

    private void removePicture() {
        userPicture.setImageBitmap(null);
        saveContact();
    }

    // Starter en Camera Activity for å ta bilde med resultatet returnert i et onActivityResult
    // callback.
    void startImageCaptureActivityForResult() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    void startNearbyActivity() {
        Intent intent = new Intent(this, NearbyActivity.class);
        startActivity(intent);
    }

    void saveContact() {
        Contact contact = createContactFromInput();
        preferences.saveContactToPreferences(contact, getApplicationContext());
        OwnContactViewModel.INSTANCE.setContact(contact);
    }

    private Contact createContactFromInput() {
        String name = userNameEditText.getText().toString();
        String email = userEmailEditText.getText().toString();
        String telephone = userTelephoneEditText.getText().toString();
        String picture = getEncodedPicture();
        return new Contact(name, email, telephone, picture);
    }

    private String getEncodedPicture() {
        if (isUserPictureSet()) {
            Bitmap bitmap = ((BitmapDrawable) userPicture.getDrawable()).getBitmap();
            return encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 10);
        } else {
            return "";
        }
    }

    private String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
        if (image != null) {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            image.compress(compressFormat, quality, byteArrayOS);
            return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
        } else {
            return "";
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            NearbyService.NearbyBinder binder = (NearbyService.NearbyBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.publishContact();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
