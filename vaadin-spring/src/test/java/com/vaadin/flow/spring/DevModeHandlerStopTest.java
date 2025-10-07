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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import com.vaadin.base.devserver.DevModeHandlerManagerImpl;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

public class DevModeHandlerStopTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Config.class,
                    SpringBootAutoConfiguration.class));

    @Test
    void devModeStartupListener_contextDestroyAfterSpringContextClosed_shouldNotThrow() {
        // DevModeStartupListener is both ServletContextListener and
        // ServletContainerInitializer so the servlet container will create two
        // instances. Let's try to simulate the same behavior in the test
        DevModeStartupListener startupListenerAsContainerInitializer = new DevModeStartupListener();
        DevModeStartupListener startupListenerAsContextListener = new DevModeStartupListener();
        AtomicReference<ServletContext> contextRef = new AtomicReference<>();
        AtomicReference<MockDevModeHandlerManager> handlerManagerRef = new AtomicReference<>();
        this.contextRunner.run((context) -> {
            handlerManagerRef
                    .set(context.getBean(MockDevModeHandlerManager.class));
            ServletContext servletContext = context.getServletContext();
            contextRef.set(servletContext);
            new LookupServletContainerInitializer()
                    .onStartup(
                            Set.of(LookupInitializer.class,
                                    SpringLookupInitializer.class),
                            servletContext);
            startupListenerAsContainerInitializer.onStartup(Set.of(),
                    servletContext);
            startupListenerAsContextListener.contextInitialized(
                    new ServletContextEvent(servletContext));
        });
        Assertions.assertTrue(handlerManagerRef.get().initialized,
                "Expecting DevModeHandlerManager initialization to be invoked, but it was not");
        Assertions.assertFalse(handlerManagerRef.get().stopped,
                "Expecting DevModeHandler not yet to be yet stopped, but it was");

        // Stop the DevModeHandler after Spring Context has been closed
        startupListenerAsContextListener
                .contextDestroyed(new ServletContextEvent(contextRef.get()));
        Assertions.assertTrue(handlerManagerRef.get().stopped,
                "Expecting DevModeHandler to be stopped by DevModeHandlerManager, but it was not");
    }

    @Test
    void shutdownCommandsShouldBeExecutedOnStoppingDevModeHandlerManager() {
        AtomicReference<Boolean> watcherClosed = new AtomicReference<>(false);
        DevModeHandlerManager devModeHandlerManager = new DevModeHandlerManagerImpl();

        Closeable mockWatcher = () -> watcherClosed.set(true);
        devModeHandlerManager.registerShutdownCommand(() -> {
            try {
                mockWatcher.close();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        devModeHandlerManager.stopDevModeHandler();
        Assertions.assertTrue(watcherClosed.get());
    }

    private static class MockDevModeHandlerManager
            implements DevModeHandlerManager {

        private boolean initialized;
        private boolean stopped;

        @Override
        public Class<?>[] getHandlesTypes() {
            return new Class[0];
        }

        @Override
        public void initDevModeHandler(Set<Class<?>> classes,
                VaadinContext context) {
            initialized = true;
        }

        @Override
        public void stopDevModeHandler() {
            stopped = true;
        }

        @Override
        public void setDevModeHandler(DevModeHandler devModeHandler) {
        }

        @Override
        public DevModeHandler getDevModeHandler() {
            return null;
        }

        @Override
        public void launchBrowserInDevelopmentMode(String url) {

        }

        @Override
        public void setApplicationUrl(String applicationUrl) {
        }

        @Override
        public void registerShutdownCommand(Command command) {

        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        DevModeHandlerManager devModeHandlerManager() {
            return new MockDevModeHandlerManager();
        }
    }
}
