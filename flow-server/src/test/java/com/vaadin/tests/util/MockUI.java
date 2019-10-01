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
package com.vaadin.tests.util;

import java.util.List;

import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class MockUI extends UI {

    public MockUI() {
        this(findOrCreateSession());
    }

    public MockUI(VaadinSession session) {
        getInternals().setSession(session);
        setCurrent(this);
    }

    @Override
    protected void init(VaadinRequest request) {
        // Do nothing
    }

    public List<PendingJavaScriptInvocation> dumpPendingJsInvocations() {
        // Ensure element invocations are also flushed
        getInternals().getStateTree().runExecutionsBeforeClientResponse();

        return getInternals().dumpPendingJavaScriptInvocations();
    }

    private static VaadinSession findOrCreateSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            session = createSession();
        }
        return session;
    }

    private static VaadinSession createSession() {
        VaadinSession session = new AlwaysLockedVaadinSession(Mockito.mock(VaadinService.class));
        VaadinSession.setCurrent(session);
        return session;
    }

    private static DeploymentConfiguration createConfiguration(
            boolean compatibilityMode) {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isCompatibilityMode())
                .thenReturn(compatibilityMode);
        Mockito.when(configuration.isBowerMode()).thenReturn(compatibilityMode);
        return configuration;
    }

    public static MockUI createCompatibilityModeUI() {
        DeploymentConfiguration configuration = createConfiguration(true);
        VaadinSession session = createSession();
        session.setConfiguration(configuration);
        return new MockUI(session);
    }

    public static MockUI createNpmModeUI() {
        DeploymentConfiguration configuration = createConfiguration(false);
        VaadinSession session = createSession();
        session.setConfiguration(configuration);
        return new MockUI(session);
    }
}
