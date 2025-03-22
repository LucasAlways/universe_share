package com.example.universe_share;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

import com.universe_share.floating_window.FloatingWindowPlugin;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.universe_share/floating_window";
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1234;
    private MethodChannel.Result pendingResult;
    private FloatingWindowPlugin floatingWindowPlugin;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        // 注册我们的插件
        floatingWindowPlugin = new FloatingWindowPlugin();
        flutterEngine.getPlugins().add(floatingWindowPlugin);

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
                        default:
                            result.notImplemented();
                            break;
                    }
                });
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
            }, 200); // 200毫秒延迟

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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}