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
package com.vaadin.cdi.itest.sessioncontextspecializes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.Specializes;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.cdi.context.VaadinSessionScopedContext;
import com.vaadin.cdi.util.ContextualStorage;
import com.vaadin.flow.server.VaadinSession;

/**
 * Replaces the framework-default
 * {@link VaadinSessionScopedContext.ContextualStorageManager} via
 * {@code @Specializes} to activate the {@code @VaadinSessionScoped} context
 * whenever a {@link VaadinSession} is set on the current thread, and to acquire
 * the session lock around storage access when the calling thread does not
 * already hold it.
 */
@ApplicationScoped
@Specializes
public class LenientSessionContextManager
        extends VaadinSessionScopedContext.ContextualStorageManager {

    @Override
    protected boolean isActive() {
        return VaadinSession.getCurrent() != null;
    }

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session.hasLock()) {
            return super.getContextualStorage(contextual, createIfNotExist);
        }
        AtomicReference<ContextualStorage> ref = new AtomicReference<>();
        session.accessSynchronously(() -> ref
                .set(super.getContextualStorage(contextual, createIfNotExist)));
        return ref.get();
    }
}
