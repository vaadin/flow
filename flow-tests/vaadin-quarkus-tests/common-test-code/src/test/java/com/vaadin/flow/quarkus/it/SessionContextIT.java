/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.quarkus.it.sessioncontext.SessionContextView;

import static com.vaadin.flow.quarkus.it.sessioncontext.SessionContextView.SessionScopedBean.DESTROY_COUNT;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusIntegrationTest
public class SessionContextIT extends AbstractCdiIT {

    @BeforeEach
    public void setUp() throws Exception {
        resetCounts();
        open();
    }

    @Test
    public void sameSessionIsAccessibleFromUIs() {
        assertLabelEquals("");
        click(SessionContextView.SETVALUEBTN_ID);
        getDriver().navigate().refresh();// creates new UI
        assertLabelEquals(SessionContextView.VALUE);
    }

    @Test
    public void httpSessionCloseDestroysSessionContext() throws Exception {
        assertDestroyCountEquals(0);
        click(SessionContextView.HTTP_INVALIDATEBTN_ID);
        assertDestroyCountEquals(1);
    }

    @Test
    @Tag("slow")
    public void httpSessionExpirationDestroysSessionContext() throws Exception {
        assertDestroyCountEquals(0);
        click(SessionContextView.EXPIREBTN_ID);
        boolean destroyed = false;
        getLogger().info("Waiting for session expiration...");
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);
            if (getCount(DESTROY_COUNT) > 0) {
                getLogger().info("session expired after {} seconds", i);
                destroyed = true;
                break;
            }
        }
        assertTrue(destroyed);
    }

    @Override
    protected String getTestPath() {
        return "/session";
    }

    private void assertLabelEquals(String expected) {
        assertTextEquals(expected, SessionContextView.VALUELABEL_ID);
    }

    private void assertDestroyCountEquals(int expectedCount)
            throws IOException {
        waitForCount(expectedCount, DESTROY_COUNT);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SessionContextIT.class);
    }

}
