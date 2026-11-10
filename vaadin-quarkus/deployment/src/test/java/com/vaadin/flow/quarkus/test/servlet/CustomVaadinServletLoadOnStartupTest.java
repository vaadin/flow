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

import jakarta.servlet.annotation.WebServlet;

import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.quarkus.QuarkusVaadinServlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a custom Vaadin servlet without {@code loadOnStartup} is still
 * started eagerly, and that the extension warns about it.
 */
public class CustomVaadinServletLoadOnStartupTest {

    private static final String WARNING = "Vaadin Servlet needs to be eagerly loaded";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(LazyServlet.class))
            .setLogRecordPredicate(
                    record -> record.getMessage().contains(WARNING))
            .assertLogRecords(records -> assertEquals(1, records.size()));

    @WebServlet(urlPatterns = "/lazy/*")
    public static class LazyServlet extends QuarkusVaadinServlet {

        static final AtomicBoolean initialized = new AtomicBoolean();

        @Override
        protected void servletInitialized() {
            initialized.set(true);
        }
    }

    @Test
    void servletWithoutLoadOnStartup_initializedBeforeFirstRequest() {
        assertTrue(LazyServlet.initialized.get());
    }
}
