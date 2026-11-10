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
package com.vaadin.flow.server.webpush;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebPushMessageTest {

    @Tag("div")
    private static class DeviceView extends Component {
    }

    @AfterEach
    void clearCurrentInstances() {
        CurrentInstance.clearAll();
    }

    @Test
    void build_optionsSet_optionsInMessage() {
        WebPushMessage message = WebPushMessage.builder("Sauna is ready")
                .body("80 °C").icon("icons/sauna.png").tag("device-SAUN")
                .renotify(true).url("device/SAUN").build();

        assertEquals(JacksonUtils.readTree("""
                {
                  "title": "Sauna is ready",
                  "options": {
                    "body": "80 °C",
                    "icon": "icons/sauna.png",
                    "tag": "device-SAUN",
                    "renotify": true,
                    "data": { "url": "device/SAUN" }
                  }
                }
                """), JacksonUtils.readTree(message.toJson()));
    }

    @Test
    void build_noOptionsSet_noOptionsInMessage() {
        WebPushMessage message = WebPushMessage.builder("Sauna is ready")
                .build();

        assertEquals(JacksonUtils.readTree("""
                { "title": "Sauna is ready", "options": {} }
                """), JacksonUtils.readTree(message.toJson()));
    }

    @Test
    void build_renotifyWithoutTag_throws() {
        WebPushMessage.Builder builder = WebPushMessage
                .builder("Sauna is ready").renotify(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, builder::build);
        assertTrue(exception.getMessage().contains("tag"),
                exception.getMessage());
    }

    @Test
    void route_routeParameters_urlOfRouteInMessage() {
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        VaadinService service = mock(VaadinService.class);
        when(service.getContext()).thenReturn(new MapVaadinContext());
        when(service.getDeploymentConfiguration()).thenReturn(configuration);
        CurrentInstance.set(VaadinService.class, service);
        RouteConfiguration.forApplicationScope().setRoute("device/:id",
                DeviceView.class);

        WebPushMessage message = WebPushMessage.builder("Sauna is ready")
                .route(DeviceView.class, new RouteParameters("id", "SAUN"))
                .build();

        assertEquals("device/SAUN",
                message.options().get("data").get("url").asString());
    }

    private static class MapVaadinContext implements VaadinContext {
        private final Map<Class<?>, Object> attributes = new HashMap<>();

        @Override
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            Object value = attributes.get(type);
            if (value == null && defaultValueSupplier != null) {
                value = defaultValueSupplier.get();
                attributes.put(type, value);
            }
            return type.cast(value);
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            attributes.put(clazz, value);
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            attributes.remove(clazz);
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    }
}
