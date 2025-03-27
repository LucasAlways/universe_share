package com.titan.universe_share.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.titan.universe_share.R;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理支付界面模板的类
 */
public class PaymentTemplateManager {
    private static final String TAG = "PaymentTemplateManager";

    // 模板类型
    public static final int TEMPLATE_PAYMENT = 0; // 支付宝付款码界面

    // 保存已加载的模板图片
    private List<Bitmap> templateBitmaps = new ArrayList<>();

    // 保存Context引用
    private Context context;

    // 正则表达式模式
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)s|付款码剩余有效期[：:]*\\s*(\\d+)s");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("(?:卡号|:|卡号[：:])\\s*([0-9]{9})");
    private static final Pattern BALANCE_PATTERN = Pattern.compile("(?:余额[：:]|\\(余额[：:])\\s*([0-9,.]+)");
    private static final Pattern FULL_CARD_PATTERN = Pattern.compile("卡号[：:]\\s*(\\d+)\\s*\\(余额[：:]\\s*([0-9,.]+)\\)");

    /**
     * 初始化并加载所有支付模板
     */
    public PaymentTemplateManager(Context context) {
        this.context = context;
        loadTemplates(context);
    }

    /**
     * 从资源加载模板图片
     */
    private void loadTemplates(Context context) {
        try {
            // 加载多个模板图像
            Bitmap[] templates = loadTemplatesFromResource();
            if (templates != null && templates.length > 0) {
                for (Bitmap template : templates) {
                    if (template != null) {
                        templateBitmaps.add(template);
                    }
                }
                Log.d(TAG, "成功加载支付模板占位图");
            }
        } catch (Exception e) {
            Log.e(TAG, "加载支付模板图片时出错: " + e.getMessage());
        }
    }

    /**
     * 从资源加载所有模板图像
     */
    private Bitmap[] loadTemplatesFromResource() {
        try {
            Bitmap[] templates = new Bitmap[2];

            // 加载第一个模板（6秒倒计时界面）
            templates[0] = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.payment_template_1);

            // 加载第二个模板（59秒倒计时界面）
            templates[1] = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.payment_template_2);

            return templates;
        } catch (Exception e) {
            Log.e(TAG, "加载模板图像失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定类型的模板图片
     */
    public Bitmap getTemplate(int templateType) {
        if (templateType >= 0 && templateType < templateBitmaps.size()) {
            return templateBitmaps.get(templateType);
        }
        return null;
    }

    /**
     * 从截图中提取信息
     * 
     * @param screenshot 截取的屏幕图片
     * @return 包含付款信息的数组: [付款码剩余时间, 卡号, 余额]
     */
    public String[] extractPaymentInfo(Bitmap screenshot) {
        final String[] result = new String[] { "30s", "未知卡号", "未知余额" };

        try {
            // 使用ML Kit的文本识别功能
            TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            InputImage image = InputImage.fromBitmap(screenshot, 0);

            // 创建一个信号量来等待异步操作完成
            final CountDownLatch latch = new CountDownLatch(1);

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        Log.d(TAG, "识别文本: " + fullText);

                        // 首先尝试匹配完整格式
                        Matcher fullMatcher = FULL_CARD_PATTERN.matcher(fullText);
                        if (fullMatcher.find()) {
                            result[1] = fullMatcher.group(1).trim();
                            result[2] = fullMatcher.group(2).trim();
                            Log.d(TAG, "找到完整信息 - 卡号: " + result[1] + ", 余额: " + result[2]);
                        }

                        // 提取时间信息
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                String lineText = line.getText();
                                Log.d(TAG, "分析行文本: " + lineText);

                                // 尝试匹配付款码剩余有效期
                                if (lineText.contains("付款码") && lineText.contains("有效期")) {
                                    Pattern specialTimePattern = Pattern.compile("(\\d+)s|有效期[：:]\\s*(\\d+)s");
                                    Matcher specialTimeMatcher = specialTimePattern.matcher(lineText);
                                    if (specialTimeMatcher.find()) {
                                        result[0] = specialTimeMatcher.group(1) != null
                                                ? specialTimeMatcher.group(1) + "s"
                                                : specialTimeMatcher.group(2) + "s";
                                        Log.d(TAG, "找到特殊时间格式: " + result[0]);
                                    } else {
                                        // 尝试直接提取数字
                                        Pattern digitsPattern = Pattern.compile("(\\d+)");
                                        Matcher digitsMatcher = digitsPattern.matcher(lineText);
                                        if (digitsMatcher.find()) {
                                            result[0] = digitsMatcher.group(1) + "s";
                                            Log.d(TAG, "找到数字时间: " + result[0]);
                                        }
                                    }
                                }

                                // 尝试匹配包含"s"的倒计时
                                Matcher timeMatcher = TIME_PATTERN.matcher(lineText);
                                if (timeMatcher.find()) {
                                    result[0] = timeMatcher.group(1) != null ? timeMatcher.group(1) + "s"
                                            : timeMatcher.group(2) + "s";
                                    Log.d(TAG, "找到时间: " + result[0]);
                                }

                                // 尝试匹配完整卡号和余额信息
                                Matcher combinedMatcher = FULL_CARD_PATTERN.matcher(lineText);
                                if (combinedMatcher.find()) {
                                    result[1] = combinedMatcher.group(1).trim();
                                    result[2] = combinedMatcher.group(2).trim();
                                    Log.d(TAG, "找到组合信息 - 卡号: " + result[1] + ", 余额: " + result[2]);
                                } else {
                                    // 单独尝试匹配卡号
                                    Matcher cardMatcher = CARD_NUMBER_PATTERN.matcher(lineText);
                                    if (cardMatcher.find()) {
                                        result[1] = cardMatcher.group(1).trim();
                                        Log.d(TAG, "找到卡号: " + result[1]);
                                    }

                                    // 单独尝试匹配余额
                                    Matcher balanceMatcher = BALANCE_PATTERN.matcher(lineText);
                                    if (balanceMatcher.find()) {
                                        result[2] = balanceMatcher.group(1).trim();
                                        Log.d(TAG, "找到余额: " + result[2]);
                                    }
                                }
                            }
                        }
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "文本识别失败: " + e.getMessage());
                        latch.countDown();
                    });

            // 等待最多3秒
            latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

            // 关闭识别器
            recognizer.close();

        } catch (Exception e) {
            Log.e(TAG, "提取付款信息时出错: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 检查截图是否是付款界面
     * 
     * @return 如果是付款界面则返回true
     */
    public boolean isPaymentScreen(Bitmap screenshot) {
        if (templateBitmaps.isEmpty() || screenshot == null) {
            return false;
        }

        try {
            // 1. 尝试匹配每个模板的图像相似度
            for (Bitmap template : templateBitmaps) {
                if (template != null) {
                    float similarity = ImageSimilarityUtils.calculateSimilarity(template, screenshot);
                    Log.d(TAG, "支付界面相似度: " + similarity);

                    // 如果相似度非常高，直接返回true
                    if (similarity >= 0.85) {
                        return true;
                    }
                }
            }

            // 2. 如果相似度不高，使用文本特征检测
            TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            InputImage image = InputImage.fromBitmap(screenshot, 0);

            // 创建一个信号量来等待异步操作完成
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] isPaymentScreen = { false };

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();

                        // 检查特征文本是否存在
                        boolean hasQrCode = fullText.contains("二维码") || fullText.contains("付款码");
                        boolean hasExpiry = fullText.contains("有效期") || fullText.contains("倒计时");
                        boolean hasCardInfo = fullText.contains("卡号")
                                || fullText.matches(".*\\d{4}\\s*\\*{4}\\s*\\d{4}.*");

                        // 在支付宝付款码页面的特征
                        if (fullText.contains("付款码剩余有效期") ||
                                (hasQrCode && hasExpiry) ||
                                (hasCardInfo && hasExpiry)) {
                            isPaymentScreen[0] = true;
                            Log.d(TAG, "通过文本特征检测到支付界面");
                        }

                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "文本识别失败: " + e.getMessage());
                        latch.countDown();
                    });

            // 等待最多2秒
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS);

            // 关闭识别器
            recognizer.close();

            return isPaymentScreen[0];
        } catch (Exception e) {
            Log.e(TAG, "检查支付界面时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查二维码是否即将过期
     * 
     * @param timeText 提取的时间文本，例如 "6s" 或 "59s" 或 "01:59"
     * @return 如果即将过期则返回true
     */
    public boolean isQrCodeExpiring(String timeText) {
        try {
            int seconds = 0;

            // 移除所有非数字、冒号和字母的字符
            String cleanText = timeText.replaceAll("[^0-9:smSM]", "").trim();

            if (cleanText.contains(":")) {
                // 格式为 mm:ss
                String[] parts = cleanText.split(":");
                if (parts.length == 2) {
                    int minutes = Integer.parseInt(parts[0]);
                    seconds = Integer.parseInt(parts[1]) + minutes * 60;
                }
            } else if (cleanText.toLowerCase().contains("s")) {
                // 格式为 XXs
                cleanText = cleanText.toLowerCase().replace("s", "");
                seconds = Integer.parseInt(cleanText);
            } else {
                // 尝试直接解析数字
                seconds = Integer.parseInt(cleanText);
            }

            Log.d(TAG, "解析倒计时: " + timeText + " => " + seconds + "秒");
            return seconds <= 15; // 15秒以内认为即将过期
        } catch (Exception e) {
            Log.e(TAG, "解析时间文本出错: " + timeText + ", " + e.getMessage());
            return false;
        }
    }

    /**
     * 释放所有模板资源
     */
    public void release() {
        for (Bitmap bitmap : templateBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        templateBitmaps.clear();
    }
}