/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.vitelogout;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import com.vaadin.base.devserver.viteproxy.ViteSessionTracker;

/**
 * Replicates Tomcat's behavior of closing WebSocket connections when an
 * authenticated HTTP session is invalidated. Jetty doesn't do this by default,
 * so this listener is needed to properly test the logout redirect fix in the
 * test environment.
 * <p>
 * Implements both ServletContextListener (to initialize the ViteSessionTracker)
 * and HttpSessionListener (to notify tracker when sessions are destroyed).
 */
@WebListener
public class CloseViteWebsocketOnSessionExpiration
        implements HttpSessionListener, ServletContextListener {

    private ViteSessionTracker tracker;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        tracker = new ViteSessionTracker();
        sce.getServletContext().setAttribute(ViteSessionTracker.class.getName(),
                tracker);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        if (tracker != null) {
            // Simulate Tomcat behavior
            // Close code 1008 is VIOLATED_POLICY per WebSocket RFC
            tracker.close(se.getSession().getId(), 1008,
                    "This connection was established under an authenticated HTTP session that has ended");
        }
    }
}
