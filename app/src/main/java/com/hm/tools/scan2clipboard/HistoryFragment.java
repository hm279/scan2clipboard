package com.hm.tools.scan2clipboard;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.handler.HistoryAsyncHandler;
import com.hm.tools.scan2clipboard.utils.Clipboard;

import java.util.ArrayList;
import java.util.LinkedList;

import static android.content.ContentValues.TAG;

/**
 * Created by hm on 15-5-30.
 */
public class HistoryFragment extends Fragment
        implements HistoryAsyncHandler.HistoryCompleteListener {

    RecyclerView recyclerView;
    HistoryRecyclerAdapter recyclerAdapter;
    RecyclerView.LayoutManager layoutManager;
    LinkedList<String> list = new LinkedList<>();
    LinkedList<Long> rowids = new LinkedList<>();
    HistoryAsyncHandler handler;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (handler == null) {
            handler = new HistoryAsyncHandler(context, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (handler == null) {
            handler = new HistoryAsyncHandler(activity, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_history);
        layoutManager = new LinearLayoutManager(getActivity());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rowids.add(0L);
        list.add("左滑复制，右滑删除");
        if (handler != null) {
            handler.startQuery();
        }
        Log.d("onView", "onView");
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
                    if (position > 0) {
                        Clipboard.setText(getActivity(), list.get(position));
                    }
                    recyclerAdapter.notifyItemChanged(position);
//                    recyclerAdapter.notifyDataSetChanged();
                } else {
                    String delete = "delete: " + list.get(position);
                    Toast.makeText(getActivity(), delete, Toast.LENGTH_SHORT).show();
                    if (position > 0) {
                        handler.startDelete(rowids.get(position));
                        list.remove(position);
                        rowids.remove(position);
                        recyclerAdapter.notifyItemRemoved(position);
                    } else {
                        recyclerAdapter.notifyItemChanged(0);
                    }
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
            rowids.add(0L);
            list.add("左滑复制，右滑删除");
            recyclerAdapter.notifyDataSetChanged();
            Toast.makeText(getActivity(), "Clear all records", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUpdateComplete(long id) {

    }

    @Override
    public void onInsertComplete(ArrayList<String> results, ArrayList<Long> ids) {
        list.addAll(1, results);
        rowids.addAll(1, ids);
        recyclerAdapter.notifyDataSetChanged();
        Log.d(TAG, "onInsertComplete: " + ids.size());
    }

    public void clearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("清空历史记录");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.startDelete(-1);
            }
        });
        builder.show();
    }

    public void insertResults(ArrayList<String> list) {
        handler.startInsert(list);
    }

}
