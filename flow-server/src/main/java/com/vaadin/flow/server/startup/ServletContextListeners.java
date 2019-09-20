/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;

/**
 * All ServletContextListeners in Flow merged into one actual listener to be
 * able to control the order they are executed in.
 *
 * @since 1.0
 */
@WebListener
public class ServletContextListeners implements ServletContextListener {

    /**
     * The servlet must be deployed before websocket support is added to it.
     */
    private ServletContextListener[] listeners = new ServletContextListener[] {
            new ServletDeployer(), new JSR356WebsocketInitializer()};

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
