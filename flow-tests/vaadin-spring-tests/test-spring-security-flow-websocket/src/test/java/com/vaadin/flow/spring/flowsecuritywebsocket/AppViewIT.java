/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
