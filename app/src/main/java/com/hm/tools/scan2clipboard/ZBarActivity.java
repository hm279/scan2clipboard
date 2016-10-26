package com.hm.tools.scan2clipboard;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.utils.Decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by hm on 15-3-16.
 */
public class ZBarActivity extends AppCompatActivity {
    private static final String TAG = "ZBar";
    private Camera mCamera;
    private Decoder decoder;

    /** Three times normal decode, once reverse decode  */
    private int interval = 0;

    private Handler autoFocusHandler;
    boolean previewing = false;
    boolean autoFocus;
    private ImageButton flash;

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

            flash = (ImageButton) findViewById(R.id.flash);
            flash.setOnClickListener(flashListener);

            decoder = Decoder.getDefaultDecoder();
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
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCameraAndPreview();
        flash.removeCallbacks(timeout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            safeCameraOpen(0);
        }
        flash.postDelayed(timeout, 60000);
        Log.d(TAG, "onResume");
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
    }

    private void startCameraAndPreview() {
        if (!previewing) {
            previewing = true;
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            if (autoFocus) {
                mCamera.autoFocus(autoFocusCallback);
            }
        }
        Log.d(TAG, "startCameraAndPreview");
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

    private boolean flashToggle() {
        boolean canFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        boolean on = false;
        if (canFlash) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (!Camera.Parameters.FLASH_MODE_TORCH.equals(parameters.getFlashMode())) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                on = true;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                on = false;
            }
            mCamera.setParameters(parameters);
        }
        return on;
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
            Log.d(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(90);
            startCameraAndPreview();
            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            startCameraAndPreview();
            return true;
        }
    };

    private View.OnClickListener flashListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (flashToggle()) {
                flash.setColorFilter(getResources().getColor(R.color.material_normal));
            } else {
                flash.setColorFilter(getResources().getColor(android.R.color.white));
            }
        }
    };

    private void setResult(ArrayList<String> resultList) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra("results", resultList);
        setResult(RESULT_OK, intent);
        finish();
    }

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ZBarActivity.this, "超时退出", Toast.LENGTH_SHORT).show();
            finish();
        }
    };
}
