package no.bouvet.androidskolen.nearbycontacts;

import no.bouvet.androidskolen.nearbycontacts.models.Contact;

public interface ContactDetectedListener {

    void onContactDetected(Contact contact);

    void onContactLost(Contact contact);

}
