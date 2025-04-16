package com.titan.universe_share.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import com.titan.universe_share.TitanApp;
import com.titan.universe_share.constant.RequestCode;
import com.titan.universe_share.service.MediaProjectionService;

public class MediaProjectionHelper {
    private static final MediaProjectionManager MEDIA_PROJECTION_MANAGER = (MediaProjectionManager) TitanApp.getApp().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    private static final Intent SERVICE_INTENT = new Intent(TitanApp.getApp(), MediaProjectionService.class);
    private static boolean mStarted = false;

    public static MediaProjectionManager getManager() {
        return MEDIA_PROJECTION_MANAGER;
    }

    public static void start(Activity activity) {
        if (mStarted) {
            return;
        }
        activity.startActivityForResult(MEDIA_PROJECTION_MANAGER.createScreenCaptureIntent(), RequestCode.START_MEDIA_PROJECTION);
    }

    public static void onStartResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != RequestCode.START_MEDIA_PROJECTION) {
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            MediaProjectionService.resultCode = resultCode;
            MediaProjectionService.resultData = resultData;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TitanApp.getApp().startForegroundService(SERVICE_INTENT);
            } else {
                TitanApp.getApp().startService(SERVICE_INTENT);
            }
            mStarted = true;
        } else {
            ToastUtils.shortCall("录制服务启动失败");
        }
    }

    public static void stop() {
        if (!mStarted) {
            return;
        }
        TitanApp.getApp().stopService(SERVICE_INTENT);
        mStarted = false;
    }
}
