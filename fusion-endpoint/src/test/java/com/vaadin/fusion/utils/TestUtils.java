/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Assert;

public final class TestUtils {
    private TestUtils() {
    }

    public static List<Path> getClassFilePath(Package pack) {
        return Collections
                .singletonList(java.nio.file.Paths.get("src/test/java",
                        pack.getName().replace('.', File.separatorChar)));
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

    public static void equalsIgnoreWhiteSpaces(String msg, String expected,
            String actual) {
        try {
            Assert.assertEquals(msg,
                    IndentationUtils.unifyIndentation(expected, 2),
                    IndentationUtils.unifyIndentation(actual, 2));
        } catch (IndentationUtils.IndentationSyntaxException e) {
            throw new AssertionError("Failed to unify indentation", e);
        }
    }
}
