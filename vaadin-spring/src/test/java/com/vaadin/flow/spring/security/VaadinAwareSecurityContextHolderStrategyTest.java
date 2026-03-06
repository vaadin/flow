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
package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaadinAwareSecurityContextHolderStrategyTest {

    private VaadinAwareSecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy;

    @BeforeEach
    void setup() {
        vaadinAwareSecurityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        CurrentInstance.clearAll();
    }

    @AfterEach
    void teardown() {
        CurrentInstance.clearAll();
    }

    @Test
    void currentSessionOverrides() {
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(vaadinSession.getSession())
                .thenReturn(new WrappedHttpSession(httpSession));
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(httpSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .thenReturn(securityContext);
        VaadinSession.setCurrent(vaadinSession);

        vaadinAwareSecurityContextHolderStrategy
                .setContext(Mockito.mock(SecurityContext.class));
        assertEquals(securityContext,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    void detachedSessionWorks() {
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        Mockito.when(vaadinSession.getSession()).thenReturn(null);
        VaadinSession.setCurrent(vaadinSession);

        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    void explicitUsedWhenNoSessionAvailable() {
        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    void getContext_invalidateSession_getsThreadSecurityContext() {
        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);

        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(vaadinSession.getSession())
                .thenReturn(new WrappedHttpSession(httpSession));
        Mockito.doThrow(IllegalStateException.class).when(httpSession)
                .getAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        VaadinSession.setCurrent(vaadinSession);

        assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }
}
