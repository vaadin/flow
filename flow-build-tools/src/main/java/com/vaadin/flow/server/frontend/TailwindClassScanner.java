/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans Java source files for CSS class names passed to HasStyle methods.
 * <p>
 * This scanner detects changes in Tailwind CSS class names by parsing Java
 * source files as plain text and extracting string literals passed to methods
 * like addClassName(), addClassNames(), setClassName().
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.1
 */
public class TailwindClassScanner {

    private static final Logger log = LoggerFactory
            .getLogger(TailwindClassScanner.class);

    /**
     * Pattern to match HasStyle method calls with string arguments.
     * Matches patterns like:
     * - addClassName("class-name")
     * - addClassNames("class1", "class2")
     * - setClassName("class-name")
     * - element.addClassName("class-name")
     */
    private static final Pattern CLASS_METHOD_PATTERN = Pattern.compile(
            // Match addClassName/addClassNames/setClassName method calls
            "(addClassName|addClassNames|setClassName)\\s*\\(([^;]*?)\\)",
            Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Pattern to extract string literals from method arguments.
     * Handles both single and double quotes.
     */
    private static final Pattern STRING_LITERAL_PATTERN = Pattern
            .compile("\"([^\"]*)\"|'([^']*)'");

    /**
     * Scans Java source directory and generates a hash of all CSS class names
     * found in HasStyle method calls.
     *
     * @param sourceDirectory
     *            the root directory containing Java source files (typically
     *            src/main/java)
     * @return a hash representing all CSS class names found, or null if
     *         scanning fails
     */
    public static String scanAndHashClassNames(File sourceDirectory) {
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            log.debug("Source directory does not exist: {}",
                    sourceDirectory);
            return null;
        }

        try {
            List<String> allClassNames = new ArrayList<>();

            // Walk through all .java files
            try (Stream<Path> paths = Files.walk(sourceDirectory.toPath())) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                List<String> classNames = extractClassNamesFromFile(
                                        path.toFile());
                                allClassNames.addAll(classNames);
                            } catch (IOException e) {
                                log.warn("Failed to scan file: {}", path, e);
                            }
                        });
            }

            // Sort for consistent hashing
            allClassNames.sort(String::compareTo);

            // Generate hash of all class names
            return generateHash(String.join("\n", allClassNames));

        } catch (IOException e) {
            log.error("Failed to scan Java sources for CSS class names", e);
            return null;
        }
    }

    /**
     * Extracts CSS class names from a single Java source file.
     *
     * @param javaFile
     *            the Java source file to scan
     * @return list of CSS class names found in the file
     * @throws IOException
     *             if file cannot be read
     */
    static List<String> extractClassNamesFromFile(File javaFile)
            throws IOException {
        List<String> classNames = new ArrayList<>();
        String content = Files.readString(javaFile.toPath(),
                StandardCharsets.UTF_8);

        // Find all method calls to HasStyle methods
        Matcher methodMatcher = CLASS_METHOD_PATTERN.matcher(content);

        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            String arguments = methodMatcher.group(2);

            if (arguments == null) {
                continue;
            }

            // Extract string literals from the arguments
            Matcher stringMatcher = STRING_LITERAL_PATTERN.matcher(arguments);

            while (stringMatcher.find()) {
                // Get the matched string (either from group 1 or 2)
                String stringValue = stringMatcher.group(1) != null
                        ? stringMatcher.group(1)
                        : stringMatcher.group(2);

                if (stringValue != null && !stringValue.trim().isEmpty()) {
                    // Split space-separated class names
                    String[] parts = stringValue.trim().split("\\s+");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            classNames.add(part);
                        }
                    }
                }
            }
        }

        return classNames;
    }

    /**
     * Generates SHA-256 hash of the given content.
     *
     * @param content
     *            the content to hash
     * @return hex-encoded hash string
     */
    private static String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            // Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Checks if CSS class names have changed between builds by comparing
     * hashes.
     *
     * @param sourceDirectory
     *            the Java source directory to scan
     * @param previousHash
     *            the hash from the previous build, or null if no previous hash
     * @return true if class names have changed (or if this is first scan)
     */
    public static boolean haveClassNamesChanged(File sourceDirectory,
            String previousHash) {
        String currentHash = scanAndHashClassNames(sourceDirectory);

        if (currentHash == null) {
            // Failed to scan, assume changed to be safe
            log.warn(
                    "Failed to scan for CSS class names, assuming changed to trigger rebuild");
            return true;
        }

        if (previousHash == null) {
            log.info("No previous CSS class name hash found, first build");
            return true;
        }

        boolean changed = !currentHash.equals(previousHash);

        if (changed) {
            log.info("CSS class names have changed (hash: {} -> {})",
                    previousHash.substring(0, 8),
                    currentHash.substring(0, 8));
        } else {
            log.debug("CSS class names unchanged (hash: {})",
                    currentHash.substring(0, 8));
        }

        return changed;
    }
}
