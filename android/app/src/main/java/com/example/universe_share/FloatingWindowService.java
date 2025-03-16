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

public class FloatingWindowService extends Service {
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

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取屏幕宽度，用于计算边缘位置
        screenWidth = getResources().getDisplayMetrics().widthPixels;

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
                // 开始托管操作
                Toast.makeText(FloatingWindowService.this,
                        "开始托管", Toast.LENGTH_SHORT).show();
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
        // 只吸附到右边缘
        animateViewToEdge(params, false);
    }

    // 将悬浮窗动画移动到边缘
    private void animateViewToEdge(final WindowManager.LayoutParams params, final boolean isLeft) {
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
                    }
                }
            };
        }

        // 开始动画
        handler.post(animationSteps[0]);
    }

    // 将隐藏的悬浮窗动画移回可见区域
    private void animateViewToVisible(final WindowManager.LayoutParams params) {
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
}