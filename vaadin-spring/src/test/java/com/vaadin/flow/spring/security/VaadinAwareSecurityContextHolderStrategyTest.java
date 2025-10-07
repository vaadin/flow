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

import jakarta.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;

public class VaadinAwareSecurityContextHolderStrategyTest {

    private VaadinAwareSecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy;

    @Before
    public void setup() {
        vaadinAwareSecurityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        CurrentInstance.clearAll();
    }

    @After
    public void teardown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void currentSessionOverrides() {
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
        Assert.assertEquals(securityContext,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    public void detachedSessionWorks() {
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        Mockito.when(vaadinSession.getSession()).thenReturn(null);
        VaadinSession.setCurrent(vaadinSession);

        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        Assert.assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    public void explicitUsedWhenNoSessionAvailable() {
        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        Assert.assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }
}
