package no.bouvet.androidskolen.nearbycontacts.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Date;


public class Contact {

    private static final Gson gson = new Gson();

    private final String name;
    private final String email;
    private final String telephone;
    private String base64EncodedPicture;
    private Date dateDiscovered;
    private boolean publish;

    public Contact(String name, String email, String telephone, String picture, boolean publish) {
        this.name = name;
        this.email = email;
        this.telephone = telephone;
        this.base64EncodedPicture = picture;
        this.publish = publish;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTelephone() {
        return telephone;
    }

    public Bitmap getPicture() {
        if (this.base64EncodedPicture != null) {
            return decodeBase64(this.base64EncodedPicture);
        } else {
            return null;
        }
    }

    public Date getDateDiscovered() {
        return dateDiscovered;
    }

    public void setDateDiscovered(Date dateDiscovered) {
        this.dateDiscovered = dateDiscovered;
    }

    public String getEncodedPicture() {
        return base64EncodedPicture;
    }

    public void setPicture(String picture) {
        this.base64EncodedPicture = "";
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Contact fromJson(String json) {
        return gson.fromJson(json, Contact.class);
    }

    public static Bitmap decodeBase64(String input) {
        Bitmap image = null;
        try {
            byte[] decodedBytes = Base64.decode(input, 0);
            image = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e("ANDROIDSKOLEN", "Kunne ikke dekode bilde");
        }

        return image;

    }
}
