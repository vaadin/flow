/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.jrebel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.zeroturnaround.javarebel.ClassEventListener;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.jrebel.JRebelInitializer.JRebelListenerReference;

public class JRebelInitializerTest {

    private BrowserLiveReload liveReload;
    private ClassEventListener listener;

    @Before
    public void setup() {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        BrowserLiveReloadAccess liveReloadAccess = Mockito
                .mock(BrowserLiveReloadAccess.class);
        liveReload = Mockito.mock(BrowserLiveReload.class);

        Mockito.when(service.getInstantiator()).thenReturn(instantiator);
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(instantiator.getOrCreate(BrowserLiveReloadAccess.class)).thenReturn(liveReloadAccess);
        Mockito.when(liveReloadAccess.getLiveReload(service))
                .thenReturn(liveReload);

        // Capture JRebel listener instance.
        ArgumentCaptor<Object> contextAttributeCaptor = ArgumentCaptor
                .forClass(Object.class);
        Mockito.doNothing().when(context)
                .setAttribute(contextAttributeCaptor.capture());

        JRebelInitializer initializer = new JRebelInitializer();

        ServiceInitEvent serviceInitEvent = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(serviceInitEvent.getSource()).thenReturn(service);
        initializer.serviceInit(serviceInitEvent);

        JRebelListenerReference listenerReference = (JRebelListenerReference) contextAttributeCaptor
                .getValue();
        listener = listenerReference.listener;
    }

    @Test
    public void reload_invoked_on_class_event() throws Exception {
        listener.onClassEvent(ClassEventListener.EVENT_RELOADED,
                JRebelView.class);

        // Wait for the reload task to be scheduled.
        Mockito.verify(liveReload,
                Mockito.timeout(JRebelInitializer.RELOAD_DELAY * 3)).reload();
    }

    private static class JRebelView extends Component {
    }
}
