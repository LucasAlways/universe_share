<<<<<<< HEAD
package com.example.universe_share;
=======
package com.titan.universe_share;
>>>>>>> dc935df

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
<<<<<<< HEAD
=======
import android.content.IntentFilter;
>>>>>>> dc935df
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
<<<<<<< HEAD
=======
import androidx.annotation.RequiresApi;
>>>>>>> dc935df
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Looper;
<<<<<<< HEAD

public class FloatingWindowService extends Service {
=======
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
import android.view.Surface; // 添加Surface导入
import android.content.BroadcastReceiver;
import android.provider.Settings;
import android.text.TextUtils;

public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
>>>>>>> dc935df
    private WindowManager windowManager;
    private View floatingView;
    private View expandedView;
    private boolean isExpanded = false;
    private boolean isPartiallyHidden = false;
    private int screenWidth = 0;
<<<<<<< HEAD
    private int screenHeight = 0; // 添加屏幕高度变量
=======
>>>>>>> dc935df
    private static final int EDGE_WIDTH = 20; // 边缘露出的宽度
    private boolean isOnLeftSide = false;

    // 用于自动隐藏的定时器
    private Handler autoHideHandler;
    private Runnable autoHideRunnable;
<<<<<<< HEAD
    private static final long AUTO_HIDE_DELAY = 5000; // 5秒无操作后自动隐藏
=======
    private static final long AUTO_HIDE_DELAY = 3000; // 3秒后自动隐藏
>>>>>>> dc935df

    // 用于保持服务存活的定时器
    private Handler keepAliveHandler;
    private Runnable keepAliveRunnable;
<<<<<<< HEAD
    private static final long KEEP_ALIVE_INTERVAL = 30000; // 每30秒执行一次
=======
    private static final long KEEP_ALIVE_INTERVAL = 10 * 60 * 1000; // 10分钟保活间隔
>>>>>>> dc935df

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

<<<<<<< HEAD
    @Override
    public void onCreate() {
        super.onCreate();
        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取屏幕尺寸，用于计算位置
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels; // 获取屏幕高度
=======
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
    private ImageReader currentImageReader;
    private VirtualDisplay currentVirtualDisplay;
    private boolean isMediaProjectionInitialized = false;
    private Intent resultData;
    private int resultCode;
    private Handler screenshotHandler;
    private HandlerThread screenshotThread;
    private int screenHeight;
    private int screenDensity;
    private Bitmap lastScreenshot;
    private int screenshotFailureCount = 0;
    private static final int MAX_SCREENSHOT_RETRY = 5;
    private long lastLogTime = 0; // 上次记录日志的时间
    private Runnable screenshotRunnable; // 持有截图任务的引用
    private static final long HOSTING_INTERVAL = 1000; // 托管任务间隔
    private Timer timer;
    private static final String SCREENSHOT_THREAD_NAME = "ScreenshotThread";
    private boolean isRequestingScreenshot = false;
    private final Object virtualDisplayLock = new Object(); // 用于同步访问VirtualDisplay

    // 广播Action
    public static final String ACTION_REQUEST_SCREENSHOT_PERMISSION = "com.titan.universe_share.REQUEST_SCREENSHOT_PERMISSION";
    public static final String ACTION_SCREENSHOT_COMPLETED = "com.titan.universe_share.SCREENSHOT_COMPLETED";

    // 在类的成员变量部分添加失败计数器
    private int screenshotFailCount = 0;

    // 添加token轮换相关字段
    private static final int TOKEN_ROTATE_COUNT = 2; // 每2次截图轮换一次token
    private int screenshotCount = 0; // 截图计数器
    private boolean needNewPermission = false; // 是否需要请求新权限

    // 在类的成员变量部分添加辅助功能服务相关字段
    private boolean useAccessibilityService = true; // 是否使用辅助功能服务进行截图
    private ScreenCaptureAccessibilityService.HostingStatusListener accessibilityServiceListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // 注册服务实例
        FloatingWindowServiceHolder.setService(this);

        // 不再需要注册截图完成广播接收器
        // registerScreenshotCompletedReceiver();

        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取屏幕宽度，用于计算边缘位置
        screenWidth = getResources().getDisplayMetrics().widthPixels;

        // 初始化支付识别器
        paymentRecognizer = new PaymentTemplateManager(this);
>>>>>>> dc935df

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
<<<<<<< HEAD
        Log.d("FloatingWindowService", "onStartCommand调用");

        // 立即显示悬浮窗，不等待系统回调
        if (floatingView == null) {
            try {
                showFloatingWindow();
                Log.d("FloatingWindowService", "在onStartCommand中显示悬浮窗");
            } catch (Exception e) {
                Log.e("FloatingWindowService", "显示悬浮窗时出错", e);
            }
        } else {
            Log.d("FloatingWindowService", "悬浮窗已存在，不需要再次创建");
        }

        return START_STICKY;
    }

