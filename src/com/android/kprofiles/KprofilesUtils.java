/*
 * Copyright (C) 2026 GuidixX
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.kprofiles;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import com.android.kprofiles.utils.FileUtils;

public final class KprofilesUtils {

    // Profile indices — match spinner positions
    public static final int STATE_DEFAULT     = 0; // use global setting
    public static final int STATE_NONE        = 1; // kp_mode = 0
    public static final int STATE_BATTERY     = 2; // kp_mode = 1
    public static final int STATE_BALANCED    = 3; // kp_mode = 2
    public static final int STATE_PERFORMANCE = 4; // kp_mode = 3

    public static final int STATE_COUNT = 5;

    /** Kernel node values indexed by STATE_* constants. */
    private static final String[] KPROFILES_VALUES = {
            /* STATE_DEFAULT     */ null,  // never written directly
            /* STATE_NONE        */ "0",
            /* STATE_BATTERY     */ "1",
            /* STATE_BALANCED    */ "2",
            /* STATE_PERFORMANCE */ "3",
    };

    private static final String KPROFILES_CONTROL = "per_app_kprofiles_control";

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    public KprofilesUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void startService(Context context) {
        if (!FileUtils.fileExists(Constants.KPROFILES_MODES_NODE)) return;
        // Only start the service if at least one per-app override exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String control = prefs.getString(KPROFILES_CONTROL, null);
        if (control == null || control.isEmpty()) return;
        // Check if any package is actually assigned (any segment contains a comma after prefix)
        boolean hasOverrides = false;
        for (String segment : control.split(":")) {
            // Each segment is like "kp.none=com.pkg," — has a package if there's a comma
            int eq = segment.indexOf('=');
            if (eq >= 0 && segment.length() > eq + 1) {
                hasOverrides = true;
                break;
            }
        }
        if (hasOverrides) {
            context.startServiceAsUser(new Intent(context, PerAppKprofilesService.class),
                    UserHandle.CURRENT);
        }
    }

    // ------------------------------------------------------------------ storage
    // Packages are stored as a single colon-separated string of
    // "prefix<pkg>," entries, one segment per profile slot (STATE_NONE..STATE_PERFORMANCE).
    // This mirrors ThermalUtils storage exactly.

    private static final String[] KPROFILES_PREFIXES = {
            /* STATE_NONE        */ "kp.none=",
            /* STATE_BATTERY     */ "kp.battery=",
            /* STATE_BALANCED    */ "kp.balanced=",
            /* STATE_PERFORMANCE */ "kp.performance=",
    };

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(KPROFILES_CONTROL, profiles).apply();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(KPROFILES_CONTROL, null);

        if (value != null) {
            String[] modes = value.split(":");
            if (modes.length < STATE_COUNT - 1) value = null;
        }

        if (value == null || value.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < KPROFILES_PREFIXES.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(KPROFILES_PREFIXES[i]);
            }
            value = sb.toString();
            writeValue(value);
        }
        return value;
    }

    /** Save the per-app profile. Pass STATE_DEFAULT (0) to clear the override. */
    public void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");

        // mode 1..4 maps to segment index 0..3
        if (mode >= STATE_NONE && mode < STATE_COUNT) {
            modes[mode - 1] = modes[mode - 1] + packageName + ",";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modes.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(modes[i]);
        }
        writeValue(sb.toString());
    }

    /** Returns the user-assigned STATE_* for a package, or STATE_DEFAULT if none. */
    public int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].contains(packageName + ",")) {
                return i + 1; // segment 0 → STATE_NONE(1), etc.
            }
        }
        return STATE_DEFAULT;
    }

    // ------------------------------------------------------------------ kernel

    public void setDefaultProfile() {
        String global = mSharedPrefs.getString(Constants.KEY_KPROFILES_MODES, "0");
        FileUtils.writeLine(Constants.KPROFILES_MODES_NODE, global);
    }

    /** Apply the per-app profile for the given package (or restore global if DEFAULT). */
    public void setKprofilesProfile(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_DEFAULT) {
            setDefaultProfile();
            return;
        }
        String value = KPROFILES_VALUES[state];
        if (value != null) {
            FileUtils.writeLine(Constants.KPROFILES_MODES_NODE, value);
        }
    }

    /**
     * If the given package is currently in the foreground, apply its profile immediately.
     * Called after the user manually assigns a profile in the UI.
     */
    public void applyIfForeground(String packageName) {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager)
                    mContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            java.util.List<android.app.ActivityManager.RunningTaskInfo> tasks =
                    am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()) {
                android.content.ComponentName top = tasks.get(0).topActivity;
                if (top != null && packageName.equals(top.getPackageName())) {
                    setKprofilesProfile(packageName);
                }
            }
        } catch (Exception e) {
            // ignore task query failures
        }
    }
}
