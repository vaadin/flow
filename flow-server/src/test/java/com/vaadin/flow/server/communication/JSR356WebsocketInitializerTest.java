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
package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinServlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JSR356WebsocketInitializerTest {

    @Test
    void init_skipInitializationAttributeSet_atmosphereNotInitialized() {
        ServletRegistration registration = mock(ServletRegistration.class);
        when(registration.getName()).thenReturn("vaadin");
        when(registration.getClassName())
                .thenReturn(VaadinServlet.class.getName());
        ServletContext context = mock(ServletContext.class);
        when(context.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(context.getServletRegistrations())
                .thenAnswer(invocation -> Map.of("vaadin", registration));
        when(context.getAttribute(
                JSR356WebsocketInitializer.SKIP_INITIALIZATION_ATTRIBUTE))
                .thenReturn(Boolean.TRUE);

        new JSR356WebsocketInitializer().init(context);

        verify(context, never()).setAttribute(anyString(), any());
    }
}
