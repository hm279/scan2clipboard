package com.hm.tools.scan2clipboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
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

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by hm on 15-3-16.
 */
public class ZBarActivity extends Activity implements FileListFragment.FileSelectedListener {
    private static final String INTENT = "com.hm.tools.scan2clipboard";
    private static final String REVERSE = "reverse";
    private static final String SHORTCUT = "shortcut";
    private static final String TAG = "ZBar";
    private Camera mCamera;
    private ImageScanner scanner;

//    private SurfaceView mPreview;
    private Handler autoFocusHandler;
    boolean previewing = true;
    boolean isSurfaceViewDestroyed = true;
    boolean autoFocus;
    boolean reverse;
    boolean shortcut;
    boolean newIntent = false;
    boolean isSetting = false;

    private TextView textView;
    private ImageButton setting;
//    private ImageButton add;
    private CheckBox checkBox_reverse;
    private CheckBox checkBox_shortcut;

    FileListFragment fileListFragment = null;

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
            scanner = new ImageScanner();
            scanner.setConfig(0, Config.X_DENSITY, 3);
            scanner.setConfig(0, Config.Y_DENSITY, 3);

            mPreview.setOnTouchListener(touchListener);

            checkBox_reverse = (CheckBox) findViewById(R.id.check_reverse);
            checkBox_reverse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    reverse = isChecked;
                }
            });
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

            getSetting();
            if (reverse) {
                checkBox_reverse.setChecked(true);
            }
            if (shortcut) {
                checkBox_shortcut.setChecked(true);
            }

            Intent intent = getIntent();
            if (intent.getAction().equals(INTENT)) {
                newIntent = true;
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
        saveSetting(reverse, shortcut);
//        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (previewing) {
            mCamera.stopPreview();
        }
//        Log.d(TAG, "onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (previewing && !isSurfaceViewDestroyed) {
            mCamera.startPreview();
        }
//        Log.d(TAG, "onStart");
    }

    @Override
    public void onBackPressed() {
        if (fileListFragment != null) {
            if (!fileListFragment.onBackPressed()) {
                dismissFileListFragment();
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

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            ArrayList<String> result = decode(size.width, size.height, data);
            if (result != null) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                setResult(result);
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
            isSurfaceViewDestroyed = false;
//            Log.d(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            if (autoFocus) {
                mCamera.autoFocus(autoFocusCallback);
            }
//            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            isSurfaceViewDestroyed = true;
//            Log.d(TAG, "surfaceDestroy");
        }
    };

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

    private ArrayList<String> decode(int w, int h, byte[] data) {
//        long s,e;
//        s = System.currentTimeMillis();
        if (reverse) {
            reverseImageData(data);
        }
        Image image = new Image(w, h, "Y800");
        image.setData(data);
        int result = scanner.scanImage(image);
        if (result != 0) {
            SymbolSet symbolSet = scanner.getResults();
            ArrayList<String> list = new ArrayList<>();
            for (Symbol symbol : symbolSet) {
                list.add(symbol.getData());
//                Log.v(TAG, symbol.getData());
            }
//            e = System.currentTimeMillis();
//            Log.v(TAG, "decode time is " + (e - s));
            return list;
        }
        return null;
    }

    private void setResult(ArrayList<String> resultList) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText(getString(R.string.app_name), resultList.get(0));
        manager.setPrimaryClip(data);
        if (newIntent) {
            finish();
        } else {
            textView.setText(resultList.get(0));
            Animation in = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_top);
            textView.setVisibility(View.VISIBLE);
            textView.startAnimation(in);
        }
    }

    private void reverseImageData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
//            data[i] = (byte) (~data[i] & 0xff);
            data[i] = (byte) (255 - data[i]);
        }
    }

    private void saveSetting(boolean reverse, boolean shortcut) {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(REVERSE, false) == reverse &&
                sharedPreferences.getBoolean(SHORTCUT, false) == shortcut) {
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(REVERSE, reverse);
        editor.putBoolean(SHORTCUT, shortcut);
        editor.apply();
    }

    private void getSetting() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        reverse = sharedPreferences.getBoolean(REVERSE, false);
        shortcut = sharedPreferences.getBoolean(SHORTCUT, false);
    }

    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!previewing) {
                previewing = true;
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startPreview();
                textView.setVisibility(View.INVISIBLE);
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
                reverse = checkBox_reverse.isChecked();
                shortcut = checkBox_shortcut.isChecked();
            } else {
                Drawable drawable = getResources()
                        .getDrawable(R.mipmap.ic_done_white_48dp);
                setting.setImageDrawable(drawable);
                checkBox_reverse.setVisibility(View.VISIBLE);
                checkBox_shortcut.setVisibility(View.VISIBLE);
                Animation in = AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.abc_grow_fade_in_from_bottom);
                setting.startAnimation(in);
                checkBox_reverse.startAnimation(in);
                checkBox_shortcut.startAnimation(in);
                isSetting = true;
            }
        }
    };

    private void dismissCheckBox() {
        Drawable drawable = getResources()
                .getDrawable(R.mipmap.ic_settings_white_48dp);
        setting.setImageDrawable(drawable);
        Animation in = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.abc_grow_fade_in_from_bottom);
        setting.startAnimation(in);

        checkBox_reverse.setVisibility(View.INVISIBLE);
        checkBox_shortcut.setVisibility(View.INVISIBLE);
        Animation out = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.abc_shrink_fade_out_from_bottom);
        checkBox_reverse.startAnimation(out);
        checkBox_shortcut.startAnimation(out);
        isSetting = false;
    }

    @Override
    public void onFileSelectedListener(File file) {
//        Log.d("onFileSelectedListener", file.getAbsolutePath());
        dismissFileListFragment();
        //image file convert to byte[] data
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (null != bitmap) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            Image data = new Image(w, h, "RGB4");
            data.setData(pixels);
            int result = scanner.scanImage(data.convert("Y800"));
            if (result != 0) {
                SymbolSet symbolSet = scanner.getResults();
                ArrayList<String> list = new ArrayList<>();
                for (Symbol symbol : symbolSet) {
                    list.add(symbol.getData());
                }
                setResult(list);
                return;
            }
        }
        Toast.makeText(this, "failed to decode", Toast.LENGTH_LONG).show();
    }

    View.OnClickListener addListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (previewing) {
                mCamera.stopPreview();
                previewing = false;
            }
            fileListFragment = new FileListFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.camera_frame, fileListFragment, "file")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    };

    private void dismissFileListFragment() {
        if (fileListFragment != null) {
            FragmentManager manager = getFragmentManager();
            manager.beginTransaction()
                    .remove(fileListFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
            fileListFragment = null;
        }
    }

}
