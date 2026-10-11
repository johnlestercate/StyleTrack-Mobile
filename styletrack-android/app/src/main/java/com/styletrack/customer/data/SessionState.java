package com.styletrack.customer.data;

public sealed interface SessionState {
    StartingState Starting = new StartingState();
    SignedOutState SignedOut = new SignedOutState();

    final class StartingState implements SessionState {
        private StartingState() {}
    }

    final class SignedOutState implements SessionState {
        private SignedOutState() {}
    }

    record SignedIn(String name) implements SessionState {}
}
