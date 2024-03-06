/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.util;

import java.util.List;

import com.vaadin.flow.router.Router;
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

    public MockUI(Router router) {
        this(createSession(router));
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
        return createSession(null);
    }

    private static VaadinSession createSession(Router router) {
        VaadinService service = Mockito.mock(VaadinService.class);

        if (router != null) {
            Mockito.when(service.getRouter()).thenReturn(router);
        }

        VaadinSession session = new AlwaysLockedVaadinSession(service);
        VaadinSession.setCurrent(session);
        return session;
    }

    private static DeploymentConfiguration createConfiguration() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        return configuration;
    }

    public static MockUI createUI() {
        DeploymentConfiguration configuration = createConfiguration();
        VaadinSession session = createSession();
        session.setConfiguration(configuration);
        return new MockUI(session);
    }
}
