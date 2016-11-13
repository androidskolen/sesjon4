package no.bouvet.androidskolen.nearbycontacts.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.bouvet.androidskolen.nearbycontacts.ContactDetectedListener;

public enum ContactLogListViewModel implements ContactDetectedListener {

    INSTANCE;

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
        fireModelUpdated();
    }

    public void onContactLost(Contact contact) {
        fireModelUpdated();
    }

    private void fireModelUpdated() {
        if (modelUpdateListener != null) {
            modelUpdateListener.onModelChanged();
        }
    }
}
