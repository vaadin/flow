/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security.stateless;

import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

public class TestSecurityContextHolderStrategy
        implements SecurityContextHolderStrategy {
    @Override
    public void clearContext() {
    }

    @Override
    public SecurityContext getContext() {
        return null;
    }

    @Override
    public void setContext(SecurityContext securityContext) {
    }

    @Override
    public SecurityContext createEmptyContext() {
        return Mockito.mock(SecurityContext.class);
    }
}
