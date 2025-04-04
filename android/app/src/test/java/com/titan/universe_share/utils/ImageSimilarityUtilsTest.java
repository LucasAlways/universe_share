package com.titan.universe_share.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImageSimilarityUtilsTest {

    @Test
    public void testIdenticalImages() {
        // 创建两个完全相同的图片
        Bitmap bitmap1 = createTestBitmap(100, 100);
        Bitmap bitmap2 = createTestBitmap(100, 100);

        float similarity = ImageSimilarityUtils.compareImages(bitmap1, bitmap2);
        assertTrue("完全相同的图片应该返回高相似度", similarity > 0.9f);

        bitmap1.recycle();
        bitmap2.recycle();
    }

    @Test
    public void testDifferentSizedImages() {
        // 创建两个不同尺寸但内容相同的图片
        Bitmap bitmap1 = createTestBitmap(100, 100);
        Bitmap bitmap2 = createTestBitmap(200, 200);

        float similarity = ImageSimilarityUtils.compareImages(bitmap1, bitmap2);
        assertTrue("不同尺寸但内容相同的图片应该返回较高相似度", similarity > 0.7f);

        bitmap1.recycle();
        bitmap2.recycle();
    }

    @Test
    public void testCompletelyDifferentImages() {
        // 创建两个完全不同的图片
        Bitmap bitmap1 = createTestBitmap(100, 100);
        Bitmap bitmap2 = createTestBitmap(100, 100);

        // 在第二张图片上绘制不同的内容
        for (int x = 0; x < bitmap2.getWidth(); x++) {
            for (int y = 0; y < bitmap2.getHeight(); y++) {
                bitmap2.setPixel(x, y, Color.RED);
            }
        }

        float similarity = ImageSimilarityUtils.compareImages(bitmap1, bitmap2);
        assertTrue("完全不同的图片应该返回低相似度", similarity < 0.3f);

        bitmap1.recycle();
        bitmap2.recycle();
    }

    @Test
    public void testSimilarImages() {
        // 创建两个相似的图片
        Bitmap bitmap1 = createTestBitmap(100, 100);
        Bitmap bitmap2 = createTestBitmap(100, 100);

        // 在第二张图片上绘制相似但略有不同的内容
        for (int x = 0; x < bitmap2.getWidth(); x++) {
            for (int y = 0; y < bitmap2.getHeight(); y++) {
                if (x < 50 && y < 50) {
                    bitmap2.setPixel(x, y, Color.BLUE);
                } else {
                    bitmap2.setPixel(x, y, Color.WHITE);
                }
            }
        }

        float similarity = ImageSimilarityUtils.compareImages(bitmap1, bitmap2);
        assertTrue("相似的图片应该返回中等相似度", similarity > 0.5f && similarity < 0.8f);

        bitmap1.recycle();
        bitmap2.recycle();
    }

    private Bitmap createTestBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x < width / 2 && y < height / 2) {
                    bitmap.setPixel(x, y, Color.BLUE);
                } else {
                    bitmap.setPixel(x, y, Color.WHITE);
                }
            }
        }
        return bitmap;
    }
}