package com.styletrack.customer

import android.app.Application
import com.styletrack.customer.data.Repository
import com.styletrack.customer.data.SessionManager
import com.styletrack.customer.data.TokenStore
import com.styletrack.customer.data.buildApi
import com.styletrack.customer.notify.Notifier

/** Holds the few app-wide objects (no dependency-injection framework needed at this size). */
class StyleTrackApp : Application() {

    lateinit var tokens: TokenStore
        private set
    lateinit var session: SessionManager
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        tokens = TokenStore(this)
        session = SessionManager(tokens)
        val api = buildApi(token = { session.token }, onUnauthorized = { session.expired() })
        repo = Repository(api, session, tokens)
        Notifier.createChannel(this)
    }
}
