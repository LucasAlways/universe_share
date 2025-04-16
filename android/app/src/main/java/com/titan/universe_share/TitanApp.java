package com.titan.universe_share;

import android.app.Activity;
import android.app.Application;

public class TitanApp extends Application {
    private static TitanApp app;
    // 在 TitanApp.java 中添加
    private static Activity currentActivity;

    public static void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static TitanApp getApp() {
        return app;
    }
}
