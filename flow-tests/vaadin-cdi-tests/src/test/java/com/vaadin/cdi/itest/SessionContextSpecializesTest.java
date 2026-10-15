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
package com.vaadin.cdi.itest;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.cdi.itest.sessioncontextspecializes.LenientSessionContextManager;
import com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView;

import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.DIRECT_CALL_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.DONE_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.ERROR_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.FIREBTN_ID;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.OBSERVED_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.UNEXPECTED_ERROR_COUNT;

/**
 * Verifies that an application can take {@code @VaadinSessionScoped} beans into
 * use from a background thread that only sets the
 * {@link com.vaadin.flow.server.VaadinSession} thread-local — without holding
 * the session lock — by providing a {@code @Specializes} lenient
 * {@link com.vaadin.cdi.context.VaadinSessionScopedContext.ContextualStorageManager}.
 * <p>
 * Reproduces the user-facing scenarios from issues #495 and #506.
 */
public class SessionContextSpecializesTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("session-context-specializes",
                SessionContextSpecializesView.class,
                SessionContextSpecializesView.BackgroundEvent.class,
                SessionContextSpecializesView.SessionScopedObserver.class,
                LenientSessionContextManager.class);
    }

    @Before
    public void setUp() throws Exception {
        resetCounts();
        open();
    }

    @Test
    public void backgroundThread_firesEventAndCallsBean_specializesAllowsIt()
            throws IOException {
        assertCountEquals(0, OBSERVED_COUNT);
        assertCountEquals(0, DIRECT_CALL_COUNT);
        assertCountEquals(0, ERROR_COUNT);
        assertCountEquals(0, UNEXPECTED_ERROR_COUNT);

        click(FIREBTN_ID);

        // The background thread updates the counters asynchronously; wait
        // for its completion signal before asserting.
        waitForCount(1, DONE_COUNT);

        assertCountEquals(1, DIRECT_CALL_COUNT);
        assertCountEquals(1, OBSERVED_COUNT);
        assertCountEquals(0, ERROR_COUNT);
        assertCountEquals(0, UNEXPECTED_ERROR_COUNT);
    }
}
