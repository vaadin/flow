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
package com.vaadin.flow.server;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AppShellRegistryPushTransportTest {

    @Push(transport = Transport.LONG_POLLING, transportName = "add-on-push")
    private static class WithTransportName {
    }

    @Push(transport = Transport.LONG_POLLING)
    private static class WithTransport {
    }

    @Test
    void setTransport_transportName_takesPrecedence() {
        PushConfiguration configuration = mock(PushConfiguration.class);

        AppShellRegistry.setTransport(configuration,
                WithTransportName.class.getAnnotation(Push.class));

        verify(configuration).setTransportName("add-on-push");
        verify(configuration, never()).setTransport(Transport.LONG_POLLING);
    }

    @Test
    void setTransport_noTransportName_setsTransport() {
        PushConfiguration configuration = mock(PushConfiguration.class);

        AppShellRegistry.setTransport(configuration,
                WithTransport.class.getAnnotation(Push.class));

        verify(configuration).setTransport(Transport.LONG_POLLING);
    }
}
