/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.tutorial;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;

public class TestTutorialCodeCoverage {
    private static final Path location = new File(".").toPath();
    private static final Path javaLocation = location.resolve("src/main/java");

    @Test
    public void verifyTutorialCode() throws IOException {
        Map<String, List<Path>> codeFileMap = new HashMap<>();

        // Populate map based on @CodeFor annotations
        Files.walk(javaLocation)
                .filter(path -> path.toFile().getName().endsWith(".java"))
                .forEach(path -> extractCodeFiles(path, codeFileMap));

        // Verify that tutorial code is actually found in the java files
        codeFileMap.forEach(TestTutorialCodeCoverage::verifyTutorialCode);
    }

    private static void verifyTutorialCode(String tutorialName,
            List<Path> codeFiles) {
        File tutorialFile = new File(tutorialName);
        if (!tutorialFile.exists()) {
            Assert.fail("Could not find tutorial file "
                    + tutorialFile.getAbsolutePath()
                    + " that was referenced from the java files " + codeFiles);
        }

        Set<String> allowedLines = extractSnippets(codeFiles);

        try {
            /*-
             * Simple parser for something like this:
             *
             * Regular tutorial text
             * [source,java]
             * ----
             * some.java.code();
             *
             * more.code++;
             * ––––
             *
             * More tutorial text
             */

            boolean javaBlockStarted = false;
            boolean inCodeBlock = false;

            for (String line : Files.readAllLines(tutorialFile.toPath())) {
                if (javaBlockStarted) {
                    Assert.assertEquals("----", line);

                    javaBlockStarted = false;
                    inCodeBlock = true;
                } else if (inCodeBlock) {
                    if (line.equals("----")) {
                        inCodeBlock = false;
                    } else {
                        String trimmedLine = trimWhitespace(line);
                        if (!allowedLines.contains(trimmedLine)) {
                            Assert.fail(tutorialName
                                    + " has code not in the java file: "
                                    + line);
                        }
                    }
                } else {
                    if ("[source,java]".equals(line)) {
                        javaBlockStarted = true;
                    }
                    // Else it's regular tutorial text that we don't care about
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> extractSnippets(List<Path> codeFiles) {
        try {
            Set<String> snippets = new HashSet<>();
            for (Path codeFile : codeFiles) {
                Files.lines(codeFile)
                        .map(TestTutorialCodeCoverage::trimWhitespace)
                        .forEach(snippets::add);
            }
            return snippets;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractCodeFiles(Path javaFile,
            Map<String, List<Path>> codeFiles) {
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
                Assert.fail("Java file without @CodeFor: " + className);
            } else {
                String tutorialName = codeFor.value();
                codeFiles.computeIfAbsent(tutorialName, n -> new ArrayList<>())
                        .add(javaFile);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String trimWhitespace(String codeLine) {
        return codeLine.replaceAll("\\s", "");
    }

}
