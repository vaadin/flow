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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.tutorial.annotations.CodeFor;

public class TestTutorialCodeCoverage {
    static final String ASCII_DOC_EXTENSION = ".asciidoc";
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

    private static final String JAVA = "java";
    private static final String HTML = "html";
    private static final String CSS = "css";

    private static final StringBuilder DOCUMENTATION_ERRORS = new StringBuilder();
    private static int documentationErrorsCount = 0;

    private Map<String, Set<String>> javaFileMap;
    private Map<String, Set<String>> cssFileMap;
    private Map<String, Set<String>> htmlFileMap;

    @Test
    public void verifyTutorialCode() throws IOException {
        javaFileMap = gatherJavaCode();
        cssFileMap = gatherWebFilesCode(CSS_LOCATION, CSS);
        htmlFileMap = gatherWebFilesCode(HTML_LOCATION, HTML);

        List<TutorialLineChecker> lineCheckers = Arrays.asList(
                new CodeFileChecker(JAVA_BLOCK_IDENTIFIER, javaFileMap),
                new CodeFileChecker(CSS_BLOCK_IDENTIFIER, cssFileMap),
                new CodeFileChecker(HTML_BLOCK_IDENTIFIER, htmlFileMap),
                new AsciiDocDocumentLinkChecker(),
                new AsciiDocImageLinkChecker());

        Set<Path> allTutorials = Files.walk(DOCS_ROOT)
                .filter(path -> path.toString().endsWith(ASCII_DOC_EXTENSION))
                .collect(Collectors.toSet());

        allTutorials.forEach(
                tutorialPath -> verifyTutorial(tutorialPath, lineCheckers));

        if (documentationErrorsCount > 0) {
            DOCUMENTATION_ERRORS.insert(0,
                    String.format("%nFound %s problems with documentation",
                            documentationErrorsCount));
            Assert.fail(DOCUMENTATION_ERRORS.toString());
        }
    }

    private void verifyTutorial(Path tutorialPath,
            List<TutorialLineChecker> lineCheckers) {
        String tutorialName = DOCS_ROOT.relativize(tutorialPath).toString();
        verifyRequiredFiles(tutorialPath);

        if (!Files.isRegularFile(tutorialPath)) {
            addDocumentationError(String.format(
                    "Tutorial %s file either does not exist or not a file.",
                    tutorialPath.toAbsolutePath()));
            return;
        }
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

    private void verifyRequiredFiles(Path tutorialPath) {
        String tutorialName = DOCS_ROOT.relativize(tutorialPath).toString();
        // Only require java file for tutorial that contains
        // [source,java] block
        checkTutorial(tutorialName, tutorialPath, javaFileMap,
                JAVA_BLOCK_IDENTIFIER, JAVA);
        // Only require html file for tutorial that contains
        // [source,html] block
        checkTutorial(tutorialName, tutorialPath, htmlFileMap,
                HTML_BLOCK_IDENTIFIER, HTML);
        // Only require css file for tutorial that contains
        // [source,css] block
        checkTutorial(tutorialName, tutorialPath, cssFileMap,
                CSS_BLOCK_IDENTIFIER, CSS);
    }

    private void checkTutorial(String tutorialName, Path tutorialPath,
            Map<String, Set<String>> map, String blockIdentifier,
            String fileType) {
        if (!map.containsKey(tutorialName)
                && tutorialContainsBlock(tutorialPath, blockIdentifier)) {
            addDocumentationError("Should be at least one " + fileType
                    + " file for " + tutorialName);
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

    private Map<String, Set<String>> gatherJavaCode() throws IOException {
        Map<String, Set<String>> codeFileMap = new HashMap<>();

        // Populate map based on @CodeFor annotations
        Files.walk(JAVA_LOCATION)
                .filter(path -> path.toString().endsWith("." + JAVA))
                .forEach(path -> extractJavaFiles(path, codeFileMap));

        return codeFileMap;
    }

    private Map<String, Set<String>> gatherWebFilesCode(Path location,
            String extension) throws IOException {
        Map<String, Set<String>> codeFileMap = new HashMap<>();

        Files.walk(location)
                .filter(path -> path.toString().endsWith("." + extension))
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
            if (idLine.startsWith("tutorial::")) {
                String tutorialName = idLine.substring(10);
                lines.forEach(line -> addLineToAllowed(allowedLines,
                        tutorialName, line));
            } else {
                addDocumentationError("Html file with faulty tutorial header: "
                        + htmlFile.toString());
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

    private static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }

    private void addDocumentationError(String documentationError) {
        documentationErrorsCount++;

        DOCUMENTATION_ERRORS.append(System.lineSeparator());
        DOCUMENTATION_ERRORS
                .append(String.format("%s. ", documentationErrorsCount));
        DOCUMENTATION_ERRORS.append(documentationError);
    }

}
