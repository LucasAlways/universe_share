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
import androidx.annotation.RequiresApi;
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
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Timer;
import java.util.TimerTask;

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
    private static final long AUTO_HIDE_DELAY = 3000; // 3秒后自动隐藏

    // 用于保持服务存活的定时器
    private Handler keepAliveHandler;
    private Runnable keepAliveRunnable;
    private static final long KEEP_ALIVE_INTERVAL = 10 * 60 * 1000; // 10分钟保活间隔

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
    private PaymentTemplateManager paymentRecognizer; // 改名为paymentRecognizer，更符合功能
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
    private static final String SCREENSHOT_THREAD_NAME = "ScreenshotThread";
    private Intent resultData; // 保存MediaProjection权限结果
    private long lastLogTime = 0; // 上次记录日志的时间
    private boolean mediaProjectionInitialized = false; // 标记MediaProjection是否已初始化
    private Bitmap lastScreenshot = null; // 最后一次截图
    private Runnable screenshotRunnable; // 持有截图任务的引用
    private static final long HOSTING_INTERVAL = 1000; // 托管任务间隔
    private boolean isMediaProjectionInitialized = false; // 标记MediaProjection是否已初始化
    private Timer timer;

    // 广播Action
    public static final String ACTION_REQUEST_SCREENSHOT_PERMISSION = "com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION";

    // 在类的成员变量部分添加失败计数器
    private int screenshotFailCount = 0;
    private static final int MAX_SCREENSHOT_RETRY = 15; // 最大重试次数

    @Override
    public void onCreate() {
        super.onCreate();

        // 注册服务实例
        FloatingWindowServiceHolder.setService(this);

        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取屏幕宽度，用于计算边缘位置
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        // 初始化支付识别器
        paymentRecognizer = new PaymentTemplateManager(this);

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
        Log.d(TAG, "服务onStartCommand: " + (intent != null ? intent.getAction() : "无action"));

        try {
            // 创建通知渠道
            createNotificationChannel();

            // 创建通知
            Notification notification = createNotification();

            // 先尝试使用基本前台服务启动
            try {
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "使用基本前台服务类型启动成功");
            } catch (Exception e) {
                Log.e(TAG, "启动前台服务失败: " + e.getMessage());
                // 如果连基本前台服务都启动失败，记录错误但继续执行
            }

            // 初始化悬浮窗
            if (windowManager == null) {
                initFloatingWindow();
            }

            // 显示悬浮窗并初始化MediaProjection（如果有权限）
            showFloatingWindow();

            // 如果已经有MediaProjection权限，延迟初始化
            if (resultData != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 使用mediaProjection前，先尝试更新前台服务类型包含MEDIA_PROJECTION
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
                                updateForegroundServiceType(serviceType);
                                Log.d(TAG, "更新前台服务类型包含MEDIA_PROJECTION");
                            } catch (Exception e) {
                                Log.e(TAG, "更新前台服务类型失败: " + e.getMessage());
                                // 继续尝试初始化MediaProjection，可能会因权限不足而失败
                            }
                        }

                        initMediaProjection(resultData);
                    } catch (Exception e) {
                        Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
                    }
                }, 500);
            }

            // 开始定时器保持服务活跃
            startKeepAliveTimer();

            return START_STICKY;
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand异常: " + e.getMessage(), e);
            return START_NOT_STICKY;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("用于支持悬浮窗和屏幕截图的通知");
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "通知渠道创建成功");
            }
        }
    }

    private void startKeepAliveTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 每5分钟检查一次服务状态
                Log.d(TAG, "服务保活检查 - 服务正在运行");
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000);
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

            // 确保窗口管理器已初始化
            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
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

            // 创建现代风格的悬浮视图（如果尚未创建）
            if (floatingView == null) {
                createModernFloatingView(params);
            }

            // 确认悬浮窗是否成功创建
            if (floatingView != null) {
                Log.d("FloatingWindowService", "悬浮窗视图创建成功");

                // 检查悬浮窗是否已添加到窗口管理器
                boolean isFloatingViewAttached = floatingView.getParent() != null;
                if (!isFloatingViewAttached) {
                    try {
                        windowManager.addView(floatingView, params);
                        Log.d("FloatingWindowService", "成功添加悬浮窗视图到窗口管理器");
                    } catch (Exception e) {
                        Log.e("FloatingWindowService", "添加悬浮窗失败: " + e.getMessage(), e);
                        Toast.makeText(this, "添加悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("FloatingWindowService", "悬浮窗视图已存在，不重复添加");
                    try {
                        // 更新布局参数
                        windowManager.updateViewLayout(floatingView, params);
                        Log.d("FloatingWindowService", "更新悬浮窗布局参数");
                    } catch (Exception e) {
                        Log.e("FloatingWindowService", "更新悬浮窗布局失败: " + e.getMessage(), e);
                    }
                }

                // 测试悬浮窗可见性
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (floatingView != null && floatingView.isShown()) {
                        Log.d(TAG, "悬浮窗显示正常");
                        Toast.makeText(this, "二维码托管服务已启动", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "悬浮窗未正常显示");
                    }
                }, 1000);

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

    // 获取窗口布局类型
    private int getWindowLayoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    // 初始化悬浮窗
    private void initFloatingWindow() {
        try {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            createModernFloatingView(null);
            Log.d(TAG, "悬浮窗初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化悬浮窗失败: " + e.getMessage(), e);
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

        // 释放识别器资源
        if (paymentRecognizer != null) {
            paymentRecognizer.release();
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

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        Log.d(TAG, "服务已销毁");
    }

    // 处理截图，分析支付信息
    private void processScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            Log.e(TAG, "截图为空");
            return;
        }

        // 保存截图
        saveScreenshotToFile(screenshot);

        // 检查是否是支付界面
        long startTime = System.currentTimeMillis();
        boolean isPaymentScreen = paymentRecognizer.isPaymentScreen(screenshot);
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
        String[] paymentInfo = paymentRecognizer.extractPaymentInfo(screenshot);
        Log.d(TAG, "付款信息提取耗时: " + (System.currentTimeMillis() - startTime) + "ms");

        if (paymentInfo == null || paymentInfo.length < 3) {
            Log.e(TAG, "提取支付信息失败");
            return;
        }

        // 获取提取的信息
        String timeText = paymentInfo[0]; // 时间在第一个位置
        String cardNumber = paymentInfo[1]; // 卡号在第二个位置
        String balance = paymentInfo[2]; // 余额在第三个位置

        Log.d(TAG, "提取信息 - 时间: " + timeText + ", 卡号: " + cardNumber + ", 余额: " + balance);

        // 检查是否需要通知（新二维码或即将过期）
        boolean isNewQrCode = !qrContent.isEmpty() && !qrContent.equals(lastQrCodeContent);
        boolean isInfoChanged = !cardNumber.equals(lastCardNumber) || !balance.equals(lastBalance);
        boolean isExpiring = paymentRecognizer.isQrCodeExpiring(timeText);

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
    public void startHosting() {
        if (isHosting) {
            return; // 避免重复启动
        }

        Log.i(TAG, "开始托管任务");
        isHosting = true;

        // 重置截图失败次数
        screenshotFailCount = 0;

        // 尝试启动截图处理线程
        if (screenshotHandler == null) {
            try {
                HandlerThread handlerThread = new HandlerThread(SCREENSHOT_THREAD_NAME);
                handlerThread.start();
                screenshotHandler = new Handler(handlerThread.getLooper());
            } catch (Exception e) {
                Log.e(TAG, "创建截图线程失败: " + e.getMessage(), e);
            }
        }

        // 首次检查是否有截图权限
        if (!isMediaProjectionInitialized || mediaProjection == null) {
            Log.w(TAG, "无截图权限，请求一次性权限");
            // 先尝试加载已保存的权限数据
            if (resultData != null) {
                Log.d(TAG, "使用保存的MediaProjection权限数据");
                initMediaProjection(resultData);
            } else {
                // 如果没有保存的权限数据，请求新权限
                requestScreenCapturePermission();
                // 显示等待授权UI
                updateScreenshotStatusUI(true, "等待用户授权截图...");

                // 为避免阻塞当前方法，下面的托管启动流程仍然会继续
                // 当权限获得后，服务会自动使用新权限
            }
        }

        // 显示取消托管按钮
        showCancelHostingButton();

        // 创建托管处理线程
        if (hostingHandler == null) {
            hostingHandler = new Handler();
        }

        // 定义截图任务
        screenshotRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isHosting) {
                    Log.d(TAG, "托管已停止，不再执行截图任务");
                    return;
                }

                try {
                    // 检查截图计数，如果超出最大重试次数则停止托管
                    if (screenshotFailCount >= MAX_SCREENSHOT_RETRY) {
                        Log.e(TAG, "截图失败次数过多（" + MAX_SCREENSHOT_RETRY + "次），停止尝试");
                        // 更新UI显示错误
                        updateScreenshotStatusUI(true, "截图失败次数过多，请检查权限或重启应用");
                        // 停止托管
                        stopHosting();
                        return;
                    }

                    // 只有在初次失败超过5次后，才尝试重新请求权限
                    if (screenshotFailCount > 5 && screenshotFailCount % 5 == 0 && !isMediaProjectionInitialized
                            && resultData == null) {
                        Log.w(TAG, "多次截图失败且无权限，请求权限");
                        requestScreenCapturePermission();
                        // 显示等待授权UI
                        updateScreenshotStatusUI(true, "等待用户授权截图...");
                        // 延迟继续尝试
                        hostingHandler.postDelayed(this, HOSTING_INTERVAL * 2);
                        return;
                    }

                    Log.d(TAG, "执行截图任务");
                    Bitmap screenshot = takeScreenshot();

                    if (screenshot != null) {
                        // 截图成功，重置失败计数
                        screenshotFailCount = 0;
                        updateScreenshotStatusUI(false, "截图成功，持续监控中...");
                        // 处理截图
                        processScreenshot(screenshot);
                    } else {
                        // 截图失败，增加失败计数
                        screenshotFailCount++;
                        Log.w(TAG, "无法获取截图，失败次数: " + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY);
                        updateScreenshotStatusUI(true,
                                "截图失败，重试中... (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");

                        // 如果已经有权限数据但截图失败，可能是权限过期，每3次失败尝试重新初始化
                        if (resultData != null && (mediaProjection == null || !isMediaProjectionInitialized)
                                && screenshotFailCount % 3 == 0) {
                            Log.d(TAG, "尝试重新初始化权限数据");
                            // 重新初始化MediaProjection
                            initMediaProjection(resultData);
                        }
                    }

                    // 安排下一次截图任务
                    if (isHosting) {
                        hostingHandler.postDelayed(this, HOSTING_INTERVAL);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "执行截图任务时出错: " + e.getMessage(), e);
                    hostingHandler.postDelayed(this, HOSTING_INTERVAL * 2); // 出错后延长间隔
                }
            }
        };

        // 启动截图任务
        hostingHandler.post(screenshotRunnable);
        Log.i(TAG, "托管任务已启动");
    }

    // 停止托管任务
    public void stopHosting() {
        Log.i(TAG, "停止托管任务");
        isHosting = false;

        // 重置截图失败计数
        screenshotFailCount = 0;

        // 清理截图任务
        screenshotRunnable = null;

        // 恢复UI
        new Handler(Looper.getMainLooper()).post(() -> {
            if (floatingView != null) {
                floatingView.setVisibility(View.VISIBLE);
            }
            if (cancelHostingView != null) {
                cancelHostingView.setVisibility(View.GONE);
            }

            Toast.makeText(this, "托管已停止", Toast.LENGTH_SHORT).show();
        });
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

    // 截取屏幕截图
    public Bitmap takeScreenshot() {
        if (resultData == null) {
            Log.e(TAG, "无法截图: 没有MediaProjection权限数据");
            screenshotFailCount++;
            updateScreenshotStatusUI(true, "截图失败 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
            requestScreenCapturePermission();
            return null;
        }

        Log.d(TAG, "开始截取屏幕");

        // 保存截图操作的时间戳
        final long startTime = System.currentTimeMillis();

        ImageReader localImageReader = null;
        VirtualDisplay localVirtualDisplay = null;
        MediaProjection localMediaProjection = null;
        Bitmap bitmap = null;

        try {
            // 创建ImageReader
            localImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);

            // 创建图像可用的闭锁和引用
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Image> imageRef = new AtomicReference<>();

            // 创建监听器 - 必须在创建VirtualDisplay之前设置
            ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
                try {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        imageRef.set(image);
                        latch.countDown();
                    } else {
                        latch.countDown();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "获取图像时出错: " + e.getMessage(), e);
                    latch.countDown();
                }
            };

            // 注意：在创建VirtualDisplay之前设置监听器
            localImageReader.setOnImageAvailableListener(onImageAvailableListener, new Handler(Looper.getMainLooper()));

            try {
                // 对每次截图使用新的MediaProjection实例
                MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
                localMediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, resultData);

                if (localMediaProjection == null) {
                    Log.e(TAG, "无法创建新的MediaProjection实例");
                    screenshotFailCount++;
                    updateScreenshotStatusUI(true,
                            "无法创建MediaProjection实例 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
                    requestScreenCapturePermission();
                    return null;
                }

                // 创建VirtualDisplay - 每次截图使用唯一的名称
                String displayName = "ScreenCapture_" + System.currentTimeMillis();
                localVirtualDisplay = localMediaProjection.createVirtualDisplay(
                        displayName,
                        screenWidth, screenHeight, screenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, // 使用AUTO_MIRROR
                        localImageReader.getSurface(), null, null);

                if (localVirtualDisplay == null) {
                    Log.e(TAG, "无法创建VirtualDisplay");
                    screenshotFailCount++;
                    updateScreenshotStatusUI(true,
                            "无法创建虚拟显示 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
                    return null;
                }

                // 等待图像可用，最多等待500毫秒
                if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "等待图像超时");
                    screenshotFailCount++;
                    updateScreenshotStatusUI(true, "等待图像超时 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
                    return null;
                }

                Image image = imageRef.get();
                if (image != null) {
                    bitmap = imageToBitmap(image);
                    image.close();

                    // 截图成功，重置失败计数
                    if (bitmap != null) {
                        screenshotFailCount = 0;
                        updateScreenshotStatusUI(false, "截图成功");
                    }
                }

                Log.d(TAG, "截图完成，耗时: " + (System.currentTimeMillis() - startTime) + "ms");
            } catch (SecurityException se) {
                Log.e(TAG, "截图安全权限错误: " + se.getMessage(), se);
                screenshotFailCount++;
                updateScreenshotStatusUI(true, "权限错误 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");

                // 请求新的权限
                requestScreenCapturePermission();
                return null;
            } catch (Exception e) {
                Log.e(TAG, "创建VirtualDisplay或获取图像时出错: " + e.getMessage(), e);
                screenshotFailCount++;
                updateScreenshotStatusUI(true, "截图出错 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "截图过程中出错: " + e.getMessage(), e);
            screenshotFailCount++;
            updateScreenshotStatusUI(true, "截图失败 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
            return null;
        } finally {
            // 在函数返回前释放资源
            if (localVirtualDisplay != null) {
                localVirtualDisplay.release();
            }
            if (localImageReader != null) {
                localImageReader.close();
            }
            if (localMediaProjection != null) {
                localMediaProjection.stop();
            }
        }

        return bitmap;
    }

    // 释放MediaProjection相关资源，包括所有创建的对象
    private void releaseMediaProjection() {
        try {
            Log.d(TAG, "正在释放MediaProjection资源");

            // 释放VirtualDisplay
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }

            // 释放ImageReader
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }

            // 释放MediaProjection
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }

            // 释放Handler和Looper
            if (screenshotHandler != null && screenshotHandler.getLooper() != null) {
                screenshotHandler.getLooper().quit();
                screenshotHandler = null;
            }

            // 释放Bitmap
            if (lastScreenshot != null) {
                lastScreenshot.recycle();
                lastScreenshot = null;
            }

            isMediaProjectionInitialized = false;
            Log.d(TAG, "MediaProjection资源已完全释放");
        } catch (Exception e) {
            Log.e(TAG, "释放MediaProjection资源时出错: " + e.getMessage(), e);
        }
    }

    // 只释放MediaProjection资源，保留其他
    private void releaseMediaProjectionResources() {
        try {
            Log.d(TAG, "正在释放MediaProjection资源");
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            isMediaProjectionInitialized = false;
            Log.d(TAG, "MediaProjection资源已释放");
        } catch (Exception e) {
            Log.e(TAG, "释放MediaProjection资源时出错: " + e.getMessage(), e);
        }
    }

    // 请求屏幕捕获权限
    private void requestScreenCapturePermission() {
        try {
            // 广播请求
            Intent intent = new Intent(ACTION_REQUEST_SCREENSHOT_PERMISSION);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
            Log.d(TAG, "需要请求屏幕捕获权限，请在MainActivity中启动权限请求");

            // 使用交互提示
            Toast.makeText(this, "需要截屏权限，请点击授权", Toast.LENGTH_LONG).show();

            // 如果已经在托管，显示等待提示
            if (isHosting) {
                updateScreenshotStatusUI(true, "等待用户授权中...");
            }

            // 额外的保险措施：尝试直接启动MainActivity
            try {
                Intent activityIntent = new Intent(this, MainActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityIntent.putExtra("action", "requestScreenshotPermission");
                startActivity(activityIntent);
            } catch (Exception e) {
                Log.e(TAG, "无法启动主活动请求权限: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "请求截屏权限失败: " + e.getMessage(), e);
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

    // 初始化媒体投影参数和权限
    // 注意：MediaProjection权限具有一定的时效性，超过一定时间后可能需要重新获取
    // 权限数据由MainActivity保存和加载，该Service可以使用保存的权限直到它过期
    private void initMediaProjection(Intent data) {
        try {
            if (data == null) {
                Log.e(TAG, "初始化MediaProjection失败: 数据为空");
                Toast.makeText(this, "截屏权限未获取", Toast.LENGTH_SHORT).show();
                isMediaProjectionInitialized = false;
                return;
            }

            Log.d(TAG, "开始初始化MediaProjection");

            // 先释放之前的资源
            releaseMediaProjection();

            // 保存权限数据，用于后续创建MediaProjection实例
            this.resultData = data;

            // 设置屏幕参数
            screenDensity = getResources().getDisplayMetrics().densityDpi;
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            screenWidth = size.x;
            screenHeight = size.y;

            // 创建MediaProjection实例
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);

            try {
                // 创建新的MediaProjection实例
                mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);

                if (mediaProjection == null) {
                    Log.e(TAG, "无法获取MediaProjection，请确保用户已授权");
                    Toast.makeText(this, "无法获取截屏权限", Toast.LENGTH_SHORT).show();
                    isMediaProjectionInitialized = false;
                    return;
                }

                // 注册回调监听投影状态
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.d(TAG, "MediaProjection已停止");
                        isMediaProjectionInitialized = false;
                        mediaProjection = null;
                    }
                }, new Handler(Looper.getMainLooper()));

                // 将MediaProjection标记为已初始化
                isMediaProjectionInitialized = true;

                // 设置第二个标志位也为true，确保一致性
                mediaProjectionInitialized = true;

                Log.i(TAG,
                        "MediaProjection初始化成功: 屏幕分辨率 " + screenWidth + "x" + screenHeight + ", 密度: " + screenDensity);

                // 重置截图失败计数
                screenshotFailCount = 0;
            } catch (Exception e) {
                Log.e(TAG, "创建MediaProjection失败: " + e.getMessage(), e);
                isMediaProjectionInitialized = false;
                mediaProjectionInitialized = false;
                Toast.makeText(this, "截屏权限初始化失败", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
            Toast.makeText(this, "截屏功能初始化错误", Toast.LENGTH_SHORT).show();
            isMediaProjectionInitialized = false;
            mediaProjectionInitialized = false;
        }
    }

    // 接收MediaProjection权限结果
    public void setMediaProjectionResult(Intent data) {
        if (data == null) {
            Log.e(TAG, "收到空的MediaProjection数据");
            return;
        }

        try {
            this.resultData = data;
            Log.d(TAG, "收到MediaProjection权限数据");

            // 确保在主线程中执行
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    // 尝试更新前台服务类型
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            // 只使用MEDIA_PROJECTION类型，避免使用SPECIAL_USE类型导致的权限问题
                            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
                            updateForegroundServiceType(serviceType);
                            Log.d(TAG, "已更新前台服务类型为MEDIA_PROJECTION");

                            // 延迟初始化，确保前台服务类型已生效
                            new Handler().postDelayed(() -> {
                                try {
                                    initMediaProjection(data);
                                    showScreenshotPermissionGrantedUI();
                                } catch (Exception e) {
                                    Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
                                    Toast.makeText(this, "截屏功能初始化失败", Toast.LENGTH_SHORT).show();
                                }
                            }, 500);
                        } catch (Exception e) {
                            Log.e(TAG, "更新前台服务类型失败: " + e.getMessage(), e);
                            // 尝试直接初始化，可能会因缺少权限而失败
                            try {
                                initMediaProjection(data);
                                showScreenshotPermissionGrantedUI();
                            } catch (Exception ex) {
                                Log.e(TAG, "初始化MediaProjection失败: " + ex.getMessage(), ex);
                                Toast.makeText(this, "截屏功能初始化失败，请检查权限设置", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // Android 9及以下没有前台服务类型限制
                        Log.d(TAG, "Android 9及以下，直接初始化MediaProjection");
                        initMediaProjection(data);
                        showScreenshotPermissionGrantedUI();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "设置MediaProjection权限时发生错误: " + e.getMessage(), e);
                    Toast.makeText(this, "截屏权限设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "处理MediaProjection结果异常: " + e.getMessage(), e);
            Toast.makeText(this, "截屏初始化错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void showScreenshotPermissionGrantedUI() {
        Toast.makeText(this, "截屏权限已获取，开始监控", Toast.LENGTH_SHORT).show();

        // 显示取消按钮
        if (cancelHostingView != null && cancelHostingView.getVisibility() != View.VISIBLE) {
            cancelHostingView.setVisibility(View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateForegroundServiceType(int serviceType) {
        try {
            Log.d(TAG, "更新前台服务类型为: " + serviceType);

            // 更新通知，确保服务类型变更生效
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification, serviceType);
            Log.d(TAG, "前台服务类型已更新");
        } catch (Exception e) {
            Log.e(TAG, "更新前台服务类型时出错: " + e.getMessage(), e);
        }
    }

    // 开始截图任务
    private void startScreenshotTask() {
        if (mediaProjectionInitialized && mediaProjection != null && screenshotHandler != null) {
            screenshotHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mediaProjectionInitialized || mediaProjection == null) {
                        Log.d(TAG, "MediaProjection已失效，停止截图任务");
                        return;
                    }

                    try {
                        // 每次截图前先创建新的ImageReader
                        if (imageReader != null) {
                            imageReader.close();
                        }
                        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

                        // 先设置监听器，再创建虚拟显示（顺序很重要）
                        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                        final Bitmap[] capturedBitmap = new Bitmap[1];

                        imageReader.setOnImageAvailableListener(reader -> {
                            try {
                                Image image = reader.acquireLatestImage();
                                if (image != null) {
                                    try {
                                        // 转换为Bitmap
                                        capturedBitmap[0] = imageToBitmap(image);
                                    } finally {
                                        image.close();
                                        // 通知已获取图像
                                        latch.countDown();
                                    }
                                } else {
                                    latch.countDown();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "处理截图失败: " + e.getMessage(), e);
                                latch.countDown();
                            }
                        }, screenshotHandler);

                        // 每次截图前创建新的VirtualDisplay
                        if (virtualDisplay != null) {
                            virtualDisplay.release();
                            virtualDisplay = null;
                        }

                        try {
                            // 创建虚拟显示
                            virtualDisplay = mediaProjection.createVirtualDisplay(
                                    "ScreenCapture",
                                    screenWidth,
                                    screenHeight,
                                    screenDensity,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    imageReader.getSurface(),
                                    null,
                                    screenshotHandler);

                            if (virtualDisplay == null) {
                                Log.e(TAG, "无法创建VirtualDisplay");
                                latch.countDown();
                                return;
                            }

                            // 等待图像可用，最多3秒
                            boolean success = latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
                            if (!success) {
                                Log.w(TAG, "等待截图超时");
                            }

                            // 获取截图
                            if (capturedBitmap[0] != null) {
                                // 更新最后截图
                                if (lastScreenshot != null) {
                                    lastScreenshot.recycle();
                                }
                                lastScreenshot = capturedBitmap[0];

                                Log.d(TAG, "截图成功: " + lastScreenshot.getWidth() + "x" + lastScreenshot.getHeight());

                                // 如果正在托管，直接处理截图
                                if (isHosting) {
                                    Bitmap copy = lastScreenshot.copy(lastScreenshot.getConfig(), true);
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        processScreenshot(copy);
                                    });
                                } else {
                                    // 保存到文件作为测试
                                    saveScreenshotToFile(lastScreenshot);
                                }
                            } else {
                                Log.e(TAG, "未能获取截图");
                            }
                        } catch (SecurityException se) {
                            Log.e(TAG, "创建虚拟显示时出现安全异常: " + se.getMessage(), se);
                            Toast.makeText(FloatingWindowService.this, "截屏权限被拒绝，请重新授权", Toast.LENGTH_LONG).show();
                            mediaProjectionInitialized = false;
                            latch.countDown();
                            if (imageReader != null) {
                                imageReader.close();
                                imageReader = null;
                            }
                            return;
                        } catch (Exception e) {
                            Log.e(TAG, "创建虚拟显示时发生异常: " + e.getMessage(), e);
                            latch.countDown();
                            if (imageReader != null) {
                                imageReader.close();
                                imageReader = null;
                            }
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "截图过程中出错: " + e.getMessage(), e);
                    } finally {
                        // 释放虚拟显示和图像读取器
                        try {
                            if (virtualDisplay != null) {
                                virtualDisplay.release();
                                virtualDisplay = null;
                            }

                            if (imageReader != null) {
                                imageReader.close();
                                imageReader = null;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "释放资源出错: " + e.getMessage());
                        }
                    }

                    // 安排下一次截图
                    if (mediaProjectionInitialized && mediaProjection != null) {
                        screenshotHandler.postDelayed(this, 2000); // 每2秒截图一次
                    }
                }
            }, 1000); // 初始延迟1秒
        } else {
            Log.w(TAG, "无法启动截图任务，MediaProjection未正确初始化");
        }
    }

    /**
     * 将截图保存到文件
     * 
     * @param screenshot 要保存的截图
     */
    private void saveScreenshotToFile(Bitmap screenshot) {
        try {
            // 创建目录（如果不存在）
            File screenshotDir = new File(getExternalFilesDir(null), "screenshot");
            if (!screenshotDir.exists()) {
                if (!screenshotDir.mkdirs()) {
                    Log.e(TAG, "无法创建screenshot目录");
                    return;
                }
            }

            // 使用当前时间创建文件名
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = dateFormat.format(new Date());
            String fileName = "screenshot_" + timestamp + ".jpg";
            File outputFile = new File(screenshotDir, fileName);

            // 保存图片到文件
            FileOutputStream fos = new FileOutputStream(outputFile);
            screenshot.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            Log.i(TAG, "截图已保存: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "保存截图失败: " + e.getMessage(), e);
        }
    }

    // 添加一个方法，用于显示截图失败状态
    private void updateScreenshotStatusUI(boolean isError, String message) {
        try {
            // 在主线程中更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    // 如果取消托管按钮可见，显示状态信息
                    if (cancelHostingView != null && cancelHostingView.getVisibility() == View.VISIBLE) {
                        // 尝试找到状态文本控件
                        View statusTextView = cancelHostingView.findViewById(R.id.status_text);
                        if (statusTextView instanceof TextView) {
                            TextView textView = (TextView) statusTextView;

                            // 设置文本
                            textView.setText(message);

                            // 如果是错误状态，设置红色
                            if (isError) {
                                textView.setTextColor(Color.RED);
                            } else {
                                textView.setTextColor(Color.WHITE);
                            }

                            // 确保可见
                            textView.setVisibility(View.VISIBLE);
                        } else {
                            // 如果找不到现有的状态文本，创建一个临时的Toast
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // 如果取消按钮不可见，用Toast显示
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新UI状态时出错: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "调用UI更新方法失败: " + e.getMessage(), e);
        }
    }

    // 将Image转换为Bitmap
    private Bitmap imageToBitmap(Image image) {
        Log.d(TAG, "图像尺寸: " + image.getWidth() + "x" + image.getHeight());
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
    }
}