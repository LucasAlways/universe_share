package com.titan.universe_share.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理支付界面识别的类
 */
public class PaymentTemplateManager {
    private static final String TAG = "PaymentTemplateManager";

    // 保存Context引用
    private Context context;

    // 正则表达式模式
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)s|付款码剩余有效期[：:]*\\s*(\\d+)s");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("(?:卡号|:|卡号[：:])\\s*([0-9]{9})");
    private static final Pattern BALANCE_PATTERN = Pattern.compile("(?:余额[：:]|\\(余额[：:])\\s*([0-9,.]+)");
    private static final Pattern FULL_CARD_PATTERN = Pattern.compile("卡号[：:]\\s*(\\d+)\\s*\\(余额[：:]\\s*([0-9,.]+)\\)");

    /**
     * 初始化支付识别管理器
     */
    public PaymentTemplateManager(Context context) {
        this.context = context;
    }

    /**
     * 从截图中提取信息
     * 
     * @param screenshot 截取的屏幕图片
     * @return 包含付款信息的数组: [付款码剩余时间, 卡号, 余额]
     */
    public String[] extractPaymentInfo(Bitmap screenshot) {
        final String[] result = new String[] { "未找到有效信息", "未找到有效信息", "未找到有效信息" };
        final boolean[] foundValidInfo = { false };

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
                            foundValidInfo[0] = true;
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
                                        foundValidInfo[0] = true;
                                        Log.d(TAG, "找到特殊时间格式: " + result[0]);
                                    } else {
                                        // 尝试直接提取数字
                                        Pattern digitsPattern = Pattern.compile("(\\d+)");
                                        Matcher digitsMatcher = digitsPattern.matcher(lineText);
                                        if (digitsMatcher.find()) {
                                            result[0] = digitsMatcher.group(1) + "s";
                                            foundValidInfo[0] = true;
                                            Log.d(TAG, "找到数字时间: " + result[0]);
                                        }
                                    }
                                }

                                // 尝试匹配包含"s"的倒计时
                                Matcher timeMatcher = TIME_PATTERN.matcher(lineText);
                                if (timeMatcher.find()) {
                                    result[0] = timeMatcher.group(1) != null ? timeMatcher.group(1) + "s"
                                            : timeMatcher.group(2) + "s";
                                    foundValidInfo[0] = true;
                                    Log.d(TAG, "找到时间: " + result[0]);
                                }

                                // 尝试匹配完整卡号和余额信息
                                Matcher combinedMatcher = FULL_CARD_PATTERN.matcher(lineText);
                                if (combinedMatcher.find()) {
                                    result[1] = combinedMatcher.group(1).trim();
                                    result[2] = combinedMatcher.group(2).trim();
                                    foundValidInfo[0] = true;
                                    Log.d(TAG, "找到组合信息 - 卡号: " + result[1] + ", 余额: " + result[2]);
                                } else {
                                    // 单独尝试匹配卡号
                                    Matcher cardMatcher = CARD_NUMBER_PATTERN.matcher(lineText);
                                    if (cardMatcher.find()) {
                                        result[1] = cardMatcher.group(1).trim();
                                        foundValidInfo[0] = true;
                                        Log.d(TAG, "找到卡号: " + result[1]);
                                    }

                                    // 单独尝试匹配余额
                                    Matcher balanceMatcher = BALANCE_PATTERN.matcher(lineText);
                                    if (balanceMatcher.find()) {
                                        result[2] = balanceMatcher.group(1).trim();
                                        foundValidInfo[0] = true;
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

            // 如果没有找到有效信息，明确记录日志
            if (!foundValidInfo[0]) {
                Log.w(TAG, "未能提取到有效的付款信息");
            }

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
        if (screenshot == null) {
            return false;
        }

        try {
            // 使用文本特征检测 - 直接使用OCR提取文字，然后进行正则表达式匹配
            TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            InputImage image = InputImage.fromBitmap(screenshot, 0);

            // 创建一个信号量来等待异步操作完成
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] isPaymentScreen = { false };

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        Log.d(TAG, "OCR识别文本: " + fullText);

                        // 必须包含的字段（正则表达式匹配）
                        boolean hasTimeInfo = Pattern.compile("付款码剩余有效期[：:]*\\s*(\\d+)s").matcher(fullText).find();
                        boolean hasCardInfo = Pattern
                                .compile("卡号[：:]\\s*(\\d{9})\\s*[（\\(]余额[：:]\\s*(\\d+\\.\\d{2})[）\\)]")
                                .matcher(fullText).find();
                        boolean hasBalanceInfo = Pattern.compile("余额\\s*=\\s*账户余额\\s*\\+\\s*补贴余额").matcher(fullText)
                                .find();

                        // 记录匹配结果
                        Log.d(TAG,
                                "支付界面特征匹配: 倒计时=" + hasTimeInfo + ", 卡号与余额=" + hasCardInfo + ", 余额说明=" + hasBalanceInfo);

                        // 至少满足两个条件即视为有效支付界面（增加容错性）
                        if ((hasTimeInfo && hasCardInfo) ||
                                (hasTimeInfo && hasBalanceInfo) ||
                                (hasCardInfo && hasBalanceInfo)) {
                            isPaymentScreen[0] = true;
                            Log.d(TAG, "通过OCR特征检测到支付界面");
                        } else {
                            // 额外检测支付特征词
                            boolean hasPayKeywords = fullText.contains("支付宝") &&
                                    (fullText.contains("付款码") || fullText.contains("二维码")) &&
                                    fullText.contains("有效期");
                            if (hasPayKeywords) {
                                isPaymentScreen[0] = true;
                                Log.d(TAG, "通过关键词检测到支付界面");
                            }
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
     * 释放资源
     */
    public void release() {
        // 没有需要释放的资源
    }
}