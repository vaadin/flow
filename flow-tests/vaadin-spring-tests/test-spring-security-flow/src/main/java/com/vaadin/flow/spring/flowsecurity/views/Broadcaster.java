/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;

public class Broadcaster {

    private static Broadcaster instance = new Broadcaster();

    private ComponentEventBus router = new ComponentEventBus(new Div());

    public static class RefreshEvent extends ComponentEvent<Component> {
        public RefreshEvent() {
            super(new Div(), false);
        }
    }

    public static void sendMessage() {
        instance.router.fireEvent(new RefreshEvent());
    }

    public static synchronized Registration addMessageListener(
            ComponentEventListener<RefreshEvent> listener) {
        return instance.router.addListener(RefreshEvent.class, listener);
    }

}
