package com.hm.tools.scan2clipboard.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.hm.tools.scan2clipboard.R;

/**
 * Created by hm on 15-6-7.
 */
public class Clipboard {
    public static void setText(Context context, String text) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText(context.getString(R.string.app_name), text);
        manager.setPrimaryClip(data);
    }
}
