package com.hm.tools.scan2clipboard;

import android.graphics.Bitmap;
import android.util.Log;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.ArrayList;

/**
 * Created by hm on 15-6-6.
 */
public class Decoder {
    public static final int ERROR_NO_BITMAP = -2;
    public static final int ERROR_NO_RESULT = -1;
    public static final int ERROR_NO_ERROR = 0;

    private ImageScanner scanner;

    public Decoder() {
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    public ArrayList<String> decode(int w, int h, byte[] data) {
        Image image = new Image(w, h, "Y800");
        image.setData(data);
        int result = scanner.scanImage(image);
        if (result != 0) {
            SymbolSet symbolSet = scanner.getResults();
            ArrayList<String> list = new ArrayList<>();
            for (Symbol symbol : symbolSet) {
                list.add(symbol.getData());
            }
            return list;
        }
        return null;
    }


    public ArrayList<String> decode(Bitmap bitmap) {
        if (null != bitmap) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            Image data = new Image(w, h, "RGB4");
            data.setData(pixels);
            int result = scanner.scanImage(data.convert("Y800"));
            if (result == 0) {
                reverseImageData(pixels);
                result = scanner.scanImage(data.convert("Y800"));
            }
            if (result != 0) {
                SymbolSet symbolSet = scanner.getResults();
                ArrayList<String> list = new ArrayList<>();
                for (Symbol symbol : symbolSet) {
                    list.add(symbol.getData());
                }
                return list;
            }
        }
        return null;
    }

    public static void reverseImageData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
//            data[i] = (byte) (~data[i] & 0xff);
            data[i] = (byte) (255 - data[i]);
        }
    }

    public static void reverseImageData(int[] pixels) {
        Log.d("Decoder.java", "reverseImageData()");
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = ~pixels[i] | 0xff000000;
        }
    }

}
