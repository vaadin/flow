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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.tests.util.MockOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionsApplicationPropertiesTest {

    @Test
    void getApplicationBooleanProperty_configAvailable_returnsPropertyValue() {
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getBooleanProperty("test.prop", false))
                .thenReturn(true);

        Options options = new MockOptions(new File("."))
                .withApplicationConfiguration(config);

        Optional<Boolean> result = options
                .getApplicationBooleanProperty("test.prop", false);
        assertEquals(Optional.of(true), result);
    }

    @Test
    void getApplicationBooleanProperty_noConfig_returnsEmpty() {
        Options options = new MockOptions(new File("."));

        Optional<Boolean> result = options
                .getApplicationBooleanProperty("test.prop", true);
        assertTrue(result.isEmpty());
    }

    @Test
    void getApplicationStringProperty_configAvailable_returnsPropertyValue() {
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getStringProperty("test.prop", "default"))
                .thenReturn("custom");

        Options options = new MockOptions(new File("."))
                .withApplicationConfiguration(config);

        Optional<String> result = options
                .getApplicationStringProperty("test.prop", "default");
        assertEquals(Optional.of("custom"), result);
    }

    @Test
    void getApplicationStringProperty_noConfig_returnsEmpty() {
        Options options = new MockOptions(new File("."));

        Optional<String> result = options
                .getApplicationStringProperty("test.prop", "default");
        assertTrue(result.isEmpty());
    }
}
