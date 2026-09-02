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
package com.vaadin.flow.internal.streams;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

/**
 * An upload or download request that is currently being served for a UI.
 * <p>
 * The UI that an active transfer belongs to is kept attached to its session
 * even if the UI is closed, so that listeners and callbacks bound to that UI
 * are still effective while the transfer is ongoing. A transfer is terminated
 * when the session is invalidated, which makes the streams that the framework
 * has handed out to the request handler refuse to transfer any more bytes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ActiveTransfer implements Serializable {

    private final String path;
    private final Element owner;

    private volatile boolean terminated;

    /**
     * Creates a record for a transfer that is served from the given path for
     * the given owner element.
     *
     * @param path
     *            the path of the request being served, used for logging
     * @param owner
     *            the element that owns the request handler, used for logging,
     *            not <code>null</code>
     */
    public ActiveTransfer(String path, Element owner) {
        this.path = path;
        this.owner = Objects.requireNonNull(owner);
    }

    /**
     * Terminates this transfer, which makes the streams handed out for it stop
     * transferring bytes.
     */
    public void terminate() {
        terminated = true;
    }

    /**
     * Checks whether this transfer has been terminated.
     *
     * @return <code>true</code> if the transfer has been terminated,
     *         <code>false</code> otherwise
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Returns the given request with any stream that it hands out replaced by
     * one that stops transferring bytes once this transfer is terminated.
     * <p>
     * A request implementation that the framework doesn't know how to wrap is
     * returned as-is.
     *
     * @param request
     *            the request to wrap, not <code>null</code>
     * @return the wrapped request
     */
    public VaadinRequest wrapRequest(VaadinRequest request) {
        return TerminableStreamsUtil.wrapRequest(request, this);
    }

    /**
     * Returns the given response with any stream that it hands out replaced by
     * one that stops transferring bytes once this transfer is terminated.
     * <p>
     * A response implementation that the framework doesn't know how to wrap is
     * returned as-is.
     *
     * @param response
     *            the response to wrap, not <code>null</code>
     * @return the wrapped response
     */
    public VaadinResponse wrapResponse(VaadinResponse response) {
        return TerminableStreamsUtil.wrapResponse(response, this);
    }

    /**
     * Describes this transfer for logging purposes.
     * <p>
     * Describing the owner renders the owner component, which is application
     * code that might fail. Since a description is only used for logging, a
     * failure is reported as part of the description instead of being thrown to
     * the caller.
     *
     * @return a description of this transfer
     */
    public String getDescription() {
        String ownerDescription;
        try {
            ownerDescription = owner.getNode().formatOwnerComponentToString();
        } catch (RuntimeException e) {
            ownerDescription = "could not be described: " + e;
        }
        return "path=" + path + ", owner=" + ownerDescription;
    }

    /**
     * Fails if this transfer has been terminated.
     *
     * @throws IOException
     *             if this transfer has been terminated
     */
    void checkNotTerminated() throws IOException {
        if (terminated) {
            throw new IOException(
                    "The transfer has been terminated because the session has been invalidated: "
                            + getDescription());
        }
    }
}