    // 创建通知渠道
=======
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

>>>>>>> dc935df
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
<<<<<<< HEAD
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

=======
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

>>>>>>> dc935df
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

<<<<<<< HEAD
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
=======
            // 确保窗口管理器已初始化
            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
>>>>>>> dc935df
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

<<<<<<< HEAD
            // 创建现代风格的悬浮视图
            createModernFloatingView(params);
=======
            // 创建现代风格的悬浮视图（如果尚未创建）
            if (floatingView == null) {
                createModernFloatingView(params);
            }
>>>>>>> dc935df

            // 确认悬浮窗是否成功创建
            if (floatingView != null) {
                Log.d("FloatingWindowService", "悬浮窗视图创建成功");
<<<<<<< HEAD
=======

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

>>>>>>> dc935df
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

<<<<<<< HEAD
                    // 判断是展开视图的点击还是拖动操作
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            // 记录初始触摸位置和悬浮窗位置
=======
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            // 记录初始位置
>>>>>>> dc935df
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
<<<<<<< HEAD
                            return true;

                        case android.view.MotionEvent.ACTION_MOVE:
                            // 计算偏移量 - 这种方式更准确地跟随手指
                            float offsetX = event.getRawX() - initialTouchX[0];
                            float offsetY = event.getRawY() - initialTouchY[0];

                            // 应用偏移量，更新悬浮窗位置
                            params.x = (int) (initialX[0] + offsetX);
                            params.y = (int) (initialY[0] + offsetY);

                            // 限制范围防止完全移出屏幕
                            params.x = Math.max(-FLOATING_BUTTON_SIZE / 2,
                                    Math.min(params.x, screenWidth - FLOATING_BUTTON_SIZE / 2));
                            params.y = Math.max(-FLOATING_BUTTON_SIZE / 2,
                                    Math.min(params.y, screenHeight - FLOATING_BUTTON_SIZE / 2));

                            // 如果移动距离超过阈值，标记为移动状态
                            if (Math.abs(offsetX) > 10 || Math.abs(offsetY) > 10) {
                                moving[0] = true;

                                // 如果展开了视图，随着悬浮窗移动而更新展开视图位置
                                if (isExpanded && expandedView != null) {
                                    updateExpandedViewPosition(params);
                                }

=======

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
>>>>>>> dc935df
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

<<<<<<< HEAD
        // 创建展开视图容器（卡片式设计）- 缩小尺寸为原来的2/3
=======
        // 创建展开视图容器（卡片式设计）
>>>>>>> dc935df
        LinearLayout cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.setBackgroundColor(COLOR_WHITE);

        // 设置圆角背景
        GradientDrawable shape = new GradientDrawable();
<<<<<<< HEAD
        shape.setCornerRadius(16); // 减小圆角
=======
        shape.setCornerRadius(20);
>>>>>>> dc935df
        shape.setColor(COLOR_WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            cardContainer.setBackground(shape);
        } else {
            cardContainer.setBackgroundDrawable(shape);
        }

<<<<<<< HEAD
        // 减小内边距，使界面更紧凑
        cardContainer.setPadding(16, 16, 16, 16); // 减小内边距

        // 设置阴影效果（仅21及以上支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardContainer.setElevation(8); // 减小阴影
=======
        cardContainer.setPadding(32, 32, 32, 32);

        // 设置阴影效果（仅21及以上支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardContainer.setElevation(10);
>>>>>>> dc935df
        }

        // 修改卡片布局参数，根据悬浮窗位置动态调整
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER_VERTICAL; // 移除 END gravity，改为动态计算位置
        cardContainer.setLayoutParams(cardParams);

<<<<<<< HEAD
        // 添加标题 - 减小字体和边距
        TextView titleText = new TextView(this);
        titleText.setText("二维码托管");
        titleText.setTextColor(COLOR_PRIMARY_DARK);
        titleText.setTextSize(14); // 减小字体
        titleText.setPadding(0, 0, 0, 8); // 减小下边距
        titleText.setGravity(Gravity.CENTER);
        cardContainer.addView(titleText);

        // 添加"开始托管"按钮 - 进一步减小按钮
        Button startButton = new Button(this);
        startButton.setText("开始托管");
        startButton.setTextColor(COLOR_WHITE);
        startButton.setTextSize(12); // 减小字体
        // 设置按钮尺寸更小
        LinearLayout.LayoutParams startBtnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        startBtnParams.setMargins(0, 0, 0, 4); // 减小边距
        startButton.setLayoutParams(startBtnParams);
        startButton.setPadding(12, 6, 12, 6); // 减小内边距

        // 设置按钮圆角背景
        GradientDrawable btnShape = new GradientDrawable();
        btnShape.setCornerRadius(20); // 减小圆角
=======
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
>>>>>>> dc935df
        btnShape.setColor(COLOR_PRIMARY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startButton.setBackground(btnShape);
        } else {
            startButton.setBackgroundDrawable(btnShape);
        }

        cardContainer.addView(startButton);

<<<<<<< HEAD
        // 添加间距 - 减小间距
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8)); // 减小间距
=======
        // 添加间距
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16));
>>>>>>> dc935df
        cardContainer.addView(spacer);

        // 添加"结束托管"按钮
        Button stopButton = new Button(this);
        stopButton.setText("结束托管");
        stopButton.setTextColor(COLOR_WHITE);
