package com.hm.tools.scan2clipboard;

import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-30.
 */
public class HistoryFragment extends ListFragment
        implements HistoryAsyncQueryHandler.HistoryCompleteListener {
    ArrayList<String> list;
    ArrayList<Long> rowids;
    HistoryAsyncQueryHandler handler;
    HistoryAdapter adapter;
    ImageView imageView;
    int clickCount = 0;

    public HistoryFragment() {
        super();
        list = new ArrayList<>();
        rowids = new ArrayList<>();
        handler = new HistoryAsyncQueryHandler(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.setBackgroundColor(getResources().getColor(android.R.color.white));
            if (view instanceof FrameLayout) {
                inflater.inflate(R.layout.imageview_item, (ViewGroup)view, true);
                imageView = (ImageView) view.findViewById(R.id.imageView);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (clickCount == 0) {
                            clickCount++;
                            imageView.setImageResource(R.mipmap.ic_done_white_48dp);

                            Toast.makeText(getActivity(), "press again to clear!!!",
                                    Toast.LENGTH_LONG).show();
                            imageView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageResource(R.mipmap.ic_clear_all_white_48dp);
                                    clickCount = 0;
                                }
                            }, 5000);
                        } else if (clickCount == 1){
                            clickCount = 0;
                            imageView.setImageResource(R.mipmap.ic_clear_all_white_48dp);
                            handler.startDelete(-1);
                        }
                    }
                });
            }
        }
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No record");
        handler.startQuery();
        //TODO: should add swipe to remove instead of long click to remove
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                handler.startDelete(rowids.get(position));
                rowids.remove(position);
                list.remove(position);
                adapter.notifyDataSetChanged();
                return true;
            }
        });
    }



    @Override
    public void onQueryComplete(Cursor cursor) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(HistorySQLiteHelper.column_text);
                int rowidIndex = cursor.getColumnIndex("rowid");
                do {
                    list.add(cursor.getString(index));
                    rowids.add(cursor.getLong(rowidIndex));
                } while (cursor.moveToNext());
            }
            adapter = new HistoryAdapter(getActivity());
            adapter.updateAdapterData(list);
            setListAdapter(adapter);
            cursor.close();
        }
    }

    @Override
    public void onDeleteComplete(int count) {
        if (count > 1) {
            //clear action
            rowids.clear();
            list.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(getActivity(), "Clear all records", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUpdateComplete(long id) {

    }

    @Override
    public void onInsertComplete(long id) {

    }

}
