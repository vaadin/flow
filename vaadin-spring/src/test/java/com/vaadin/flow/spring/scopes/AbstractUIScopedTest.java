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
package com.vaadin.flow.spring.scopes;

import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractUIScopedTest extends AbstractScopeTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
        ui = null;
    }

    @AfterEach
    void clearUI() {
        ui = null;
        UI.setCurrent(null);
    }

    protected UI mockUI() {
        VaadinSession session = mockSession();

        Router router = mock(Router.class);
        VaadinService service = session.getService();
        when(service.getRouter()).thenReturn(router);

        Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        when(service.getMainDivId(any(), any())).thenReturn("abc");

        final Map<String, Object> attributeMap = new HashMap<>();

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(anyString()))
                .then(invocationOnMock -> attributeMap
                        .get(invocationOnMock.getArguments()[0].toString()));
        doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1])).when(servletContext)
                .setAttribute(anyString(), any());

        VaadinServletContext context = new VaadinServletContext(servletContext);
        when(service.getContext()).thenReturn(context);
        Lookup lookup = mock(Lookup.class);
        when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        when(appConfig.getContext()).thenReturn(context);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                appConfig, getClass(), initParameters);
        when(service.getDeploymentConfiguration()).thenReturn(config);

        ui = new UI();
        ui.getInternals().setSession(session);
        ui.doInit(null, 1, "foo");

        UI.setCurrent(ui);

        return ui;
    }
}
