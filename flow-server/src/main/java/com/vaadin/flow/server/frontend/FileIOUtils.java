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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for file I/O operations, including conditional file writing,
 * file searching, and content comparison.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class FileIOUtils {

    static boolean deleteFileQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            return file.delete();
        } catch (final Exception ignored) {
            return false;
        }
    }

    static String urlToString(URL url) throws IOException {
        try (InputStream input = url.openStream()) {
            return new String(input.readAllBytes());
        }
    }

    /**
     * Copies a directory recursively.
     *
     * @param source
     *            the source directory
     * @param target
     *            the target directory
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void copyDirectory(File source, File target)
            throws IOException {
        copyDirectory(source.toPath(), target.toPath(), null);
    }

    /**
     * Copies a directory recursively with a file filter.
     *
     * @param source
     *            the source directory
     * @param target
     *            the target directory
     * @param filter
     *            the file filter to apply, or null to copy all files
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void copyDirectory(File source, File target,
            FileFilter filter) throws IOException {
        copyDirectory(source.toPath(), target.toPath(), filter);
    }

    private static void copyDirectory(Path source, Path target,
            FileFilter filter) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                if (filter != null && !filter.accept(dir.toFile())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                if (filter == null || filter.accept(file.toFile())) {
                    Files.copy(file, target.resolve(source.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a file or directory recursively. Throws an exception if the
     * deletion fails.
     *
     * @param file
     *            the file or directory to delete
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void delete(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        Path path = file.toPath();
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                        IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(path);
        }
    }

    /**
     * Gets the user's home directory.
     *
     * @return the user's home directory
     */
    static File getUserDirectory() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Lists all files in a directory with the specified extensions,
     * recursively.
     *
     * @param directory
     *            the directory to search
     * @param extensions
     *            the file extensions to include (without dots)
     * @param recursive
     *            whether to search recursively
     * @return a list of files matching the criteria
     * @throws IOException
     *             if an I/O error occurs
     */
    static List<File> listFiles(File directory, String[] extensions,
            boolean recursive) throws IOException {
        List<File> result = new ArrayList<>();
        if (!directory.isDirectory()) {
            return result;
        }

        List<String> extensionList = extensions != null
                ? Arrays.asList(extensions)
                : List.of();

        try (Stream<Path> stream = recursive ? Files.walk(directory.toPath())
                : Files.list(directory.toPath())) {
            stream.filter(Files::isRegularFile).filter(path -> {
                if (extensionList.isEmpty()) {
                    return true;
                }
                String fileName = path.getFileName().toString();
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot == -1) {
                    return false;
                }
                String extension = fileName.substring(lastDot + 1);
                return extensionList.contains(extension);
            }).forEach(path -> result.add(path.toFile()));
        }

        return result;
    }

    /**
     * Compares the content of two InputStreams.
     *
     * @param input1
     *            the first InputStream
     * @param input2
     *            the second InputStream
     * @return true if the content is equal, false otherwise
     * @throws IOException
     *             if an I/O error occurs
     */
    static boolean contentEquals(InputStream input1, InputStream input2)
            throws IOException {
        return Arrays.equals(input1.readAllBytes(), input2.readAllBytes());
    }

    /**
     * Closes a resource quietly without throwing an exception.
     *
     * @param closeable
     *            the resource to close
     */
    static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Ignore
            }
        }
    }

    /**
     * Removes the extension from a filename.
     *
     * @param filename
     *            the filename
     * @return the filename without extension
     */
    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        int lastSeparator = Math.max(filename.lastIndexOf('/'),
                filename.lastIndexOf('\\'));
        if (lastDot > lastSeparator && lastDot > 0) {
            return filename.substring(0, lastDot);
        }
        return filename;
    }

    /**
     * Checks if a string matches a wildcard pattern.
     *
     * @param text
     *            the text to check
     * @param pattern
     *            the wildcard pattern (* and ? are supported)
     * @return true if the text matches the pattern, false otherwise
     */
    public static boolean wildcardMatch(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*")
                .replace("?", ".");
        return Pattern.matches(regex, text);
    }

    /**
     * Checks if a directory is empty.
     *
     * @param directory
     *            the directory to check
     * @return true if the directory is empty, false otherwise
     * @throws IOException
     *             if an I/O error occurs
     */
    static boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

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

        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), content);
        return true;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(FileIOUtils.class);
    }

    private static String getExistingFileContent(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        return Files.readString(file.toPath());
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
