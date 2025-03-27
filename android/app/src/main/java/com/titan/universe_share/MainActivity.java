package com.titan.universe_share;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.universe_share/floating_window";
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1234;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 5678;
    private MethodChannel.Result pendingResult;
    private BroadcastReceiver permissionRequestReceiver;
    private static final String TAG = "MainActivity";
    private boolean mediaProjectionRequested = false;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        // 设置方法通道
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    Log.d("MainActivity", "Method call received: " + call.method);
                    switch (call.method) {
                        case "startFloatingWindow":
                            startFloatingWindow(result);
                            break;
                        case "stopFloatingWindow":
                            stopFloatingWindow(result);
                            break;
                        case "checkFloatingWindowPermission":
                            result.success(checkOverlayPermission());
                            break;
                        case "requestFloatingWindowPermission":
                            pendingResult = result;
                            requestOverlayPermission();
                            break;
                        case "requestScreenCapturePermission":
                            requestMediaProjectionPermission();
                            result.success(true);
                            break;
                        default:
                            result.notImplemented();
                            break;
                    }
                });

        // 注册广播接收器以接收截屏权限请求
        registerScreenCapturePermissionReceiver();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 不在创建时自动请求权限，等待用户操作触发
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (permissionRequestReceiver != null) {
            try {
                unregisterReceiver(permissionRequestReceiver);
            } catch (Exception e) {
                Log.e(TAG, "注销广播接收器失败: " + e.getMessage());
            }
        }
    }

    private void startFloatingWindow(MethodChannel.Result result) {
        if (!checkOverlayPermission()) {
            result.error("PERMISSION_DENIED", "没有悬浮窗权限", null);
            return;
        }

        try {
            Intent intent = new Intent(this, FloatingWindowService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            // 先返回结果表示成功启动
            result.success(true);

            // 如果还没请求过截屏权限，主动请求
            if (!mediaProjectionRequested) {
                new Handler().postDelayed(this::requestMediaProjectionPermission, 500);
                mediaProjectionRequested = true;
            }

            // 添加短暂延迟确保服务已启动
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 退出到桌面
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                }
            }, 1000); // 增加延迟时间

        } catch (Exception e) {
            result.error("START_ERROR", "启动悬浮窗服务失败: " + e.getMessage(), null);
            e.printStackTrace();
        }
    }

    private void stopFloatingWindow(MethodChannel.Result result) {
        try {
            Intent intent = new Intent(this, FloatingWindowService.class);
            stopService(intent);
            result.success(true);
        } catch (Exception e) {
            result.error("STOP_ERROR", "停止悬浮窗服务失败: " + e.getMessage(), null);
            e.printStackTrace();
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        } else {
            // 对于 Android 6.0 以下的设备，默认已授权
            if (pendingResult != null) {
                pendingResult.success(true);
                pendingResult = null;
            }
        }
    }

    // 请求MediaProjection权限
    private void requestMediaProjectionPermission() {
        try {
            Log.d(TAG, "正在请求截屏权限");
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION);
        } catch (Exception e) {
            Log.e(TAG, "请求截屏权限失败: " + e.getMessage(), e);
            Toast.makeText(this, "获取截屏权限失败，托管功能可能受限", Toast.LENGTH_LONG).show();
        }
    }

    // 注册广播接收器，接收服务的权限请求
    private void registerScreenCapturePermissionReceiver() {
        permissionRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION".equals(intent.getAction())) {
                    Log.d(TAG, "收到截屏权限请求广播");
                    // 使用主线程处理器延迟请求，避免频繁请求
                    new Handler().postDelayed(() -> {
                        // 避免连续多次请求
                        if (!mediaProjectionRequested) {
                            requestMediaProjectionPermission();
                            mediaProjectionRequested = true;
                        }
                    }, 500);
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION");
        registerReceiver(permissionRequestReceiver, filter);
    }

    private void sendMediaProjectionResult(Intent data) {
        if (data == null) {
            Log.e(TAG, "MediaProjection数据为空");
            return;
        }

        try {
            // 延迟发送，确保服务已启动
            new Handler().postDelayed(() -> {
                try {
                    // 尝试重启服务确保获得最新实例
                    Intent serviceIntent = new Intent(this, FloatingWindowService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }

                    // 再次延迟，确保服务已启动
                    new Handler().postDelayed(() -> {
                        FloatingWindowService service = FloatingWindowServiceHolder.getService();
                        if (service != null) {
                            service.setMediaProjectionResult(data);
                            Log.i(TAG, "成功将截屏权限传递给服务");
                            Toast.makeText(this, "截屏功能已准备就绪", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "无法获取FloatingWindowService实例");
                            Toast.makeText(this, "截屏功能初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    }, 500);
                } catch (Exception e) {
                    Log.e(TAG, "传递截屏权限失败: " + e.getMessage(), e);
                }
            }, 500);
        } catch (Exception e) {
            Log.e(TAG, "传递MediaProjection结果时出错: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            // 检查权限是否已授予
            boolean hasPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasPermission = Settings.canDrawOverlays(this);
            }

            if (pendingResult != null) {
                pendingResult.success(hasPermission);
                pendingResult = null;
            }

            // 如果没有获得权限，显示提示
            if (!hasPermission) {
                Toast.makeText(this, "需要悬浮窗权限才能正常工作", Toast.LENGTH_LONG).show();
            }
            return;
        } else if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            Log.d(TAG, "收到截屏权限结果: " + resultCode);
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "用户同意截屏权限");
                // 确保保存了权限结果
                mediaProjectionRequested = true;
                // 发送结果给服务
                sendMediaProjectionResult(data);
            } else {
                Log.e(TAG, "用户拒绝授予MediaProjection权限");
                Toast.makeText(this, "截屏权限被拒绝，托管功能将使用备选模式", Toast.LENGTH_LONG).show();
                mediaProjectionRequested = false;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}