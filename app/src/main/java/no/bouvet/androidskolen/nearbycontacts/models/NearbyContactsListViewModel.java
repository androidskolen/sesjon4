package no.bouvet.androidskolen.nearbycontacts.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.bouvet.androidskolen.nearbycontacts.ContactDetectedListener;

public enum NearbyContactsListViewModel implements ContactDetectedListener {

    INSTANCE;

    private Map<String, Contact> detectedContacts = new HashMap<>();
    private ModelUpdateListener modelUpdateListener;

    public void setModelUpdateListener(ModelUpdateListener listener) {
        modelUpdateListener = listener;
    }

    public void removeModelUpdateListener(ModelUpdateListener listener) {
        if (modelUpdateListener == listener) {
            modelUpdateListener = null;
        }
    }

    public void onContactDetected(Contact contact) {
        detectedContacts.put(contact.getName(), contact);
        fireModelUpdated();
    }

    public void onContactLost(Contact contact) {
        detectedContacts.remove(contact.getName());
        fireModelUpdated();
    }

    public List<Contact> getNearbyContacts() {
        return new ArrayList<>(detectedContacts.values());
    }

    private void fireModelUpdated() {
        if (modelUpdateListener != null) {
            modelUpdateListener.onModelChanged();
        }
    }

    public void reset() {
        detectedContacts.clear();
    }
}