<<<<<<< HEAD
        stopButton.setTextSize(14); // 减小字体
        // 设置按钮尺寸更小
        LinearLayout.LayoutParams stopBtnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        stopBtnParams.setMargins(0, 0, 0, 0);
        stopButton.setLayoutParams(stopBtnParams);
        stopButton.setPadding(16, 8, 16, 8); // 减小内边距

        // 设置按钮圆角背景
        GradientDrawable stopBtnShape = new GradientDrawable();
        stopBtnShape.setCornerRadius(20); // 减小圆角
=======

        // 设置按钮圆角背景
        GradientDrawable stopBtnShape = new GradientDrawable();
        stopBtnShape.setCornerRadius(30);
>>>>>>> dc935df
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
<<<<<<< HEAD
                // 开始托管操作
                Toast.makeText(FloatingWindowService.this,
                        "开始托管", Toast.LENGTH_SHORT).show();
=======
                // 显示确认对话框
                showConfirmStartHostingDialog(params);

>>>>>>> dc935df
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

<<<<<<< HEAD
=======
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
                        // 开始托管操f作
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

>>>>>>> dc935df
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

<<<<<<< HEAD
                    // 获取卡片容器并设置位置
                    LinearLayout cardContainer = (LinearLayout) ((FrameLayout) expandedView).getChildAt(0);
                    if (cardContainer != null) {
                        FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) cardContainer
                                .getLayoutParams();

                        // 计算位置
                        calculateExpandedViewPosition(params, cardParams);

                        // 应用布局参数
=======
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
>>>>>>> dc935df
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

<<<<<<< HEAD
    // 更新展开视图位置的方法
    private void updateExpandedViewPosition(WindowManager.LayoutParams floatingParams) {
        try {
            if (expandedView != null) {
                LinearLayout cardContainer = (LinearLayout) ((FrameLayout) expandedView).getChildAt(0);
                if (cardContainer != null) {
                    FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) cardContainer.getLayoutParams();

                    // 计算位置 - 基于浮动窗口的当前位置
                    calculateExpandedViewPosition(floatingParams, cardParams);

                    // 应用布局参数
                    cardContainer.setLayoutParams(cardParams);
                }
            }
        } catch (Exception e) {
            Log.e("FloatingWindowService", "更新展开视图位置失败: " + e.getMessage(), e);
        }
    }

    // 计算展开视图的最佳位置 - 修改为智能上下显示
    private void calculateExpandedViewPosition(WindowManager.LayoutParams floatingParams,
            FrameLayout.LayoutParams cardParams) {
        // 获取悬浮窗的中心点位置
        int floatingCenterX = floatingParams.x + FLOATING_BUTTON_SIZE / 2;
        int floatingCenterY = floatingParams.y + FLOATING_BUTTON_SIZE / 2;

        // 清除所有边距
        cardParams.leftMargin = 0;
        cardParams.rightMargin = 0;
        cardParams.topMargin = 0;
        cardParams.bottomMargin = 0;

        // 计算水平位置
        boolean isLeftSide = floatingCenterX < screenWidth / 2;

        // 判断悬浮窗是否在屏幕上半部分
        boolean isTopHalf = floatingCenterY < screenHeight / 2;

        // 设置重力属性
        int verticalGravity, horizontalGravity;

        // 水平位置
        horizontalGravity = isLeftSide ? Gravity.START : Gravity.END;

        // 垂直位置 - 根据悬浮窗在屏幕上下位置决定二维码界面显示在上方还是下方
        // 如果悬浮窗在上半部分，二维码界面显示在下方；如果在下半部分，显示在上方
        verticalGravity = isTopHalf ? Gravity.TOP : Gravity.BOTTOM;

        cardParams.gravity = verticalGravity | horizontalGravity;

        // 安全间距，靠近悬浮窗但不覆盖
        int safeDistance = 5; // 减小间距使其更紧贴悬浮窗

        // 水平方向位置计算
        if (isLeftSide) {
            // 悬浮窗在左侧，二维码界面显示在右侧
            cardParams.leftMargin = floatingParams.x + FLOATING_BUTTON_SIZE + safeDistance;
        } else {
            // 悬浮窗在右侧，二维码界面显示在左侧
            cardParams.rightMargin = screenWidth - floatingParams.x + safeDistance;
        }

        // 垂直方向位置计算
        if (isTopHalf) {
            // 悬浮窗在上半部分，二维码界面显示在下方
            cardParams.topMargin = floatingParams.y + FLOATING_BUTTON_SIZE + safeDistance;
        } else {
            // 悬浮窗在下半部分，二维码界面显示在上方
            cardParams.bottomMargin = screenHeight - floatingParams.y + safeDistance;
        }

        Log.d("FloatingWindowService", "展开视图位置计算: " +
                "isLeftSide=" + isLeftSide + ", isTopHalf=" + isTopHalf +
                ", leftMargin=" + cardParams.leftMargin +
                ", rightMargin=" + cardParams.rightMargin +
                ", topMargin=" + cardParams.topMargin +
                ", bottomMargin=" + cardParams.bottomMargin);
    }

    // 吸附到屏幕边缘 - 修改为靠近最近的边缘
    private void snapToEdge(WindowManager.LayoutParams params) {
        int screenCenter = screenWidth / 2;
        boolean isCloserToLeft = params.x < screenCenter;

        // 根据当前位置决定靠近哪个边缘
        animateViewToEdge(params, isCloserToLeft);

        // 记录当前靠哪边
        isOnLeftSide = isCloserToLeft;

        Log.d("FloatingWindowService", "悬浮窗自动靠近" + (isCloserToLeft ? "左" : "右") + "边缘");
=======
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
>>>>>>> dc935df
    }

    // 将悬浮窗动画移动到边缘
    private void animateViewToEdge(final WindowManager.LayoutParams params, final boolean isLeft) {
<<<<<<< HEAD
        final int targetX;
        if (isLeft) {
            // 设置为负值，使大部分区域隐藏在左边缘外
            targetX = -FLOATING_BUTTON_SIZE + EDGE_WIDTH;
=======
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
>>>>>>> dc935df
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
<<<<<<< HEAD
                    float progress = (float) (step + 1) / STEPS;
                    params.x = startX + (int) (distance * progress);

                    if (windowManager != null && floatingView != null) {
                        windowManager.updateViewLayout(floatingView, params);
                    }

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
=======
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
>>>>>>> dc935df
                    }
                }
            };
        }

        // 开始动画
        handler.post(animationSteps[0]);
    }

    // 将隐藏的悬浮窗动画移回可见区域
    private void animateViewToVisible(final WindowManager.LayoutParams params) {
<<<<<<< HEAD
=======
        // 先检查视图是否已经被移除
        if (floatingView == null || !isViewAttached(floatingView)) {
            Log.e(TAG, "悬浮窗已被移除，无法执行动画");
            return;
        }

>>>>>>> dc935df
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
<<<<<<< HEAD
                    float progress = (float) (step + 1) / STEPS;
                    params.x = startX + (int) (distance * progress);

                    if (windowManager != null && floatingView != null) {
                        windowManager.updateViewLayout(floatingView, params);
                    }

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
=======
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
>>>>>>> dc935df
                    }
                }
            };
        }

        // 开始动画
        handler.post(animationSteps[0]);
    }

