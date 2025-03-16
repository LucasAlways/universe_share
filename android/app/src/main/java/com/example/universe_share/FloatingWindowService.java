package com.example.universe_share;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private View expandedView;
    private boolean isExpanded = false;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FloatingWindowChannel";
    private static final String CHANNEL_NAME = "悬浮窗服务";

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 在Android 8.0及以上版本，需要创建前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingView == null) {
            showFloatingWindow();
        }

        return START_STICKY;
    }

    // 创建通知渠道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    // 创建通知
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("二维码托管")
                .setContentText("二维码托管服务正在运行")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void showFloatingWindow() {
        try {
            // 创建悬浮视图参数
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            // 默认位置
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 100;

            // 创建简单的悬浮视图（不使用XML布局）
            createSimpleFloatingView(params);
        } catch (Exception e) {
            Toast.makeText(this, "创建悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void createSimpleFloatingView(WindowManager.LayoutParams params) {
        // 创建一个简单的悬浮按钮
        ImageView floatingButton = new ImageView(this);
        floatingButton.setBackgroundColor(0xFF2196F3); // 蓝色背景
        floatingButton.setImageResource(android.R.drawable.ic_input_add); // 使用系统自带的加号图标
        floatingButton.setPadding(16, 16, 16, 16);

        // 设置按钮大小
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(120, 120);
        floatingButton.setLayoutParams(buttonParams);

        // 创建一个容器包裹按钮
        LinearLayout floatingContainer = new LinearLayout(this);
        floatingContainer.setBackgroundColor(0x00000000); // 透明背景
        floatingContainer.setPadding(24, 24, 24, 24);
        floatingContainer.addView(floatingButton);

        floatingView = floatingContainer;

        // 创建展开视图
        LinearLayout expandedContainer = new LinearLayout(this);
        expandedContainer.setOrientation(LinearLayout.VERTICAL);
        expandedContainer.setPadding(32, 32, 32, 32);
        expandedContainer.setBackgroundColor(0xFFFFFFFF); // 白色背景

        // 添加标题
        TextView titleText = new TextView(this);
        titleText.setText("二维码托管");
        titleText.setTextColor(0xFF000000);
        titleText.setTextSize(18);
        titleText.setPadding(0, 0, 0, 24);
        titleText.setGravity(Gravity.CENTER);
        expandedContainer.addView(titleText);

        // 添加"开始托管"按钮
        Button startButton = new Button(this);
        startButton.setText("开始托管");
        startButton.setBackgroundColor(0xFF4CAF50); // 绿色
        startButton.setTextColor(0xFFFFFFFF); // 白色
        expandedContainer.addView(startButton);

        // 添加间距
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16));
        expandedContainer.addView(spacer);

        // 添加"结束托管"按钮
        Button stopButton = new Button(this);
        stopButton.setText("结束托管");
        stopButton.setBackgroundColor(0xFFF44336); // 红色
        stopButton.setTextColor(0xFFFFFFFF); // 白色
        expandedContainer.addView(stopButton);

        expandedView = expandedContainer;

        // 设置点击悬浮按钮的动作
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 切换视图
                try {
                    if (isExpanded) {
                        windowManager.removeView(expandedView);
                        windowManager.addView(floatingView, params);
                    } else {
                        windowManager.removeView(floatingView);
                        windowManager.addView(expandedView, params);
                    }
                    isExpanded = !isExpanded;
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FloatingWindowService.this,
                            "切换悬浮窗失败: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 设置开始托管按钮的点击事件
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始托管操作
                Toast.makeText(FloatingWindowService.this,
                        "开始托管", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置结束托管按钮的点击事件
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止托管操作
                Toast.makeText(FloatingWindowService.this,
                        "结束托管", Toast.LENGTH_SHORT).show();
                stopSelf(); // 停止服务
            }
        });

        // 添加视图到窗口
        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "添加悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int getWindowLayoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 清理视图
        try {
            if (floatingView != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (floatingView.isAttachedToWindow()) {
                        windowManager.removeView(floatingView);
                    }
                } else {
                    // 在旧版本上尝试移除，忽略可能的异常
                    try {
                        windowManager.removeView(floatingView);
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                floatingView = null;
            }

            if (expandedView != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (expandedView.isAttachedToWindow()) {
                        windowManager.removeView(expandedView);
                    }
                } else {
                    // 在旧版本上尝试移除，忽略可能的异常
                    try {
                        windowManager.removeView(expandedView);
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                expandedView = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}