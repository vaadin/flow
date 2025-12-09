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
package com.vaadin.base.devserver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * A fake DevModeHandler whose only purpose is to eagerly show a "build in
 * progress" HTML page to the user, during the creation of the development
 * bundle.
 * <p>
 *
 * The {@link #getPort()} method returns a fixed value of {@literal -1}, meaning
 * that this handler will not start a server listening for incoming requests.
 * <p>
 *
 * Most of the other methods should not be invoked, and they may throw an
 * exception if called.
 */
public final class DevBundleBuildingHandler implements DevModeHandler {

    private final transient CompletableFuture<Void> buildCompletedFuture;

    public DevBundleBuildingHandler(
            CompletableFuture<Void> buildCompletedFuture) {
        this.buildCompletedFuture = buildCompletedFuture;
    }

    @Override
    public String getFailedOutput() {
        return null;
    }

    @Override
    public HttpURLConnection prepareConnection(String path, String method) {
        throw new UnsupportedOperationException("Must never be invoked");
    }

    @Override
    public boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) {
        return false;
    }

    @Override
    public void stop() {
        // Nothing to do
    }

    @Override
    public File getProjectRoot() {
        throw new UnsupportedOperationException("Must never be invoked");
    }

    /**
     * Gets always {@literal -1}, as this handler does not start a server.
     *
     * @return -1
     */
    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        return AbstractDevServerRunner.handleRequestInternal(session, request,
                response, buildCompletedFuture, new AtomicBoolean());
    }

    /**
     * Waits for the dev bundle to be built.
     * <p>
     * Suspends the caller's thread until the dev bundle is created (or failed
     * to create).
     */
    public void waitForDevBundle() {
        buildCompletedFuture.join();
    }

}
