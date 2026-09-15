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

import com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView;

import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.DIRECT_CALL_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.DONE_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.ERROR_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.FIREBTN_ID;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.OBSERVED_COUNT;
import static com.vaadin.cdi.itest.sessioncontextspecializes.SessionContextSpecializesView.UNEXPECTED_ERROR_COUNT;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the framework's supported activation condition for
 * {@code @VaadinSessionScoped}: when the current thread has set the
 * {@link com.vaadin.flow.server.VaadinSession} thread-local but does not hold
 * the session lock, the context is inactive — bean lookups fail and observer
 * methods on session-scoped beans are not invoked. Regression guard for the
 * lock check introduced in {@code VaadinSessionScopedContext.isActive()}.
 */
public class SessionContextStrictTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("session-context-strict",
                SessionContextSpecializesView.class,
                SessionContextSpecializesView.BackgroundEvent.class,
                SessionContextSpecializesView.SessionScopedObserver.class);
    }

    @Before
    public void setUp() throws Exception {
        resetCounts();
        open();
    }

    @Test
    public void backgroundThread_withoutSpecializes_contextInactive()
            throws IOException {
        assertCountEquals(0, OBSERVED_COUNT);
        assertCountEquals(0, DIRECT_CALL_COUNT);
        assertCountEquals(0, ERROR_COUNT);
        assertCountEquals(0, UNEXPECTED_ERROR_COUNT);

        click(FIREBTN_ID);

        // The background thread updates the counters asynchronously; wait
        // for its completion signal so the zero-assertions below are
        // meaningful instead of passing on a not-yet-run thread.
        waitForCount(1, DONE_COUNT);

        // Neither the proxy call nor the observer dispatch succeeded:
        assertCountEquals(0, DIRECT_CALL_COUNT);
        assertCountEquals(0, OBSERVED_COUNT);
        // At least one of the two attempts threw ContextNotActiveException
        // because the context is inactive on an unlocked thread.
        assertTrue("Expected at least one ContextNotActiveException, got "
                + getCount(ERROR_COUNT), getCount(ERROR_COUNT) >= 1);
        // Any other RuntimeException would indicate something went wrong
        // unrelated to the inactive context — guard against false positives.
        assertCountEquals(0, UNEXPECTED_ERROR_COUNT);
    }
}
