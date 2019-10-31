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
package com.vaadin.flow.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vaadin.flow.demo.SourceCodeExample.SourceType;

/**
 * Utility class for obtaining {@link SourceCodeExample}s for classes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SourceContentResolver {

    // @formatter::off
    private static final ConcurrentHashMap<Class<? extends DemoView>, List<SourceCodeExample>> CACHED_SOURCE_EXAMPLES = new ConcurrentHashMap<>();
    // @formatter::on

    private static final Pattern SOURCE_CODE_EXAMPLE_BEGIN_PATTERN = Pattern
            .compile("\\s*// begin-source-example");
    private static final Pattern SOURCE_CODE_EXAMPLE_END_PATTERN = Pattern
            .compile("\\s*// end-source-example");
    private static final Pattern SOURCE_CODE_EXAMPLE_HEADING_PATTERN = Pattern
            .compile("\\s*// source-example-heading: (.*)");
    private static final Pattern SOURCE_CODE_EXAMPLE_TYPE_PATTERN = Pattern
            .compile("\\s*// source-example-type: ([A-Z]+)");

    private SourceContentResolver() {
    }

    /**
     * Get all {@link SourceCodeExample}s from a given class.
     *
     * @param demoViewClass
     *            the class to retrieve source code examples for
     * @return an unmodifiable list of source code examples
     */
    public static List<SourceCodeExample> getSourceCodeExamplesForClass(
            Class<? extends DemoView> demoViewClass) {
        return CACHED_SOURCE_EXAMPLES.computeIfAbsent(demoViewClass,
                SourceContentResolver::parseSourceCodeExamplesForClass);
    }

    private static List<SourceCodeExample> parseSourceCodeExamplesForClass(
            Class<? extends DemoView> demoViewClass) {

        String resourcePath = getResourcePath(demoViewClass);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                SourceContentResolver.class.getClassLoader()
                        .getResourceAsStream(resourcePath),
                StandardCharsets.UTF_8))) {

            List<String> lines = reader.lines().collect(Collectors.toList());
            return Collections.unmodifiableList(parseSourceCodeExamples(lines));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error reading source examples for class " + demoViewClass,
                    e);
        }
    }

    private static String getResourcePath(
            Class<? extends DemoView> demoViewClass) {
        String javaFile = demoViewClass.getSimpleName() + ".java";
        String formattedPackageName = demoViewClass.getPackage().getName()
                .replaceAll("\\.", "/");
        String resourcePath = "../test-classes/" + formattedPackageName + "/"
                + javaFile;
        if (!isAvailable(resourcePath)) {
            resourcePath = "../test-classes/"
                    + demoViewClass.getPackage().getName() + "/" + javaFile;
            if (!isAvailable(resourcePath)) {
                resourcePath = "/" + formattedPackageName + "/" + javaFile;

                if (!isAvailable(resourcePath)) {
                    throw new IllegalArgumentException(
                            "Could not find file resources for '"
                                    + demoViewClass.getName() + "'!");
                }
            }
        }
        return resourcePath;
    }

    private static boolean isAvailable(String resourcePath) {
        return SourceContentResolver.class.getClassLoader()
                .getResource(resourcePath) != null;
    }

    private static List<SourceCodeExample> parseSourceCodeExamples(
            List<String> sourceLines) {
        List<SourceCodeExample> examples = new ArrayList<>();
        int startIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < sourceLines.size(); i++) {
            if (SOURCE_CODE_EXAMPLE_BEGIN_PATTERN.matcher(sourceLines.get(i))
                    .matches()) {
                startIndex = i;
            } else if (SOURCE_CODE_EXAMPLE_END_PATTERN
                    .matcher(sourceLines.get(i)).matches()) {
                endIndex = i;
            }
            if (startIndex != -1 && endIndex != -1
                    && startIndex + 1 < endIndex) {
                examples.add(parseSourceCodeExample(
                        sourceLines.subList(startIndex + 1, endIndex)));
                startIndex = -1;
                endIndex = -1;
            }
        }
        return examples;
    }

    private static SourceCodeExample parseSourceCodeExample(
            List<String> sourceLines) {
        String heading = parseValueFromPattern(sourceLines,
                SOURCE_CODE_EXAMPLE_HEADING_PATTERN, Function.identity(),
                () -> null);
        SourceType sourceType = parseValueFromPattern(sourceLines,
                SOURCE_CODE_EXAMPLE_TYPE_PATTERN, SourceType::valueOf,
                () -> SourceType.UNDEFINED);

        SourceCodeExample example = new SourceCodeExample();
        example.setHeading(heading);
        example.setSourceType(sourceType);
        example.setSourceCode(
                String.join("\n", trimWhitespaceAtStart(sourceLines)));
        return example;
    }

    private static <T> T parseValueFromPattern(List<String> sourceLines,
            Pattern pattern, Function<String, T> valueProvider,
            Supplier<T> nullValueProvider) {
        for (int i = 0; i < sourceLines.size(); i++) {
            Matcher matcher = pattern.matcher(sourceLines.get(i));
            if (matcher.matches()) {
                sourceLines.remove(i);
                return valueProvider.apply(matcher.group(1));
            }
        }
        return nullValueProvider.get();
    }

    private static List<String> trimWhitespaceAtStart(
            List<String> sourceLines) {
        int minIndent = Integer.MAX_VALUE;
        for (String line : sourceLines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            int indent = getWhitespaceCountAtStart(line);
            if (indent < minIndent) {
                minIndent = indent;
            }
        }
        List<String> trimmed = new ArrayList<>();
        for (String line : sourceLines) {
            if (line == null || line.isEmpty()) {
                trimmed.add("");
            } else {
                trimmed.add(line.substring(minIndent));
            }
        }
        return trimmed;
    }

    private static int getWhitespaceCountAtStart(String line) {
        int indent = 0;
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return indent;
            }
            indent++;
        }
        return indent;
    }
}
