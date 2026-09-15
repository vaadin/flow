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
import org.junit.jupiter.api.Test;

import com.vaadin.flow.quarkus.it.sessioncontext.SessionContextView;

import static com.vaadin.flow.quarkus.it.sessioncontext.SessionContextView.SessionScopedBean.DESTROY_COUNT;

@QuarkusIntegrationTest
public class SessionContextCloseIT extends AbstractCdiIT {

    @Override
    protected String getTestPath() {
        return "/session";
    }

    @BeforeEach
    public void setUp() throws Exception {
        resetCounts();
        open();
    }

    @Test
    public void vaadinSessionCloseDestroysSessionContext() throws Exception {
        assertDestroyCountEquals(0);
        click(SessionContextView.INVALIDATEBTN_ID);
        assertDestroyCountEquals(1);
    }

    private void assertDestroyCountEquals(int expectedCount)
            throws IOException {
        waitForCount(expectedCount, DESTROY_COUNT);
    }
}
