package no.bouvet.androidskolen.nearbycontacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import no.bouvet.androidskolen.nearbycontacts.models.Contact;

public class Preferences {

    private final static String PREFERENCE_FILE = "OwnContactInfo";
    private final static String PREFERENCE_NAME = "OwnContactName";
    private final static String PREFERENCE_EMAIL = "OwnContactEmail";
    private final static String PREFERENCE_TELEPHONE = "OwnContactTelephone";
    private final static String PREFERENCES_PICTURE = "OwnContactPicture";

    public Contact createContactFromPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_FILE, 0);

        String name = preferences.getString(PREFERENCE_NAME, "");
        String email = preferences.getString(PREFERENCE_EMAIL, "");
        String telephone = preferences.getString(PREFERENCE_TELEPHONE, "");
        String picture = preferences.getString(PREFERENCES_PICTURE, "");

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(email) && TextUtils.isEmpty(telephone) && TextUtils.isEmpty(picture))
            return null;

        return new Contact(name, email, telephone, picture);
    }

    public void saveContactToPreferences(Contact contact, Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_FILE, 0);

        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(PREFERENCE_NAME, contact.getName());
        edit.putString(PREFERENCE_EMAIL, contact.getEmail());
        edit.putString(PREFERENCE_TELEPHONE, contact.getTelephone());
        edit.putString(PREFERENCES_PICTURE, contact.getEncodedPicture());

        edit.apply();
    }
}
