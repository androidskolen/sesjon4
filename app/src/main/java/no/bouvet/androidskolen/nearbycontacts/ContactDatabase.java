package no.bouvet.androidskolen.nearbycontacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import no.bouvet.androidskolen.nearbycontacts.models.Contact;

public class ContactDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "contacts";
    private static final String CONTACTS_TABLE_NAME = "contacts_log";

    private static final int DATABASE_VERSION = 2;

    private final static String TABLE_CREATE =
            "CREATE TABLE " + CONTACTS_TABLE_NAME +
                    "(ID integer primary key, " +
                    "time_stamp datetime default current_timestamp, " +
                    "name text, " +
                    "phone text, " +
                    "email text, " +
                    "photo text)";

    public ContactDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SQLiteDatabase mDatabase = db;
        mDatabase.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insertContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", contact.getName());
        contentValues.put("phone", contact.getTelephone());
        contentValues.put("email", contact.getEmail());
        contentValues.put("photo", contact.getEncodedPicture());
        db.insert(CONTACTS_TABLE_NAME, null, contentValues);
        return true;
    }

    public List<Contact> getLastEntryForAllContacts() {
        String query = "select c.name, c.time_stamp " +
                "from " + CONTACTS_TABLE_NAME + " c " +
                "inner join (" +
                "    select name, max(time_stamp) as MaxDate" +
                "    from "+ CONTACTS_TABLE_NAME +
                "    group by name" +
                ") cm on c.name = cm.name and c.time_stamp = cm.MaxDate " +
                " ORDER BY time_stamp desc";
        return executeQuery(query);
    }

    public List<Contact> getAllContactsEntries() {
        String query = "select * from " + CONTACTS_TABLE_NAME +
                " ORDER BY time_stamp desc";
        return executeQuery(query);
    }

    @NonNull
    private List<Contact> executeQuery(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Contact> contactHistory = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Contact contact = new Contact(
                        cursor.getString(cursor.getColumnIndex("name")), null, null, null, false);
                contact.setDateDiscovered(Timestamp.valueOf(cursor.getString(cursor.getColumnIndex("time_stamp"))));
                contactHistory.add(contact);
            }
        }
        return contactHistory;
    }
}


