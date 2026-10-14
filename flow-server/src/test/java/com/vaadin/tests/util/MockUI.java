/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.util;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinRequest;
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
        MockServletServiceSessionSetup setup = new MockServletServiceSessionSetup();
        VaadinSession session = setup.getSession();
        if (router != null) {
            setup.getService().setRouter(router);
        }
        VaadinSession.setCurrent(session);
        return session;
    }

    public static MockUI createUI() {
        VaadinSession session = createSession();
        return new MockUI(session);
    }
}
