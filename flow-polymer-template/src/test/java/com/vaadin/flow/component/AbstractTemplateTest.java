/*
 * Copyright 2000-2022 Vaadin Ltd.
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
