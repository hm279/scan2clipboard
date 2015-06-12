package com.hm.tools.scan2clipboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.Dialog.ListResultDialog;
import com.hm.tools.scan2clipboard.handler.AsyncDecodeHandler;
import com.hm.tools.scan2clipboard.handler.HistoryAsyncHandler;
import com.hm.tools.scan2clipboard.utils.Clipboard;
import com.hm.tools.scan2clipboard.utils.Decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by hm on 15-3-16.
 */
public class ZBarActivity extends Activity
        implements FileListFragment.FileSelectedListener, AsyncDecodeHandler.DecodeCompleteListener{
    private static final String INTENT = "com.hm.tools.scan2clipboard";
    private static final String SHORTCUT = "shortcut";
    private static final String TAG = "ZBar";
    private Camera mCamera;
    private Decoder decoder;
//    private HistorySQLiteHelper helper;
    private HistoryAsyncHandler asyncQueryHandler;

    /** Three times normal decode, once reverse decode  */
    private int interval = 0;

    private Handler autoFocusHandler;
    boolean previewing = false;
    boolean autoFocus;
    boolean shortcut;
    boolean newIntent = false;
    boolean isSetting = false;
    boolean single;
    private Record record = null;
    private TextView textView;
    private ImageButton setting;
    private CheckBox checkBox_shortcut;

    FileListFragment fileListFragment = null;
    boolean canDismiss = false;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (checkCameraHardware() && safeCameraOpen(0)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            autoFocus = checkCameraAutoFocus();
            SurfaceView mPreview = (SurfaceView) findViewById(R.id.camera_preview);
            mPreview.getHolder().addCallback(holderCallback);
            autoFocusHandler = new Handler();

            mPreview.setOnTouchListener(touchListener);

            checkBox_shortcut = (CheckBox) findViewById(R.id.check_notification);
            checkBox_shortcut.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        setNotification();
                    } else {
                        cancelNotification();
                    }
                }
            });
            textView = (TextView) findViewById(R.id.textView);
            setting = (ImageButton) findViewById(R.id.setting);
            setting.setOnClickListener(settingListener);
            ImageButton add = (ImageButton) findViewById(R.id.add);
            add.setOnClickListener(addListener);
            ImageButton history = (ImageButton) findViewById(R.id.history);
            history.setOnClickListener(historyListener);

            getSetting();
            if (shortcut) {
                checkBox_shortcut.setChecked(true);
            }
            decoder = new Decoder();
            asyncQueryHandler = new HistoryAsyncHandler(this, null);

            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            if (action.equals(INTENT)) {
                newIntent = true;
            } else if (type != null && type.startsWith("image/")) {
                if (Intent.ACTION_SEND.equals(action)) {
                    handleIntentSingleImg(intent);
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    handleIntentMultipleImg(intent);
                }
            }
        } else {
            Toast.makeText(this, "failed to open Camera", Toast.LENGTH_LONG).show();
            finish();
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .penaltyDeath()
                    .penaltyLog()
                    .build());
            Log.d(TAG, "onCreate");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
        if (shortcut) {
            setNotification();
        }
        saveSetting(shortcut);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (previewing) {
            mCamera.stopPreview();
        }
    }

    /**
    @Override
    protected void onStart() {
        super.onStart();
        //auto start
        if (previewing && !isSurfaceViewDestroyed) {
            mCamera.startPreview();
        }
//        Log.d(TAG, "onStart");
    }
     */

    @Override
    public void onBackPressed() {
        if (canDismiss) {
            if (fileListFragment != null) {
                if (!fileListFragment.onBackPressed()) {
                    dismissFragment();
                    fileListFragment = null;
                }
            } else {
                dismissFragment();
            }
            return;
        }
        super.onBackPressed();
    }

    public void setNotification() {
        Intent intent = new Intent(this, ZBarActivity.class);
        intent.setAction(INTENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Scan barcode to clipboard")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, notification);
    }

    public void cancelNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(0);
    }


    private boolean safeCameraOpen(int id) {
        boolean bOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            bOpened = (mCamera != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bOpened) {
            Log.e(TAG, "failed to open Camera");
//            Log.d(TAG, "Numbers " + Camera.getNumberOfCameras());
        }
        return bOpened;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
//        Log.d(TAG, "releaseCameraAndPreview");
    }

    private boolean checkCameraHardware() {
        PackageManager manager = getPackageManager();
        if (manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true;
        }
//        Log.d(TAG, "No Camera Found");
        return false;
    }

    private boolean checkCameraAutoFocus() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> modes = parameters.getSupportedFocusModes();
        if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ||
                modes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            return true;
        }
        return false;
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            interval++;
            if (interval > 3) {
                Decoder.reverseImageData(data);
                interval = 0;
            }
            ArrayList<String> result = decoder.decode(size.width, size.height, data);
            if (result != null) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                setResult(result);
                //record result
                asyncQueryHandler.startInsert(result);
            }
        }
    };

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.post(doAutoFocus);
//            Log.d(TAG, "auto focus");
        }
    };

    private Runnable doAutoFocus = new Runnable() {
        @Override
        public void run() {
            if (previewing) {
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };

    SurfaceHolder.Callback holderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(previewCallback);
//            mCamera.startPreview();
//            if (autoFocus) {
//                mCamera.autoFocus(autoFocusCallback);
//            }
            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!previewing) {
                previewing = true;
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startPreview();
                if (autoFocus) {
                    mCamera.autoFocus(autoFocusCallback);
                }
                textView.setVisibility(View.INVISIBLE);
                Log.d(TAG, "onTouch");
            }
            if (isSetting) {
                //dismiss checkbox without save them;
                dismissCheckBox();
            }
            return true;
        }
    };

    View.OnClickListener settingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isSetting) {
                // save setting;
                dismissCheckBox();
                shortcut = checkBox_shortcut.isChecked();
            } else {
                Drawable drawable = getResources()
                        .getDrawable(R.mipmap.ic_done_white_48dp);
                setting.setImageDrawable(drawable);
                checkBox_shortcut.setVisibility(View.VISIBLE);
                Animation in = AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.abc_grow_fade_in_from_bottom);
                setting.startAnimation(in);
                checkBox_shortcut.startAnimation(in);
                isSetting = true;
            }
        }
    };

    @Override
    public void onFileSelectedListener(File file) {
//        Log.d("onFileSelectedListener", file.getAbsolutePath());
        dismissFragment();
        //image file convert to byte[] data
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            Toast.makeText(this, "selected file maybe not an image", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> result = decoder.decode(bitmap);
        if (result == null) {
            Toast.makeText(this, "none code", Toast.LENGTH_LONG).show();
        } else {
            setResult(result);
            //record result
            asyncQueryHandler.startInsert(result);
            Toast.makeText(this, "has code, showing in top", Toast.LENGTH_LONG).show();
        }
    }

    View.OnClickListener addListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (previewing) {
                mCamera.stopPreview();
                previewing = false;
            }
            fileListFragment = new FileListFragment();
            showFragment(fileListFragment);
        }
    };

    private View.OnClickListener historyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (previewing) {
                mCamera.stopPreview();
                previewing = false;
            }
            HistoryFragment fragment = new HistoryFragment();
            showFragment(fragment);
        }
    };

    private void setResult(ArrayList<String> resultList) {
        //record result
//        asyncQueryHandler.startInsert(resultList);

        if (resultList.size() == 1){
            Clipboard.setText(this, resultList.get(0));
            if (newIntent) {
                finish();
            } else {
                textView.setText(resultList.get(0));
                Animation in = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_top);
                textView.setVisibility(View.VISIBLE);
                textView.startAnimation(in);
            }
        } else {
            showMultipleResults(resultList);
        }
    }

    private void saveSetting(boolean shortcut) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(SHORTCUT, false) == shortcut) {
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SHORTCUT, shortcut);
        editor.apply();
    }

    private void getSetting() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        shortcut = sharedPreferences.getBoolean(SHORTCUT, false);
    }

    private void showFragment(Fragment fragment) {
        if (canDismiss) {
            return;
        }
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction()
                .add(R.id.camera_frame, fragment, TAG)
                .setTransition(FragmentTransaction.TRANSIT_ENTER_MASK) //TODO:setTransition
                .commit();
        canDismiss = true;
    }

    private void dismissFragment() {
        FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentByTag(TAG);
        if (fragment != null) {
            manager.beginTransaction()
                    .remove(fragment)
                    .setTransition(FragmentTransaction.TRANSIT_EXIT_MASK)
                    .commit();
            canDismiss = false;
        }
    }

    private void dismissCheckBox() {
        Drawable drawable = getResources()
                .getDrawable(R.mipmap.ic_settings_white_48dp);
        setting.setImageDrawable(drawable);
        Animation in = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.abc_grow_fade_in_from_bottom);
        setting.startAnimation(in);

        checkBox_shortcut.setVisibility(View.INVISIBLE);
        Animation out = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.abc_shrink_fade_out_from_bottom);
        checkBox_shortcut.startAnimation(out);
        isSetting = false;
    }

    private void showMultipleResults(ArrayList<String> list) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ListResultDialog.key, list);
        ListResultDialog dialog = new ListResultDialog();
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), null);
    }

    private void handleIntentSingleImg(Intent intent) {
        Uri imgUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imgUri != null) {
            single = true;
            AsyncDecodeHandler handler = new AsyncDecodeHandler(this, decoder, this);
            handler.decodeBitmap(imgUri);
        }
    }

    private void singleDecodeComplete(ArrayList<String> result, int error) {
        if (error == Decoder.ERROR_NO_RESULT) {
            Toast.makeText(this, "none code", Toast.LENGTH_LONG).show();
        } else if (error == Decoder.ERROR_NO_ERROR){
            //in the async thread the result had been recorded.
            setResult(result);
            Toast.makeText(this, "has code, showing in top", Toast.LENGTH_LONG).show();
        } else if (error == Decoder.ERROR_NO_BITMAP){
            Toast.makeText(this, "input data not found", Toast.LENGTH_LONG).show();
        }
    }

    private void handleIntentMultipleImg(Intent intent) {
        if (record != null) {
            Toast.makeText(this, "running decoding, please wait until finish", Toast.LENGTH_LONG).show();
            return;
        }
        single = false;
        ArrayList<Uri> uriArrayList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        record = new Record(uriArrayList.size());

        AsyncDecodeHandler handler = new AsyncDecodeHandler(this, decoder, this);
        for (Uri uri : uriArrayList) {
            handler.decodeBitmap(uri);
        }
    }

    @Override
    public void onDecodeComplete(ArrayList<String> result, int error) {
        if (single) {
            singleDecodeComplete(result, error);
            return;
        }
        record.count++;
        if (error == Decoder.ERROR_NO_ERROR) {
            record.success++;
            record.results.addAll(result);
        } else if (error == Decoder.ERROR_NO_RESULT){
            record.fail++;
        } else if (error == Decoder.ERROR_NO_BITMAP) {
            record.miss++;
        }
        textView.setText(record.total + "/" + record.count);
        if (record.count == record.total) {
            //in the async thread the result had been recorded.
            setResult(record.results);
            String str = "total results: " + record.results.size()
                    + "\n has code images: " + record.success
                    + "\n no code images: " + record.fail
                    + "\n missing images: " + record.miss;
            Log.d("results", str);
            record = null;
        }
    }

    private class Record {
        ArrayList<String> results;
        int success = 0;
        int miss = 0;
        int fail = 0;
        int count = 0;
        int total;

        public Record(int total) {
            this.total = total;
            results = new ArrayList<>();
        }
    }

}
