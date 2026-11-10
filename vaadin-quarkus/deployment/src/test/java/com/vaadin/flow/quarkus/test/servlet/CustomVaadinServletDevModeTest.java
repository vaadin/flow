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

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.HANDLED_BY_VAADIN;
import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.sendHeartbeat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that a custom Vaadin servlet starts in Quarkus dev mode. There the
 * application classes are loaded by a class loader of their own, apart from the
 * Vaadin jars, so Vaadin has to load the servlet class through the application
 * class loader.
 */
public class CustomVaadinServletDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CustomVaadinServlet.class));

    @TestHTTPResource("/custom/")
    URI custom;

    @Test
    void customServlet_servesItsMappingInDevMode() throws Exception {
        assertEquals(HANDLED_BY_VAADIN, sendHeartbeat(custom));
    }
}
