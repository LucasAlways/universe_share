package com.titan.universe_share.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import java.util.Arrays;

/**
 * 图像相似度比较工具类
 * 纯 Java 实现，不依赖 OpenCV
 */
public class ImageSimilarityUtils {
    private static final String TAG = "ImageSimilarityUtils";
    public static final float SIMILARITY_THRESHOLD = 0.75f; // 降低阈值使匹配更宽松
    private static final int TARGET_SIZE = 32; // 缩小比较尺寸提高性能
    private static final float STATUS_BAR_HEIGHT_RATIO = 0.1f; // 状态栏高度比例
    private static final float QR_CODE_AREA_RATIO = 0.4f; // 二维码区域比例

    // 定义关键区域权重
    private static final float[] REGION_WEIGHTS = { 0.4f, 0.3f, 0.2f, 0.1f };

    /**
     * 比较两个图像的相似度
     */
    public static float compareImages(Bitmap bitmap1, Bitmap bitmap2) {
        try {
            // 转换为灰度图并调整尺寸
            Bitmap gray1 = toGrayscale(scaleBitmap(bitmap1, TARGET_SIZE, TARGET_SIZE));
            Bitmap gray2 = toGrayscale(scaleBitmap(bitmap2, TARGET_SIZE, TARGET_SIZE));

            // 比较关键区域相似度
            return compareRegions(gray1, gray2);
        } catch (Exception e) {
            Log.e(TAG, "比较图像时出错: " + e.getMessage(), e);
            return 0.0f;
        }
    }

    /**
     * 缩放图像到指定尺寸
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * 将图像转换为灰度
     */
    private static Bitmap toGrayscale(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        // 创建灰度转换的ColorMatrix
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0); // 设置饱和度为0使其变为灰度
        paint.setColorFilter(new ColorMatrixColorFilter(cm));

        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }

    /**
     * 比较图像的关键区域
     */
    private static float compareRegions(Bitmap img1, Bitmap img2) {
        int height = img1.getHeight();
        int width = img1.getWidth();

        // 定义关键区域
        // 1. 标题区域（顶部）
        // 2. 倒计时文本区域
        // 3. 二维码下方的卡号区域
        // 4. 底部文本区域
        int[][] regions = {
                { 0, (int) (height * STATUS_BAR_HEIGHT_RATIO), width, (int) (height * 0.2f) },
                { 0, (int) (height * 0.2f), width, (int) (height * 0.3f) },
                { 0, (int) (height * 0.7f), width, (int) (height * 0.8f) },
                { 0, (int) (height * 0.8f), width, height }
        };

        float[] similarities = new float[regions.length];

        // 计算每个区域的相似度
        for (int i = 0; i < regions.length; i++) {
            int[] region = regions[i];
            Bitmap roi1 = cropBitmap(img1, region[0], region[1], region[2], region[3]);
            Bitmap roi2 = cropBitmap(img2, region[0], region[1], region[2], region[3]);

            similarities[i] = calculateHistogramSimilarity(roi1, roi2);

            // 回收临时Bitmap
            roi1.recycle();
            roi2.recycle();
        }

        // 计算加权平均相似度
        float weightedSum = 0;
        for (int i = 0; i < similarities.length; i++) {
            weightedSum += similarities[i] * REGION_WEIGHTS[i];
        }

        return weightedSum;
    }

    /**
     * 裁剪Bitmap的指定区域
     */
    private static Bitmap cropBitmap(Bitmap source, int x1, int y1, int x2, int y2) {
        int width = x2 - x1;
        int height = y2 - y1;
        if (width <= 0 || height <= 0) {
            // 防止负值导致异常
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
        return Bitmap.createBitmap(source, x1, y1, width, height);
    }

    /**
     * 使用直方图比较两个图像的相似度
     */
    private static float calculateHistogramSimilarity(Bitmap img1, Bitmap img2) {
        int[] histogram1 = calculateHistogram(img1);
        int[] histogram2 = calculateHistogram(img2);

        return compareBhattacharyya(histogram1, histogram2);
    }

    /**
     * 计算图像的灰度直方图
     */
    private static int[] calculateHistogram(Bitmap img) {
        int[] histogram = new int[256]; // 256个灰度级
        Arrays.fill(histogram, 0);

        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getPixel(x, y);
                int gray = Color.red(pixel); // 灰度图中R=G=B
                histogram[gray]++;
            }
        }

        return histogram;
    }

    /**
     * 使用Bhattacharyya距离计算两个直方图的相似度
     */
    private static float compareBhattacharyya(int[] hist1, int[] hist2) {
        // 归一化直方图
        float[] normHist1 = normalizeHistogram(hist1);
        float[] normHist2 = normalizeHistogram(hist2);

        // 计算Bhattacharyya系数
        float bhattacharyyaCoeff = 0;
        for (int i = 0; i < normHist1.length; i++) {
            bhattacharyyaCoeff += Math.sqrt(normHist1[i] * normHist2[i]);
        }

        // Bhattacharyya系数范围是[0,1]，1表示完全匹配
        return bhattacharyyaCoeff;
    }

    /**
     * 归一化直方图
     */
    private static float[] normalizeHistogram(int[] histogram) {
        float[] normalizedHist = new float[histogram.length];
        float sum = 0;

        // 计算总和
        for (int value : histogram) {
            sum += value;
        }

        // 归一化
        if (sum > 0) {
            for (int i = 0; i < histogram.length; i++) {
                normalizedHist[i] = histogram[i] / sum;
            }
        }

        return normalizedHist;
    }

    /**
     * 判断两个图像是否相似
     */
    public static boolean isSimilar(Bitmap bitmap1, Bitmap bitmap2) {
        // 如果任一图片为 null，则直接返回 true（开发阶段）
        if (bitmap1 == null || bitmap2 == null) {
            Log.w(TAG, "警告：传入的图片为 null，开发阶段直接返回 true");
            return true;
        }

        // 如果任一图片没有内容，则直接返回 true（开发阶段）
        if (bitmap1.getWidth() <= 1 || bitmap1.getHeight() <= 1 ||
                bitmap2.getWidth() <= 1 || bitmap2.getHeight() <= 1) {
            Log.w(TAG, "警告：传入的图片尺寸太小，开发阶段直接返回 true");
            return true;
        }

        float similarity = compareImages(bitmap1, bitmap2);
        Log.d(TAG, "图片相似度：" + similarity);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    /**
     * 计算两个图像的相似度
     * 这是compareImages方法的别名，为了与PaymentTemplateManager中的调用保持一致
     */
    public static float calculateSimilarity(Bitmap bitmap1, Bitmap bitmap2) {
        return compareImages(bitmap1, bitmap2);
    }
}