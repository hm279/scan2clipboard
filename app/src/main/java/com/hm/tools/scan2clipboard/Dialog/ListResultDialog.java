package com.hm.tools.scan2clipboard.Dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.utils.Clipboard;

import java.util.ArrayList;

/**
 * Created by hm on 15-6-7.
 */
public class ListResultDialog extends DialogFragment{
    public static String key = "results";
    private ArrayList<String> results;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle bundle = getArguments();
        ArrayList<String> list = bundle.getStringArrayList(key);
        if (list != null) {
            ListResultAdapter adapter = new ListResultAdapter(getActivity(), list);
            builder.setAdapter(adapter, listener);
            results = list;
            Log.d(key, "size" + results.size());
        } else {
            builder.setMessage("No result");
        }
        builder.setTitle("decode result");
        return builder.create();
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Clipboard.setText(getActivity(), results.get(which));
            Toast.makeText(getActivity(), "set to clipboard", Toast.LENGTH_LONG).show();
        }
    };
}
