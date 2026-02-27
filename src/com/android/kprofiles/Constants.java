package com.android.kprofiles;

public class Constants {

    /* KProfiles */
    public static final String KEY_KPROFILES_AUTO = "pref_kprofiles_auto";
    public static final String KPROFILES_AUTO_NODE = "/sys/module/kprofiles/parameters/auto_kp";
    public static final String KEY_KPROFILES_MODES = "pref_kprofiles_modes";
    public static final String KPROFILES_MODES_NODE = "/sys/kernel/kprofiles/kp_mode";
    public static final String KPROFILES_MODES_INFO = "pref_kprofiles_modes_info";

    /* Broadcast action sent when kprofiles mode changes */
    public static final String ACTION_KPROFILE_SETTING_CHANGED = "com.android.kprofiles.battery.KPROFILE_CHANGED";

    /* Per-app Kprofiles */
    public static final String KEY_PER_APP_KPROFILES_DISABLED_PACKAGES = "per_app_kprofiles_disabled_packages"; // StringSet
    public static final String KEY_PER_APP_KPROFILES_MODES = "per_app_kprofiles_modes"; // StringSet of "packageName:mode" entries

    /** Mode value meaning "inherit the global mode" (no per-app override). */
    public static final int PER_APP_MODE_DEFAULT = -1;

    private Constants() {
        // utility class
    }

}
