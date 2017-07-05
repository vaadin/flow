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
package com.vaadin.generator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentBasicType;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class with utility methods for the code generation process.
 */
public final class ComponentGeneratorUtils {

    private ComponentGeneratorUtils() {
    }

    /**
     * Creates a valid Java class name based on the ES6 class name.
     * 
     * @param webcomponentClassName
     *            The ES6 class name.
     * @return A valid Java class name.
     */
    public static String generateValidJavaClassName(
            String webcomponentClassName) {
        return StringUtils.capitalize(
                formatStringToValidJavaIdentifier(webcomponentClassName));
    }

    /**
     * Formats property name to valid java identifier with CamelCase
     *
     * @param prefix
     *            property prefix (e.g. set, get, is etc.), not
     *            <code>null</code>
     * @param propertyName
     *            property name to convert
     * 
     * @return property method name
     */
    public static String generateMethodNameForProperty(String prefix,
            String propertyName) {
        assert prefix != null : "prefix should not be null";
        return prefix + StringUtils
                .capitalize(formatStringToValidJavaIdentifier(propertyName));
    }

    /**
     * Formats a given name (which can be a property name, function name or
     * event name) to a valid Java identifier.
     * 
     * @param name
     *            The name of the property that would be exposed to Java code.
     * @return A valid Java identifier based on the input name.
     * 
     * @see Character#isJavaIdentifierStart(char)
     * @see Character#isJavaIdentifierPart(char)
     */
    public static String formatStringToValidJavaIdentifier(String name) {
        String trimmed = name.trim();
        StringBuilder sb = new StringBuilder();
        if (!Character.isJavaIdentifierStart(trimmed.charAt(0))) {
            sb.append('_');
        }

        boolean toTitleCase = false;

        for (char c : trimmed.toCharArray()) {
            if (!Character.isJavaIdentifierPart(c)
                    || Character.getType(c) == Character.CONNECTOR_PUNCTUATION
                    || Character.getType(c) == Character.END_PUNCTUATION) {
                toTitleCase = true;
            } else if (toTitleCase) {
                sb.append(Character.toTitleCase(c));
                toTitleCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generates the directory structure based on a package name.
     * 
     * @param basePath
     *            The base path of the directory to be generated.
     * @param packageName
     *            The name of the package to be converted to directories.
     * @param createDirectories
     *            <code>true</code> to actually create the directories on the
     *            disk, <code>false</code> to just generate the File objects
     *            without creating them.
     * @return a new File that represents the final directory of the package,
     *         with basePath as root.
     * 
     * @throws IOException
     *             When createDirectories is <code>true</code> and the target
     *             directory cannot be created.
     */
    public static File convertPackageToDirectory(File basePath,
            String packageName, boolean createDirectories) throws IOException {
        File directory = new File(basePath,
                packageName.replace('.', File.separatorChar));
        if (createDirectories && !directory.isDirectory()
                && !directory.mkdirs()) {
            throw new IOException(
                    "Directory \"" + directory + "\" could not be created.");
        }
        return directory;
    }

    /**
     * Generates a package name based on a file path structure. The names are
     * converted to lowercase, and each folder of the file path is converted to
     * a package (split by ".").
     * 
     * @param path
     *            The file path to be converted to a package name, such as
     *            "/components/vaadin-button/vaadin-button.html".
     * @return A valid package name based on the input file path.
     */
    public static String convertFilePathToPackage(String path) {
        assert path != null : "Path should not be null";

        String normalized = path.replace('\\', '/');

        // the last part of the path is supposed to be the file name, which is
        // discarded for the package
        int idx = normalized.lastIndexOf('/');
        if (idx > 0) {
            normalized = normalized.substring(0, idx);
        } else {
            return "";
        }

        // replace all special characters for /
        normalized = normalized.replaceAll("[\\W+_]", "/");
        String[] split = normalized.split("/");

        return Arrays.stream(split).filter(StringUtils::isNotBlank)
                .map(ComponentGeneratorUtils::formatStringToValidJavaIdentifier)
                .map(String::toLowerCase).collect(Collectors.joining("."));
    }

    /**
     * Formats and ordinary String as a multi-line Java comment, that can be
     * safely added to any part of a Java code.
     * 
     * @param input
     *            The text to be converted as a comment. Should not be
     *            <code>null</code>.
     * 
     * @return The text ready to be added as a comment in a Java file.
     */
    public static String formatStringToJavaComment(String input) {
        assert input != null;
        StringBuilder builder = new StringBuilder("/*\n * ");
        builder.append(input.replace("\n", "\n * "));
        builder.append("\n */\n");
        return builder.toString();
    }

    public static String generateElementApiGetterForType(
            ComponentBasicType basicType, String propertyName) {
        switch (basicType) {
        case STRING:
            return String.format("return getElement().getProperty(\"%s\");",
                    propertyName);
        case BOOLEAN:
            return String.format(
                    "return getElement().getProperty(\"%s\", false);",
                    propertyName);
        case NUMBER:
            return String.format(
                    "return getElement().getProperty(\"%s\", 0.0);",
                    propertyName);
        case DATE:
            return String.format("return getElement().getProperty(\"%s\");",
                    propertyName);
        case ARRAY:
            return String.format(
                    "return (JsonArray) getElement().getPropertyRaw(\"%s\");",
                    propertyName);
        case OBJECT:
            return String.format(
                    "return (JsonObject) getElement().getPropertyRaw(\"%s\");",
                    propertyName);
        case UNDEFINED:
            return String.format(
                    "return (JsonValue) getElement().getPropertyRaw(\"%s\");",
                    propertyName);
        default:
            throw new IllegalArgumentException(
                    "Not a supported type: " + basicType);
        }
    }

    public static String generateElementApiSetterForType(
            ComponentBasicType basicType, String propertyName) {
        switch (basicType) {
        case ARRAY:
        case UNDEFINED:
        case OBJECT:
            return String.format("getElement().setPropertyJson(\"%s\", %s);",
                    propertyName, propertyName);
        case STRING:
            // Don't insert null as property value. Insert empty String instead.
            return String.format(
                    "getElement().setProperty(\"%s\", %s == null ? \"\" : %s);",
                    propertyName, propertyName, propertyName);
        default:
            return String.format("getElement().setProperty(\"%s\", %s);",
                    propertyName, propertyName);
        }
    }

    public static Class<?> toJavaType(ComponentBasicType type) {
        switch (type) {
        case STRING:
            return String.class;
        case NUMBER:
            return double.class;
        case BOOLEAN:
            return boolean.class;
        case ARRAY:
            return JsonArray.class;
        case DATE:
            return Date.class;
        case OBJECT:
            return JsonObject.class;
        case UNDEFINED:
            return JsonValue.class;
        default:
            throw new ComponentGenerationException(
                    "Not a supported type: " + type);
        }
    }

}
