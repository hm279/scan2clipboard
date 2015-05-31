package com.hm.tools.scan2clipboard;

import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

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

    public HistoryFragment() {
        super();
        list = new ArrayList<>();
        rowids = new ArrayList<>();
        handler = new HistoryAsyncQueryHandler(getActivity(), this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No record");
        handler.startQuery();
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
            cursor.moveToNext();
            int index = cursor.getColumnIndex(HistorySQLiteHelper.column_text);
            int rowidIndex = cursor.getColumnIndex("rowid");
            do {
                list.add(cursor.getString(index));
                rowids.add(cursor.getLong(rowidIndex));
            } while (cursor.moveToNext());
            adapter = new HistoryAdapter(getActivity());
            adapter.updateAdapterData(list);
            setListAdapter(adapter);

            cursor.close();
        }
    }

    @Override
    public void onDeleteComplete(long id) {

    }

    @Override
    public void onUpdateComplete(long id) {

    }

    @Override
    public void onInsertComplete(long id) {

    }

}
