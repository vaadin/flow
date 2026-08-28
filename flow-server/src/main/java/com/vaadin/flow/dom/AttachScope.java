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
package com.vaadin.flow.dom;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

/**
 * Keeps the state created by an attach handler alive for exactly as long as an
 * element is attached, and runs the matching cleanup when the element is
 * detached again.
 *
 * @see Element#whenAttached(SerializableFunction)
 */
class AttachScope implements Registration {

    private final Element owner;
    private final SerializableFunction<UI, Registration> attachHandler;

    private @Nullable Registration attachListener;
    private @Nullable Registration detachListener;

    /**
     * Cleanup returned by the handler for the current attach, or
     * <code>null</code> if the element is detached or if the handler had
     * nothing to clean up.
     */
    private @Nullable Registration cleanup;

    /**
     * UI that the element was attached to when {@link #cleanup} was created,
     * used for routing cleanup failures to the right session.
     */
    private @Nullable UI attachedUi;

    private boolean removed;

    AttachScope(Element owner,
            SerializableFunction<UI, Registration> attachHandler) {
        this.owner = Objects.requireNonNull(owner, "Owner cannot be null");
        this.attachHandler = Objects.requireNonNull(attachHandler,
                "Attach handler cannot be null");

        attachListener = owner.addAttachListener(event -> handleAttach());
        detachListener = owner.addDetachListener(event -> runCleanup());

        if (owner.getNode().isAttached()) {
            handleAttach();
        }
    }

    private void handleAttach() {
        /*
         * A node can be reset and re-attached without a detach event reaching
         * its listeners, e.g. through StateNode.removeFromTree(false). Clean up
         * defensively so that a scope never has two live cleanups at the same
         * time.
         */
        runCleanup();

        UI ui = ((StateTree) owner.getNode().getOwner()).getUI();
        attachedUi = ui;
        cleanup = attachHandler.apply(ui);
    }

    private void runCleanup() {
        Registration pending = cleanup;
        UI ui = attachedUi;
        cleanup = null;
        attachedUi = null;

        if (pending == null) {
            return;
        }
        try {
            pending.remove();
        } catch (RuntimeException e) {
            /*
             * Mirrors DetachNotifier: a failing cleanup must not prevent the
             * rest of the detach handling from running.
             */
            VaadinSession session = ui != null ? ui.getSession()
                    : VaadinSession.getCurrent();
            if (session == null || session.getErrorHandler() == null) {
                throw e;
            }
            session.getErrorHandler().error(new ErrorEvent(e));
        }
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;

        if (attachListener != null) {
            attachListener.remove();
            attachListener = null;
        }
        if (detachListener != null) {
            detachListener.remove();
            detachListener = null;
        }
        runCleanup();
    }
}
