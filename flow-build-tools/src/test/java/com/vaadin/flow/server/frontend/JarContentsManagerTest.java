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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.testutil.TestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
class JarContentsManagerTest {
    @TempDir
    File testDirectory;

    private final JarContentsManager jarContentsManager = new JarContentsManager();
    private final File testJar = TestUtils.getTestJar();

    @Test
    void getFileContents_directoryInsteadOfJar() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> jarContentsManager
                        .getFileContents(testDirectory, "test"));
        assertTrue(ex.getMessage().contains(String
                .format("Expect '%s' to be an existing file", testDirectory)));
    }

    @Test
    void getFileContents_notAJarFile() throws IOException {
        File testFile = new File(testDirectory, "test");
        testFile.createNewFile();

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> jarContentsManager.getFileContents(testFile, "test"));
        assertTrue(ex.getMessage()
                .contains(String.format(
                        "Failed to retrieve file '%s' from jar '%s'", "test",
                        testFile)));
    }

    @Test
    void getFileContents_nonExistingJarFile() {
        File test = new File("test");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.getFileContents(test, "test"));
        assertTrue(ex.getMessage().contains(
                String.format("Expect '%s' to be an existing file", test)));
    }

    @Test
    void getFileContents_nonExistingFile() {
        byte[] fileContents = jarContentsManager.getFileContents(testJar,
                "blah");

        assertNull(fileContents, "Expect to have non-empty file from jar");
    }

    @Test
    void getFileContents_existingFile() {
        byte[] fileContents = jarContentsManager.getFileContents(testJar,
                "META-INF/resources/webjars/paper-button/2.0.0/bower.json");

        assertNotNull(fileContents, "Expect to have non-empty file from jar");
        assertTrue(fileContents.length > 0,
                "Expect to have non-empty file from jar");
    }

    @Test
    void containsPath_directoryInsteadOfJar() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.containsPath(testDirectory, "test"));
        assertTrue(ex.getMessage().contains(String
                .format("Expect '%s' to be an existing file", testDirectory)));
    }

    @Test
    void containsPath_notAJarFile() throws IOException {
        File testFile = new File(testDirectory, "test");
        testFile.createNewFile();

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> jarContentsManager.containsPath(testFile, "test"));
        assertTrue(ex.getMessage()
                .contains(String.format(
                        "Failed to retrieve file '%s' from jar '%s'", "test",
                        testFile)));
    }

    @Test
    void containsPath_nonExistingJarFile() {
        File test = new File("test");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.containsPath(test, "test"));
        assertTrue(ex.getMessage().contains(
                String.format("Expect '%s' to be an existing file", test)));
    }

    @Test
    void containsPath_nonExistingPath() {
        String nonExistingPath = "should not exist";

        assertFalse(jarContentsManager.containsPath(testJar, nonExistingPath),
                String.format("Test jar '%s' should not contain path '%s'",
                        testJar, nonExistingPath));
    }

    @Test
    void containsPath_existingFile() {
        String existingPath = "META-INF/resources/webjars/";

        assertTrue(jarContentsManager.containsPath(testJar, existingPath),
                String.format("Test jar '%s' should contain path '%s'", testJar,
                        existingPath));
    }

    /*
     * Test for issue: flow fails to serve static resources from latest webjars
     * #6241 https://github.com/vaadin/flow/issues/6241
     */
    @Test
    void containsPath_missingDirectoryStructure_scansForMatch() {
        String existingPathLocal = "META-INF/resources/webjars/";
        File testJarLocal = TestUtils.getTestJar("test-jar-issue-6241.jar");

        assertTrue(
                jarContentsManager.containsPath(testJarLocal,
                        existingPathLocal),
                String.format("Test jar '%s' should contain path '%s'", testJar,
                        existingPathLocal));
    }

    @Test
    void findFiles_directoryInsteadOfJar() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> jarContentsManager
                        .findFiles(testDirectory, "test", "test"));
        assertTrue(ex.getMessage().contains(String
                .format("Expect '%s' to be an existing file", testDirectory)));
    }

    @Test
    void findFiles_notAJarFile() throws IOException {
        File testFile = new File(testDirectory, "test");
        testFile.createNewFile();

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> jarContentsManager.findFiles(testFile, "test", "test"));
        assertTrue(ex.getMessage()
                .contains("java.util.zip.ZipException: zip file is empty"));
    }

    @Test
    void findFiles_nonExistingJarFile() {
        File test = new File("test");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.findFiles(test, "test", "test"));
        assertTrue(ex.getMessage().contains(
                String.format("Expect '%s' to be an existing file", test)));
    }

    @Test
    void findFiles_nonExistingFile() {
        List<String> result = jarContentsManager.findFiles(testJar, "blah",
                "nope");

        assertTrue(result.isEmpty(),
                "Expect to have empty results for non-existing file");
    }

    @Test
    void findFiles_existingFiles() {
        String resourceName = "vaadin-charts-webjar-6.0.0-alpha3.jar";
        String searchName = "bower.json";

        List<String> bowerJsons = jarContentsManager
                .findFiles(TestUtils.getTestJar(resourceName), "", searchName);

        assertEquals(2, bowerJsons.size(),
                String.format("Expect '%s' WebJar to contain two '%s' files",
                        resourceName, searchName));
        assertTrue(
                bowerJsons.stream()
                        .allMatch(path -> path.endsWith('/' + searchName)),
                String.format(
                        "Expect all found paths to end with the file name searched for: '%s'",
                        searchName));
    }

    @Test
    void findFiles_existingFiles_baseDirectoryMatters() {
        String resourceName = "vaadin-charts-webjar-6.0.0-alpha3.jar";
        String testPath = "META-INF/resources/webjars/highcharts/5.0.14/";
        String searchName = "bower.json";

        List<String> bowerJson = jarContentsManager.findFiles(
                TestUtils.getTestJar(resourceName), testPath, searchName);

        assertEquals(1, bowerJson.size(), String.format(
                "Expect '%s' WebJar to contain one '%s' file in directory '%s'",
                resourceName, searchName, testPath));
    }

    @Test
    void copyFilesFromJar_nullJarFile() {
        assertThrows(NullPointerException.class, () -> jarContentsManager
                .copyFilesFromJarTrimmingBasePath(null, null, testDirectory));
    }

    @Test
    void copyFilesFromJar_notAJarFile() throws IOException {
        File testFile = new File(testDirectory, "test");
        testFile.createNewFile();

        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> jarContentsManager.copyFilesFromJarTrimmingBasePath(
                        testFile, null, testDirectory));
        assertTrue(ex.getMessage().contains(String.format(
                "Failed to extract files from jarFile '%s' to directory '%s'",
                testFile, testDirectory)));
    }

    @Test
    void copyFilesFromJar_nonExistingJarFile() {
        File test = new File("test");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.copyFilesFromJarTrimmingBasePath(test,
                        null, testDirectory));
        assertTrue(ex.getMessage().contains(
                String.format("Expect '%s' to be an existing file", test)));
    }

    @Test
    void copyFilesFromJar_directoryInsteadOfJar() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.copyFilesFromJarTrimmingBasePath(
                        testDirectory, null, testDirectory));
        assertTrue(ex.getMessage().contains(String
                .format("Expect '%s' to be an existing file", testDirectory)));
    }

    @Test
    void copyFilesFromJar_nullOutputDirectory() {
        assertThrows(NullPointerException.class, () -> jarContentsManager
                .copyFilesFromJarTrimmingBasePath(testJar, null, null));
    }

    @Test
    void copyFilesFromJar_fileInsteadOfDirectory() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jarContentsManager.copyFilesFromJarTrimmingBasePath(
                        testJar, null, testJar));
        assertTrue(ex.getMessage().contains(String
                .format("Expect '%s' to be an existing directory", testJar)));
    }

    @Test
    void copyFilesFromJar_noBasePath_noExclusions() {
        assertThat(
                "Do not expect any files in temporary directory before the test",
                TestUtils.listFilesRecursively(testDirectory).size(), is(0));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                testDirectory);

        assertThat("Temporary directory should have files after jar copied",
                TestUtils.listFilesRecursively(testDirectory).size(),
                is(not(0)));
    }

    @Test
    void copyFilesFromJar_noBasePath_excludeEverything() {
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                testDirectory, "*");
        assertThat(
                "Do not expect any files with filter that excludes everything",
                TestUtils.listFilesRecursively(testDirectory).size(), is(0));
    }

    @Test
    void copyFilesFromJar_withBasePath_noExclusions() throws IOException {
        String basePath = "META-INF/maven/";
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                testDirectory);

        List<String> resultingPaths = TestUtils
                .listFilesRecursively(testDirectory);
        assertThat(String.format(
                "Expect jar '%s' to contain files with base path '%s'", testJar,
                basePath), resultingPaths.size(), is(not(0)));
        assertTrue(
                resultingPaths.stream()
                        .noneMatch(path -> path.contains(basePath)),
                "Resulting paths should not contain base path = " + basePath);
    }

    @Test
    void copyFilesFromJar_exclusionsWork() throws IOException {
        String basePath = "META-INF/maven";

        File notFilteredCopyingDirectory = new File(testDirectory,
                "notFiltered");
        notFilteredCopyingDirectory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                notFilteredCopyingDirectory);
        List<String> notFilteredPaths = TestUtils
                .listFilesRecursively(notFilteredCopyingDirectory);

        File filteredCopyingDirectory = new File(testDirectory, "filtered");

        filteredCopyingDirectory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                filteredCopyingDirectory, "*.xml");
        List<String> filteredPaths = TestUtils
                .listFilesRecursively(filteredCopyingDirectory);

        assertTrue(filteredPaths.size() < notFilteredPaths.size(),
                "Filtered paths' count should be less than non filtered paths' count");
        assertTrue(
                notFilteredPaths.stream()
                        .anyMatch(path -> path.endsWith(".xml")),
                "Not filtered paths should contain xml files");
        assertTrue(
                filteredPaths.stream().noneMatch(path -> path.endsWith(".xml")),
                "Paths with '*.xml' exclusion should not contain xml files");
    }

    @Test
    void copyFilesFromJar_basePathAppendedWithTrailingSlash()
            throws IOException {
        String basePath1 = "META-INF/maven";
        File basePath1Directory = new File(testDirectory, "basePath1");
        basePath1Directory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath1,
                basePath1Directory);
        List<String> basePath1Paths = TestUtils
                .listFilesRecursively(basePath1Directory);

        String basePath2 = basePath1 + '/';
        File basePath2Directory = new File(testDirectory, "basePath2");
        basePath2Directory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath2,
                basePath2Directory);
        List<String> basePath2Paths = TestUtils
                .listFilesRecursively(basePath2Directory);

        assertEquals(basePath1Paths, basePath2Paths,
                "Base path without trailing slash should be treated the same as base path with one");
    }

    @Test
    void copyFilesFromJar_copiedFromBasePathResultsAreContainedInAllPaths()
            throws IOException {
        File allFilesDirectory = new File(testDirectory, "all");
        allFilesDirectory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                allFilesDirectory);
        List<String> allPaths = TestUtils
                .listFilesRecursively(allFilesDirectory);

        String basePath = "/META-INF/maven";
        File filteredFilesDirectory = new File(testDirectory, "filtered");
        filteredFilesDirectory.mkdirs();
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                filteredFilesDirectory);
        List<String> filteredPaths = TestUtils
                .listFilesRecursively(filteredFilesDirectory);
        List<String> filteredPathsPrefixedByBasePath = filteredPaths.stream()
                .map(path -> basePath + path).collect(Collectors.toList());

        assertTrue(filteredPaths.size() < allPaths.size(),
                "Filtered paths' count should be less than all paths' count");
        assertTrue(allPaths.containsAll(filteredPathsPrefixedByBasePath),
                "base path + filtered path should be contained in all paths");
    }

    @Test
    void copyFilesFromJar_casePreserved() {
        File outputDirectory = testDirectory;
        String jarDirectory = "META-INF/resources/webjars/paper-button/2.0.0/.github/";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        List<String> originalFiles = listFilesInJar(testJar, jarDirectory);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        Set<String> copiedFiles = new HashSet<>(
                TestUtils.listFilesRecursively(outputDirectory));

        assertEquals(originalFiles.size(), copiedFiles.size(), String.format(
                "Number of files in jar '%s' in jar directory '%s' and number of copied files should match.",
                testJar, jarDirectory));

        copiedFiles.forEach(copiedFile -> assertTrue(
                originalFiles.stream()
                        .anyMatch(file -> file.endsWith(copiedFile)),
                String.format(
                        "Failed to find copied file '%s' in files '%s' from jar '%s'",
                        copiedFile, originalFiles, testJar)));
    }

    @Test
    void copyFilesFromJar_doNotUpdateFileIfContentIsTheSame() {
        File outputDirectory = testDirectory;
        String jarDirectory = "META-INF/resources/webjars/paper-button";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        File jsonFile = copyFilesFromJar(outputDirectory, jarDirectory,
                testJar);

        long timestamp = jsonFile.lastModified();

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        // The file is unmodified
        assertEquals(timestamp, jsonFile.lastModified());
    }

    @Test
    void copyFilesFromJar_updateFileIfContentIsNotTheSame() throws IOException {
        File outputDirectory = testDirectory;
        String jarDirectory = "META-INF/resources/webjars/paper-button";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        File jsonFile = copyFilesFromJar(outputDirectory, jarDirectory,
                testJar);

        String originalContent = FileUtils
                .readLines(jsonFile, StandardCharsets.UTF_8).stream()
                .collect(Collectors.joining(""));

        String content = "{}";
        FileUtils.write(jsonFile, content, StandardCharsets.UTF_8);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        assertNotEquals(content,
                FileUtils.readLines(jsonFile, StandardCharsets.UTF_8).stream()
                        .collect(Collectors.joining("")));
        assertEquals(originalContent,
                FileUtils.readLines(jsonFile, StandardCharsets.UTF_8).stream()
                        .collect(Collectors.joining("")));
    }

    private File copyFilesFromJar(File outputDirectory, String jarDirectory,
            File testJar) {
        List<String> originalFiles = listFilesInJar(testJar, jarDirectory);

        Optional<String> json = originalFiles.stream()
                .filter(fileName -> fileName.endsWith(".json")).findFirst();

        // self check
        assert json.isPresent();

        String jsonPath = json.get();
        jsonPath = jsonPath.substring(jarDirectory.length() + 1);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        File jsonFile = new File(outputDirectory, jsonPath);

        // self check
        assert jsonFile.exists();
        return jsonFile;
    }

    private List<String> listFilesInJar(File jar, String jarDirectory) {
        try (JarFile jarFile = new JarFile(jar, false)) {
            return jarFile.stream().filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().startsWith(jarDirectory))
                    .map(ZipEntry::getName).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Failed to list files in jarFile '%s'", jar),
                    e);
        }
    }
}
