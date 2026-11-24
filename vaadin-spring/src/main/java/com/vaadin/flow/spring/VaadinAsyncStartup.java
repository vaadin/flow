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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Runs the Vaadin servlet context initialization (the class path scan for
 * routes and the other annotated classes) on another thread, so that it
 * overlaps with the creation of the application's beans, and holds the
 * application back until it has finished.
 * <p>
 * With an embedded server, Spring Boot starts the servlet context while it
 * refreshes the application context, before it creates the singleton beans, and
 * initializes the servlets only after all beans exist. The wait happens in two
 * places, whichever comes first:
 * <ul>
 * <li>{@link #start()}, which runs before any other lifecycle bean, so neither
 * the web server nor an application runner sees a half-initialized Vaadin;</li>
 * <li>{@link SpringServlet#init(jakarta.servlet.ServletConfig)}, which covers a
 * WAR deployment, where the context is initialized after the application
 * context.</li>
 * </ul>
 * A failure of the initialization is thrown from the wait, so that startup
 * fails as it does when the initialization runs on the calling thread.
 * <p>
 * Enabled with {@code vaadin.async-startup=true}.
 */
final class VaadinAsyncStartup implements SmartLifecycle {

    private static final String ATTRIBUTE = VaadinAsyncStartup.class.getName();

    private final Executor executor;
    private final CompletableFuture<Void> initialized = new CompletableFuture<>();
    private volatile boolean submitted;
    private volatile boolean running;

    /**
     * Creates an instance that runs the initialization on the given executor.
     *
     * @param executor
     *            the executor to run the initialization on
     */
    VaadinAsyncStartup(Executor executor) {
        this.executor = executor;
    }

    /**
     * Runs the initialization of the given servlet context on the executor. The
     * task runs with the context class loader of the calling thread, which the
     * servlet container has set to the class loader of the application.
     *
     * @param servletContext
     *            the servlet context being initialized
     * @param initialization
     *            the initialization to run
     */
    void submit(ServletContext servletContext, Runnable initialization) {
        servletContext.setAttribute(ATTRIBUTE, this);
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        submitted = true;
        try {
            executor.execute(() -> {
                Thread thread = Thread.currentThread();
                ClassLoader previous = thread.getContextClassLoader();
                thread.setContextClassLoader(classLoader);
                try {
                    initialization.run();
                    initialized.complete(null);
                } catch (Throwable t) {
                    initialized.completeExceptionally(t);
                } finally {
                    thread.setContextClassLoader(previous);
                }
            });
        } catch (RuntimeException e) {
            // The executor rejected the task
            initialized.completeExceptionally(e);
        }
    }

    /**
     * Waits until the initialization has finished, if one was submitted.
     *
     * @throws RuntimeException
     *             if the initialization failed
     */
    void await() {
        if (!submitted) {
            return;
        }
        try {
            initialized.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(
                    "Vaadin servlet context initialization failed", cause);
        }
    }

    /**
     * Waits until the initialization has finished, ignoring a failure. Used
     * before the context is destroyed, so that destroying does not race with a
     * still running initialization.
     */
    void awaitIgnoringFailure() {
        if (submitted) {
            try {
                initialized.join();
            } catch (CompletionException e) {
                LoggerFactory.getLogger(VaadinAsyncStartup.class).debug(
                        "Vaadin servlet context initialization failed", e);
            }
        }
    }

    /**
     * Waits until the initialization of the given servlet context has finished,
     * if it runs asynchronously.
     *
     * @param servletContext
     *            the servlet context
     * @throws RuntimeException
     *             if the initialization failed
     */
    static void await(ServletContext servletContext) {
        if (servletContext.getAttribute(
                ATTRIBUTE) instanceof VaadinAsyncStartup startup) {
            startup.await();
        }
    }

    @Override
    public void start() {
        await();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Before every other lifecycle bean, the web server included
        return Integer.MIN_VALUE;
    }
}
