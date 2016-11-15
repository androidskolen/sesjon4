package no.bouvet.androidskolen.nearbycontacts;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class NoPictureDialogFragment extends DialogFragment {

    public static NoPictureDialogFragment newInstance() {
        return new NoPictureDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_picture_dialog_message)
                .setTitle(R.string.no_picture_dialog_title)
                .setPositiveButton(R.string.take_picture, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((OwnContactActivity)getActivity()).startImageCaptureActivityForResult();
                            }
                        }
                )
                .setNegativeButton(R.string.nope, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((OwnContactActivity)getActivity()).startNearbyActivity();
                    }
                });

        return builder.create();
    }
}
