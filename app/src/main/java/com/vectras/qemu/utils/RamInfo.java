package com.vectras.qemu.utils;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.vectras.vm.utils.TextUtils;

public class RamInfo {
    public static Activity activity;
    private static final int MIN_VALID_VM_MEMORY_MB = 128;

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    static int ensureMinimumVmMemoryMb(int memoryMb) {
        return Math.max(MIN_VALID_VM_MEMORY_MB, memoryMb);
    }

    public static int vectrasMemory(Activity activity) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long freeMem = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        int freeRamInt = safeLongToInt(freeMem);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean("customMemory", false) && TextUtils.isNumberOnly(prefs.getString("memory", String.valueOf(256)))) {
            return ensureMinimumVmMemoryMb(Integer.parseInt(prefs.getString("memory", String.valueOf(256))));
        } else {
            return ensureMinimumVmMemoryMb(freeRamInt - 100);
        }
    }
}
