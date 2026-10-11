package com.styletrack.customer;

import android.app.Application;

import com.styletrack.customer.data.Api;
import com.styletrack.customer.data.ApiClient;
import com.styletrack.customer.data.Repository;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.data.SessionManager;
import com.styletrack.customer.data.TokenStore;
import com.styletrack.customer.notify.Notifier;

/** Holds the few app-wide objects (no dependency-injection framework needed at this size). */
public class StyleTrackApp extends Application {
    private TokenStore tokens;
    private SessionManager session;
    private RepositoryCore repoCore;
    private Repository repo;

    @Override
    public void onCreate() {
        super.onCreate();
        tokens = new TokenStore(this);
        session = new SessionManager(tokens);
        Api api = ApiClient.build(session::getToken, session::expired);
        repoCore = new RepositoryCore(api, session, tokens);
        repo = new Repository(repoCore);
        Notifier.createChannel(this);
    }

    public TokenStore getTokens() {
        return tokens;
    }

    public SessionManager getSession() {
        return session;
    }

    public Repository getRepo() {
        return repo;
    }

    /** The Java business-logic layer, for Java callers (e.g. NotificationWorker) that can't use the suspend-based Repository facade. */
    public RepositoryCore getRepoCore() {
        return repoCore;
    }
}
