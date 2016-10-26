package com.hm.tools.scan2clipboard;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.hm.tools.scan2clipboard.Dialog.ListResultDialog;
import com.hm.tools.scan2clipboard.handler.AsyncDecodeHandler;
import com.hm.tools.scan2clipboard.utils.Clipboard;
import com.hm.tools.scan2clipboard.utils.Decoder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements AsyncDecodeHandler.DecodeCompleteListener {
    private static final String INTENT = "com.hm.tools.scan2clipboard";
    private final static int SCAN = 900;
    private final static int PICK_IMAGE = 1000;

    private HistoryFragment historyFragment;

    boolean single;
    private Record record = null;
    boolean newIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ZBarActivity.class);
                startActivityForResult(intent, SCAN);
            }
        });

        historyFragment = new HistoryFragment();
        showFragment(historyFragment);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (INTENT.equals(action)) {
            newIntent = true;
        } else if (type != null && type.startsWith("image/")) {
            if (Intent.ACTION_SEND.equals(action)) {
                handleIntentSingleImg(intent);
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                handleIntentMultipleImg(intent);
            }
        }
    }

    private void showFragment(Fragment fragment) {
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction()
                .replace(R.id.content_main, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.decode_image:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
//                startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE);
                startActivityForResult(intent, PICK_IMAGE);
                break;
            case R.id.clear_history:
                historyFragment.clearHistory();
                break;
            case R.id.action_settings:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case PICK_IMAGE:
                    if (data.getData() != null) {
                        Log.d("TAG", "onActivityResult: " + data.toString());
                        handleIntentSingleImg(data.getData());
                    }
                    break;
                case SCAN:
                    setResult(data.getStringArrayListExtra("results"));
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void setResult(ArrayList<String> resultList) {
        if (resultList == null) {
            Toast.makeText(this, "没有结果", Toast.LENGTH_LONG).show();
            return;
        }
        historyFragment.insertResults(resultList);
        if (resultList.size() == 1) {
            Clipboard.setText(this, resultList.get(0));
            Toast.makeText(this, "已复制至粘贴板", Toast.LENGTH_LONG).show();
        } else {
            showMultipleResults(resultList);
        }
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
        handleIntentSingleImg(imgUri);
    }

    private void handleIntentSingleImg(Uri uri) {
        if (uri != null) {
            single = true;
            AsyncDecodeHandler handler =
                    new AsyncDecodeHandler(this, Decoder.getDefaultDecoder(), this);
            handler.decodeBitmap(uri);
        }
    }

    private void singleDecodeComplete(ArrayList<String> result, int error) {
        if (error == Decoder.ERROR_NO_RESULT) {
            Toast.makeText(this, "没有结果", Toast.LENGTH_LONG).show();
        } else if (error == Decoder.ERROR_NO_ERROR){
            //in the async thread the result had been recorded.
            setResult(result);
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

        AsyncDecodeHandler handler = new AsyncDecodeHandler(this, Decoder.getDefaultDecoder(), this);
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
        } else if (error == Decoder.ERROR_NO_RESULT) {
            record.fail++;
        } else if (error == Decoder.ERROR_NO_BITMAP) {
            record.miss++;
        }
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

}
