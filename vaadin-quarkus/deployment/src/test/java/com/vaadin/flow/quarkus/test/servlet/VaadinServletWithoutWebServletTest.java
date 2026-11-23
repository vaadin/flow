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
package com.vaadin.flow.quarkus.test.servlet;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.quarkus.QuarkusVaadinServlet;

import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.HANDLED_BY_VAADIN;
import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.sendHeartbeat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that a Vaadin servlet subclass without {@code @WebServlet} is not
 * registered, and that the extension then registers its default servlet on
 * {@code /*}.
 */
public class VaadinServletWithoutWebServletTest {

    private static final String WARNING = "extends VaadinServlet without @WebServlet, skipping";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(
                            UnannotatedServlet.class, VaadinHeartbeat.class))
            .setLogRecordPredicate(
                    record -> record.getMessage().contains(WARNING))
            .assertLogRecords(records -> assertEquals(1, records.size()));

    @TestHTTPResource("/")
    URI root;

    public static class UnannotatedServlet extends QuarkusVaadinServlet {

        static final AtomicBoolean initialized = new AtomicBoolean();

        @Override
        protected void servletInitialized() {
            initialized.set(true);
        }
    }

    @Test
    void servletWithoutWebServlet_skipped_defaultServletRegistered()
            throws Exception {
        assertFalse(UnannotatedServlet.initialized.get());
        assertEquals(HANDLED_BY_VAADIN, sendHeartbeat(root));
    }
}
