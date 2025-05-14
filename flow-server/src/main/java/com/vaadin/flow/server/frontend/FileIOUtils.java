/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIOUtils {

    private FileIOUtils() {
        // Utils only
    }

    /**
     * Writes the given content into the given file unless the file already
     * contains that content.
     *
     * @param file
     *            the file to write to
     * @param content
     *            the lines to write
     * @return true if the content was written to the file, false otherwise
     * @throws IOException
     *             if something went wrong
     */
    public static boolean writeIfChanged(File file, List<String> content)
            throws IOException {
        return writeIfChanged(file,
                content.stream().collect(Collectors.joining("\n")));
    }

    /**
     * Writes the given content into the given file unless the file already
     * contains that content.
     *
     * @param file
     *            the file to write to
     * @param content
     *            the content to write
     * @return true if the content was written to the file, false otherwise
     * @throws IOException
     *             if something went wrong
     */
    public static boolean writeIfChanged(File file, String content)
            throws IOException {
        String existingFileContent = getExistingFileContent(file);
        if (content.equals(existingFileContent)) {
            // Do not write the same contents to avoid frontend recompiles
            log().debug("skipping writing to file '{}' because content matches",
                    file);
            return false;
        }

        log().debug("writing to file '{}' because content does not match",
                file);

        FileUtils.forceMkdirParent(file);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        return true;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(FileIOUtils.class);
    }

    private static String getExistingFileContent(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * Try determining the project folder from the classpath.
     *
     * @return A file referring to the project folder or null if the folder
     *         could not be determined
     */
    public static File getProjectFolderFromClasspath() {
        try {
            URL url = FileIOUtils.class.getClassLoader().getResource(".");
            if (url != null && url.getProtocol().equals("file")) {
                return getProjectFolderFromClasspath(url);
            }
        } catch (Exception e) {
            log().warn("Unable to determine project folder using classpath", e);
        }
        return null;

    }

    static File getProjectFolderFromClasspath(URL rootFolder)
            throws URISyntaxException {
        // URI decodes the path so that e.g. " " works correctly
        // Path.of makes windows paths work correctly
        Path path = Path.of(rootFolder.toURI());
        if (path.endsWith(Path.of("target", "classes"))) {
            return path.getParent().getParent().toFile();
        }

        return null;
    }

    /**
     * Checks if the given file is likely a temporary file created by an editor.
     *
     * @param file
     *            the file to check
     * @return true if the file is likely a temporary file, false otherwise
     */
    public static boolean isProbablyTemporaryFile(File file) {
        return file.getName().endsWith("~");
    }

    /**
     * Get a list of files in a given directory that match a given glob pattern.
     *
     * @param baseDir
     *            a directory to walk in
     * @param pattern
     *            glob pattern to filter files, e.g. "*.js".
     * @return a list of files matching a given pattern
     * @throws IOException
     *             if an I/O error is thrown while walking through the tree in
     *             base directory
     */
    public static List<Path> getFilesByPattern(Path baseDir, String pattern)
            throws IOException {
        if (baseDir == null || !baseDir.toFile().exists()) {
            throw new IllegalArgumentException(
                    "Base directory is empty or doesn't exist: " + baseDir);
        }

        if (pattern == null || pattern.isBlank()) {
            pattern = "*";
        }

        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);

        List<Path> matchingPaths = new ArrayList<>();
        Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) {
                if (matcher.matches(file)) {
                    matchingPaths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return matchingPaths;
    }

    /**
     * Compare two file content strings ignoring indentation and EOL characters.
     *
     * @param content1
     *            the first file content to compare
     * @param content2
     *            the second file content to compare
     * @param compareFn
     *            a function to compare the normalized strings
     * @return true if the normalized strings are equal, false otherwise
     */
    public static boolean compareIgnoringIndentationAndEOL(String content1,
            String content2, BiPredicate<String, String> compareFn) {
        return compareFn.test(replaceIndentationAndEOL(content1),
                replaceIndentationAndEOL(content2));
    }

    /**
     * Compare two file content strings ignoring indentation, EOL characters and
     * white space where it does not matter (before and after {, }, ' and :
     * chars).
     *
     * @param content1
     *            the first file content to compare
     * @param content2
     *            the second file content to compare
     * @param compareFn
     *            a function to compare the normalized strings
     * @return true if the normalized strings are equal, false otherwise
     */
    public static boolean compareIgnoringIndentationEOLAndWhiteSpace(
            String content1, String content2,
            BiPredicate<String, String> compareFn) {
        return compareFn.test(
                replaceWhiteSpace(replaceIndentationAndEOL(content1)),
                replaceWhiteSpace(replaceIndentationAndEOL(content2)));
    }

    // Normalize EOL and removes indentation and potential EOL at the end of the
    // FILE
    private static String replaceIndentationAndEOL(String text) {
        return text.replace("\r\n", "\n").replaceFirst("\n$", "")
                .replaceAll("(?m)^(\\s)+", "");
    }

    private static String replaceWhiteSpace(String text) {
        for (String character : Stream.of("{", "}", ":", "'", "[", "]")
                .toList()) {
            text = replaceWhiteSpaceAround(text, character);
        }
        return text;
    }

    private static String replaceWhiteSpaceAround(String text,
            String character) {
        return text
                .replaceAll(String.format("(\\s)*\\%s", character), character)
                .replaceAll(String.format("\\%s(\\s)*", character), character);
    }
}
