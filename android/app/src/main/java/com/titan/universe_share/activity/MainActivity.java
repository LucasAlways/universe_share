package com.titan.universe_share;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.util.Base64;
import android.graphics.Bitmap;

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
    private static final int REQUEST_ACCESSIBILITY_SETTINGS = 2;
    private MethodChannel.Result pendingResult;
    private BroadcastReceiver permissionRequestReceiver;
    private static final String TAG = "MainActivity";
    private boolean mediaProjectionRequested = false;
    private Intent resultData; // 保存用户授予的MediaProjection权限
    private static Intent mediaProjectionResultData = null;

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
                        case "startFloatingService":
                            startFloatingService();
                            result.success(true);
                            break;
                        case "checkAccessibilityService":
                            boolean enabled = isAccessibilityServiceEnabled();
                            result.success(enabled);
                            break;
                        case "openAccessibilitySettings":
                            openAccessibilitySettings();
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

        // 尝试加载保存的MediaProjection授权数据
        Intent savedProjectionData = loadSavedMediaProjectionData();
        if (savedProjectionData != null) {
            Log.d(TAG, "已找到保存的MediaProjection授权数据");
            resultData = savedProjectionData;
            mediaProjectionRequested = true;
        } else {
            Log.d(TAG, "未找到有效的授权数据，需要重新申请权限");
        }

        // 检查是否需要请求屏幕录制权限
        Intent intent = getIntent();
        if (intent != null && "requestScreenshotPermission".equals(intent.getStringExtra("action"))) {
            requestMediaProjectionPermission();
        }

        // 在应用启动时检查辅助功能服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            // 提示用户启用辅助功能服务
            Toast.makeText(this, "请启用辅助功能服务以支持自动截图功能", Toast.LENGTH_LONG).show();

            // 可以选择自动打开辅助功能设置页面
            // openAccessibilitySettings();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && "requestScreenshotPermission".equals(intent.getStringExtra("action"))) {
            requestMediaProjectionPermission();
        }
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
        // 检查是否有悬浮窗权限
        if (!checkOverlayPermission()) {
            if (result != null) {
                result.error("PERMISSION_DENIED", "没有悬浮窗权限", null);
            }
            return;
        }

        try {
            // 确保服务已停止，以便重新启动
            try {
                stopService(new Intent(this, FloatingWindowService.class));
                Log.d(TAG, "停止现有的悬浮窗服务（如果存在）");
            } catch (Exception e) {
                Log.d(TAG, "停止服务出错（可能服务不存在）: " + e.getMessage());
            }

            // 稍微延迟重新启动，确保服务已完全停止
            new Handler().postDelayed(() -> {
                try {
                    Intent intent = new Intent(this, FloatingWindowService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }

                    Log.d(TAG, "悬浮窗服务启动成功");
                    if (result != null) {
                        result.success(true);
                    }

                    // 先退出到桌面
                    if (mediaProjectionRequested && resultData != null) {
                        Log.d(TAG, "传递保存的MediaProjection权限");
                        sendMediaProjectionResult(resultData);
                        exitToHome();
                    } else {
                        // 如果还没请求过截屏权限，主动请求
                        requestMediaProjectionPermission();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "启动悬浮窗服务失败: " + e.getMessage(), e);
                    if (result != null) {
                        result.error("START_ERROR", "启动悬浮窗服务失败: " + e.getMessage(), null);
                    }
                }
            }, 500);
        } catch (Exception e) {
            if (result != null) {
                result.error("START_ERROR", "启动悬浮窗服务失败: " + e.getMessage(), null);
            }
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
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE_MEDIA_PROJECTION);

            // 显示提示，告知用户需要给予权限
            Toast.makeText(this, "请授予屏幕录制权限以启用截图功能", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "请求截屏权限失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法请求截屏权限: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 注册广播接收器，接收服务的权限请求
    private void registerScreenCapturePermissionReceiver() {
        try {
            permissionRequestReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "收到截屏权限请求广播");

                    // 检查是否已有有效的权限数据
                    if (mediaProjectionRequested && resultData != null) {
                        Log.d(TAG, "已有有效的MediaProjection权限数据，直接使用");
                        // 重新发送已有的权限数据给服务
                        sendMediaProjectionResult(resultData);
                        return;
                    }

                    // 尝试加载保存的权限数据
                    Intent savedData = loadSavedMediaProjectionData();
                    if (savedData != null) {
                        Log.d(TAG, "找到保存的MediaProjection权限数据，使用保存的数据");
                        resultData = savedData;
                        mediaProjectionRequested = true;
                        sendMediaProjectionResult(resultData);
                        return;
                    }

                    // 没有有效的权限数据，需要重新请求
                    Log.d(TAG, "没有有效的权限数据，需要重新请求");
                    mediaProjectionRequested = false;
                    resultData = null;

                    // 检查服务是否存在
                    if (FloatingWindowServiceHolder.getService() != null) {
                        Log.d(TAG, "服务已存在，重新请求权限");
                        requestMediaProjectionPermission();
                    } else {
                        Log.d(TAG, "无法找到服务实例，先启动服务");
                        startFloatingWindow(null);
                        // 延迟请求权限，确保服务已启动
                        new Handler().postDelayed(() -> {
                            requestMediaProjectionPermission();
                        }, 1000);
                    }
                }
            };

            // 注册广播接收器，使用正确的导出标志
            IntentFilter filter = new IntentFilter("com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 确保使用 RECEIVER_NOT_EXPORTED 标志
                registerReceiver(permissionRequestReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(permissionRequestReceiver, filter);
            }

            Log.d(TAG, "截屏权限请求广播接收器已注册");
        } catch (Exception e) {
            Log.e(TAG, "注册截屏权限请求广播接收器失败: " + e.getMessage(), e);
        }
    }

    private void sendMediaProjectionResult(Intent data) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (FloatingWindowServiceHolder.getService() != null) {
                    FloatingWindowServiceHolder.getService().setMediaProjectionResult(data);
                } else {
                    // 启动服务并重试
                    Intent serviceIntent = new Intent(this, FloatingWindowService.class);
                    startService(serviceIntent);
                    new Handler().postDelayed(() -> {
                        if (FloatingWindowServiceHolder.getService() != null) {
                            FloatingWindowServiceHolder.getService().setMediaProjectionResult(data);
                        }
                    }, 1000);
                }
            } catch (Exception e) {
                Log.e(TAG, "传递权限失败: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            // MediaProjection不再使用，直接提示用户使用辅助功能服务
            Log.d(TAG, "不再使用MediaProjection，改用辅助功能服务");

            // 提示用户启用辅助功能服务
            Toast.makeText(this, "请启用辅助功能服务以使用自动截图功能", Toast.LENGTH_LONG).show();

            // 打开辅助功能设置
            openAccessibilitySettings();

            // 更新UI
            if (pendingResult != null) {
                pendingResult.success(true);
                pendingResult = null;
            }

            return;
        } else if (requestCode == REQUEST_ACCESSIBILITY_SETTINGS) {
            // 用户从辅助功能设置页面返回，检查是否已启用
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "辅助功能服务已启用，可以开始使用截图功能", Toast.LENGTH_SHORT).show();

                // 启动悬浮窗服务
                startFloatingService();
            } else {
                Toast.makeText(this, "请启用辅助功能服务以支持自动截图功能", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 保存MediaProjection权限数据到SharedPreferences
    private void saveMediaProjectionData(int resultCode, Intent data) {
        try {
            // 注意：这种方法并不适用于所有设备，因为Intent序列化可能有限制
            // 但我们仍然尝试保存，以便在支持的设备上工作
            SharedPreferences preferences = getSharedPreferences("MediaProjectionPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            // 保存结果代码
            editor.putInt("result_code", resultCode);

            // 序列化Intent数据
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                editor.putString("intent_extras", bundleToString(bundle));
            }

            // 保存Intent的action, data等基本信息
            if (data.getAction() != null) {
                editor.putString("intent_action", data.getAction());
            }
            if (data.getDataString() != null) {
                editor.putString("intent_data", data.getDataString());
            }

            // 保存授权时间
            editor.putLong("auth_time", System.currentTimeMillis());

            editor.apply();
            Log.d(TAG, "成功保存MediaProjection授权数据");
        } catch (Exception e) {
            Log.e(TAG, "保存MediaProjection数据失败: " + e.getMessage(), e);
        }
    }

    // 从SharedPreferences加载MediaProjection权限数据
    public Intent loadSavedMediaProjectionData() {
        try {
            SharedPreferences preferences = getSharedPreferences("MediaProjectionPref", MODE_PRIVATE);

            // 检查是否有保存的数据
            if (!preferences.contains("result_code")) {
                Log.d(TAG, "没有找到保存的MediaProjection数据");
                return null;
            }

            // 检查授权时间是否超过24小时
            long authTime = preferences.getLong("auth_time", 0);
            if (System.currentTimeMillis() - authTime > 24 * 60 * 60 * 1000) {
                Log.d(TAG, "保存的MediaProjection授权已过期");
                clearSavedMediaProjectionData();
                return null;
            }

            // 重建Intent
            Intent intent = new Intent();

            // 设置基本信息
            String action = preferences.getString("intent_action", null);
            if (action != null) {
                intent.setAction(action);
            }

            String dataString = preferences.getString("intent_data", null);
            if (dataString != null) {
                intent.setData(Uri.parse(dataString));
            }

            // 尝试恢复extras
            String bundleString = preferences.getString("intent_extras", null);
            if (bundleString != null) {
                try {
                    Bundle bundle = stringToBundle(bundleString);
                    intent.putExtras(bundle);
                } catch (Exception e) {
                    Log.e(TAG, "恢复Intent extras失败: " + e.getMessage());
                }
            }

            Log.d(TAG, "成功加载保存的MediaProjection数据");

            // 设置已请求标志
            mediaProjectionRequested = true;
            resultData = intent;

            return intent;
        } catch (Exception e) {
            Log.e(TAG, "加载MediaProjection数据失败: " + e.getMessage(), e);
            return null;
        }
    }

    // 清除保存的MediaProjection数据
    private void clearSavedMediaProjectionData() {
        try {
            SharedPreferences preferences = getSharedPreferences("MediaProjectionPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            Log.d(TAG, "已清除保存的MediaProjection数据");
        } catch (Exception e) {
            Log.e(TAG, "清除MediaProjection数据失败: " + e.getMessage());
        }
    }

    // 将Bundle转换为String
    private String bundleToString(Bundle bundle) {
        try {
            Parcel parcel = Parcel.obtain();
            bundle.writeToParcel(parcel, 0);
            byte[] bytes = parcel.marshall();
            parcel.recycle();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Bundle序列化失败: " + e.getMessage());
            return null;
        }
    }

    // 将String转换回Bundle
    private Bundle stringToBundle(String str) {
        try {
            byte[] bytes = Base64.decode(str, Base64.DEFAULT);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Bundle bundle = parcel.readBundle(getClassLoader());
            parcel.recycle();
            return bundle;
        } catch (Exception e) {
            Log.e(TAG, "Bundle反序列化失败: " + e.getMessage());
            return null;
        }
    }

    // 退出到桌面的方法
    private void exitToHome() {
        new Handler().postDelayed(() -> {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        }, 800); // 延迟0.8秒退出到桌面，可根据实际情况调整
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        startService(intent);

        Toast.makeText(this, "悬浮窗服务已启动", Toast.LENGTH_SHORT).show();
    }

    /**
     * 检查辅助功能服务是否已启用
     */
    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + ScreenCaptureAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServices != null) {
            return enabledServices.contains(serviceName);
        }

        return false;
    }

    /**
     * 打开辅助功能设置页面
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_SETTINGS);

        Toast.makeText(this, "请在辅助功能中找到并启用\"宇宙分享截屏服务\"", Toast.LENGTH_LONG).show();
    }
}