package no.bouvet.androidskolen.nearbycontacts.adapers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import no.bouvet.androidskolen.nearbycontacts.R;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;


public class ContactLogAdapter<C> extends ArrayAdapter<Contact> {
    public ContactLogAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_log_listview_item, parent, false);
        }
        TextView timeStamp = (TextView) convertView.findViewById(R.id.log_list_view_date);
        TextView name = (TextView) convertView.findViewById(R.id.log_list_view_name);
        TextView email = (TextView) convertView.findViewById(R.id.log_list_view_email);
        TextView telephone = (TextView) convertView.findViewById(R.id.log_list_view_telephone);

        timeStamp.setText(contact.getDateDiscovered().toString());
        name.setText(contact.getName());
        email.setText(contact.getEmail());
        telephone.setText(contact.getTelephone());
        return convertView;
    }
}
