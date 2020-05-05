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
 */
package com.vaadin.flow.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility to get build data.
 *
 * Reads {@code "build.properties"} file in the classpath which contains
 * property values which may be set during build.
 *
 * @author Vaadin Ltd
 *
 */
public final class BuildUtil {

    private static final Properties PROPERTIES = loadProperties();

    /**
     * The name of platform versions file.
     */
    private static final String BUILD_PROPERTIES = "/build.properties";

    private BuildUtil() {
        // avoid instantiation
    }

    public static String getBuildProperty(String property) {
        return PROPERTIES.getProperty(property);
    }

    private static Properties loadProperties() {
        InputStream stream = BuildUtil.class
                .getResourceAsStream(BUILD_PROPERTIES);
        if (stream == null) {
            throw new IllegalStateException(
                    "Couldn't find " + BUILD_PROPERTIES + " file.");
        }
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read " + BUILD_PROPERTIES + " file.", e);
        }
        return properties;
    }

}
