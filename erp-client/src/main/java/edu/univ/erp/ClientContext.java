package edu.univ.erp;

import edu.univ.erp.domain.UserAuth;

/**
 * Lightweight client-side context to hold the currently-authenticated user.
 * This allows the rich dashboard (Application/FormManager forms) to access
 * the authenticated user's id without requiring compile-time coupling to
 * the login UI flow.
 */
public final class ClientContext {

    private static volatile UserAuth currentUser;

    private ClientContext() {}

    public static void setCurrentUser(UserAuth u) {
        currentUser = u;
    }

    public static UserAuth getCurrentUser() {
        return currentUser;
    }
}
