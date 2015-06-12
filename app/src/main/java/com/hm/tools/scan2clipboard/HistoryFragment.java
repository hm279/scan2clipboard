package com.hm.tools.scan2clipboard;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.handler.HistoryAsyncHandler;
import com.hm.tools.scan2clipboard.utils.Clipboard;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-30.
 */
public class HistoryFragment extends Fragment
        implements HistoryAsyncHandler.HistoryCompleteListener {

    RecyclerView recyclerView;
    HistoryRecyclerAdapter recyclerAdapter;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<String> list;
    ArrayList<Long> rowids;
    HistoryAsyncHandler handler;
    ImageView imageView;
    int clickCount = 0;

    public HistoryFragment() {
        super();
        list = new ArrayList<>();
        rowids = new ArrayList<>();
        handler = new HistoryAsyncHandler(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_history);
        layoutManager = new LinearLayoutManager(getActivity());

//        view.setBackgroundColor(getResources().getColor(R.color.material_normal));

        view = inflater.inflate(R.layout.imageview_item, (ViewGroup)view, true);
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
                    }, 4000);
                } else if (clickCount == 1) {
                    clickCount = 0;
                    imageView.setImageResource(R.mipmap.ic_clear_all_white_48dp);
                    handler.startDelete(-1);
                }
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handler.startQuery();
    }

    private void setRecyclerAdapter() {
        recyclerView.setLayoutManager(layoutManager);
        recyclerAdapter = new HistoryRecyclerAdapter(list);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL_LIST));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder viewHolder1) {
                Log.d("onMove", "move");
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
                int position = viewHolder.getAdapterPosition();
                if (i == ItemTouchHelper.LEFT) {
                    String copy = "copy: " + list.get(position);
                    Toast.makeText(getActivity(), copy, Toast.LENGTH_SHORT).show();
                    Clipboard.setText(getActivity(), list.get(position));
//                    recyclerAdapter.notifyItemChanged(position);
                    recyclerAdapter.notifyDataSetChanged();
                } else {
                    String delete = "delete: " + list.get(position);
                    Toast.makeText(getActivity(), delete, Toast.LENGTH_SHORT).show();
                    handler.startDelete(rowids.get(position));
                    list.remove(position);
                    rowids.remove(position);
                    recyclerAdapter.notifyItemRemoved(position);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
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
            setRecyclerAdapter();
            cursor.close();
        }
    }

    @Override
    public void onDeleteComplete(int count) {
        if (count > 1) {
            //clear action
            rowids.clear();
            list.clear();
            recyclerAdapter.notifyDataSetChanged();
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
