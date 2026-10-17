/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractTemplateTest {

    private VaadinService service;

    private UI ui;

    @BeforeEach
    void init() throws Exception {
        service = mock(VaadinService.class);
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);

        when(service.getDeploymentConfiguration()).thenReturn(configuration);

        VaadinSession session = mock(VaadinSession.class);

        ui = new UI();
        ui.getInternals().setSession(session);

        when(session.getService()).thenReturn(service);

        Instantiator instantiator = mock(Instantiator.class);
        when(instantiator.createComponent(any()))
                .thenAnswer(invocation -> ReflectTools
                        .createInstance(invocation.getArgument(0)));

        when(service.getInstantiator()).thenReturn(instantiator);

        CurrentInstance.setCurrent(ui);
        VaadinService.setCurrent(service);
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    protected UI getUI() {
        return ui;
    }

}
