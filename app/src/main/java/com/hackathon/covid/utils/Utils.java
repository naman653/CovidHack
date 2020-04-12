package com.hackathon.covid.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hackathon.covid.data.MultiLingual;

public class Utils {
    public static boolean isNullOrEmpty(@Nullable String str) {
        if (str == null) {
            return true;
        }

        return str.trim().isEmpty();
    }

    public static MultiLingual getMultiLingual(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        MultiLingual multiLingual = new MultiLingual();

        if (!Utils.isNullOrEmpty(text)) {
            int englishCount = 0;
            int hindiCount = 0;
            for (char c : text.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                    englishCount++;
                } else {
                    hindiCount++;
                }
            }

            if (englishCount > hindiCount) {
                multiLingual.setEn(text);
            } else {
                multiLingual.setHi(text);
            }
        }
        return multiLingual;
    }

    public static void logException(String tag, @NonNull Throwable e) {
        Log.e(tag, e.getMessage(), e);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
