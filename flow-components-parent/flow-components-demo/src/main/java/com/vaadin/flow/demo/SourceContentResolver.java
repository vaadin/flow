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
package com.vaadin.flow.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.flow.demo.model.SourceCodeExample;
import com.vaadin.flow.demo.model.SourceCodeExample.SourceType;
import com.vaadin.flow.demo.views.DemoView;

/**
 * Utility class for obtaining {@link SourceCodeExample}s for classes.
 * 
 * @author Vaadin Ltd
 */
public class SourceContentResolver {

    // @formatter::off
    private static final ConcurrentHashMap<Class<? extends DemoView>, List<SourceCodeExample>>
        CACHED_SOURCE_EXAMPLES = new ConcurrentHashMap<>();
    // @formatter::on
    
    private static final Pattern SOURCE_CODE_EXAMPLE_BEGIN_PATTERN = Pattern
            .compile("\\s*// begin-source-example");
    private static final Pattern SOURCE_CODE_EXAMPLE_END_PATTERN = Pattern
            .compile("\\s*// end-source-example");
    private static final Pattern SOURCE_CODE_EXAMPLE_HEADING_PATTERN = Pattern
            .compile("\\s*// source-example-heading: (.*)");
    private static final Pattern SOURCE_CODE_EXAMPLE_TYPE_PATTERN = Pattern
            .compile("\\s*// source-example-type: ([A-Z]+)");
    
    /**
     * Get all {@link SourceCodeExample}s from a given class.
     * 
     * @param demoViewClass
     *            the class to retrieve source code examples for
     * @return a list of source code examples
     */
    public static List<SourceCodeExample> getSourceCodeExamplesForClass(
            Class<? extends DemoView> demoViewClass) {
        return CACHED_SOURCE_EXAMPLES.computeIfAbsent(demoViewClass,
                SourceContentResolver::parseSourceCodeExamplesForClass);
    }

    private static List<SourceCodeExample> parseSourceCodeExamplesForClass(
            Class<? extends DemoView> demoViewClass) {

        String classFilePath = demoViewClass.getProtectionDomain()
                .getCodeSource().getLocation().getPath()
                + demoViewClass.getPackage().getName().replaceAll("\\.", "/");
        Path sourceFilePath = Paths.get(classFilePath,
                demoViewClass.getSimpleName() + ".java");

        try {
            return parseSourceCodeExamplesFromSourceLines(
                    Files.readAllLines(sourceFilePath));
        } catch (IOException ioe) {
            throw new RuntimeException(
                    String.format(
                            "IO exception when trying to read sources for class '%s' from path '%s'.",
                            demoViewClass.getName(), sourceFilePath),
                    ioe);
        } catch (SecurityException se) {
            throw new RuntimeException(
                    String.format(
                            "Security exception when reading source file for class '%s' from path '%s', check read permissions",
                            demoViewClass.getName(), sourceFilePath),
                    se);
        }
    }

    private static List<SourceCodeExample> parseSourceCodeExamplesFromSourceLines(
            List<String> sourceLines) {
        List<SourceCodeExample> examples = new ArrayList<>();
        Iterator<String> lineIterator = sourceLines.iterator();
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();
            if (SOURCE_CODE_EXAMPLE_BEGIN_PATTERN.matcher(line).matches()) {
                List<String> exampleLines = new ArrayList<String>();
                while (lineIterator.hasNext()) {
                    line = lineIterator.next();
                    if (SOURCE_CODE_EXAMPLE_END_PATTERN.matcher(line)
                            .matches()) {
                        examples.add(parseSourceCodeExampleFromSourceLines(
                                exampleLines));
                        break;
                    }
                    exampleLines.add(line);
                }
            }
        }
        return examples;
    }

    private static SourceCodeExample parseSourceCodeExampleFromSourceLines(
            List<String> sourceLines) {
        SourceCodeExample example = new SourceCodeExample();
        example.setHeading(parseHeadingFromSourceLines(sourceLines));
        example.setSourceType(parseSourceTypeFromSourceLines(sourceLines));
        example.setSourceCode(String.join("\n", trimWhiteSpaceAtStart(sourceLines)));
        return example;
    }

    private static String parseHeadingFromSourceLines(
            List<String> sourceLines) {
        for (int i = 0; i < sourceLines.size(); i++) {
            Matcher headingMatcher = SOURCE_CODE_EXAMPLE_HEADING_PATTERN
                    .matcher(sourceLines.get(i));
            if (headingMatcher.matches()) {
                sourceLines.remove(i);
                return headingMatcher.group(1);
            }
        }
        return null;
    }

    private static SourceType parseSourceTypeFromSourceLines(
            List<String> sourceLines) {
        for (int i = 0; i < sourceLines.size(); i++) {
            Matcher typeMatcher = SOURCE_CODE_EXAMPLE_TYPE_PATTERN
                    .matcher(sourceLines.get(i));
            if (typeMatcher.matches()) {
                sourceLines.remove(i);
                SourceType sourceType = SourceType
                        .valueOf(typeMatcher.group(1));
                if (sourceType == null) {
                    return SourceType.UNDEFINED;
                }
                return sourceType;
            }
        }
        return SourceType.UNDEFINED;
    }

    private static List<String> trimWhiteSpaceAtStart(
            List<String> sourceLines) {
        int minIndent = Integer.MAX_VALUE;
        for (String line : sourceLines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            int indent = 0;
            for (int i = 0; i < line.length(); i++) {
                if (!Character.isWhitespace(line.charAt(i))) {
                    break;
                }
                indent++;
            }
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
}
