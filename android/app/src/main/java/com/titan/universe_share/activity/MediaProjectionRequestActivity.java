package com.titan.universe_share.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.titan.universe_share.constant.RequestCode;
import com.titan.universe_share.utils.MediaProjectionHelper;

public class MediaProjectionRequestActivity extends Activity {
    private static final String TAG = "MediaProjRequestAct";
    private static final String ACTION_MEDIA_PROJECTION_GRANTED = "com.titan.universe_share.MEDIA_PROJECTION_GRANTED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 无需设置内容视图，保持透明

        Log.d(TAG, "开始请求媒体投影权限");
        // 直接请求权限
        MediaProjectionHelper.start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "收到权限请求结果: code=" + requestCode + ", result=" + resultCode);

        if (requestCode == RequestCode.START_MEDIA_PROJECTION) {
            // 处理权限结果
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "媒体投影权限已授予，广播通知服务");

                // 发送广播通知 FloatingWindowService
                Intent intent = new Intent(ACTION_MEDIA_PROJECTION_GRANTED);
                sendBroadcast(intent);

                // 同时调用Helper处理
                MediaProjectionHelper.onStartResult(requestCode, resultCode, data);

                Toast.makeText(this, "已获取屏幕录制权限", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "媒体投影权限被拒绝");
                Toast.makeText(this, "屏幕录制权限被拒绝", Toast.LENGTH_SHORT).show();
            }

            // 结束活动
            finish();
        }
    }
}