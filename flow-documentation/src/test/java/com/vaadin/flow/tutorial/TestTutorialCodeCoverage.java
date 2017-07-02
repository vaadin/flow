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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.tutorial.annotations.CodeFor;

public class TestTutorialCodeCoverage {
    private static final String ASCII_DOC_EXTENSION = ".asciidoc";
    private static final String WEB_SOURCE_MARK = "tutorial::";

    private static final Path DOCS_ROOT = new File(".").toPath();
    private static final Path JAVA_LOCATION = DOCS_ROOT
            .resolve(Paths.get("src", "main", "java"));
    private static final Path HTML_LOCATION = DOCS_ROOT
            .resolve(Paths.get("src", "main", "html"));
    private static final Path CSS_LOCATION = DOCS_ROOT
            .resolve(Paths.get("src", "main", "css"));

    private static final String JAVA_BLOCK_IDENTIFIER = "[source,java]";
    private static final String HTML_BLOCK_IDENTIFIER = "[source,html]";
    private static final String CSS_BLOCK_IDENTIFIER = "[source,css]";

    private final StringBuilder documentationErrors = new StringBuilder();
    private int documentationErrorsCount;

    @Test
    public void verifyTutorialCode() throws IOException {
        List<TutorialLineChecker> lineCheckers = Arrays.asList(
                new CodeFileChecker(JAVA_BLOCK_IDENTIFIER, gatherJavaCode()),
                new CodeFileChecker(CSS_BLOCK_IDENTIFIER, gatherWebFilesCode(CSS_LOCATION, "css")),
                new CodeFileChecker(HTML_BLOCK_IDENTIFIER, gatherWebFilesCode(HTML_LOCATION, "html")),
                new AsciiDocLinkWithDescriptionChecker("image:",
                        Pattern.compile("image:(.*?)\\[(.*?)]")),
                new AsciiDocLinkWithDescriptionChecker("#,",
                        Pattern.compile("<<(.*?)#,(.*?)>>"),
                        ASCII_DOC_EXTENSION));

        Files.walk(DOCS_ROOT)
                .filter(path -> path.toString().endsWith(ASCII_DOC_EXTENSION))
                .collect(Collectors.toSet())
                .forEach(tutorialPath -> verifyTutorial(tutorialPath,
                        lineCheckers));

        if (documentationErrorsCount > 0) {
            documentationErrors.insert(0,
                    String.format("%nFound %s problems with documentation",
                            documentationErrorsCount));
            Assert.fail(documentationErrors.toString());
        }
    }

    private void verifyTutorial(Path tutorialPath,
            List<TutorialLineChecker> lineCheckers) {
        String tutorialName = DOCS_ROOT.relativize(tutorialPath).toString();
        try {
            for (String line : Files.readAllLines(tutorialPath)) {
                lineCheckers.stream()
                        .map(checker -> checker.verifyTutorialLine(tutorialPath,
                                tutorialName, line))
                        .filter(errorList -> !errorList.isEmpty())
                        .flatMap(Collection::stream)
                        .forEach(this::addDocumentationError);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "An error during file read occurred, file = "
                            + tutorialPath.toAbsolutePath(),
                    e);
        }
    }

    private Map<String, Set<String>> gatherJavaCode() throws IOException {
        Map<String, Set<String>> codeFileMap = new HashMap<>();

        // Populate map based on @CodeFor annotations
        Files.walk(JAVA_LOCATION)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> extractJavaFiles(path, codeFileMap));

        return codeFileMap;
    }

    private Map<String, Set<String>> gatherWebFilesCode(Path location,
            String extension) throws IOException {
        Map<String, Set<String>> codeFileMap = new HashMap<>();

        Files.walk(location)
                .filter(path -> path.toString().endsWith('.' + extension))
                .forEach(path -> extractWebFiles(path, codeFileMap));

        return codeFileMap;
    }

    private void extractJavaFiles(Path javaFile,
            Map<String, Set<String>> allowedLines) {
        String className = JAVA_LOCATION.relativize(javaFile).toString()
                .replace(File.separatorChar, '.').replaceAll("\\.java$", "");

        try {
            Class<?> clazz = Class.forName(className);
            if (clazz == CodeFor.class) {
                // Ignore the annotation itself
                return;
            }

            CodeFor codeFor = clazz.getAnnotation(CodeFor.class);
            if (codeFor == null) {
                addDocumentationError(
                        "Java file without @CodeFor: " + className);
                return;
            }

            String tutorialName = codeFor.value();

            Files.lines(javaFile).forEach(
                    line -> addLineToAllowed(allowedLines, tutorialName, line));
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractWebFiles(Path htmlFile,
            Map<String, Set<String>> allowedLines) {
        try {
            List<String> lines = Files.readAllLines(htmlFile);
            String idLine = lines.remove(0);
            if (idLine.startsWith(WEB_SOURCE_MARK)) {
                String tutorialName = idLine
                        .substring(WEB_SOURCE_MARK.length());
                lines.forEach(line -> addLineToAllowed(allowedLines,
                        tutorialName, line));
            } else {
                addDocumentationError("Html file with faulty tutorial header: "
                        + htmlFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addLineToAllowed(
            Map<String, Set<String>> allowedLines, String tutorialName,
            String line) {
        return allowedLines
                .computeIfAbsent(tutorialName.replace('/', File.separatorChar),
                        n -> new HashSet<>())
                .add(trimWhitespace(line));
    }

    static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }

    private void addDocumentationError(String documentationError) {
        documentationErrorsCount++;

        documentationErrors.append(System.lineSeparator());
        documentationErrors
                .append(String.format("%s. ", documentationErrorsCount));
        documentationErrors.append(documentationError);
    }

}
