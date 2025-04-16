package com.titan.universe_share.utils;

import android.widget.Toast;

import com.titan.universe_share.TitanApp;

public class ToastUtils {
    private static Toast mToast;

    public static void shortCall(String text) {
        cancel();
        mToast = Toast.makeText(TitanApp.getApp(), text, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void longCall(String text) {
        cancel();
        mToast = Toast.makeText(TitanApp.getApp(), text, Toast.LENGTH_LONG);
        mToast.show();
    }

    private static void cancel() {
        if (mToast != null) {
            mToast.cancel();
        }
    }
}
