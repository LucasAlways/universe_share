package com.titan.universe_share;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Looper;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.Image;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.Point;
import android.view.Display;
import android.util.DisplayMetrics;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.titan.universe_share.utils.PaymentTemplateManager;
import android.graphics.BitmapFactory;
import com.titan.universe_share.utils.ImageSimilarityUtils;
import android.os.HandlerThread;
import android.app.Activity;
import android.os.Vibrator;
import java.nio.ByteBuffer;

public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    private WindowManager windowManager;
    private View floatingView;
    private View expandedView;
    private boolean isExpanded = false;
    private boolean isPartiallyHidden = false;
    private int screenWidth = 0;
    private static final int EDGE_WIDTH = 20; // 边缘露出的宽度
    private boolean isOnLeftSide = false;

    // 用于自动隐藏的定时器
    private Handler autoHideHandler;
    private Runnable autoHideRunnable;
    private static final long AUTO_HIDE_DELAY = 5000; // 5秒无操作后自动隐藏

    // 用于保持服务存活的定时器
    private Handler keepAliveHandler;
    private Runnable keepAliveRunnable;
    private static final long KEEP_ALIVE_INTERVAL = 30000; // 每30秒执行一次

    // 颜色定义
    private static final int COLOR_PRIMARY = 0xFF2196F3; // 浅蓝色
    private static final int COLOR_PRIMARY_DARK = 0xFF1976D2;
    private static final int COLOR_ACCENT = 0xFF03A9F4;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TRANSPARENT = 0x00000000;
    private static final int COLOR_EDGE_TRANSPARENT = 0x802196F3; // 半透明浅蓝色
    private static final int COLOR_BACKGROUND_TRANSPARENT = 0x80000000; // 半透明黑色背景

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FloatingWindowChannel";
    private static final String CHANNEL_NAME = "悬浮窗服务";

    // 悬浮窗大小
    private static final int FLOATING_BUTTON_SIZE = 160;

    private View cancelHostingView;
    private boolean isHosting = false;
    private Handler hostingHandler;
    private static final long SCREENSHOT_INTERVAL = 1000; // 每秒检查一次
    private PaymentTemplateManager templateManager; // 替换templateBitmap
    private long lastQrCodeExpiryTime = 0;
    private String lastQrCodeContent = "";
    private String lastCardNumber = "";
    private String lastBalance = "";

    // 屏幕截图相关成员变量
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenDensity;
    private int screenHeight;
    private Handler screenshotHandler;
    private static final String SCREENSHOT_THREAD_NAME = "screenshot-thread";
    private Intent resultData; // 保存MediaProjection权限结果
    private long lastLogTime = 0; // 上次记录日志的时间
    private boolean mediaProjectionInitialized = false; // 标记MediaProjection是否已初始化
    private Bitmap lastScreenshot = null; // 最后一次截图
    private Runnable screenshotRunnable; // 持有截图任务的引用
    private static final long HOSTING_INTERVAL = 1000; // 托管任务间隔

    @Override
    public void onCreate() {
        super.onCreate();

        // 注册服务实例
        FloatingWindowServiceHolder.setService(this);

        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取屏幕宽度，用于计算边缘位置
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        // 初始化模板管理器
        templateManager = new PaymentTemplateManager(this);

        // 初始化自动隐藏定时器
        autoHideHandler = new Handler(Looper.getMainLooper());
        autoHideRunnable = new Runnable() {
            @Override
            public void run() {
                if (floatingView != null && !isExpanded && !isPartiallyHidden) {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                    snapToEdge(params);
                }
            }
        };

        // 初始化保持服务存活的定时器
        keepAliveHandler = new Handler(Looper.getMainLooper());
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // 检查悬浮窗是否存在，如果不存在则重新创建
                if (floatingView == null) {
                    try {
                        showFloatingWindow();
                        Log.d("FloatingWindowService", "重新创建悬浮窗");
                    } catch (Exception e) {
                        Log.e("FloatingWindowService", "重新创建悬浮窗失败", e);
                    }
                }
                // 安排下一次执行
                keepAliveHandler.postDelayed(this, KEEP_ALIVE_INTERVAL);
            }
        };

        // 启动保持存活的任务
        keepAliveHandler.postDelayed(keepAliveRunnable, KEEP_ALIVE_INTERVAL);

        // 创建通知渠道，但不启动为前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            // 不启动为前台服务，避免权限问题
        }

        Log.d("FloatingWindowService", "服务已创建");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // 创建通知渠道
        createNotificationChannel();

        // 立即启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            }
            startForeground(NOTIFICATION_ID, createNotification(), serviceType);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        // 显示悬浮窗
        showFloatingWindow();

        // 启动保持服务存活的定时器
        keepAliveHandler.postDelayed(keepAliveRunnable, KEEP_ALIVE_INTERVAL);

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

    // 重置自动隐藏计时器
    private void resetAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
    }

    // 取消自动隐藏计时器
    private void cancelAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }

    private void showFloatingWindow() {
        try {
            Log.d("FloatingWindowService", "开始创建悬浮窗");

            // 确保悬浮窗没有被添加过
            if (floatingView != null) {
                try {
                    windowManager.removeView(floatingView);
                    Log.d("FloatingWindowService", "移除现有悬浮窗");
                } catch (Exception e) {
                    // 忽略如果视图没有被添加的错误
                    Log.d("FloatingWindowService", "移除现有悬浮窗失败，可能没有被添加: " + e.getMessage());
                }
                floatingView = null;
            }

            // 创建悬浮视图参数
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    getWindowLayoutType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            // 设置位置 - 默认放在右侧中间
            params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            params.x = 0; // 右侧距离
            params.y = 0; // 垂直居中位置

            // 创建现代风格的悬浮视图
            createModernFloatingView(params);

            // 确认悬浮窗是否成功创建
            if (floatingView != null) {
                Log.d("FloatingWindowService", "悬浮窗视图创建成功");
                // 启动自动隐藏计时器
                resetAutoHideTimer();
            } else {
                Log.e("FloatingWindowService", "悬浮窗视图创建失败");
            }
        } catch (Exception e) {
            Log.e("FloatingWindowService", "创建悬浮窗失败: " + e.getMessage(), e);
            Toast.makeText(this, "创建悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void createModernFloatingView(final WindowManager.LayoutParams params) {
        try {
            // 创建主容器
            FrameLayout rootContainer = new FrameLayout(this);

            // 创建一个圆形按钮容器
            FrameLayout buttonContainer = new FrameLayout(this);
            buttonContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FLOATING_BUTTON_SIZE, FLOATING_BUTTON_SIZE));

            // 创建一个圆形背景
            ShapeDrawable circleDrawable = new ShapeDrawable(new OvalShape());
            circleDrawable.getPaint().setColor(COLOR_PRIMARY);

            // 创建悬浮按钮
            ImageView floatingButton = new ImageView(this);
            floatingButton.setLayoutParams(new FrameLayout.LayoutParams(
                    FLOATING_BUTTON_SIZE, FLOATING_BUTTON_SIZE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                floatingButton.setBackground(circleDrawable);
            } else {
                floatingButton.setBackgroundDrawable(circleDrawable);
            }

            // 设置加号图标为白色
            floatingButton.setImageResource(android.R.drawable.ic_input_add);
            floatingButton.setColorFilter(COLOR_WHITE, PorterDuff.Mode.SRC_IN);
            floatingButton.setPadding(30, 30, 30, 30);
            floatingButton.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // 添加按钮到容器
            buttonContainer.addView(floatingButton);
            rootContainer.addView(buttonContainer);

            floatingView = rootContainer;

            // 初始位置记录变量
            final float[] initialTouchX = new float[1];
            final float[] initialTouchY = new float[1];
            final int[] initialX = new int[1];
            final int[] initialY = new int[1];
            final boolean[] moving = new boolean[1];
            final long[] lastClickTime = new long[1];
            final long CLICK_TIME_THRESHOLD = 200; // 毫秒

            // 设置触摸监听器 - 实现拖动和边缘停靠功能
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    // 每次触摸都重置自动隐藏计时器
                    resetAutoHideTimer();

                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            // 记录初始位置
                            initialX[0] = params.x;
                            initialY[0] = params.y;
                            initialTouchX[0] = event.getRawX();
                            initialTouchY[0] = event.getRawY();
                            moving[0] = false;
                            lastClickTime[0] = System.currentTimeMillis();

                            // 如果当前隐藏在边缘，则改变透明度以指示正在交互
                            if (isPartiallyHidden) {
                                View buttonView = ((FrameLayout) floatingView).getChildAt(0);
                                if (buttonView != null) {
                                    buttonView.setAlpha(1.0f);
                                }
                            }

                            return true;

                        case android.view.MotionEvent.ACTION_MOVE:
                            // 直接使用手指位置，而不是计算偏移量
                            float rawX = event.getRawX();
                            float rawY = event.getRawY();

                            // 计算新位置，考虑按钮大小，确保不会超出屏幕边界
                            params.x = (int) (rawX - FLOATING_BUTTON_SIZE / 2);
                            params.y = (int) (rawY - FLOATING_BUTTON_SIZE / 2);

                            // 限制X坐标范围
                            params.x = Math.max(0, Math.min(params.x, screenWidth - FLOATING_BUTTON_SIZE));

                            // 标记为移动状态
                            if (Math.abs(rawX - initialTouchX[0]) > 10 ||
                                    Math.abs(rawY - initialTouchY[0]) > 10) {
                                moving[0] = true;
                                // 如果是从边缘拖出，将其标记为非隐藏状态
                                if (isPartiallyHidden) {
                                    isPartiallyHidden = false;
                                    View buttonView = ((FrameLayout) floatingView).getChildAt(0);
                                    if (buttonView != null) {
                                        buttonView.setAlpha(1.0f);
                                    }
                                }
                            }

                            // 更新视图位置
                            if (windowManager != null && floatingView != null) {
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;

                        case android.view.MotionEvent.ACTION_UP:
                            // 处理点击事件
                            long clickDuration = System.currentTimeMillis() - lastClickTime[0];

                            if (!moving[0] && clickDuration < CLICK_TIME_THRESHOLD) {
                                if (isPartiallyHidden) {
                                    // 如果当前隐藏在边缘，则移出来
                                    animateViewToVisible(params);
                                    Toast.makeText(FloatingWindowService.this, "悬浮窗已恢复", Toast.LENGTH_SHORT).show();
                                } else if (isExpanded) {
                                    // 如果已经展开了二维码界面，则收起
                                    toggleExpandedView(params);
                                } else {
                                    // 否则，展开二维码界面
                                    toggleExpandedView(params);
                                }
                            } else {
                                // 拖动结束，判断是否应该自动吸附到屏幕边缘
                                snapToEdge(params);
                            }
                            return true;
                    }
                    return false;
                }
            });

            // 创建展开视图（二维码托管界面）
            createExpandedView(params);

            // 添加视图到窗口
            try {
                if (windowManager != null && floatingView != null) {
                    Log.d("FloatingWindowService", "正在添加悬浮窗视图到窗口管理器");
                    windowManager.addView(floatingView, params);
                    Log.d("FloatingWindowService", "成功添加悬浮窗视图到窗口管理器");
                } else {
                    Log.e("FloatingWindowService", "windowManager或floatingView为空，无法添加视图");
                }
            } catch (Exception e) {
                Log.e("FloatingWindowService", "添加悬浮窗失败: " + e.getMessage(), e);
                e.printStackTrace();
                Toast.makeText(this, "添加悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("FloatingWindowService", "创建悬浮窗视图失败: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "创建悬浮窗视图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            floatingView = null;
        }
    }

    private void createExpandedView(final WindowManager.LayoutParams params) {
        // 创建一个透明的背景层用于点击外部关闭
        FrameLayout backgroundView = new FrameLayout(this);
        FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        backgroundView.setLayoutParams(bgParams);
        backgroundView.setBackgroundColor(COLOR_TRANSPARENT); // 完全透明

        // 创建展开视图容器（卡片式设计）
        LinearLayout cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.setBackgroundColor(COLOR_WHITE);

        // 设置圆角背景
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(20);
        shape.setColor(COLOR_WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            cardContainer.setBackground(shape);
        } else {
            cardContainer.setBackgroundDrawable(shape);
        }

        cardContainer.setPadding(32, 32, 32, 32);

        // 设置阴影效果（仅21及以上支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardContainer.setElevation(10);
        }

        // 修改卡片布局参数，根据悬浮窗位置动态调整
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER_VERTICAL; // 移除 END gravity，改为动态计算位置
        cardContainer.setLayoutParams(cardParams);

        // 添加标题
        TextView titleText = new TextView(this);
        titleText.setText("二维码托管");
        titleText.setTextColor(COLOR_PRIMARY_DARK);
        titleText.setTextSize(18);
        titleText.setPadding(0, 0, 0, 24);
        titleText.setGravity(Gravity.CENTER);
        cardContainer.addView(titleText);

        // 添加"开始托管"按钮
        Button startButton = new Button(this);
        startButton.setText("开始托管");
        startButton.setTextColor(COLOR_WHITE);

        // 设置按钮圆角背景
        GradientDrawable btnShape = new GradientDrawable();
        btnShape.setCornerRadius(30);
        btnShape.setColor(COLOR_PRIMARY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startButton.setBackground(btnShape);
        } else {
            startButton.setBackgroundDrawable(btnShape);
        }

        cardContainer.addView(startButton);

        // 添加间距
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16));
        cardContainer.addView(spacer);

        // 添加"结束托管"按钮
        Button stopButton = new Button(this);
        stopButton.setText("结束托管");
        stopButton.setTextColor(COLOR_WHITE);

        // 设置按钮圆角背景
        GradientDrawable stopBtnShape = new GradientDrawable();
        stopBtnShape.setCornerRadius(30);
        stopBtnShape.setColor(0xFFF44336); // 红色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            stopButton.setBackground(stopBtnShape);
        } else {
            stopButton.setBackgroundDrawable(stopBtnShape);
        }

        cardContainer.addView(stopButton);

        // 添加卡片到背景层
        backgroundView.addView(cardContainer);

        // 保存展开视图
        expandedView = backgroundView;

        // 设置点击背景层的监听器，点击非卡片区域关闭展开视图
        backgroundView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpandedView(params);
            }
        });

        // 设置点击卡片不传递事件到背景
        cardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 事件被消费，不向下传递
                // 重置自动隐藏计时器
                resetAutoHideTimer();
            }
        });

        // 设置开始托管按钮的点击事件
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示确认对话框
                showConfirmStartHostingDialog(params);

                // 重置自动隐藏计时器
                resetAutoHideTimer();
            }
        });

        // 设置结束托管按钮的点击事件
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止托管操作并关闭展开视图
                Toast.makeText(FloatingWindowService.this,
                        "结束托管", Toast.LENGTH_SHORT).show();
                toggleExpandedView(params);

                // 取消自动隐藏计时器
                cancelAutoHideTimer();

                // 可选：延迟一会儿再停止服务，以便用户看到 Toast
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                }, 1000);
            }
        });
    }

    // 添加显示确认对话框的方法
    private void showConfirmStartHostingDialog(final WindowManager.LayoutParams params) {
        try {
            // 使用系统对话框样式
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(this);
            }

            builder.setTitle("确认启动托管")
                    .setMessage("请确保当前界面为正确的二维码展示界面，否则无法正常启动服务！")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("是", (dialog, which) -> {
                        dialog.dismiss();
                        // 关闭展开视图
                        toggleExpandedView(params);
                        // 开始托管操作
                        Toast.makeText(FloatingWindowService.this, "开始托管", Toast.LENGTH_SHORT).show();
                        // 调用startHosting方法开始实际托管
                        Log.d(TAG, "*********开始---托管*********");
                        startHosting();
                    })
                    .setNegativeButton("关闭", (dialog, which) -> {
                        dialog.dismiss();
                        // 不做任何操作，保持悬浮窗状态
                        Toast.makeText(FloatingWindowService.this, "已取消托管", Toast.LENGTH_SHORT).show();
                    })
                    .setCancelable(false); // 强制用户选择一个选项

            AlertDialog dialog = builder.create();
            // 设置对话框显示层级，确保它显示在其他窗口之上
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(getWindowLayoutType());
            }
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "显示确认对话框时出错", e);
            Toast.makeText(this, "无法显示确认对话框", Toast.LENGTH_SHORT).show();
        }
    }

    // 切换展开/收起托管界面
    private void toggleExpandedView(WindowManager.LayoutParams params) {
        try {
            if (isExpanded) {
                // 如果当前已展开，则关闭展开视图
                if (expandedView != null && windowManager != null) {
                    windowManager.removeView(expandedView);
                    Log.d("FloatingWindowService", "关闭展开视图");
                }
                // 重置自动隐藏计时器
                resetAutoHideTimer();
            } else {
                // 如果当前已收起，则显示展开视图
                if (expandedView != null && windowManager != null) {
                    WindowManager.LayoutParams expandedParams = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            getWindowLayoutType(),
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT);
                    expandedParams.gravity = Gravity.CENTER;
                    windowManager.addView(expandedView, expandedParams);

                    // 获取卡片容器
                    LinearLayout cardContainer = (LinearLayout) ((FrameLayout) expandedView).getChildAt(0);
                    if (cardContainer != null) {
                        // 计算卡片位置
                        FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) cardContainer
                                .getLayoutParams();

                        // 根据悬浮窗位置决定卡片显示在左侧还是右侧
                        if (params.x < screenWidth / 2) {
                            // 悬浮窗在左侧，卡片显示在右侧
                            cardParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
                            cardParams.rightMargin = screenWidth - params.x - FLOATING_BUTTON_SIZE - 20;
                        } else {
                            // 悬浮窗在右侧，卡片显示在左侧
                            cardParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                            cardParams.leftMargin = params.x - 20;
                        }
                        cardContainer.setLayoutParams(cardParams);
                    }

                    Log.d("FloatingWindowService", "显示展开视图");
                }
                // 展开视图时取消自动隐藏
                cancelAutoHideTimer();
            }
            isExpanded = !isExpanded;
        } catch (Exception e) {
            Log.e("FloatingWindowService", "切换展开视图失败: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // 吸附到屏幕边缘，这里只保留在右侧
    private void snapToEdge(WindowManager.LayoutParams params) {
        // 先检查视图是否已经被移除
        if (floatingView == null || !isViewAttached(floatingView)) {
            Log.e(TAG, "悬浮窗已被移除，无法吸附到边缘");
            return;
        }

        // 只吸附到右边缘
        animateViewToEdge(params, false);
    }

    // 检查视图是否附加到窗口管理器
    private boolean isViewAttached(View view) {
        if (view == null)
            return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return view.isAttachedToWindow();
        } else {
            // 对于API 19以下，没有可靠的方法检查，只能尝试更新布局
            try {
                windowManager.updateViewLayout(view, view.getLayoutParams());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    // 将悬浮窗动画移动到边缘
    private void animateViewToEdge(final WindowManager.LayoutParams params, final boolean isLeft) {
        // 先检查视图是否已经被移除
        if (floatingView == null || !isViewAttached(floatingView)) {
            Log.e(TAG, "悬浮窗已被移除，无法执行动画");
            return;
        }

        final int targetX;
        // 强制使用右侧
        if (isLeft) {
            // 设置为负值，使大部分区域隐藏在左边缘外 - 但我们只使用右侧
            targetX = -params.width + EDGE_WIDTH;
        } else {
            // 设置为屏幕宽度减去悬浮窗的露出部分宽度
            targetX = screenWidth - EDGE_WIDTH;
        }

        // 记录开始位置
        final int startX = params.x;
        final int distance = targetX - startX;

        // 设置动画持续时间
        final int DURATION = 300; // 毫秒
        final int STEPS = 20; // 动画步数
        final int STEP_DURATION = DURATION / STEPS;

        // 使用Handler进行简单动画
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] animationSteps = new Runnable[STEPS];

        for (int i = 0; i < STEPS; i++) {
            final int step = i;
            animationSteps[i] = new Runnable() {
                @Override
                public void run() {
                    // 检查视图是否还在窗口中
                    if (floatingView == null || !isViewAttached(floatingView)) {
                        // 如果视图已被移除，终止动画
                        Log.e(TAG, "动画过程中悬浮窗已被移除，终止动画");
                        return;
                    }

                    float progress = (float) (step + 1) / STEPS;
                    params.x = startX + (int) (distance * progress);

                    try {
                        windowManager.updateViewLayout(floatingView, params);

                        if (step < STEPS - 1) {
                            handler.postDelayed(animationSteps[step + 1], STEP_DURATION);
                        } else {
                            // 动画结束，标记为部分隐藏状态
                            isPartiallyHidden = true;
                            isOnLeftSide = isLeft;

                            // 设置边缘部分半透明，但可见
                            View buttonView = ((FrameLayout) floatingView).getChildAt(0);
                            if (buttonView != null) {
                                buttonView.setAlpha(0.7f);
                            }

                            Log.d("FloatingWindowService", "悬浮窗已隐藏到边缘，isLeft=" + isLeft);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "更新悬浮窗布局时出错", e);
                    }
                }
            };
        }

        // 开始动画
        handler.post(animationSteps[0]);
    }

    // 将隐藏的悬浮窗动画移回可见区域
    private void animateViewToVisible(final WindowManager.LayoutParams params) {
        // 先检查视图是否已经被移除
        if (floatingView == null || !isViewAttached(floatingView)) {
            Log.e(TAG, "悬浮窗已被移除，无法执行动画");
            return;
        }

        final int targetX;
        if (isOnLeftSide) {
            targetX = 0; // 左侧显示时X为0
        } else {
            targetX = screenWidth - FLOATING_BUTTON_SIZE; // 右侧显示时X为屏幕宽度减按钮宽度
        }

        final int startX = params.x; // 当前位置
        final int distance = targetX - startX;

        // 设置动画持续时间
        final int DURATION = 300; // 毫秒
        final int STEPS = 20; // 动画步数
        final int STEP_DURATION = DURATION / STEPS;

        // 使用Handler进行简单动画
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] animationSteps = new Runnable[STEPS];

        for (int i = 0; i < STEPS; i++) {
            final int step = i;
            animationSteps[i] = new Runnable() {
                @Override
                public void run() {
                    // 检查视图是否还在窗口中
                    if (floatingView == null || !isViewAttached(floatingView)) {
                        // 如果视图已被移除，终止动画
                        Log.e(TAG, "动画过程中悬浮窗已被移除，终止动画");
                        return;
                    }

                    float progress = (float) (step + 1) / STEPS;
                    params.x = startX + (int) (distance * progress);

                    try {
                        windowManager.updateViewLayout(floatingView, params);

                        if (step < STEPS - 1) {
                            handler.postDelayed(animationSteps[step + 1], STEP_DURATION);
                        } else {
                            // 动画结束，标记为完全可见状态
                            isPartiallyHidden = false;

                            // 恢复完全不透明
                            View buttonView = ((FrameLayout) floatingView).getChildAt(0);
                            if (buttonView != null) {
                                buttonView.setAlpha(1.0f);
                            }

                            // 重置自动隐藏计时器
                            resetAutoHideTimer();

                            Log.d("FloatingWindowService", "悬浮窗已恢复显示");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "更新悬浮窗布局时出错", e);
                    }
                }
            };
        }

        // 开始动画
        handler.post(animationSteps[0]);
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

        // 清除服务实例
        FloatingWindowServiceHolder.setService(null);

        // 释放媒体投影资源
        releaseMediaProjection();

        // 停止托管
        stopHosting();

        // 移除悬浮窗
        if (floatingView != null && floatingView.isShown()) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }

        // 移除展开视图
        if (expandedView != null && expandedView.isShown()) {
            windowManager.removeView(expandedView);
            expandedView = null;
        }

        // 释放模板管理器资源
        if (templateManager != null) {
            templateManager.release();
        }

        // 移除定时器任务
        if (autoHideHandler != null) {
            autoHideHandler.removeCallbacksAndMessages(null);
        }

        if (keepAliveHandler != null) {
            keepAliveHandler.removeCallbacksAndMessages(null);
        }

        if (hostingHandler != null) {
            hostingHandler.removeCallbacksAndMessages(null);
        }

        Log.d(TAG, "服务已销毁");
    }

    // 处理截图，分析支付信息
    private void processScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            Log.e(TAG, "截图为空");
            return;
        }

        // 检查是否是支付界面
        long startTime = System.currentTimeMillis();
        boolean isPaymentScreen = templateManager.isPaymentScreen(screenshot);
        Log.d(TAG, "支付界面检测耗时: " + (System.currentTimeMillis() - startTime) + "ms, 结果: " + isPaymentScreen);

        if (!isPaymentScreen) {
            Log.e(TAG, "当前不是支付界面");
            showInvalidInterfaceDialog();
            return;
        }

        // 识别二维码内容
        startTime = System.currentTimeMillis();
        String qrContent = recognizeQrCode(screenshot);
        Log.d(TAG, "二维码识别耗时: " + (System.currentTimeMillis() - startTime) + "ms");

        // 提取付款信息
        startTime = System.currentTimeMillis();
        String[] paymentInfo = templateManager.extractPaymentInfo(screenshot);
        Log.d(TAG, "付款信息提取耗时: " + (System.currentTimeMillis() - startTime) + "ms");

        if (paymentInfo == null || paymentInfo.length < 3) {
            Log.e(TAG, "提取支付信息失败");
            return;
        }

        // 根据utils包中的PaymentTemplateManager调整索引顺序
        String timeText = paymentInfo[0]; // 时间在第一个位置
        String cardNumber = paymentInfo[1]; // 卡号在第二个位置
        String balance = paymentInfo[2]; // 余额在第三个位置

        Log.d(TAG, "提取信息 - 时间: " + timeText + ", 卡号: " + cardNumber + ", 余额: " + balance);

        // 检查是否需要通知（新二维码或即将过期）
        boolean isNewQrCode = !qrContent.isEmpty() && !qrContent.equals(lastQrCodeContent);
        boolean isInfoChanged = !cardNumber.equals(lastCardNumber) || !balance.equals(lastBalance);
        boolean isExpiring = templateManager.isQrCodeExpiring(timeText);

        Log.d(TAG, "状态 - 新二维码: " + isNewQrCode + ", 信息变化: " + isInfoChanged + ", 即将过期: " + isExpiring);

        // 如果识别到新信息或信息变化，更新存储的数据
        if (isNewQrCode || isInfoChanged || isExpiring) {
            // 更新信息
            if (!qrContent.isEmpty()) {
                lastQrCodeContent = qrContent;
            }
            lastCardNumber = cardNumber;
            lastBalance = balance;
            lastQrCodeExpiryTime = parseExpiryTime(timeText);

            // 记录信息
            Log.i(TAG, "---------- 支付信息更新 ----------");
            if (!qrContent.isEmpty()) {
                Log.i(TAG, String.format("二维码内容: %s", qrContent));
            }
            Log.i(TAG, String.format("卡号: %s", cardNumber));
            Log.i(TAG, String.format("余额: %s", balance));
            Log.i(TAG, String.format("剩余时间: %s", timeText));

            // 通知UI更新（可以通过Toast显示）
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "已识别到支付码信息: " + cardNumber + " / " + balance, Toast.LENGTH_SHORT).show();
            });

            // 如果即将过期，增加警告日志
            if (isExpiring) {
                Log.w(TAG, "⚠️ 二维码即将过期，请注意更新!");
                // 可以在这里添加发送通知或振动提醒的代码
                showExpiringNotification();
            }
        } else {
            // 如果信息没有变化，减少日志输出
            Log.d(TAG, "支付信息未变化，继续监控中...");
        }
    }

    // 显示二维码即将过期通知
    private void showExpiringNotification() {
        try {
            // 在主线程上运行
            new Handler(Looper.getMainLooper()).post(() -> {
                // 显示Toast提醒
                Toast.makeText(this, "⚠️ 二维码即将过期，请注意更新!", Toast.LENGTH_LONG).show();

                // 可以增加振动提醒
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(
                            android.os.VibrationEffect.createOneShot(500,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(500);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "显示过期通知失败: " + e.getMessage());
        }
    }

    // 开始二维码托管
    private void startHosting() {
        if (isHosting) {
            Log.d(TAG, "托管已在进行中，不重复启动");
            return;
        }

        isHosting = true;

        // 清除之前的信息记录
        lastQrCodeContent = null;
        lastCardNumber = null;
        lastBalance = null;
        lastQrCodeExpiryTime = 0;

        Log.i(TAG, "开始托管支付码...");

        // 初始化Handler用于定期任务
        if (hostingHandler == null) {
            hostingHandler = new Handler(Looper.getMainLooper());
        }

        // 创建一个Runnable用于截图任务
        screenshotRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isHosting) {
                    Log.d(TAG, "托管已停止，取消截图任务");
                    return;
                }

                try {
                    // 捕获屏幕
                    Bitmap screenshot = takeScreenshot();

                    if (screenshot != null) {
                        // 记录截图尺寸
                        Log.d(TAG, "截图尺寸: " + screenshot.getWidth() + "x" + screenshot.getHeight());

                        // 处理截图
                        processScreenshot(screenshot);

                        // 回收位图避免内存泄漏
                        screenshot.recycle();
                    } else {
                        Log.w(TAG, "截图捕获失败");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "截图任务执行错误: " + e.getMessage(), e);
                } finally {
                    // 继续下一次截图，无论成功与否
                    if (isHosting && hostingHandler != null) {
                        hostingHandler.postDelayed(this, HOSTING_INTERVAL);
                    }
                }
            }
        };

        // 立即开始第一次截图任务
        if (hostingHandler != null) {
            hostingHandler.post(screenshotRunnable);
        }

        // 隐藏悬浮窗
        if (floatingView != null && floatingView.isShown()) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败: " + e.getMessage());
            }
        }

        // 显示取消托管按钮
        showCancelHostingButton();

        Toast.makeText(this, "已开始托管支付码", Toast.LENGTH_SHORT).show();
    }

    // 显示取消托管按钮
    private void showCancelHostingButton() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        cancelHostingView = inflater.inflate(R.layout.cancel_hosting_button, null);

        Button btnCancelHosting = cancelHostingView.findViewById(R.id.btnCancelHosting);
        btnCancelHosting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopHosting();
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;

        try {
            windowManager.addView(cancelHostingView, params);
        } catch (Exception e) {
            Log.e(TAG, "添加取消托管按钮时出错", e);
        }
    }

    // 截取屏幕
    public Bitmap takeScreenshot() {
        // 一些设备上可能需要额外权限或实现
        // 应该使用MediaProjection API进行屏幕截图
        try {
            // 如果已初始化MediaProjection，尝试捕获实际屏幕
            if (mediaProjectionInitialized && lastScreenshot != null) {
                // 返回深拷贝以避免并发修改
                Bitmap screenshot = lastScreenshot.copy(lastScreenshot.getConfig(), true);
                Log.d(TAG, "成功捕获屏幕截图: " + screenshot.getWidth() + "x" + screenshot.getHeight());
                return screenshot;
            } else {
                // 如果10秒内没有日志，则记录这个消息
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 10000) {
                    if (!mediaProjectionInitialized) {
                        Log.w(TAG, "MediaProjection未初始化，无法截屏，将使用模板图像");
                    } else {
                        Log.w(TAG, "截图未准备好，将使用模板图像");
                    }
                    lastLogTime = currentTime;

                    // 请求截屏权限
                    requestScreenCapturePermission();
                }
            }

            // 如果无法获取实际截图，则使用模板图像进行测试
            if (templateManager != null) {
                Bitmap template = templateManager.getTemplate(PaymentTemplateManager.TEMPLATE_PAYMENT);
                if (template != null) {
                    // 返回深拷贝以避免并发修改
                    return template.copy(template.getConfig(), true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "截屏时发生错误: " + e.getMessage(), e);
        }

        return null;
    }

    // 请求屏幕捕获权限
    private void requestScreenCapturePermission() {
        Log.d(TAG, "需要请求屏幕捕获权限，请在MainActivity中启动权限请求");
        // 通知MainActivity请求权限
        Intent intent = new Intent("com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION");
        sendBroadcast(intent);
    }

    // 释放屏幕捕获资源
    private void releaseMediaProjection() {
        try {
            Log.d(TAG, "正在释放MediaProjection资源");
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            if (screenshotHandler != null && screenshotHandler.getLooper() != null) {
                screenshotHandler.getLooper().quit();
                screenshotHandler = null;
            }
            if (lastScreenshot != null) {
                lastScreenshot.recycle();
                lastScreenshot = null;
            }
            mediaProjectionInitialized = false;
            Log.d(TAG, "MediaProjection资源已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放MediaProjection资源时出错: " + e.getMessage(), e);
        }
    }

    // 显示无效界面对话框
    private void showInvalidInterfaceDialog() {
        // 确保在主线程运行
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // 使用系统提醒对话框样式
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(this);
                }

                builder.setTitle("无效的支付界面")
                        .setMessage("当前不是有效的支付码界面，请确保您处于支付宝支付码显示页面")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("确定", (dialog, which) -> {
                            dialog.dismiss();
                            // 停止托管，因为当前不是有效界面
                            stopHosting();
                        })
                        .setCancelable(false); // 强制用户确认

                AlertDialog dialog = builder.create();
                // 设置对话框显示层级，确保它显示在其他窗口之上
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setType(getWindowLayoutType());
                }
                dialog.show();

                // 显示Toast提醒，以防对话框未显示
                Toast.makeText(this, "当前不是有效的支付码界面", Toast.LENGTH_LONG).show();

                // 在3秒后自动关闭对话框（防止用户无响应）
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        // 停止托管，因为当前不是有效界面
                        stopHosting();
                    }
                }, 3000);
            } catch (Exception e) {
                Log.e(TAG, "显示无效界面对话框时出错", e);
                // 如果对话框显示失败，确保通过Toast通知用户
                Toast.makeText(this, "当前不是有效的支付码界面，托管已停止", Toast.LENGTH_LONG).show();
                // 直接停止托管
                stopHosting();
            }
        });
    }

    // 识别二维码内容
    private String recognizeQrCode(Bitmap screenshot) {
        try {
            // 使用ML Kit的条码扫描功能
            com.google.mlkit.vision.barcode.BarcodeScanner scanner = com.google.mlkit.vision.barcode.BarcodeScanning
                    .getClient(new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                            .build());

            // 原始图像可能太大，裁剪二维码区域以提高识别率
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();

            // 二维码通常在屏幕中间偏上位置，计算裁剪区域
            int cropX = width / 4;
            int cropY = height / 4;
            int cropWidth = width / 2;
            int cropHeight = width / 2; // 使用正方形区域，因为二维码是正方形的

            // 确保裁剪区域不超出边界
            if (cropX + cropWidth > width)
                cropWidth = width - cropX;
            if (cropY + cropHeight > height)
                cropHeight = height - cropY;

            // 裁剪二维码区域
            Bitmap qrCodeBitmap = null;
            try {
                qrCodeBitmap = Bitmap.createBitmap(screenshot, cropX, cropY, cropWidth, cropHeight);
                Log.d(TAG, "已裁剪二维码区域: " + cropX + "," + cropY + "," + cropWidth + "," + cropHeight);
            } catch (Exception e) {
                Log.e(TAG, "裁剪二维码区域失败，使用原图: " + e.getMessage());
                qrCodeBitmap = screenshot;
            }

            com.google.mlkit.vision.common.InputImage inputImage = com.google.mlkit.vision.common.InputImage
                    .fromBitmap(qrCodeBitmap, 0);

            // 创建一个信号量来等待异步操作完成
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final StringBuilder result = new StringBuilder();

            // 执行二维码扫描
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (com.google.mlkit.vision.barcode.common.Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.isEmpty()) {
                                result.append(rawValue);
                                Log.d(TAG, "成功识别二维码: " + rawValue);
                                break; // 只处理第一个找到的二维码
                            }
                        }
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "二维码识别失败: " + e.getMessage());
                        latch.countDown();
                    });

            // 等待最多2秒
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS);

            // 释放资源
            scanner.close();

            // 回收裁剪图像
            if (qrCodeBitmap != null && qrCodeBitmap != screenshot) {
                qrCodeBitmap.recycle();
            }

            if (result.length() > 0) {
                return result.toString();
            } else {
                // 如果没有识别到，返回空字符串
                Log.w(TAG, "未能识别二维码");
                return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "二维码识别过程中出错: " + e.getMessage(), e);
            // 发生错误时返回空字符串
            return "";
        }
    }

    // 解析过期时间
    private long parseExpiryTime(String timeText) {
        try {
            // 假设时间格式为 "00:59"，表示还有59秒过期
            String[] parts = timeText.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return System.currentTimeMillis() + (minutes * 60 + seconds) * 1000;
            }
        } catch (Exception e) {
            Log.e(TAG, "解析过期时间出错", e);
        }

        // 默认1分钟后过期
        return System.currentTimeMillis() + 60000;
    }

    // 接收MediaProjection权限结果
    public void setMediaProjectionResult(Intent data) {
        Log.d(TAG, "收到MediaProjection权限结果");
        this.resultData = data;
        // 初始化媒体投影
        initMediaProjection(data);

        // 如果当前正在托管，重新开始托管任务以使用新的权限
        if (isHosting && hostingHandler != null) {
            Log.d(TAG, "托管任务已在运行，将使用新授权的截屏能力");
        }
    }

    // 初始化屏幕捕获
    private void initMediaProjection(Intent data) {
        if (data == null) {
            Log.e(TAG, "初始化MediaProjection失败：数据为空");
            return;
        }

        try {
            Log.d(TAG, "开始初始化MediaProjection");
            // 释放旧的资源
            releaseMediaProjection();

            // 获取屏幕信息
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenDensity = metrics.densityDpi;
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;

            // 创建ImageReader来接收屏幕图像
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                try {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        try {
                            // 转换为Bitmap
                            Bitmap bitmap = imageToBitmap(image);
                            if (bitmap != null) {
                                // 保存最后一次截图
                                if (lastScreenshot != null) {
                                    lastScreenshot.recycle();
                                }
                                lastScreenshot = bitmap;
                            }
                        } finally {
                            image.close();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "处理截图失败: " + e.getMessage(), e);
                }
            }, null);

            // 创建MediaProjection
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);

            if (mediaProjection == null) {
                Log.e(TAG, "无法创建MediaProjection实例");
                mediaProjectionInitialized = false;
                return;
            }

            // 创建VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    null);

            if (virtualDisplay == null) {
                Log.e(TAG, "无法创建VirtualDisplay");
                releaseMediaProjection();
                mediaProjectionInitialized = false;
                return;
            }

            Log.i(TAG, "MediaProjection初始化成功，屏幕尺寸: " + screenWidth + "x" + screenHeight);
            mediaProjectionInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
            releaseMediaProjection();
            mediaProjectionInitialized = false;
        }
    }

    // 停止托管
    private void stopHosting() {
        if (!isHosting) {
            return;
        }

        isHosting = false;

        // 停止截图定时任务
        if (hostingHandler != null) {
            hostingHandler.removeCallbacksAndMessages(null);
        }

        // 隐藏取消托管按钮
        if (cancelHostingView != null) {
            try {
                if (cancelHostingView.isShown()) {
                    windowManager.removeView(cancelHostingView);
                }
            } catch (Exception e) {
                Log.e(TAG, "移除取消托管按钮时出错", e);
            }
            cancelHostingView = null;
        }

        // 显示悬浮窗
        if (floatingView != null && !floatingView.isShown()) {
            try {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getWindowLayoutType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                params.x = 0;
                params.y = 0;
                windowManager.addView(floatingView, params);
            } catch (Exception e) {
                // 视图可能已经添加
                Log.e(TAG, "添加悬浮窗时出错", e);
            }
        }

        Toast.makeText(this, "已停止托管支付码", Toast.LENGTH_SHORT).show();
    }

    // 将Image转换为Bitmap
    private Bitmap imageToBitmap(Image image) {
        if (image == null) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        if (planes.length == 0) {
            return null;
        }

        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // 如果有padding，裁剪为正确的尺寸
        if (rowPadding > 0) {
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, image.getWidth(), image.getHeight());
            bitmap.recycle();
            return croppedBitmap;
        }

        return bitmap;
    }
}