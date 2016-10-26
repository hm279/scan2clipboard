package com.hm.tools.scan2clipboard;

import android.app.Application;

/**
 * Created by huang on 10/26/16.
 */

public class BaseApplication extends Application {
    private static BaseApplication application;
    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    public static BaseApplication getApplication() {
        return application;
    }
}
