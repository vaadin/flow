package com.vaadin.flow.server.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;

/**
 * All ServletContextListeners in Flow merged into one actual listener to be
 * able to control the order they are executed in.
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
