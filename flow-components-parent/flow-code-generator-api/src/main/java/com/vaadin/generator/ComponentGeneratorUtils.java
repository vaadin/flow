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
package com.vaadin.generator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.dom.Element;
import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentBasicType;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Utility methods for the code generation process.
 *
 * @since 1.0
 */
public final class ComponentGeneratorUtils {

    private static final Set<String> JAVA_RESERVED_WORDS = new HashSet<>(
            Arrays.asList("abstract", "assert", "boolean", "break", "byte",
                    "case", "catch", "char", "class", "const", "default", "do",
                    "double", "else", "enum", "extends", "false", "final",
                    "finally", "float", "for", "goto", "if", "implements",
                    "import", "instanceof", "int", "interface", "long",
                    "native", "new", "null", "package", "private", "protected",
                    "public", "return", "short", "static", "strictfp", "super",
                    "switch", "synchronized", "this", "throw", "throws",
                    "transient", "true", "try", "void", "volatile", "while",
                    "continue"));

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
        return prefix + StringUtils.capitalize(
                formatStringToValidJavaIdentifier(propertyName, true));
    }

    /**
     * Same as calling
     * {@link #formatStringToValidJavaIdentifier(String, boolean)} with
     * <code>false</code> - not ignoring Java reserved words.
     *
     * @param name
     *            The name of the property that would be exposed to Java code.
     * @return A valid Java identifier based on the input name.
     */
    public static String formatStringToValidJavaIdentifier(String name) {
        return formatStringToValidJavaIdentifier(name, false);
    }

    /**
     * Formats a given name (which can be a property name, function name or
     * event name) to a valid Java identifier.
     * <p>
     * If the end result is a Java reserved word, and the flag
     * <code>ignoreReservedWords</code> is <code>false</code>, the identifier is
     * suffixed with the <code>_</code> character.
     *
     * @param name
     *            The name of the property that would be exposed to Java code.
     * @param ignoreReservedWords
     *            <code>true</code> to ignore Java reserved words, such as "if"
     *            and "for", <code>false</code> otherwise.
     * @return A valid Java identifier based on the input name.
     *
     * @see Character#isJavaIdentifierStart(char)
     * @see Character#isJavaIdentifierPart(char)
     */
    public static String formatStringToValidJavaIdentifier(String name,
            boolean ignoreReservedWords) {
        String trimmed = name.trim();
        StringBuilder sb = new StringBuilder();
        if (!Character.isJavaIdentifierStart(trimmed.charAt(0))) {
            sb.append('_');
        }

        boolean toTitleCase = false;

        for (char character : trimmed.toCharArray()) {
            if (!Character.isJavaIdentifierPart(character)
                    || Character.getType(
                            character) == Character.CONNECTOR_PUNCTUATION
                    || Character
                            .getType(character) == Character.END_PUNCTUATION) {
                toTitleCase = true;
            } else if (toTitleCase) {
                sb.append(Character.toTitleCase(character));
                toTitleCase = false;
            } else {
                sb.append(character);
            }
        }

        String identifier = sb.toString();

        if (!ignoreReservedWords && JAVA_RESERVED_WORDS.contains(identifier)) {
            return '_' + identifier;
        }

        return identifier;
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

    /**
     * Generates a code snippet that uses the {@link Element} API to retrieve
     * properties from the client model.
     *
     * @param basicType
     *            The javascript basic type of the property.
     * @param propertyName
     *            The name of the property in the javascript model.
     * @return the code snippet ready to be added in a Java source code.
     */
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
            throw new ComponentGenerationException(
                    "Not a supported type for getters: " + basicType);
        }
    }

    /**
     * Generates a code snippet that uses the {@link Element} API to retrieve
     * properties from the client model when it implements the {@link HasValue}
     * contract.
     * <p>
     * The getter checks whether the return value is <code>null</code>, and
     * returns {@link HasValue#getEmptyValue()}.
     *
     * @param basicType
     *            The javascript basic type of the property.
     * @param propertyName
     *            The name of the property in the javascript model.
     * @return the code snippet ready to be added in a Java source code.
     */
    public static String generateElementApiValueGetterForType(
            ComponentBasicType basicType, String propertyName) {
        Objects.requireNonNull(propertyName);
        if (propertyName.isEmpty()) {
            throw new IllegalArgumentException("propertyName can not be empty");
        }

        String variableName = StringUtils
                .uncapitalize(formatStringToValidJavaIdentifier(propertyName));

        switch (basicType) {
        case STRING:
        case DATE:
            return String.format(
                    "String %s = getElement().getProperty(\"%s\");"
                            + "return %s == null ? getEmptyValue() : %s;",
                    variableName, propertyName, variableName, variableName);
        case ARRAY:
            return generateElementApiValueGetterForTypeRaw("JsonArray",
                    propertyName, variableName);
        case OBJECT:
            return generateElementApiValueGetterForTypeRaw("JsonObject",
                    propertyName, variableName);
        case UNDEFINED:
            return generateElementApiValueGetterForTypeRaw("JsonValue",
                    propertyName, variableName);
        case BOOLEAN:
        case NUMBER:
            return generateElementApiGetterForType(basicType, propertyName);

        default:
            throw new ComponentGenerationException(
                    "Not a supported type for getters: " + basicType);
        }
    }

    private static String generateElementApiValueGetterForTypeRaw(
            String returnType, String propertyName, String variableName) {
        return String.format(
                "Object %s = getElement().getPropertyRaw(\"%s\");"
                        + "return (%s) (%s == null ? getEmptyValue() : %s);",
                variableName, propertyName, returnType, variableName,
                variableName);
    }

    /**
     * Generates a code snippet that uses the {@link Element} API to set
     * properties to the client model.
     *
     * @param basicType
     *            The javascript basic type of the property.
     * @param propertyName
     *            The name of the property in the javascript model.
     * @param parameterName
     *            The name of the parameter in the Java source code.
     * @param nullable
     *            whether the value can be null or not
     * @return the code snippet ready to be added in a Java source code.
     */
    public static String generateElementApiSetterForType(
            ComponentBasicType basicType, String propertyName,
            String parameterName, boolean nullable) {
        switch (basicType) {
        case ARRAY:
        case UNDEFINED:
        case OBJECT:
            return String.format("getElement().setPropertyJson(\"%s\", %s);",
                    propertyName, parameterName);
        case STRING:
            // Don't insert null as property value. Insert empty String instead.
            return String.format(nullable
                    ? "getElement().setProperty(\"%s\", %s == null ? \"\" : %s);"
                    : "getElement().setProperty(\"%s\", %s);", propertyName,
                    parameterName, parameterName);
        default:
            return String.format("getElement().setProperty(\"%s\", %s);",
                    propertyName, parameterName);
        }
    }

    /**
     * Converts a javascript basic type to a Java type.
     *
     * @param type
     *            The javascript basic type.
     * @return the corresponding Java type.
     */
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

    /**
     * Converts a string in camel-case to a lower case string where each term is
     * separated by an hyphen.
     * <p>
     * For instance: {@code hasValue} would be converted to {@code has-value}.
     *
     * @param text
     *            the text to be converted
     * @return the converted text
     */
    public static String convertCamelCaseToHyphens(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] charArray = text.toCharArray();
        for (char character : charArray) {
            if (Character.isUpperCase(character)) {
                if (builder.length() > 0) {
                    builder.append("-");
                }
                builder.append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    /**
     * Adds a parameter with the specified {@code type} and {@code name} to the
     * given {@code method}, considering simple name for {@code java.lang}
     * package.
     *
     * @param javaClass
     *            the java class
     * @param method
     *            the method to add the parameter to
     * @param type
     *            the parameter type
     * @param name
     *            the parameter name
     * @return the added parameter
     */
    public static ParameterSource<JavaClassSource> addMethodParameter(
            JavaClassSource javaClass, MethodSource<JavaClassSource> method,
            Class<?> type, String name) {
        if (!type.isPrimitive()
                && !"java.lang".equals(type.getPackage().getName())) {
            javaClass.addImport(type);
        }
        return method.addParameter(type.getSimpleName(), name);
    }

}
