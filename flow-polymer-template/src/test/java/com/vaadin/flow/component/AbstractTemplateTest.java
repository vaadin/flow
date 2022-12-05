/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public abstract class AbstractTemplateTest {

    private VaadinService service;

    private UI ui;

    @Before
    public void init() throws Exception {
        service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        VaadinSession session = Mockito.mock(VaadinSession.class);

        ui = new UI();
        ui.getInternals().setSession(session);

        Mockito.when(session.getService()).thenReturn(service);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.createComponent(Mockito.any()))
                .thenAnswer(invocation -> ReflectTools
                        .createInstance(invocation.getArgument(0)));

        Mockito.when(service.getInstantiator()).thenReturn(instantiator);

        CurrentInstance.setCurrent(ui);
        VaadinService.setCurrent(service);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    protected UI getUI() {
        return ui;
    }

}
