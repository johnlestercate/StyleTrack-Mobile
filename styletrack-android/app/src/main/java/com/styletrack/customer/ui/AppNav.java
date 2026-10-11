package com.styletrack.customer.ui;

import android.os.Bundle;

/** Small helpers for building nav-graph argument bundles. Replaces the route-string helpers in AppNav.kt. */
public final class AppNav {
    private AppNav() {}

    public static Bundle idArg(String key, long id) {
        Bundle b = new Bundle();
        b.putLong(key, id);
        return b;
    }

    public static Bundle bookArgs(long serviceId, long stylistId, boolean home) {
        Bundle b = new Bundle();
        b.putLong("serviceId", serviceId);
        b.putLong("stylistId", stylistId);
        b.putBoolean("home", home);
        return b;
    }
}
