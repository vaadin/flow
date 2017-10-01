/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

/**
 * Tests for {@link DefaultDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 7.2
 */
public class DefaultDeploymentConfigurationTest {
    @Test
    public void testGetSystemPropertyForDefaultPackage()
            throws ClassNotFoundException {
        Class<?> clazz = Class.forName("ClassInDefaultPackage");
        String value = "value";
        String prop = "prop";
        System.setProperty(prop, value);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                clazz, new Properties(), (base, consumer) -> {
                });
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void testGetSystemProperty() throws ClassNotFoundException {
        String value = "value";
        String prop = "prop";
        System.setProperty(
                DefaultDeploymentConfigurationTest.class.getPackage().getName()
                        + '.' + prop,
                value);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, new Properties(),
                (base, consumer) -> {
                });
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void webComponentsBase_defaultSetting_fileFound() {
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, new Properties(),
                (base, consumer) -> {
                    consumer.test(
                            "bower_components/webcomponentsjs/webcomponents-loader.js");
                });

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElseThrow(AssertionError::new);

        assertEquals("context://bower_components/webcomponentsjs/",
                webComponentsPolyfillBase);
    }

    @Test
    public void testWebComponentsBase_defaultSetting_fileMissing() {
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, new Properties(),
                (base, consumer) -> {
                });

        assertFalse(config.getWebComponentsPolyfillBase().isPresent());
    }

    @Test
    public void testWebComponentsBase_defaultSetting_multipleFiles() {
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, new Properties(),
                (base, consumer) -> {
                    consumer.test("foo/webcomponents-lite.js");
                    consumer.test("bar/webcomponents-lite.js");
                });

        assertFalse(config.getWebComponentsPolyfillBase().isPresent());
    }

    @Test
    public void testWebComponentsBase_explicitSetting() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_POLYFILL_BASE, "foo");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElseThrow(AssertionError::new);

        assertEquals("foo", webComponentsPolyfillBase);
    }

    @Test
    public void testWebComponentsBase_explicitDisable() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_POLYFILL_BASE, "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(config.getWebComponentsPolyfillBase().isPresent());
    }

    @Test
    public void booleanValueReadIgnoreTheCase_true() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "tRUe");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertTrue(
                "Boolean value equal to 'true' ignoring case should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueReadIgnoreTheCase_false() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "FaLsE");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(
                "Boolean value equal to 'false' ignoring case should be interpreted as 'false'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueRead_emptyIsTrue() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertTrue("Empty boolean value should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties();
        initParameters.put(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        createDeploymentConfig(initParameters);
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters,
                (base, consumer) -> {
                });
    }
}