<<<<<<< HEAD
=======
    // 获取窗口布局类型
>>>>>>> dc935df
    private int getWindowLayoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

<<<<<<< HEAD
=======
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

>>>>>>> dc935df
    @Override
    public void onDestroy() {
        super.onDestroy();

<<<<<<< HEAD
        // 取消所有计时器
        cancelAutoHideTimer();
        if (keepAliveHandler != null && keepAliveRunnable != null) {
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
        }

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
=======
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

        // 隐藏悬浮按钮，只保留底部取消托管按钮
        if (floatingView != null) {
            floatingView.setVisibility(View.GONE);
        }

        // 显示取消托管按钮
        showCancelHostingButton();

        // 设置处理Handler
        if (hostingHandler == null) {
            hostingHandler = new Handler(Looper.getMainLooper());
        }

        // 检查是否可以使用辅助功能服务进行截图
        if (isAccessibilityServiceEnabled()) {
            // 使用辅助功能服务进行截图
            Log.d(TAG, "使用辅助功能服务进行截图");

            // 初始化监听器
            initAccessibilityServiceListener();

            // 获取辅助功能服务实例并开始托管
            ScreenCaptureAccessibilityService service = ScreenCaptureAccessibilityService.getInstance();
            if (service != null) {
                service.setHostingStatusListener(accessibilityServiceListener);

                // 启动托管
                service.startHosting();

                // 更新UI状态
                updateScreenshotStatusUI(false, "使用辅助功能服务进行截图");
            } else {
                // 无法获取服务实例，提示用户启用辅助功能
                Log.w(TAG, "无法获取辅助功能服务实例，请确保已启用辅助功能");
                updateScreenshotStatusUI(true, "请启用辅助功能服务");

                // 弹出提示
                hostingHandler.post(() -> {
                    Toast.makeText(this, "请先启用辅助功能服务", Toast.LENGTH_LONG).show();
                    showAccessibilitySettingsDialog();
                });

                // 停止托管
                stopHosting();
            }
        } else {
            // 无法使用辅助功能服务，提示用户启用
            Log.d(TAG, "辅助功能服务未启用，请用户启用");
            updateScreenshotStatusUI(true, "请启用辅助功能服务");

            // 弹出提示
            hostingHandler.post(() -> {
                Toast.makeText(this, "请先启用辅助功能服务", Toast.LENGTH_LONG).show();
                showAccessibilitySettingsDialog();
            });

            // 停止托管
            stopHosting();
        }
    }

    /**
     * 显示辅助功能设置对话框
     */
    private void showAccessibilitySettingsDialog() {
        try {
            // 创建一个Intent跳转到辅助功能设置页面
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 弹出提示对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("需要授权")
                    .setMessage("需要启用辅助功能服务才能进行自动截图。请在设置中找到Universe Share并启用。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "显示辅助功能设置对话框失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化辅助功能服务监听器
     */
    private void initAccessibilityServiceListener() {
        if (accessibilityServiceListener == null) {
            accessibilityServiceListener = new ScreenCaptureAccessibilityService.HostingStatusListener() {
                @Override
                public void onHostingStatusChanged(boolean hosting) {
                    if (!hosting && isHosting) {
                        // 如果辅助功能服务停止了托管，而FloatingWindowService认为仍在托管，则停止托管
                        stopHosting();
                    }
                }

                @Override
                public void onScreenshotTaken(boolean success, String message) {
                    // 更新UI状态
                    updateScreenshotStatusUI(!success, message);
                }

                @Override
                public void onPaymentInfoRecognized(String qrCode, String cardNumber, String balance, long expiryTime) {
                    // 处理识别到的支付信息
                    processPaymentInfo(qrCode, cardNumber, balance, expiryTime);
                }

                @Override
                public void onHostingError(String errorMessage) {
                    // 处理托管错误
                    Log.e(TAG, "托管错误: " + errorMessage);
                    updateScreenshotStatusUI(true, "错误: " + errorMessage);
                }
            };
        }
    }

    /**
     * 处理识别到的支付信息
     */
    private void processPaymentInfo(String qrCode, String cardNumber, String balance, long expiryTime) {
        try {
            // 更新缓存的信息
            lastQrCodeContent = qrCode;
            lastCardNumber = cardNumber;
            lastBalance = balance;
            lastQrCodeExpiryTime = expiryTime;

            // 更新UI显示
            String expiryTimeDisplay = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(expiryTime));
            updateScreenshotStatusUI(false, "二维码有效至: " + expiryTimeDisplay);

            // 处理二维码刷新 - 此处可添加您的业务逻辑
            Log.d(TAG, "识别到付款码: " + qrCode.substring(0, Math.min(20, qrCode.length())) + "...");
            Log.d(TAG, "卡号: " + cardNumber + ", 余额: " + balance + ", 过期时间: " + expiryTimeDisplay);

            // 这里可以添加更多处理逻辑，如上传数据、更新UI等
        } catch (Exception e) {
            Log.e(TAG, "处理支付信息时出错: " + e.getMessage(), e);
        }
    }

    public void stopHosting() {
        Log.i(TAG, "停止托管任务");
        isHosting = false;

        // 重置截图失败计数
        screenshotFailCount = 0;

        // 清理截图任务
        screenshotRunnable = null;

        // 如果使用辅助功能服务，停止服务的托管
        ScreenCaptureAccessibilityService service = ScreenCaptureAccessibilityService.getInstance();
        if (service != null) {
            service.stopHosting();
        }

        // 恢复UI
        new Handler(Looper.getMainLooper()).post(() -> {
            // 恢复悬浮按钮显示
            if (floatingView != null) {
                floatingView.setVisibility(View.VISIBLE);
            }
            if (cancelHostingView != null) {
                cancelHostingView.setVisibility(View.GONE);
            }

            Toast.makeText(this, "托管已停止", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 创建截图任务Runnable（使用MediaProjection方式截图时使用）
     */
    private void createScreenshotRunnable() {
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
                        // 停止托管并弹窗提示
                        showScreenshotFailedDialog();
                        return;
                    }

                    // 检查是否已有截图请求正在进行
                    if (isRequestingScreenshot) {
                        Log.d(TAG, "已有截图请求正在处理中，等待...");
                        hostingHandler.postDelayed(this, HOSTING_INTERVAL);
                        return;
                    }

                    Log.d(TAG, "执行截图任务");

                    // 执行截图，复用MediaProjection
                    Bitmap screenshot = takeScreenshot();

                    if (screenshot != null) {
                        // 截图成功，处理截图
                        Log.d(TAG, "截图成功，开始处理");

                        // 保存截图到文件
                        saveScreenshotToFile(screenshot);

                        // 检查二维码是否已过期
                        long currentTime = System.currentTimeMillis();
                        if (lastQrCodeExpiryTime <= currentTime) {
                            // 处理截图
                            processScreenshot(screenshot);
                        } else {
                            // 显示剩余时间
                            long remainingTime = (lastQrCodeExpiryTime - currentTime) / 1000;
                            Log.d(TAG, "二维码未过期，剩余 " + remainingTime + " 秒");

                            // 如果剩余时间少于10秒，提前更新
                            if (remainingTime < 10) {
                                processScreenshot(screenshot);
                            }
                        }
                    }

                    // 安排下一次截图任务
                    if (isHosting) {
                        hostingHandler.postDelayed(this, HOSTING_INTERVAL);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "执行截图任务时出错: " + e.getMessage(), e);
                    hostingHandler.postDelayed(this, HOSTING_INTERVAL * 2);
                }
            }
        };
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

    /**
     * 接收MediaProjection权限结果
     * 在复用模式下，权限只需获取一次，然后保存MediaProjection实例供整个托管过程使用
     */
    public void setMediaProjectionResult(Intent data) {
        if (data == null) {
            Log.e(TAG, "收到空的MediaProjection数据");
            isRequestingScreenshot = false;
            updateScreenshotStatusUI(true, "未获取到截图权限");
            screenshotFailCount++;
            return;
        }

        try {
            Log.d(TAG, "收到MediaProjection权限数据，初始化并保存");

            // 保存权限数据
            resultData = data;

            // 在主线程中完成初始化
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    // 初始化屏幕参数
                    screenDensity = getResources().getDisplayMetrics().densityDpi;
                    Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    Point size = new Point();
                    display.getRealSize(size);
                    screenWidth = size.x;
                    screenHeight = size.y;

                    // 创建MediaProjection实例
                    MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);
                    mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);

                    if (mediaProjection == null) {
                        Log.e(TAG, "无法创建MediaProjection实例");
                        updateScreenshotStatusUI(true, "创建MediaProjection失败");
                        isRequestingScreenshot = false;
                        screenshotFailCount++;
                        return;
                    }

                    // 注册回调，处理MediaProjection停止事件
                    mediaProjection.registerCallback(new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            Log.d(TAG, "MediaProjection已停止");
                            // 清理资源
                            releaseVirtualDisplayResources();
                            isMediaProjectionInitialized = false;
                            super.onStop();
                        }
                    }, new Handler(Looper.getMainLooper()));

                    // 标记初始化成功
                    isMediaProjectionInitialized = true;
                    Log.i(TAG, "MediaProjection初始化成功: 屏幕分辨率 " + screenWidth + "x" + screenHeight);

                    // 重置截图请求标志
                    isRequestingScreenshot = false;

                    // 成功初始化后，如果正在托管，立即触发一次截图
                    if (isHosting) {
                        hostingHandler.post(screenshotRunnable);
                    }

                    // 更新UI提示
                    updateScreenshotStatusUI(false, "截图权限已获取，开始监控");

                } catch (Exception e) {
                    Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
                    isRequestingScreenshot = false;
                    screenshotFailCount++;
                    updateScreenshotStatusUI(true, "初始化失败: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "处理MediaProjection结果异常: " + e.getMessage(), e);
            Toast.makeText(this, "截图初始化错误", Toast.LENGTH_SHORT).show();
            isRequestingScreenshot = false;
            screenshotFailCount++;
            updateScreenshotStatusUI(true, "初始化错误");
        }
    }

    // 初始化 ImageReader 和 VirtualDisplay
    private void initImageReaderAndVirtualDisplay() {
        try {
            currentImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            currentImageReader.setOnImageAvailableListener(reader -> {
                // 图像可用时会自动触发（但定时逻辑通过主动获取最新帧实现）
            }, screenshotHandler);

            Surface surface = currentImageReader.getSurface();
            currentVirtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface,
                    null,
                    screenshotHandler);

            if (currentVirtualDisplay == null) {
                throw new IllegalStateException("无法创建VirtualDisplay");
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 释放VirtualDisplay相关资源，但保留MediaProjection实例
     */
    private void releaseVirtualDisplayResources() {
        synchronized (virtualDisplayLock) {
            try {
                if (currentVirtualDisplay != null) {
                    currentVirtualDisplay.release();
                    currentVirtualDisplay = null;
                    Log.d(TAG, "VirtualDisplay已释放");
                }

                if (currentImageReader != null) {
                    currentImageReader.close();
                    currentImageReader = null;
                    Log.d(TAG, "ImageReader已释放");
                }
            } catch (Exception e) {
                Log.e(TAG, "释放VirtualDisplay资源时出错: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 截取当前屏幕 - 全新实例模式
     * 在此模式下，每次截图都会创建全新的MediaProjection实例，完全避免安全异常
     * 
     * @return Bitmap图像，如果失败则返回null
     */
    public Bitmap takeScreenshot() {
        try {
            // 如果已经在请求截图，避免重复请求
            if (isRequestingScreenshot) {
                Log.d(TAG, "已有截图请求正在进行中，跳过此次请求");
                return null;
            }

            // 标记为请求中
            isRequestingScreenshot = true;

            // 检查是否有权限数据
            if (resultData == null) {
                Log.d(TAG, "没有权限数据，请求新的截图权限");
                // 请求新的截图权限
                requestScreenCapturePermission();
                // 更新UI状态
                updateScreenshotStatusUI(true, "正在获取截图权限...");
                return null;
            }

            Log.d(TAG, "开始创建全新MediaProjection实例进行截图");

            // 创建用于截图的线程
            HandlerThread handlerThread = new HandlerThread("ScreenCaptureThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            // 创建同步等待工具
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Bitmap> bitmapRef = new AtomicReference<>();

            // 使用新线程执行截图，以隔离资源
            new Thread(() -> {
                // MediaProjection oneTimeMediaProjection = null;
                // ImageReader oneTimeImageReader = null;
                // VirtualDisplay oneTimeVirtualDisplay = null;

                try {
                    // 每次都创建全新的MediaProjection实例
                    // MediaProjectionManager projectionManager = (MediaProjectionManager)
                    // getSystemService(
                    // Context.MEDIA_PROJECTION_SERVICE);
                    // oneTimeMediaProjection =
                    // projectionManager.getMediaProjection(Activity.RESULT_OK,
                    // (Intent) resultData.clone()); // 使用clone避免修改原始数据

                    // if (mediaProjection == null) {
                    // Log.e(TAG, "创建MediaProjection实例");
                    // MediaProjectionManager projectionManager = (MediaProjectionManager)
                    // getSystemService(
                    // Context.MEDIA_PROJECTION_SERVICE);
                    // mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK,
                    // (Intent) resultData.clone());
                    // return;
                    // }

                    // 确保屏幕参数设置正确
                    if (screenWidth <= 0 || screenHeight <= 0 || screenDensity <= 0) {
                        screenDensity = getResources().getDisplayMetrics().densityDpi;
                        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                                .getDefaultDisplay();
                        Point size = new Point();
                        display.getRealSize(size);
                        screenWidth = size.x;
                        screenHeight = size.y;
                    }

                    // // 创建一次性ImageReader
                    // oneTimeImageReader = ImageReader.newInstance(screenWidth, screenHeight,
                    // PixelFormat.RGBA_8888, 2);

                    // 设置图像可用监听器
                    currentImageReader.setOnImageAvailableListener(reader -> {
                        try {
                            Image image = reader.acquireLatestImage();
                            if (image != null) {
                                try {
                                    Bitmap bitmap = imageToBitmap(image);
                                    bitmapRef.set(bitmap);
                                    Log.d(TAG, "成功捕获屏幕: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                } finally {
                                    image.close();
                                    latch.countDown();
                                }
                            } else {
                                Log.e(TAG, "获取图像失败");
                                latch.countDown();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理图像失败: " + e.getMessage(), e);
                            latch.countDown();
                        }
                    }, handler);

                    // 创建一次性VirtualDisplay
                    Surface surface = currentImageReader.getSurface();
                    currentVirtualDisplay = mediaProjection.createVirtualDisplay(
                            "OneTimeScreenCapture",
                            screenWidth, screenHeight, screenDensity,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            surface, null, handler);

                    if (currentVirtualDisplay == null) {
                        throw new IllegalStateException("无法创建VirtualDisplay");
                    }

                    // 等待图像可用，最多5秒
                    boolean success = latch.await(5, TimeUnit.SECONDS);
                    if (!success) {
                        Log.e(TAG, "等待截图超时");
                        screenshotFailCount++;
                    }

                } catch (SecurityException se) {
                    Log.e(TAG, "截图安全异常: " + se.getMessage(), se);

                    // 出现安全异常，token可能已过期，请求新权限
                    screenshotFailCount++;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(FloatingWindowService.this, "截图权限已过期，请重新授权", Toast.LENGTH_SHORT).show();
                        resultData = null; // 清除可能已过期的token
                        requestScreenCapturePermission();
                    });

                } catch (Exception e) {
                    Log.e(TAG, "截图过程中出现错误: " + e.getMessage(), e);
                    screenshotFailCount++;

                } finally {
                    // 释放所有资源 - 这是关键，每次都完全释放
                    // try {
                    // if (oneTimeVirtualDisplay != null) {
                    // oneTimeVirtualDisplay.release();
                    // }
                    // if (oneTimeImageReader != null) {
                    // oneTimeImageReader.close();
                    // }
                    // if (mediaProjection != null) {
                    // oneTimeMediaProjection.stop(); // 关键: 立即停止MediaProjection
                    // }
                    // } catch (Exception e) {
                    // Log.e(TAG, "释放截图资源时出错: " + e.getMessage(), e);
                    // }

                    // 完成后标记请求已结束
                    isRequestingScreenshot = false;
                }
            }).start();

            // 等待截图完成，最多5秒
            boolean success = latch.await(5, TimeUnit.SECONDS);
            handlerThread.quitSafely();

            // 获取截图结果
            Bitmap result = bitmapRef.get();
            if (result != null) {
                // 截图成功，重置失败计数
                screenshotFailCount = 0;
                updateScreenshotStatusUI(false, "截图成功");
                return result;
            } else {
                // 截图失败，更新UI
                screenshotFailCount++;
                updateScreenshotStatusUI(true, "截图失败 (" + screenshotFailCount + "/" + MAX_SCREENSHOT_RETRY + ")");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "截图方法异常: " + e.getMessage(), e);
            isRequestingScreenshot = false;
            screenshotFailCount++;
            updateScreenshotStatusUI(true, "截图错误: " + e.getMessage());
            return null;
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
        if (isMediaProjectionInitialized && mediaProjection != null && screenshotHandler != null) {
            screenshotHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isMediaProjectionInitialized || mediaProjection == null) {
                        Log.d(TAG, "MediaProjection已失效，停止截图任务");
                        return;
                    }

                    try {
                        // 每次截图前先创建新的ImageReader
                        if (currentImageReader != null) {
                            currentImageReader.close();
                        }
                        currentImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888,
                                2);

                        // 先设置监听器，再创建虚拟显示（顺序很重要）
                        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                        final Bitmap[] capturedBitmap = new Bitmap[1];

                        currentImageReader.setOnImageAvailableListener(reader -> {
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
                        if (currentVirtualDisplay != null) {
                            currentVirtualDisplay.release();
                            currentVirtualDisplay = null;
                        }

                        try {
                            // 创建虚拟显示
                            currentVirtualDisplay = mediaProjection.createVirtualDisplay(
                                    "ScreenCapture",
                                    screenWidth,
                                    screenHeight,
                                    screenDensity,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    currentImageReader.getSurface(),
                                    null,
                                    screenshotHandler);

                            if (currentVirtualDisplay == null) {
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
                            isMediaProjectionInitialized = false;
                            latch.countDown();
                            if (currentImageReader != null) {
                                currentImageReader.close();
                                currentImageReader = null;
                            }
                            return;
                        } catch (Exception e) {
                            Log.e(TAG, "创建虚拟显示时发生异常: " + e.getMessage(), e);
                            latch.countDown();
                            if (currentImageReader != null) {
                                currentImageReader.close();
                                currentImageReader = null;
                            }
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "截图过程中出错: " + e.getMessage(), e);
                    } finally {
                        // 释放虚拟显示和图像读取器
                        try {
                            if (currentVirtualDisplay != null) {
                                currentVirtualDisplay.release();
                                currentVirtualDisplay = null;
                            }

                            if (currentImageReader != null) {
                                currentImageReader.close();
                                currentImageReader = null;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "释放资源出错: " + e.getMessage());
                        }
                    }

                    // 安排下一次截图
                    if (isMediaProjectionInitialized && mediaProjection != null) {
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

    // 显示截图失败对话框
    private void showScreenshotFailedDialog() {
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

                builder.setTitle("截图失败")
                        .setMessage("连续" + MAX_SCREENSHOT_RETRY + "次截图失败，请确认截图权限已授予或重启应用")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("确定", (dialog, which) -> {
                            dialog.dismiss();
                            // 停止托管
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
                Toast.makeText(this, "截图失败，托管已停止", Toast.LENGTH_LONG).show();

                // 在3秒后自动关闭对话框（防止用户无响应）
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        // 停止托管
                        stopHosting();
                    }
                }, 5000);
            } catch (Exception e) {
                Log.e(TAG, "显示截图失败对话框时出错", e);
                // 如果对话框显示失败，确保通过Toast通知用户
                Toast.makeText(this, "截图失败，托管已停止", Toast.LENGTH_LONG).show();
                // 直接停止托管
                stopHosting();
            }
        });
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

            // 保存权限数据，用于后续创建MediaProjection实例，但不创建全局MediaProjection实例
            this.resultData = data;

            // 设置屏幕参数
            screenDensity = getResources().getDisplayMetrics().densityDpi;
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            screenWidth = size.x;
            screenHeight = size.y;

            // 权限数据已经保存，每次截图时都会创建新的MediaProjection
            isMediaProjectionInitialized = true;
            Log.i(TAG, "MediaProjection权限数据保存成功: 屏幕分辨率 " + screenWidth + "x" + screenHeight + ", 密度: " + screenDensity);

            // 重置截图失败计数
            screenshotFailCount = 0;
        } catch (Exception e) {
            Log.e(TAG, "初始化MediaProjection失败: " + e.getMessage(), e);
            Toast.makeText(this, "截屏功能初始化错误", Toast.LENGTH_SHORT).show();
            isMediaProjectionInitialized = false;
        }
    }

    // 释放MediaProjection相关资源，包括所有创建的对象
    private void releaseMediaProjection() {
        try {
            Log.d(TAG, "正在释放MediaProjection资源");

            // 先释放VirtualDisplay资源
            releaseVirtualDisplayResources();

            // 释放MediaProjection
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
                Log.d(TAG, "MediaProjection已停止");
            }

            isMediaProjectionInitialized = false;
            resultData = null;

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

            Log.d(TAG, "MediaProjection资源已完全释放");
        } catch (Exception e) {
            Log.e(TAG, "释放MediaProjection资源时出错: " + e.getMessage(), e);
        }
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
>>>>>>> dc935df
}