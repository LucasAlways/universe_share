package com.titan.universe_share;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.titan.universe_share.utils.PaymentTemplateManager;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * 辅助功能服务，用于截取屏幕内容进行托管
 * 注意：需要用户在设置中启用此辅助功能
 */
public class ScreenCaptureAccessibilityService extends AccessibilityService {
    private static final String TAG = "ScreenCaptureAccessibility";
    private static ScreenCaptureAccessibilityService instance;
    private static final long DEFAULT_SCREENSHOT_INTERVAL = 5000; // 默认5秒

    // 保存图片路径
    private static final String SCREENSHOTS_DIR = "/screenshots/";

    // 用于处理截图的线程
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    // 托管相关变量
    private boolean isHosting = false;
    private final Object hostingLock = new Object();
    private Runnable screenshotRunnable;
    private long nextScreenshotTime = 0;
    private long qrCodeExpiryTime = 0;
    private String lastQrCodeContent = "";
    private PaymentTemplateManager paymentRecognizer;

    // 屏幕相关变量
    private int screenWidth;
    private int screenHeight;

    // 托管监听器
    private HostingStatusListener hostingStatusListener;

    /**
     * 托管状态监听接口
     */
    public interface HostingStatusListener {
        void onHostingStatusChanged(boolean isHosting);

        void onScreenshotTaken(boolean success, String message);

        void onPaymentInfoRecognized(String qrCode, String cardNumber, String balance, long expiryTime);

        void onHostingError(String errorMessage);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ScreenCaptureAccessibilityService创建");
        instance = this;

        // 创建处理线程
        handlerThread = new HandlerThread("ScreenshotThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化支付识别器
        paymentRecognizer = new PaymentTemplateManager(this);

        // 获取屏幕参数
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ScreenCaptureAccessibilityService销毁");
        stopHosting();

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }

        instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 我们主要使用定时截图，不需要处理特定的辅助功能事件
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "辅助功能服务被中断");
    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "辅助功能服务已连接");

        // 配置服务
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;

        // 设置可获取窗口内容 (Android兼容性修改)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        }

        // Android 11+ 添加截屏权限 (0x4 is FLAG_REQUEST_TAKE_SCREENSHOT)
        if (Build.VERSION.SDK_INT >= 30) { // Build.VERSION_CODES.R
            try {
                // 直接使用FLAG_REQUEST_TAKE_SCREENSHOT的值
                info.flags |= 0x4; // 硬编码FLAG_REQUEST_TAKE_SCREENSHOT的值
                Log.d(TAG, "已请求截屏权限 FLAG_REQUEST_TAKE_SCREENSHOT");
            } catch (Exception e) {
                Log.e(TAG, "设置截屏权限失败: " + e.getMessage());
            }
        }

        setServiceInfo(info);
        Log.d(TAG, "AccessibilityService配置已设置完成");
    }

    /**
     * 获取服务实例
     */
    public static ScreenCaptureAccessibilityService getInstance() {
        return instance;
    }

    /**
     * 设置托管状态监听器
     */
    public void setHostingStatusListener(HostingStatusListener listener) {
        this.hostingStatusListener = listener;
    }

    /**
     * 开始托管
     */
    public void startHosting() {
        synchronized (hostingLock) {
            if (isHosting) {
                return; // 已经在托管中
            }

            isHosting = true;
            if (hostingStatusListener != null) {
                hostingStatusListener.onHostingStatusChanged(true);
            }

            // 显示托管开始提示
            mainHandler.post(() -> Toast.makeText(this, "托管已开始，请确保处于正确的二维码展示界面", Toast.LENGTH_LONG).show());

            // 初始化截图任务
            screenshotRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isHosting) {
                        return;
                    }

                    // 检查是否到了下次截图时间
                    long currentTime = System.currentTimeMillis();
                    if (currentTime < nextScreenshotTime) {
                        // 还没到时间，继续等待
                        long delay = nextScreenshotTime - currentTime;
                        backgroundHandler.postDelayed(this, Math.min(delay, 1000));
                        return;
                    }

                    // 执行截图
                    takeScreenshotAndProcess();
                }
            };

            // 立即开始第一次截图
            nextScreenshotTime = System.currentTimeMillis();
            backgroundHandler.post(screenshotRunnable);

            Log.d(TAG, "托管已开始");
        }
    }

    /**
     * 停止托管
     */
    public void stopHosting() {
        synchronized (hostingLock) {
            if (!isHosting) {
                return;
            }

            isHosting = false;
            if (backgroundHandler != null && screenshotRunnable != null) {
                backgroundHandler.removeCallbacks(screenshotRunnable);
            }

            if (hostingStatusListener != null) {
                hostingStatusListener.onHostingStatusChanged(false);
            }

            // 显示托管结束提示
            mainHandler.post(() -> Toast.makeText(this, "托管已停止", Toast.LENGTH_SHORT).show());

            Log.d(TAG, "托管已停止");
        }
    }

    /**
     * 判断是否正在托管
     */
    public boolean isHosting() {
        synchronized (hostingLock) {
            return isHosting;
        }
    }

    /**
     * 执行截图并处理
     */
    private void takeScreenshotAndProcess() {
        if (!isHosting) {
            return;
        }

        try {
            Log.d(TAG, "开始截图...");

            if (hostingStatusListener != null) {
                hostingStatusListener.onScreenshotTaken(true, "正在截图...");
            }

            if (Build.VERSION.SDK_INT >= 30) { // Android 11 (R) API 30
                // 使用Android 11的原生AccessibilityService截图
                takeAccessibilityNativeScreenshot();
            } else {
                Log.e(TAG, "设备Android版本过低，不支持AccessibilityService截图功能");
                stopHosting();

                if (hostingStatusListener != null) {
                    hostingStatusListener.onHostingError("设备不支持AccessibilityService截图功能，需要Android 11或更高版本");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "执行截图时出错: " + e.getMessage(), e);
            scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);

            if (hostingStatusListener != null) {
                hostingStatusListener.onHostingError("执行截图出错: " + e.getMessage());
            }
        }
    }

    /**
     * 使用AccessibilityService原生截图API
     */
    @RequiresApi(api = 30) // Build.VERSION_CODES.R
    @SuppressLint("NewApi")
    private void takeAccessibilityNativeScreenshot() {
        try {
            Log.d(TAG, "调用AccessibilityService原生截图API...");

            // 创建回调
            AccessibilityService.TakeScreenshotCallback callback = new AccessibilityService.TakeScreenshotCallback() {
                @Override
                public void onSuccess(AccessibilityService.ScreenshotResult result) {
                    try {
                        Log.d(TAG, "原生截图成功");

                        // 直接获取Bitmap
                        Bitmap bitmap = result.getBitmap();

                        if (bitmap != null) {
                            Log.d(TAG, "成功获取截图Bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                            // 创建副本以防止原始位图被回收
                            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                            // 处理截图
                            backgroundHandler.post(() -> handleScreenshotResult(copy));
                        } else {
                            Log.e(TAG, "截图Bitmap为空");
                            scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理截图结果失败: " + e.getMessage(), e);
                        Log.e(TAG, "详细错误: ", e);
                        scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    Log.e(TAG, "截图失败，错误码: " + errorCode);
                    scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
                }
            };

            // 调用系统API进行截图
            takeScreenshot(Display.DEFAULT_DISPLAY, getApplicationContext().getMainExecutor(), callback);
            Log.d(TAG, "已请求系统进行截图，等待回调");
        } catch (Exception e) {
            Log.e(TAG, "请求截图失败: " + e.getMessage(), e);
            Log.e(TAG, "详细错误: ", e);
            scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
        }
    }

    /**
     * 处理截图结果
     */
    private void handleScreenshotResult(Bitmap screenshot) {
        try {
            Log.d(TAG, "处理截图结果: " + screenshot.getWidth() + "x" + screenshot.getHeight());

            // 保存截图
            saveScreenshotToFile(screenshot);

            // 处理截图
            processScreenshot(screenshot);

            // 通知监听器
            if (hostingStatusListener != null) {
                hostingStatusListener.onScreenshotTaken(true, "截图成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "处理截图结果失败: " + e.getMessage(), e);
            scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);

            if (hostingStatusListener != null) {
                hostingStatusListener.onHostingError("处理截图结果失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理截图
     */
    private void processScreenshot(Bitmap screenshot) {
        try {
            if (!isHosting) {
                return;
            }

            // 检查是否为支付界面
            boolean isPaymentScreen = checkIsPaymentScreen(screenshot);
            if (!isPaymentScreen) {
                Log.w(TAG, "当前不是支付界面");
                scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);

                if (hostingStatusListener != null) {
                    hostingStatusListener.onHostingError("当前不是支付界面");
                }
                return;
            }

            // 识别二维码
            String qrCodeContent = recognizeQRCode(screenshot);
            if (qrCodeContent.isEmpty()) {
                Log.w(TAG, "未能识别二维码");
                scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
                return;
            }

            // 识别其他信息
            String cardNumber = recognizeCardNumber(screenshot);
            String balance = recognizeBalance(screenshot);
            String expiryTimeText = recognizeExpiryTime(screenshot);

            // 解析过期时间
            long expiryTime = parseExpiryTime(expiryTimeText);
            qrCodeExpiryTime = expiryTime;
            lastQrCodeContent = qrCodeContent;

            Log.d(TAG, "成功识别支付信息 - 卡号: " + cardNumber + ", 余额: " + balance +
                    ", 二维码过期时间: " + new Date(expiryTime));

            // 通知监听器
            if (hostingStatusListener != null) {
                hostingStatusListener.onPaymentInfoRecognized(qrCodeContent, cardNumber, balance, expiryTime);
            }

            // 安排下一次截图
            scheduleNextScreenshotBasedOnExpiry(expiryTime);

        } catch (Exception e) {
            Log.e(TAG, "处理截图失败: " + e.getMessage(), e);
            scheduleNextScreenshot(DEFAULT_SCREENSHOT_INTERVAL);
        }
    }

    /**
     * 检查是否为支付界面
     */
    private boolean checkIsPaymentScreen(Bitmap screenshot) {
        // 此处应该使用PaymentTemplateManager的方法，但目前直接返回true以便测试
        return true;
    }

    /**
     * 识别二维码
     */
    private String recognizeQRCode(Bitmap screenshot) {
        // 此处应该使用PaymentTemplateManager的方法，但目前返回测试值
        return "test_qr_code_content";
    }

    /**
     * 识别卡号
     */
    private String recognizeCardNumber(Bitmap screenshot) {
        // 此处应该使用PaymentTemplateManager的方法，但目前返回测试值
        return "6222 **** **** 1234";
    }

    /**
     * 识别余额
     */
    private String recognizeBalance(Bitmap screenshot) {
        // 此处应该使用PaymentTemplateManager的方法，但目前返回测试值
        return "¥1,234.56";
    }

    /**
     * 识别过期时间
     */
    private String recognizeExpiryTime(Bitmap screenshot) {
        // 此处应该使用PaymentTemplateManager的方法，但目前返回测试值
        return "00:59";
    }

    /**
     * 保存截图到文件
     */
    private void saveScreenshotToFile(Bitmap bitmap) {
        try {
            File directory = new File(getExternalFilesDir(null) + SCREENSHOTS_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(directory, "screenshot_" + timestamp + ".jpg");

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "截图已保存到: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "保存截图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据过期时间安排下一次截图
     */
    private void scheduleNextScreenshotBasedOnExpiry(long expiryTime) {
        long currentTime = System.currentTimeMillis();
        long timeUntilExpiry = expiryTime - currentTime;

        if (timeUntilExpiry <= 0) {
            // 已经过期，立即截图
            scheduleNextScreenshot(1000);
        } else if (timeUntilExpiry < 10000) {
            // 不到10秒，每秒截图一次
            scheduleNextScreenshot(1000);
        } else if (timeUntilExpiry < 30000) {
            // 不到30秒，每3秒截图一次
            scheduleNextScreenshot(3000);
        } else {
            // 超过30秒，每10秒截图一次
            scheduleNextScreenshot(10000);
        }
    }

    /**
     * 安排下一次截图
     */
    private void scheduleNextScreenshot(long delay) {
        if (!isHosting || backgroundHandler == null || screenshotRunnable == null) {
            return;
        }

        nextScreenshotTime = System.currentTimeMillis() + delay;
        backgroundHandler.postDelayed(screenshotRunnable, delay);
        Log.d(TAG, "已安排下一次截图，延迟: " + delay + "ms");
    }

    /**
     * 解析过期时间
     */
    private long parseExpiryTime(String timeText) {
        try {
            // 假设格式为 "00:59"，表示还有59秒过期
            String[] parts = timeText.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return System.currentTimeMillis() + (minutes * 60 + seconds) * 1000;
            }
        } catch (Exception e) {
            Log.e(TAG, "解析过期时间出错: " + e.getMessage(), e);
        }

        // 默认1分钟后过期
        return System.currentTimeMillis() + 60000;
    }
}