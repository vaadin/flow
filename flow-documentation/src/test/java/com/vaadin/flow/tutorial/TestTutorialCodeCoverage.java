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
package com.vaadin.flow.tutorial;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.tutorial.annotations.CodeFor;

public class TestTutorialCodeCoverage {
    private static final Path location = new File(".").toPath();
    private static final Path javaLocation = location.resolve("src/main/java");
    private static final Path htmlLocation = location.resolve("src/main/html");

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    private static final String JAVA_BLOCK_IDENTIFIER = "[source,java]";
    private static final String HTML_BLOCK_IDENTIFIER = "[source,html]";

    private static final StringBuilder exceptions = new StringBuilder();
    private static int exceptionAmount = 0;

    @Test
    public void verifyTutorialCode() throws IOException {
        Map<String, Set<String>> codeFileMap = verifyJavaCode();
        Map<String, Set<String>> htmlFileMap = verifyHtml();

        // Verify that there's at least one java and html file for each tutorial
        Files.walk(location)
                .filter(path -> path.toFile().getName()
                        .matches("tutorial-.*\\.asciidoc"))
                .forEach(tutorialPath -> {
                    verifyRequiredFiles(codeFileMap, htmlFileMap, tutorialPath);
                });

        if (exceptionAmount > 0) {
            exceptions.insert(0,
                    String.format("%sFound %s problems with documentation",
                            LINE_SEPARATOR, exceptionAmount));
            Assert.fail(exceptions.toString());
        }
    }

    private void verifyRequiredFiles(Map<String, Set<String>> codeFileMap,
            Map<String, Set<String>> htmlFileMap, Path tutorialPath) {
        String tutorialName = location.relativize(tutorialPath).toString();
        // Only require java file for tutorial that contains
        // [source,java] block
        if (!codeFileMap.containsKey(tutorialName)
                && tutorialContainsBlock(tutorialPath, JAVA_BLOCK_IDENTIFIER)) {
            addException(
                    "Should be at least one java file for " + tutorialName);
        }
        // Only require html file for tutorial that contains
        // [source,html] block
        if (!htmlFileMap.containsKey(tutorialName)
                && tutorialContainsBlock(tutorialPath, HTML_BLOCK_IDENTIFIER)) {
            addException(
                    "Should be at least one html file for " + tutorialName);
        }
    }

    private boolean tutorialContainsBlock(Path tutorialPath,
            String blockIdentifier) {
        try {
            return Files.lines(tutorialPath)
                    .anyMatch(line -> line.equals(blockIdentifier));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Set<String>> verifyJavaCode() throws IOException {
        Map<String, Set<String>> codeFileMap = new HashMap<>();

        // Populate map based on @CodeFor annotations
        Files.walk(javaLocation)
                .filter(path -> path.toFile().getName().endsWith(".java"))
                .forEach(path -> extractCodeFiles(path, codeFileMap));

        // Verify that tutorial code is actually found in the java files
        codeFileMap.forEach(TestTutorialCodeCoverage::verifyTutorialCode);
        return codeFileMap;
    }

    private Map<String, Set<String>> verifyHtml() throws IOException {
        Map<String, Set<String>> htmlFileMap = new HashMap<>();

        Files.walk(htmlLocation)
                .filter(path -> path.toFile().getName().endsWith(".html"))
                .forEach(path -> extractHtmlFiles(path, htmlFileMap));

        htmlFileMap.forEach(TestTutorialCodeCoverage::verifyTutorialHtml);
        return htmlFileMap;
    }

    private static void verifyTutorialCode(String tutorialName,
            Set<String> codeLines) {
        verifyBlockContents(tutorialName, codeLines, JAVA_BLOCK_IDENTIFIER,
                "java");
    }

    private static void verifyTutorialHtml(String tutorialName,
            Set<String> htmlLines) {
        verifyBlockContents(tutorialName, htmlLines, HTML_BLOCK_IDENTIFIER,
                "html");
    }

    private static void verifyBlockContents(String tutorialName,
            Set<String> allowedLines, String blockIdentifier, String fileType) {
        File tutorialFile = new File(tutorialName);
        if (!tutorialFile.exists()) {
            addException(String.format(
                    "Could not find tutorial file %s that was referenced from the %s files.",
                    tutorialFile.getAbsolutePath(), fileType));
            return;
        }

        try {
            /*-
             * Simple parser for something like this:
             *
             * Regular tutorial text
             * [source,TYPE]
             * ----
             * some.TYPE.source
             *
             * more.sources++
             * ––––
             *
             * More tutorial text
             */
            boolean blockStarted = false;
            boolean inBlock = false;

            for (String line : Files.readAllLines(tutorialFile.toPath())) {
                if (blockStarted) {
                    Assert.assertEquals("----", line);

                    blockStarted = false;
                    inBlock = true;
                } else if (inBlock) {
                    if (line.equals("----")) {
                        inBlock = false;
                    } else {
                        String trimmedLine = trimWhitespace(line);
                        if (!allowedLines.contains(trimmedLine)) {
                            addException(String.format(
                                    "%s has code not in the %s file: %s",
                                    tutorialName, fileType, line));
                        }
                    }
                } else {
                    if (blockIdentifier.equals(line)) {
                        blockStarted = true;
                    }
                    // Else it's regular tutorial text that we don't care about
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractCodeFiles(Path javaFile,
            Map<String, Set<String>> allowedLines) {
        String className = javaLocation.relativize(javaFile).toString()
                .replace('/', '.').replaceAll("\\.java$", "");

        try {
            Class<?> clazz = Class.forName(className);
            if (clazz == CodeFor.class) {
                // Ignore the annotation itself
                return;
            }

            CodeFor codeFor = clazz.getAnnotation(CodeFor.class);
            if (codeFor == null) {
                addException("Java file without @CodeFor: " + className);
                return;
            }

            String tutorialName = codeFor.value();

            Files.lines(javaFile)
                    .forEach(line -> allowedLines
                            .computeIfAbsent(tutorialName, n -> new HashSet<>())
                            .add(trimWhitespace(line)));
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractHtmlFiles(Path htmlFile,
            Map<String, Set<String>> allowedLines) {
        try {
            List<String> lines = Files.readAllLines(htmlFile);
            String idLine = lines.remove(0);
            if (idLine.startsWith("tutorial::")) {
                String tutorialName = idLine.substring(10);
                lines.forEach(line -> allowedLines
                        .computeIfAbsent(tutorialName, n -> new HashSet<>())
                        .add(trimWhitespace(line)));
            } else {
                addException("Html file with faulty tutorial header: "
                        + htmlFile.toFile().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }

    private static void addException(String exception) {
        exceptionAmount++;

        exceptions.append(LINE_SEPARATOR);
        exceptions.append(String.format("%s. ", exceptionAmount));
        exceptions.append(exception);
    }

}
