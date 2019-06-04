package com.vaadin.flow.server.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.server.DevModeHandler;

class StopDevMode implements ServletContextListener, Serializable {

    private static final AtomicInteger SERVLET_CONTEXTS = new AtomicInteger();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SERVLET_CONTEXTS.incrementAndGet();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (SERVLET_CONTEXTS.decrementAndGet() == 0) {
            DevModeHandler handler = DevModeHandler.getDevModeHandler();
            if (handler != null) {
                handler.stop();
            }
        }
    }

}