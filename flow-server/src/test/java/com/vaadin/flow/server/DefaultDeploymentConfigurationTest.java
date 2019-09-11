/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DefaultDeploymentConfiguration}
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultDeploymentConfigurationTest {

    private static Properties DEFAULT_PARAMS = new Properties();

    {
        DEFAULT_PARAMS.setProperty(
                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                Boolean.TRUE.toString());
    }

    @Test
    public void testGetSystemPropertyForDefaultPackage()
            throws ClassNotFoundException {
        Class<?> clazz = Class.forName("ClassInDefaultPackage");
        String value = "value";
        String prop = "prop";
        System.setProperty(prop, value);
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                clazz, initParameters);
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
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters);
        assertEquals(value, config.getSystemProperty(prop));
    }

    @Test
    public void booleanValueReadIgnoreTheCase_true() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS, "tRUe");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertTrue(
                "Boolean value equal to 'true' ignoring case should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueReadIgnoreTheCase_false() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS, "FaLsE");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertFalse(
                "Boolean value equal to 'false' ignoring case should be interpreted as 'false'",
                config.isSendUrlsAsParameters());
    }

    @Test
    public void booleanValueRead_emptyIsTrue() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS, "");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertTrue("Empty boolean value should be interpreted as 'true'",
                config.isSendUrlsAsParameters());
    }

    @Test(expected = IllegalArgumentException.class)
    public void booleanValueRead_exceptionOnNonBooleanValue() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(
                Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                "incorrectValue");

        createDeploymentConfig(initParameters);
    }

    @Test
    public void frontendPrefixes_developmentMode() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(Constants.FRONTEND_URL_ES5,
                "context://build/frontend-es5/");
        initParameters.setProperty(Constants.FRONTEND_URL_ES6,
                "context://build/frontend-es6/");
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.FALSE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        String developmentPrefix = Constants.FRONTEND_URL_DEV_DEFAULT;
        assertThat(String.format(
                "In development mode, both es5 and es6 prefixes should be equal to '%s'",
                developmentPrefix), config.getEs5FrontendPrefix(),
                is(developmentPrefix));
        assertThat(String.format(
                "In development mode, both es5 and es6 prefixes should be equal to '%s'",
                developmentPrefix), config.getEs6FrontendPrefix(),
                is(developmentPrefix));
    }

    @Test
    public void frontendPrefixes_productionMode() {
        String es5Prefix = "context://build/frontend-es5/";
        String es6Prefix = "context://build/frontend-es6/";

        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(Constants.FRONTEND_URL_ES5, es5Prefix);
        initParameters.setProperty(Constants.FRONTEND_URL_ES6, es6Prefix);
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                Boolean.TRUE.toString());

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);

        assertThat(String.format(
                "In production mode, es5 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_ES5), config.getEs5FrontendPrefix(),
                is(es5Prefix));
        assertThat(String.format(
                "In production mode, es6 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_ES6), config.getEs6FrontendPrefix(),
                is(es6Prefix));
    }

    private DefaultDeploymentConfiguration createDeploymentConfig(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(
                DefaultDeploymentConfigurationTest.class, initParameters);
    }

    @Test
    public void defaultPushUrl() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushURL(), is(""));
    }

    @Test
    public void pushUrl() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PUSH_URL, "foo");

        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        assertThat(config.getPushURL(), is("foo"));
    }

    @Test
    public void bundleIsEnabledInProduction() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        Assert.assertTrue(config.useCompiledFrontendResources());
    }

    @Test
    public void bundleCanBeDisabled() {
        Properties initParameters = new Properties(DEFAULT_PARAMS);
        initParameters.setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        initParameters.setProperty(Constants.USE_ORIGINAL_FRONTEND_RESOURCES,
                "true");
        DefaultDeploymentConfiguration config = createDeploymentConfig(
                initParameters);
        Assert.assertFalse(config.useCompiledFrontendResources());
    }

}
