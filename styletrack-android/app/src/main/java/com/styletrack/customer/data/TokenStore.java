package com.styletrack.customer.data;

import android.content.Context;
import android.content.SharedPreferences;

/** The token and a few small values live in app-private storage (backups are disabled in the manifest). */
public class TokenStore {
    private static final String KEY_TOKEN = "token";
    private static final String KEY_LAST_NOTIFIED = "last_notified_id";

    private final SharedPreferences prefs;

    public TokenStore(Context context) {
        prefs = context.getSharedPreferences("styletrack_session", Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void setToken(String value) {
        SharedPreferences.Editor editor = prefs.edit();
        if (value == null) editor.remove(KEY_TOKEN); else editor.putString(KEY_TOKEN, value);
        editor.apply();
    }

    /** Highest notification id the device has already shown or the customer has seen; -1 = not set yet. */
    public long getLastNotifiedId() {
        return prefs.getLong(KEY_LAST_NOTIFIED, -1L);
    }

    public void setLastNotifiedId(long value) {
        prefs.edit().putLong(KEY_LAST_NOTIFIED, value).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
