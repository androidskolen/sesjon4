package no.bouvet.androidskolen.nearbycontacts.models;

public enum SelectedContactViewModel {

    INSTANCE;

    private Contact selectedContact;
    private ModelUpdateListener modelUpdateListener;

    SelectedContactViewModel() {
    }

    public void setModelUpdateListener(ModelUpdateListener listener) {
        modelUpdateListener = listener;
    }

    public void removeModelUpdateListener(ModelUpdateListener listener) {
        if (modelUpdateListener == listener) {
            modelUpdateListener = null;
        }
    }

    public void setSelectedContact(Contact contact) {
        selectedContact = contact;
        fireModelUpdated();
    }

    public Contact getContact() {
        return selectedContact;
    }


    private void fireModelUpdated() {
        if (modelUpdateListener != null) {
            modelUpdateListener.onModelChanged();
        }
    }

    public void reset() {
        selectedContact = null;
    }
}
