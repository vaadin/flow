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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
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
 * 
 * @since 25.0
 */
public class FileIOUtils {

    private FileIOUtils() {
        // Utils only
    }

    /**
     * Deletes file if it exists and eats exceptions.
     *
     * Note, this is an internal helper method, use only from framework code.
     *
     * @param file
     *            to be deleted
     * @return true if succeeded
     * @deprecated use {@link #deleteQuietly(File)} instead, which also deletes
     *             directory contents and logs why a deletion failed instead of
     *             failing silently
     */
    @Deprecated(since = "25.3", forRemoval = true)
    public static boolean deleteFileQuietly(File file) {
        return deleteQuietly(file);
    }

    /**
     * Deletes a file, or a directory and its contents, logging a warning that
     * explains how to unblock the deletion if it fails.
     *
     * Note, this is an internal helper method, use only from framework code.
     *
     * @param file
     *            the file or directory to delete, may be {@code null}
     * @return {@code true} if the file was deleted or did not exist
     * @since 25.3
     */
    public static boolean deleteQuietly(File file) {
        return file != null && deleteQuietly(file.toPath());
    }

    /**
     * Deletes a file, or a directory and its contents, logging a warning that
     * explains how to unblock the deletion if it fails.
     *
     * Note, this is an internal helper method, use only from framework code.
     *
     * @param path
     *            the file or directory to delete, may be {@code null}
     * @return {@code true} if the file was deleted or did not exist
     * @since 25.3
     */
    public static boolean deleteQuietly(Path path) {
        if (path == null) {
            return false;
        }
        try {
            delete(path);
            return true;
        } catch (IOException | RuntimeException e) {
            log().warn(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reads the content from given URL into UTF 8 String.
     * 
     * Note, this is an internal helper method, use only from framework code.
     * 
     * @param url
     *            the URL to read
     * @return string from the content
     * @throws IOException
     */
    public static String urlToString(URL url) throws IOException {
        try (InputStream input = url.openStream()) {
            return StringUtil.toUTF8String(input);
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
     * Deletes a file, or a directory and its contents, recursively. Does
     * nothing if the file does not exist.
     * <p>
     * Symbolic links and Windows junctions are removed as links, the contents
     * they point to are left untouched.
     *
     * @param file
     *            the file or directory to delete
     * @throws IOException
     *             if the file or any of its contents could not be deleted, with
     *             a message describing how the deletion can be unblocked
     */
    public static void delete(File file) throws IOException {
        delete(file.toPath());
    }

    /**
     * Deletes a file, or a directory and its contents, recursively. Does
     * nothing if the file does not exist.
     * <p>
     * Symbolic links and Windows junctions are removed as links, the contents
     * they point to are left untouched.
     *
     * @param path
     *            the file or directory to delete
     * @throws IOException
     *             if the file or any of its contents could not be deleted, with
     *             a message describing how the deletion can be unblocked
     * @since 25.3
     */
    public static void delete(Path path) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            return;
        } catch (IOException e) {
            throw deletionFailed(path, e);
        }

        if (!hasDeletableContents(attributes)) {
            deleteSingle(path);
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                if (hasDeletableContents(attrs)) {
                    return FileVisitResult.CONTINUE;
                }
                // A junction looks like a directory but must be removed as a
                // link so that the linked contents are not deleted
                deleteSingle(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                deleteSingle(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                throw deletionFailed(file, exc);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                // A failure to list the contents surfaces as a failure to
                // delete the then non-empty directory
                deleteSingle(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Checks whether the given attributes describe a directory that can be
     * traversed to delete its contents.
     * <p>
     * A symbolic link to a directory is not reported as a directory as the
     * attributes are read without following links. A Windows junction is
     * reported as a directory but also as {@code other}, which no real
     * directory is.
     */
    private static boolean hasDeletableContents(
            BasicFileAttributes attributes) {
        return attributes.isDirectory() && !attributes.isOther();
    }

    private static void deleteSingle(Path path) throws IOException {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw deletionFailed(path, e);
        }
    }

    /**
     * Wraps a deletion failure into an exception that suggests how to unblock
     * the deletion, as the reported cause is usually another process holding
     * the file open rather than the actual root cause.
     */
    private static IOException deletionFailed(Path path, IOException cause) {
        String advice = FrontendUtils.isWindows() ? """
                On Windows this usually means that another process, such as a \
                running development server, an editor or a virus scanner, is \
                keeping the file open. Find the process holding it with \
                'openfiles /query' or with Sysinternals \
                'handle64 <path>', stop it with 'taskkill /PID <pid> /F' and \
                then run the build again.""" : """
                Check that no other process is using the file, for example \
                with 'lsof <path>', and that you have permission to delete \
                it, and then run the build again.""";
        return new IOException("Failed to delete " + path + ". "
                + advice.replace("<path>", path.toAbsolutePath().toString()),
                cause);
    }

    /**
     * Gets the user's home directory.
     *
     * @return the user's home directory
     */
    public static File getUserDirectory() {
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
    public static List<File> listFiles(File directory, String[] extensions,
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
    public static boolean contentEquals(InputStream input1, InputStream input2)
            throws IOException {
        return Arrays.equals(input1.readAllBytes(), input2.readAllBytes());
    }

    /**
     * Closes a resource quietly without throwing an exception.
     *
     * @param closeable
     *            the resource to close
     */
    public static void closeQuietly(AutoCloseable closeable) {
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
    public static boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
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
        writeAtomically(file.toPath(), content);
        return true;
    }

    /**
     * Writes the given content to the target path atomically.
     * <p>
     * The content is first written to a temporary file in the same directory
     * and then moved over the target, so that a file system watcher (such as
     * Vite's during development) never observes a truncated, partially written
     * or momentarily missing file while the content is being updated. Observing
     * such an intermediate state would otherwise make the dev server fail to
     * resolve imports between generated files.
     */
    private static void writeAtomically(Path target, String content)
            throws IOException {
        Path directory = target.getParent();
        Path tempFile = Files.createTempFile(directory,
                target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, content);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Fall back to a non-atomic replace if the file system does not
                // support atomic moves. This is still a single move operation
                // and avoids the truncate-then-write window of a direct write.
                log().debug("atomic move not supported for '{}', "
                        + "falling back to a regular move", target, e);
                Files.move(tempFile, target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
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
