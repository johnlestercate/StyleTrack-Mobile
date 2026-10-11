package com.styletrack.customer.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SessionManager {
    private final TokenStore store;
    private final MutableLiveData<SessionState> state = new MutableLiveData<>(SessionState.Starting);

    public SessionManager(TokenStore store) {
        this.store = store;
    }

    public LiveData<SessionState> getState() {
        return state;
    }

    public String getToken() {
        return store.getToken();
    }

    public void signedIn(String token, String name) {
        store.setToken(token);
        state.postValue(new SessionState.SignedIn(name));
    }

    public void restored(String name) {
        state.postValue(new SessionState.SignedIn(name));
    }

    public void signedOut() {
        store.clear();
        state.postValue(SessionState.SignedOut);
    }

    /** Called by the HTTP layer on a 401. */
    public void expired() {
        if (state.getValue() instanceof SessionState.SignedIn) signedOut();
    }
}
