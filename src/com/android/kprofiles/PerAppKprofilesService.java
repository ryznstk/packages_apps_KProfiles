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

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class PerAppKprofilesService extends Service {

    private static final String TAG = "PerAppKprofilesService";
    private static final boolean DEBUG = false;

    private String mPreviousApp;
    private KprofilesUtils mKprofilesUtils;

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        mKprofilesUtils = new KprofilesUtils(this);
        registerTaskStackListener();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mKprofilesUtils.setDefaultProfile();
        super.onDestroy();
    }

    private void registerTaskStackListener() {
        TaskStackListener taskListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                onForegroundAppChanged();
            }
        };

        try {
            IActivityTaskManager atm = ActivityTaskManager.getService();
            atm.registerTaskStackListener(taskListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener", e);
        }
    }

    private void onForegroundAppChanged() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                if (topActivity != null) {
                    String foregroundApp = topActivity.getPackageName();
                    if (!foregroundApp.equals(mPreviousApp)) {
                        mKprofilesUtils.setKprofilesProfile(foregroundApp);
                        mPreviousApp = foregroundApp;
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Error getting foreground app", e);
        }
    }
}