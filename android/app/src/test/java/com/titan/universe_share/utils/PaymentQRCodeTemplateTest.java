package com.titan.universe_share.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import org.junit.Test;
import static org.junit.Assert.*;

public class PaymentQRCodeTemplateTest {

    @Test
    public void testPaymentTemplateMatching() {
        // 创建两个模拟的付款码界面
        Bitmap template1 = createPaymentTemplate("285469147", "3011.78", "6s");
        Bitmap template2 = createPaymentTemplate("292675883", "0.00", "59s");

        float similarity = ImageSimilarityUtils.compareImages(template1, template2);
        assertTrue("付款码界面模板应该具有较高的相似度（忽略动态内容）", similarity > 0.7f);

        template1.recycle();
        template2.recycle();
    }

    @Test
    public void testDifferentDeviceSizes() {
        // 模拟不同设备尺寸的付款码界面
        Bitmap template1 = createPaymentTemplate("285469147", "3011.78", "6s", 1080, 2400); // 标准尺寸
        Bitmap template2 = createPaymentTemplate("285469147", "3011.78", "6s", 720, 1600); // 较小尺寸

        float similarity = ImageSimilarityUtils.compareImages(template1, template2);
        assertTrue("不同设备尺寸的付款码界面应该能够匹配", similarity > 0.7f);

        template1.recycle();
        template2.recycle();
    }

    @Test
    public void testDifferentThemeColors() {
        // 模拟不同主题色的付款码界面（有些用户可能使用深色模式）
        Bitmap template1 = createPaymentTemplate("285469147", "3011.78", "6s", true); // 浅色模式
        Bitmap template2 = createPaymentTemplate("285469147", "3011.78", "6s", false); // 深色模式

        float similarity = ImageSimilarityUtils.compareImages(template1, template2);
        assertTrue("不同主题色的付款码界面应该能够匹配", similarity > 0.6f);

        template1.recycle();
        template2.recycle();
    }

    private Bitmap createPaymentTemplate(String cardNumber, String balance, String countdown) {
        return createPaymentTemplate(cardNumber, balance, countdown, 1080, 2400, true);
    }

    private Bitmap createPaymentTemplate(String cardNumber, String balance, String countdown, boolean isLightTheme) {
        return createPaymentTemplate(cardNumber, balance, countdown, 1080, 2400, isLightTheme);
    }

    private Bitmap createPaymentTemplate(String cardNumber, String balance, String countdown, int width, int height) {
        return createPaymentTemplate(cardNumber, balance, countdown, width, height, true);
    }

    private Bitmap createPaymentTemplate(String cardNumber, String balance, String countdown, int width, int height,
            boolean isLightTheme) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 设置背景色
        canvas.drawColor(isLightTheme ? Color.rgb(92, 115, 246) : Color.rgb(45, 56, 120));

        // 绘制白色卡片背景
        paint.setColor(Color.WHITE);
        float cardTop = height * 0.15f;
        float cardBottom = height * 0.5f;
        canvas.drawRect(width * 0.1f, cardTop, width * 0.9f, cardBottom, paint);

        // 绘制倒计时文本
        paint.setColor(Color.BLACK);
        paint.setTextSize(width * 0.04f);
        String countdownText = "付款码剩余有效期: " + countdown;
        canvas.drawText(countdownText, width * 0.2f, cardTop + height * 0.05f, paint);

        // 模拟二维码区域（简化为黑色方块）
        paint.setColor(Color.BLACK);
        float qrSize = width * 0.4f;
        float qrLeft = (width - qrSize) / 2;
        float qrTop = cardTop + height * 0.1f;
        canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, paint);

        // 绘制卡号和余额信息
        paint.setColor(Color.BLACK);
        paint.setTextSize(width * 0.035f);
        String cardInfo = "卡号: " + cardNumber + " (余额: " + balance + ")";
        canvas.drawText(cardInfo, width * 0.2f, cardBottom - height * 0.05f, paint);

        return bitmap;
    }
}