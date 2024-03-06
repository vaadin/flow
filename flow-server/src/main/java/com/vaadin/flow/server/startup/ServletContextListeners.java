/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;

/**
 * All ServletContextListeners in Flow merged into one actual listener to be
 * able to control the order they are executed in.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@WebListener
public class ServletContextListeners implements ServletContextListener {

    /**
     * The servlet must be deployed before websocket support is added to it.
     */
    private ServletContextListener[] listeners = new ServletContextListener[] {
            new ServletDeployer(), new JSR356WebsocketInitializer() };

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        for (ServletContextListener listener : listeners) {
            listener.contextInitialized(sce);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (ServletContextListener listener : listeners) {
            listener.contextDestroyed(sce);
        }
    }

}
