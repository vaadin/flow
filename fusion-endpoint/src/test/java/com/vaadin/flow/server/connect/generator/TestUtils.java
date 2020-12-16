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

package com.vaadin.flow.server.connect.generator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Assert;

public final class TestUtils {
    private TestUtils() {
    }

    public static Properties readProperties(String filePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(filePath));
        } catch (Exception e) {
            throw new AssertionError(String.format(
                    "Failed to read the properties file '%s", filePath));
        }
        return properties;
    }

    public static String readResource(URL resourceUrl) {
        String text;
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(resourceUrl.openStream()))) {
            text = input.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new AssertionError(String.format(
                    "Failed to read resource from '%s'", resourceUrl), e);
        }
        return text;
    }

    public static void equalsIgnoreWhiteSpaces(String expected, String actual) {
        equalsIgnoreWhiteSpaces(null, expected, actual);
    }

    public static void equalsIgnoreWhiteSpaces(String msg, String expected, String actual) {
        Assert.assertEquals(msg, expected.replaceAll("\\s+", " ").trim(),
                actual.replaceAll("\\s++", " ").trim());
    }
}
