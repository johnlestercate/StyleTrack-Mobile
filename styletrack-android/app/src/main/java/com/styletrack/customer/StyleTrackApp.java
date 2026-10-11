package com.styletrack.customer;

import android.app.Application;

import com.styletrack.customer.data.Api;
import com.styletrack.customer.data.ApiClient;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.SessionManager;
import com.styletrack.customer.data.TokenStore;
import com.styletrack.customer.notify.Notifier;

/** Holds the few app-wide objects (no dependency-injection framework needed at this size). */
public class StyleTrackApp extends Application {
    private TokenStore tokens;
    private SessionManager session;
    private RepositoryCore repoCore;

    @Override
    public void onCreate() {
        super.onCreate();
        tokens = new TokenStore(this);
        session = new SessionManager(tokens);
        Api api = ApiClient.build(session::getToken, session::expired);
        repoCore = new RepositoryCore(api, session, tokens);
        Notifier.createChannel(this);
    }

    public TokenStore getTokens() {
        return tokens;
    }

    public SessionManager getSession() {
        return session;
    }

    /** The Java business-logic layer: the sole facade the (now plain-Java/Views) UI layer calls. */
    public RepositoryCore getRepoCore() {
        return repoCore;
    }
}
