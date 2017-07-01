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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.tutorial.annotations.CodeFor;

public class TestTutorialCodeCoverage {
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
    private Map<String, Set<String>> htmlFileMap;
    private Map<String, Set<String>> cssFileMap;

    @Test
    public void verifyTutorialCode() throws IOException {
        javaFileMap = verifyJavaCode();
        htmlFileMap = verifyHtml();
        cssFileMap = verifyCss();

        // Verify that there's at least one java and html file for each tutorial
        Files.walk(DOCS_ROOT)
                .filter(path -> path.toString().endsWith(".asciidoc"))
                .forEach(this::verifyRequiredFiles);

        if (documentationErrorsCount > 0) {
            DOCUMENTATION_ERRORS.insert(0,
                    String.format("%nFound %s problems with documentation", documentationErrorsCount));
            Assert.fail(DOCUMENTATION_ERRORS.toString());
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
            addDocumentationError("Should be at least one " + fileType + " file for "
                    + tutorialName);
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
        Files.walk(JAVA_LOCATION)
                .filter(path -> path.toString().endsWith("." + JAVA))
                .forEach(path -> extractCodeFiles(path, codeFileMap));

        // Verify that tutorial code is actually found in the java files
        codeFileMap.forEach((tutorial, lines) -> verifyTutorialCode(tutorial,
                lines, JAVA_BLOCK_IDENTIFIER, JAVA));
        return codeFileMap;
    }

    private Map<String, Set<String>> verifyHtml() throws IOException {
        Map<String, Set<String>> htmlFileMap = new HashMap<>();

        Files.walk(HTML_LOCATION)
                .filter(path -> path.toString().endsWith("." + HTML))
                .forEach(path -> extractHtmlFiles(path, htmlFileMap));

        htmlFileMap.forEach((tutorial, lines) -> verifyTutorialCode(tutorial,
                lines, HTML_BLOCK_IDENTIFIER, HTML));
        return htmlFileMap;
    }

    private Map<String, Set<String>> verifyCss() throws IOException {
        Map<String, Set<String>> cssFileMap = new HashMap<>();

        Files.walk(CSS_LOCATION)
                .filter(path -> path.toString().endsWith("." + CSS))
                .forEach(path -> extractHtmlFiles(path, cssFileMap));

        cssFileMap.forEach((tutorial, lines) -> verifyTutorialCode(tutorial,
                lines, CSS_BLOCK_IDENTIFIER, CSS));
        return cssFileMap;
    }

    private static void verifyTutorialCode(String tutorialName,
            Set<String> codeLines, String blockIdentifier, String fileType) {
        File tutorialFile = new File(tutorialName);
        if (!tutorialFile.exists()) {
            addDocumentationError(String.format(
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
                        if (!codeLines.contains(trimmedLine)) {
                            addDocumentationError(String.format(
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
                addDocumentationError("Java file without @CodeFor: " + className);
                return;
            }

            String tutorialName = codeFor.value();

            Files.lines(javaFile).forEach(
                    line -> allowedLines
                            .computeIfAbsent(
                                    tutorialName.replace('/',
                                            File.separatorChar),
                                    n -> new HashSet<>())
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
                lines.forEach(
                        line -> allowedLines
                                .computeIfAbsent(
                                        tutorialName.replace('/',
                                                File.separatorChar),
                                        n -> new HashSet<>())
                                .add(trimWhitespace(line)));
            } else {
                addDocumentationError("Html file with faulty tutorial header: "
                        + htmlFile.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }

    private static void addDocumentationError(String documentationError) {
        documentationErrorsCount++;

        DOCUMENTATION_ERRORS.append(System.lineSeparator());
        DOCUMENTATION_ERRORS.append(String.format("%s. ", documentationErrorsCount));
        DOCUMENTATION_ERRORS.append(documentationError);
    }

}
