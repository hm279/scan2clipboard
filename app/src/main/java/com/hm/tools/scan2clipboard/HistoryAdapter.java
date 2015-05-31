package com.hm.tools.scan2clipboard;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-30.
 */
public class HistoryAdapter extends BaseAdapter{
    private ArrayList<String> list;
    private LayoutInflater inflater;

    public HistoryAdapter(Context context) {
        list = new ArrayList<>();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if (list.size() > position) {
            return list.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.textview_item, parent, false);
        }
        TextView textView = (TextView) view.findViewById(R.id.text_item);
        textView.setAutoLinkMask(Linkify.WEB_URLS);
        textView.setText(list.get(position));
        return view;
    }

    public void updateAdapterData(ArrayList<String> list) {
        this.list = list;
        notifyDataSetChanged();
    }

}
