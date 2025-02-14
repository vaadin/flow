package com.vaadin.flow.spring.flowsecuritywebsocket;

import org.junit.Ignore;
import org.junit.Test;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    @Test
    @Ignore("""
            With WEBSOCKET transport the WS connection is closed when session
            is invalidated, but Flow client attempts a reconnection and
            re-enables heartbeat. The heartbeat ping resolves in a 403 HTTP
            status code because of session expiration, causing the client-side
            session expiration handler to redirect to the timeout page instead
            of the logout view, because the logout process is still ongoing.
            """)
    public void logout_via_doLogoutURL_redirects_to_logout() {
        super.logout_via_doLogoutURL_redirects_to_logout();
    }

    @Test
    public void websocket_roles_checked_correctly_during_navigation() {
        open("admin");
        loginAdmin();
        navigateTo("");
        assertRootPageShown();
        navigateTo("admin");
        assertAdminPageShown(ADMIN_FULLNAME);
    }
}
