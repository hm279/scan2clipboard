package com.hm.tools.scan2clipboard.handler;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;

import com.hm.tools.scan2clipboard.HistorySQLiteHelper;
import com.hm.tools.scan2clipboard.utils.Decoder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by hm on 15-6-6.
 */
public class AsyncDecodeHandler extends Handler{
    private static final int EVENT_DECODE_BITMAP = 1;

    private static Looper sLooper = null;
    private WorkerHandler mWorkerHandler;
    private HistorySQLiteHelper helper;
    private WeakReference<DecodeCompleteListener> listenerWeakReference;
    private Decoder decoder;
    private Context context;

    protected static final class WorkerArgs {
        public Handler handler;
        Uri uri;
        ArrayList<String> result;
        int error;
    }

    public AsyncDecodeHandler(Context context, Decoder decoder, DecodeCompleteListener listener) {
        super();
        this.context = context.getApplicationContext();
        this.decoder = decoder;
        this.helper = HistorySQLiteHelper.getInstance(context);
        listenerWeakReference = new WeakReference<>(listener);
        synchronized (this) {
            if (sLooper == null) {
                HandlerThread thread = new HandlerThread("AsyncDecodeWorker");
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
                case EVENT_DECODE_BITMAP:
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                context.getContentResolver(), args.uri);
                        args.result = decoder.decode(bitmap);
                        if (args.result != null) {
                            helper.insert(args.result);
                            args.error = Decoder.ERROR_NO_ERROR;
                        } else {
                            args.error = Decoder.ERROR_NO_RESULT;
                        }
                    } catch (IOException e) {
                        args.error = Decoder.ERROR_NO_BITMAP;
                    }
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
        DecodeCompleteListener listener = listenerWeakReference.get();
        if (listener == null) {
            return;
        }
        WorkerArgs args = (WorkerArgs) msg.obj;
        switch (msg.what) {
            case EVENT_DECODE_BITMAP:
                listener.onDecodeComplete(args.result, args.error);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public void decodeBitmap(Uri uri) {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.uri = uri;
        Message message = mWorkerHandler.obtainMessage(EVENT_DECODE_BITMAP);
        message.obj = args;
        message.sendToTarget();
    }

    public interface DecodeCompleteListener {
        void onDecodeComplete(ArrayList<String> result, int error);
    }

}
