package no.bouvet.androidskolen.nearbycontacts.models;

public enum OwnContactViewModel {

    INSTANCE;

    private Contact contact;

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Contact getContact() {
        return contact;
    }

}
