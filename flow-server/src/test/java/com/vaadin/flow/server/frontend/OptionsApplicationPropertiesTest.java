/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.tests.util.MockOptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptionsApplicationPropertiesTest {

    @Test
    public void getApplicationBooleanProperty_configAvailable_returnsPropertyValue() {
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
    public void getApplicationBooleanProperty_noConfig_returnsEmpty() {
        Options options = new MockOptions(new File("."));

        Optional<Boolean> result = options
                .getApplicationBooleanProperty("test.prop", true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getApplicationStringProperty_configAvailable_returnsPropertyValue() {
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
    public void getApplicationStringProperty_noConfig_returnsEmpty() {
        Options options = new MockOptions(new File("."));

        Optional<String> result = options
                .getApplicationStringProperty("test.prop", "default");
        assertTrue(result.isEmpty());
    }
}
