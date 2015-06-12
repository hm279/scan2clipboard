package com.hm.tools.scan2clipboard.Dialog;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hm.tools.scan2clipboard.R;

import java.util.ArrayList;

/**
 * Created by hm on 15-6-7.
 */
public class ListResultAdapter extends BaseAdapter{
    private ArrayList<String> list;
    private LayoutInflater inflater;

    public ListResultAdapter(Context context, ArrayList<String> result) {
        if (result != null) {
            list = result;
        } else {
            list = new ArrayList<>();
        }
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.textview_item, parent, false);
        } else {
            view = convertView;
        }
        TextView textView = (TextView) view.findViewById(R.id.text_item);
        textView.setText(list.get(position));
        textView.setAutoLinkMask(Linkify.WEB_URLS);
        return view;
    }
}
