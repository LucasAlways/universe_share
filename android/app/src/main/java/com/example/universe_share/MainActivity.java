package com.example.universe_share;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.universe_share/floating_window";
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1234;
    private MethodChannel.Result pendingResult;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
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
                                    requestOverlayPermission(result);
                                    break;
                                default:
                                    result.notImplemented();
                                    break;
                            }
                        });
    }

    private void startFloatingWindow(MethodChannel.Result result) {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_LONG).show();
            result.success(false);
            return;
        }

        try {
            // 确保服务未运行
            stopService(new Intent(this, FloatingWindowService.class));

            // 启动服务
            Intent intent = new Intent(this, FloatingWindowService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            // 延迟一点，确保服务有时间启动
            new Handler().postDelayed(() -> {
                // 尝试最小化应用
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);

                result.success(true);
            }, 500);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "启动悬浮窗失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            result.error("SERVICE_START_ERROR", "无法启动悬浮窗服务", e.getMessage());
        }
    }

    private void stopFloatingWindow(MethodChannel.Result result) {
        try {
            Intent intent = new Intent(this, FloatingWindowService.class);
            stopService(intent);
            result.success(true);
        } catch (Exception e) {
            result.error("SERVICE_STOP_ERROR", "无法停止悬浮窗服务", e.getMessage());
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission(MethodChannel.Result result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingResult = result;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
        } else {
            result.success(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (pendingResult != null) {
                pendingResult.success(checkOverlayPermission());
                pendingResult = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}