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
package com.vaadin.quarkus.context;

import java.util.Properties;

import jakarta.enterprise.inject.spi.CDI;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

public class SessionUnderTestContext implements UnderTestContext {

    private VaadinSession session;
    private static ServiceUnderTestContext serviceUnderTestContext;

    private void mockSession() {
        if (serviceUnderTestContext == null) {
            serviceUnderTestContext = new ServiceUnderTestContext(
                    CDI.current().getBeanManager());
            serviceUnderTestContext.activate();
        }
        session = Mockito.mock(TestSession.class,
                Mockito.withSettings().useConstructor());
        doCallRealMethod().when(session).setAttribute(Mockito.any(String.class),
                Mockito.any());
        doCallRealMethod().when(session)
                .getAttribute(Mockito.any(String.class));
        doCallRealMethod().when(session).getService();

        when(session.getState()).thenReturn(VaadinSessionState.OPEN);

        when(session.hasLock()).thenReturn(true);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        when(session.getConfiguration()).thenReturn(configuration);
        Properties props = new Properties();
        when(configuration.getInitParameters()).thenReturn(props);

        doCallRealMethod().when(session).addUI(Mockito.any());
        doCallRealMethod().when(session).getUIs();

        Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, Command.class).execute();
            return null;
        }).when(session).access(Mockito.any());
    }

    @Override
    public void activate() {
        if (session == null) {
            mockSession();
        }
        VaadinSession.setCurrent(session);
    }

    @Override
    public void tearDownAll() {
        VaadinSession.setCurrent(null);
        if (serviceUnderTestContext != null) {
            serviceUnderTestContext.tearDownAll();
            serviceUnderTestContext = null;
        }
    }

    @Override
    public void destroy() {
        if (session != null) {
            session.getService().fireSessionDestroy(session);
        }
    }

    public VaadinSession getSession() {
        return session;
    }

    public static class TestSession extends VaadinSession {

        public TestSession() {
            super(serviceUnderTestContext.getService());
        }

    }
}
