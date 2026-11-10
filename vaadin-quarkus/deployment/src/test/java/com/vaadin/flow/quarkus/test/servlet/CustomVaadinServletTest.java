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

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.vaadin.quarkus.QuarkusVaadinServletService;

import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.HANDLED_BY_DEFAULT_SERVLET;
import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.HANDLED_BY_VAADIN;
import static com.vaadin.flow.quarkus.test.servlet.VaadinHeartbeat.sendHeartbeat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies that a {@code @WebServlet} annotated subclass of
 * {@link com.vaadin.quarkus.QuarkusVaadinServlet} is registered with the
 * attributes of its annotation, and replaces the default Vaadin servlet the
 * extension registers on {@code /*}.
 */
public class CustomVaadinServletTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(
                            CustomVaadinServlet.class, VaadinHeartbeat.class));

    @TestHTTPResource("/")
    URI root;

    @TestHTTPResource("/custom/")
    URI custom;

    @Test
    void customServlet_registeredWithAnnotationAttributes() {
        CustomVaadinServlet servlet = CustomVaadinServlet.initialized.get();
        assertEquals(CustomVaadinServlet.NAME, servlet.getServletName());
        assertEquals(CustomVaadinServlet.PARAM_VALUE,
                servlet.getService().getDeploymentConfiguration()
                        .getStringProperty(CustomVaadinServlet.PARAM_NAME,
                                null));
        assertInstanceOf(QuarkusVaadinServletService.class,
                servlet.getService());
    }

    @Test
    void customServlet_servesItsMapping_defaultServletNotRegistered()
            throws Exception {
        assertEquals(HANDLED_BY_VAADIN, sendHeartbeat(custom));
        assertEquals(HANDLED_BY_DEFAULT_SERVLET, sendHeartbeat(root));
    }
}
