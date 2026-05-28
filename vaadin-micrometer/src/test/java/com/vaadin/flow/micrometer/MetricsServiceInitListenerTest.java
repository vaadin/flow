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
package com.vaadin.flow.micrometer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinService;

public class MetricsServiceInitListenerTest {

    @After
    public void tearDown() {
        VaadinMicrometer.uninstall();
    }

    @Test
    public void zeroArgNoOpsWhenNotInstalled() {
        MetricsServiceInitListener listener = new MetricsServiceInitListener();
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);

        listener.serviceInit(event);

        Mockito.verifyNoInteractions(service);
        Assert.assertEquals(0L,
                event.getAddedVaadinRequestInterceptor().count());
    }

    @Test
    public void zeroArgUsesInstalledRegistryAndConfig() {
        VaadinMicrometer.install(new SimpleMeterRegistry(),
                VaadinMetricsConfig.defaults());
        MetricsServiceInitListener listener = new MetricsServiceInitListener();
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);

        listener.serviceInit(event);

        Mockito.verify(service)
                .addSessionInitListener(Mockito.any(SessionInitListener.class));
        Mockito.verify(service).addSessionDestroyListener(
                Mockito.any(SessionDestroyListener.class));
        Mockito.verify(service)
                .addUIInitListener(Mockito.any(UIInitListener.class));
        Assert.assertEquals(1L,
                event.getAddedVaadinRequestInterceptor().count());
    }

    @Test
    public void disabledTogglesSkipRegistration() {
        VaadinMetricsConfig config = VaadinMetricsConfig.builder()
                .sessions(false).uis(false).navigation(false).requests(false)
                .errors(false).build();
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), config);
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);

        listener.serviceInit(event);

        Mockito.verifyNoInteractions(service);
        Assert.assertEquals(0L,
                event.getAddedVaadinRequestInterceptor().count());
    }

    @Test
    public void registeredBinderTypesMatchConfig() {
        VaadinMetricsConfig config = VaadinMetricsConfig.builder()
                .sessions(true).uis(true).navigation(true).build();
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), config);
        VaadinService service = Mockito.mock(VaadinService.class);

        listener.serviceInit(new ServiceInitEvent(service));

        ArgumentCaptor<UIInitListener> uiListener = ArgumentCaptor
                .forClass(UIInitListener.class);
        Mockito.verify(service).addUIInitListener(uiListener.capture());
        Assert.assertTrue(uiListener.getValue() instanceof UiMetricsBinder);
    }

    @Test
    public void requestInterceptorIsRegistered() {
        MetricsServiceInitListener listener = new MetricsServiceInitListener(
                new SimpleMeterRegistry(), VaadinMetricsConfig.defaults());
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);

        listener.serviceInit(event);

        VaadinRequestInterceptor[] interceptors = event
                .getAddedVaadinRequestInterceptor()
                .toArray(VaadinRequestInterceptor[]::new);
        Assert.assertEquals(1, interceptors.length);
        Assert.assertTrue(interceptors[0] instanceof RequestMetricsBinder);
    }
}
