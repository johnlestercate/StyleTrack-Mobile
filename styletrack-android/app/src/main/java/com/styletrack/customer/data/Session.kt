package com.styletrack.customer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The token and a few small values live in app-private storage (backups are disabled in the manifest). */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("styletrack_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value) }.apply()

    /** Highest notification id the device has already shown or the customer has seen; -1 = not set yet. */
    var lastNotifiedId: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFIED, value).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_LAST_NOTIFIED = "last_notified_id"
    }
}

sealed interface SessionState {
    data object Starting : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val name: String) : SessionState
}

class SessionManager(private val store: TokenStore) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Starting)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val token: String? get() = store.token

    fun signedIn(token: String, name: String) {
        store.token = token
        _state.value = SessionState.SignedIn(name)
    }

    fun restored(name: String) {
        _state.value = SessionState.SignedIn(name)
    }

    fun signedOut() {
        store.clear()
        _state.value = SessionState.SignedOut
    }

    /** Called by the HTTP layer on a 401. */
    fun expired() {
        if (_state.value is SessionState.SignedIn) signedOut()
    }
}
