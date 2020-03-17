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

package com.vaadin.flow.internal;

import javax.servlet.ServletContext;
import java.util.Collections;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.JRebelInitializer.JRebelListenerReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.zeroturnaround.javarebel.ClassEventListener;

public class JRebelInitializerTest {

    private BrowserLiveReload liveReload;
    private ClassEventListener listener;

    @Before
    public void setup() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        // Provide the liveReload.
        liveReload = Mockito.mock(BrowserLiveReloadImpl.class);
        Mockito.when(servletContext
                .getAttribute(BrowserLiveReloadImpl.class.getName()))
                .thenReturn(liveReload);

        // Capture JRebel listener instance.
        ArgumentCaptor<Object> contextAttributeCaptor = ArgumentCaptor
                .forClass(Object.class);
        Mockito.doNothing().when(servletContext).setAttribute(
                Matchers.anyString(), contextAttributeCaptor.capture());

        JRebelInitializer initializer = new JRebelInitializer();
        initializer.onStartup(Collections.emptySet(), servletContext);

        JRebelListenerReference listenerReference = (JRebelListenerReference) contextAttributeCaptor
                .getValue();
        listener = listenerReference.listener;
    }

    @Test
    public void reload_invoked_on_class_event() throws Exception {
        listener.onClassEvent(ClassEventListener.EVENT_RELOADED,
                JRebelView.class);

        // Wait for the reload task to be scheduled.
        Thread.sleep(JRebelInitializer.RELOAD_DELAY * 3);

        Mockito.verify(liveReload).reload();
    }

    private static class JRebelView extends Component {
    }
}
