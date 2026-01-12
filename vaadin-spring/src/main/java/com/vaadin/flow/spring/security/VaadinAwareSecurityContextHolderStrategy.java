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
package com.vaadin.flow.spring.security;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.server.VaadinSession;

import static java.util.Objects.requireNonNull;

/**
 * A strategy that uses an available VaadinSession for retrieving the security
 * context.
 * <p>
 * Falls back to the default thread specific security context when no
 * vaadinSession is available.
 */
public final class VaadinAwareSecurityContextHolderStrategy
        implements SecurityContextHolderStrategy {

    private final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    @Override
    public void clearContext() {
        contextHolder.remove();
    }

    @Override
    @NonNull
    public SecurityContext getContext() {
        /*
         * We prefer the vaadin session information over the threadlocal as it
         * is more specific. It makes a huge difference if you for instance to
         * `otherSessionUI.access` in a request thread. In this case the
         * security context is expected to reflect the "otherSession" and not
         * the current request.
         */
        SecurityContext context = getFromVaadinSession()
                .orElseGet(contextHolder::get);
        if (context == null) {
            context = createEmptyContext();
            contextHolder.set(context);
        }
        return context;
    }

    @NonNull
    private Optional<SecurityContext> getFromVaadinSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || session.getSession() == null) {
            return Optional.empty();
        }
        try {
            Object securityContext = session.getSession().getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (securityContext instanceof SecurityContext context) {
                return Optional.of(context);
            } else {
                return Optional.empty();
            }
        } catch (IllegalStateException ignored) {
            // Session throws IllegalStateException when accessing
            // attributes of an invalid session
            return Optional.empty();
        }
    }

    @Override
    public void setContext(@NonNull SecurityContext securityContext) {
        contextHolder.set(requireNonNull(securityContext));
    }

    @Override
    @NonNull
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
}
