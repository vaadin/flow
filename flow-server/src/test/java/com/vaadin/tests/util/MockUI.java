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
package com.vaadin.tests.util;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

public class MockUI extends UI {

    private Page page;

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
    public Page getPage() {
        if (this.page != null) {
            return this.page;
        }
        return super.getPage();
    }

    public void setPage(Page page) {
        this.page = page;
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

    /**
     * The only invocation that has been scheduled, which is what a test that
     * asserts one call reads.
     */
    public JavaScriptInvocation onlyScheduledInvocation() {
        List<PendingJavaScriptInvocation> invocations = dumpPendingJsInvocations();
        if (invocations.size() != 1) {
            throw new AssertionError(
                    "Expected exactly one scheduled invocation, got "
                            + invocations.size() + ": " + invocations);
        }
        return invocations.get(0).getInvocation();
    }

    /**
     * The call of declared JavaScript that the only scheduled invocation
     * carries, which says which method of which definition was called and with
     * what.
     */
    public JsCall onlyScheduledJsCall() {
        return onlyScheduledInvocation().getJsCall();
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
        return new MockUI(createSession());
    }
}
