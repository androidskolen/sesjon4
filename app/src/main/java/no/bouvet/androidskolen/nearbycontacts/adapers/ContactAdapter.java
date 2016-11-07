package no.bouvet.androidskolen.nearbycontacts.adapers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import no.bouvet.androidskolen.nearbycontacts.R;
import no.bouvet.androidskolen.nearbycontacts.models.Contact;


public class ContactAdapter<C> extends ArrayAdapter<Contact> {
    public ContactAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {

        Contact contact = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.nearby_contacts_listview_item, parent, false);
        }
        TextView name = (TextView) convertView.findViewById(R.id.list_view_name);
        TextView email = (TextView) convertView.findViewById(R.id.list_view_email);
        TextView telephone = (TextView) convertView.findViewById(R.id.list_view_telephone);
        ImageView picture = (ImageView) convertView.findViewById(R.id.list_view_picture);

        name.setText(contact.getName());
        email.setText(contact.getEmail());
        telephone.setText(contact.getTelephone());
        picture.setImageBitmap(contact.getPicture());
        return convertView;
    }
}
