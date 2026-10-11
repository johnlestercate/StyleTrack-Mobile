package com.styletrack.customer.data;

import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

public class SessionManager {
    private final TokenStore store;
    private final MutableStateFlow<SessionState> state = StateFlowKt.MutableStateFlow(SessionState.Starting);

    public SessionManager(TokenStore store) {
        this.store = store;
    }

    public StateFlow<SessionState> getState() {
        return FlowKt.asStateFlow(state);
    }

    public String getToken() {
        return store.getToken();
    }

    public void signedIn(String token, String name) {
        store.setToken(token);
        state.setValue(new SessionState.SignedIn(name));
    }

    public void restored(String name) {
        state.setValue(new SessionState.SignedIn(name));
    }

    public void signedOut() {
        store.clear();
        state.setValue(SessionState.SignedOut);
    }

    /** Called by the HTTP layer on a 401. */
    public void expired() {
        if (state.getValue() instanceof SessionState.SignedIn) signedOut();
    }
}
