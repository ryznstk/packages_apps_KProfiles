package com.android.kprofiles.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.android.kprofiles.Constants;
import com.android.kprofiles.utils.FileUtils;
import com.android.kprofiles.R;

public class KProfilesModesTileService extends TileService {

    private boolean mSelfChange = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (intent.getAction().equals(Constants.ACTION_KPROFILE_SETTING_CHANGED)) {
                if (mSelfChange) {
                    mSelfChange = false;
                    return;
                }
                updateTileContent();
            }
        }
    };

    @Override
    public void onStartListening() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_KPROFILE_SETTING_CHANGED);
        registerReceiver(stateReceiver, filter);

        updateTileContent();
    }

    @Override
    public void onStopListening() {
        try {
            unregisterReceiver(stateReceiver);
        } catch (Exception e) {
            // ignore if receiver was not registered
        }
        super.onStopListening();
    }

    @Override
    public void onClick() {
        String mode = getMode();
        switch (mode) {
            case "0":
                mode = "1"; // Set mode from none to battery
                break;
            case "1":
                mode = "2"; // Set mode from battery to balanced
                break;
            case "2":
                mode = "3"; // Set mode from balanced to performance
                break;
            case "3":
                mode = "0"; // Set mode from performance to none
                break;
            default:
                mode = "0";
                break;
        }

        mSelfChange = true;
        final Intent intent = new Intent(Constants.ACTION_KPROFILE_SETTING_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        getApplicationContext().sendBroadcastAsUser(intent, UserHandle.CURRENT);

        setMode(mode);
        updateTileContent();
        super.onClick();
    }

    private void setMode(String mode) {
        try {
            FileUtils.writeLine(Constants.KPROFILES_MODES_NODE, mode);
        } catch (Exception e) {
            // ignore write failures
        }
    }

    private String getMode() {
        final String value = FileUtils.readOneLine(Constants.KPROFILES_MODES_NODE);
        return value != null ? value : "0";
    }

    private void updateTileContent() {
        Tile tile = getQsTile();
        String mode = getMode();
        boolean isActive = mode != null && !mode.equals("0");

        tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        setTileTextForMode(mode, tile);
        tile.updateTile();
    }

    private void setTileTextForMode(String mode, Tile tile) {
        final CharSequence none = getResources().getString(R.string.kprofiles_modes_none);
        final CharSequence battery = getResources().getString(R.string.kprofiles_modes_battery);
        final CharSequence balanced = getResources().getString(R.string.kprofiles_modes_balanced);
        final CharSequence perf = getResources().getString(R.string.kprofiles_modes_performance);

        if (mode == null) mode = "0";
        switch (mode) {
            case "1":
                tile.setContentDescription(battery);
                tile.setSubtitle(battery);
                break;
            case "2":
                tile.setContentDescription(balanced);
                tile.setSubtitle(balanced);
                break;
            case "3":
                tile.setContentDescription(perf);
                tile.setSubtitle(perf);
                break;
            case "0":
            default:
                tile.setContentDescription(none);
                tile.setSubtitle(none);
                break;
        }
    }
}
