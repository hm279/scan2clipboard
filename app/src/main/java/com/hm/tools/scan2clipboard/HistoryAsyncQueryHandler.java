package com.hm.tools.scan2clipboard;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * Created by hm on 15-5-30.
 */
public class HistoryAsyncQueryHandler extends Handler {
    private static final int EVENT_MY_QUERY = 1;
    private static final int EVENT_MY_INSERT = 2;
    private static final int EVENT_MY_UPDATE = 3;
    private static final int EVENT_MY_DELETE = 4;

    private static Looper sLooper = null;
    private WorkerHandler mWorkerHandler;
    private HistorySQLiteHelper helper;
    private WeakReference<HistoryCompleteListener> weakReference;

    protected static final class WorkerArgs {
        Handler handler;
        Cursor cursor;
        long rowid;
        String text;
        ArrayList<String> results;
    }

    public HistoryAsyncQueryHandler(Context context, HistoryCompleteListener listener) {
        super();
        helper = HistorySQLiteHelper.getInstance(context);
        weakReference = new WeakReference<>(listener);
        synchronized (HistoryAsyncQueryHandler.class) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("HistoryAsyncQueryWorker");
                thread.start();

                sLooper = thread.getLooper();
            }
        }
        mWorkerHandler = new WorkerHandler(sLooper);
    }

    private class WorkerHandler extends Handler{
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            switch (msg.what) {
                case EVENT_MY_QUERY:
                    //do query
                    args.cursor = helper.query();
                    break;
                case EVENT_MY_INSERT:
                    ArrayList<Long> ids = helper.insert(args.results);
                    args.rowid = ids.get(0);
                    break;
                case EVENT_MY_UPDATE:
                    args.rowid = helper.update(args.rowid, args.text);
                    break;
                case EVENT_MY_DELETE:
                    args.rowid = helper.delete(args.rowid);
                    break;
                default:
                    return;
            }
            Message message = args.handler.obtainMessage(msg.what);
            message.obj = args;
            message.sendToTarget();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        HistoryCompleteListener listener = weakReference.get();
        if (listener == null) {
            return;
        }
        WorkerArgs args = (WorkerArgs) msg.obj;
        switch (msg.what) {
            case EVENT_MY_QUERY:
                //finish query
                listener.onQueryComplete(args.cursor);
                break;
            case EVENT_MY_INSERT:
                listener.onInsertComplete(args.rowid);
                break;
            case EVENT_MY_UPDATE:
                listener.onUpdateComplete(args.rowid);
                break;
            case EVENT_MY_DELETE:
                listener.onDeleteComplete((int) args.rowid);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public void startQuery() {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        Message message = mWorkerHandler.obtainMessage(EVENT_MY_QUERY);
        message.obj = args;
        message.sendToTarget();
    }

    public void startInsert(ArrayList<String> results) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.results = results;
        Message message = mWorkerHandler.obtainMessage(EVENT_MY_INSERT);
        message.obj = args;
        message.sendToTarget();

    }

    public void startUpdate(long id, String text) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.rowid = id;
        args.text = text;
        Message message = mWorkerHandler.obtainMessage(EVENT_MY_UPDATE);
        message.obj = args;
        message.sendToTarget();
    }

    public void startDelete(long id) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.rowid = id;
        Message message = mWorkerHandler.obtainMessage(EVENT_MY_DELETE);
        message.obj = args;
        message.sendToTarget();
    }

    interface HistoryCompleteListener {
        void onQueryComplete(Cursor cursor);
        void onInsertComplete(long id);
        void onUpdateComplete(long id);
        void onDeleteComplete(int count);
    }

}
