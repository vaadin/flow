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
package com.vaadin.flow.server;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static com.vaadin.flow.server.Constants.VAADIN_PREFIX;

/**
 * Provides a configuration based on string properties.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public abstract class AbstractPropertyConfiguration
        implements AbstractConfiguration {

    private final Map<String, String> properties;

    /**
     * Creates a new instance with given {@code properties}.
     *
     * @param properties
     *            configuration properties
     */
    public AbstractPropertyConfiguration(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String getStringProperty(String name, String defaultValue) {
        return getApplicationOrSystemProperty(name, defaultValue,
                Function.identity());
    }

    @Override
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        /*
         * Considers {@code ""} to be equal {@code true} in order to treat
         * params like {@code -Dtest.param} as enabled ({@code test.param ==
         * true}).
         */
        String booleanString = getStringProperty(name, null);
        if (booleanString == null) {
            return defaultValue;
        } else if (booleanString.isEmpty()) {
            return true;
        } else {
            boolean parsedBoolean = Boolean.parseBoolean(booleanString);
            if (Boolean.toString(parsedBoolean)
                    .equalsIgnoreCase(booleanString)) {
                return parsedBoolean;
            } else {
                throw new IllegalArgumentException(String.format(
                        "Property named '%s' is boolean, but contains incorrect value '%s' that is not boolean '%s'",
                        name, booleanString, parsedBoolean));
            }
        }
    }

    /**
     * Gets an application property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    public String getApplicationProperty(String parameterName) {
        return getApplicationProperty(getProperties()::get, parameterName);
    }

    /**
     * Gets unmodifiable underlying properties.
     *
     * @return the properties map
     */
    protected Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Gets a configured property. The properties are typically read from e.g.
     * web.xml or from system properties of the JVM.
     *
     * @param propertyName
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @param converter
     *            the way string should be converted into the required property
     * @param <T>
     *            type of a property
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    public <T> T getApplicationOrSystemProperty(String propertyName,
            T defaultValue, Function<String, T> converter) {
        // Try system properties
        String val = getSystemProperty(propertyName);
        if (val != null) {
            return converter.apply(val);
        }

        // Try application properties
        val = getApplicationProperty(propertyName);
        if (val != null) {
            return converter.apply(val);
        }

        return defaultValue;
    }

    /**
     * Gets an system property value.
     *
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    protected String getSystemProperty(String parameterName) {
        // version prefixed with just "vaadin."
        return System.getProperty(VAADIN_PREFIX + parameterName);
    }

    /**
     * Gets application property value using the {@code valueProvider}.
     *
     * @param valueProvider
     *            a value provider for the property
     * @param propertyName
     *            the name or the parameter.
     * @return String value or null if not found
     */
    protected String getApplicationProperty(
            Function<String, String> valueProvider, String propertyName) {
        String val = valueProvider.apply(propertyName);
        if (val != null) {
            return val;
        }

        // Try lower case application properties for backward compatibility with
        // 3.0.2 and earlier
        val = valueProvider.apply(propertyName.toLowerCase(Locale.ENGLISH));

        return val;
    }

}
