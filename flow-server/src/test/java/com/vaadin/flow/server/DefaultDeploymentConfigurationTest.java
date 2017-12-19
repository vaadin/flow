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
package com.vaadin.flow.server;

import java.util.Objects;
import java.util.Properties;

import org.junit.Test;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
                (base, consumer) -> consumer.test(
                        "bower_components/webcomponentsjs/webcomponents-loader.js"));

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElseThrow(() -> new AssertionError(
                        "Unexpected: did not find any webcomponents polyfill"));

        assertEquals("frontend://bower_components/webcomponentsjs/",
                webComponentsPolyfillBase);
    }

    @Test
    public void webComponentsBase_webjarsEnabled_webJarFileFound() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.DISABLE_WEBJARS, Boolean.FALSE.toString());

        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters,
                (base, consumer) -> {
                    if (Objects.equals(base, "/")) {
                        consumer.test("webjars/bower_components/webcomponentsjs/webcomponents-loader.js");
                    }
                });

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElseThrow(() -> new AssertionError(
                        "Unexpected: did not find any webcomponents polyfill"));

        assertEquals("context://webjars/bower_components/webcomponentsjs/",
                webComponentsPolyfillBase);
    }

    @Test
    public void webComponentsBase_webJarsDisabled_noPolyfillFound() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.DISABLE_WEBJARS, Boolean.TRUE.toString());

        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters,
                (base, consumer) -> {
                    if (Objects.equals(base, "/")) {
                        consumer.test("webjars/bower_components/webcomponentsjs/webcomponents-loader.js");
                    }
                });

        assertFalse("When webjars are disabled, no webjars paths should be used",
                config.getWebComponentsPolyfillBase().isPresent());
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

    /**
     * @see <a href="https://github.com/vaadin/flow/issues/3142">https://github.com/vaadin/flow/issues/3142</a>
     */
    @Test
    public void testWebComponentsBase_defaultSetting_samePathRepeatedMultipleTimes() {
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, new Properties(),
                (base, consumer) -> {
                    int numberOfVisits = 0;
                    while (consumer.test("foo//")) {
                        numberOfVisits++;
                        if (numberOfVisits > 2) {
                            throw new AssertionError(
                                    "Should not visit the same path twice");
                        }
                    }
                });

        assertFalse(config.getWebComponentsPolyfillBase().isPresent());
    }

    @Test
    public void testWebComponentsBase_explicitSetting() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.SERVLET_PARAMETER_POLYFILL_BASE, "foo");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElseThrow(() -> new AssertionError(
                        "Unexpected: did not find any webcomponents polyfill"));

        assertEquals("foo", webComponentsPolyfillBase);
    }

    @Test
    public void testWebComponentsBase_explicitDisable() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.SERVLET_PARAMETER_POLYFILL_BASE, "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(config.getWebComponentsPolyfillBase().isPresent());
    }

    @Test
    public void booleanValueReadIgnoreTheCase_true() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
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
        initParameters.setProperty(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
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
        initParameters.setProperty(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertTrue("Empty boolean value should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        createDeploymentConfig(initParameters);
    }

    @Test
    public void frontendPrefixes_developmentMode() {
        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.FRONTEND_URL_ES5, "context://build/frontend-es5/");
        initParameters.setProperty(Constants.FRONTEND_URL_ES6, "context://build/frontend-es6/");
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, Boolean.FALSE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(initParameters);
        String developmentPrefix = Constants.FRONTEND_URL_DEV_DEFAULT;
        assertThat(String.format("In development mode, both es5 and es6 prefixes should be equal to '%s'", developmentPrefix),
                config.getEs5FrontendPrefix(), is(developmentPrefix));
        assertThat(String.format("In development mode, both es5 and es6 prefixes should be equal to '%s'", developmentPrefix),
                config.getEs6FrontendPrefix(), is(developmentPrefix));
    }

    @Test
    public void frontendPrefixes_productionMode() {
        String es5Prefix = "context://build/frontend-es5/";
        String es6Prefix = "context://build/frontend-es6/";

        Properties initParameters = new Properties();
        initParameters.setProperty(Constants.FRONTEND_URL_ES5, es5Prefix);
        initParameters.setProperty(Constants.FRONTEND_URL_ES6, es6Prefix);
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, Boolean.TRUE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertThat(String.format("In production mode, es5 prefix should be equal to '%s' parameter value", Constants.FRONTEND_URL_ES5),
                config.getEs5FrontendPrefix(), is(es5Prefix));
        assertThat(String.format("In production mode, es6 prefix should be equal to '%s' parameter value", Constants.FRONTEND_URL_ES6),
                config.getEs6FrontendPrefix(), is(es6Prefix));
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters,
                (base, consumer) -> {
                });
    }
}
