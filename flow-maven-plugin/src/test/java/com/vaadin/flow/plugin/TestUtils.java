/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.plugin;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared code to use in the unit tests.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public final class TestUtils {

    public static final String SERVER_JAR = "annotation-extractor-test/flow-server-1.5-SNAPSHOT.jar";

    public static final String DATA_JAR = "annotation-extractor-test/flow-data-1.5-SNAPSHOT.jar";

    private TestUtils() {
    }

    /**
     * An easy way to get a test jar. Fails if the file was not found.
     *
     * @return test jar file
     */
    public static File getTestJar() {
        return getTestJar("paper-button-2.0.0.jar");
    }

    /**
     * Gets a test jar file by its resource name using
     * {@link TestUtils#getTestResource(String)}.
     *
     * @param jarName
     *            the resource name of a jar file
     * @return corresponding test jar file
     */
    public static File getTestJar(String jarName) {
        return new File(getTestResource(jarName).getFile());
    }

    /**
     * Gets a test resouce by its name using using
     * {@link ClassLoader#getResource(String)}. Fails if the file was not found
     * ({@code null}).
     *
     * @param resourceName
     *            the resource name
     * @return corresponding resource url
     */
    public static URL getTestResource(String resourceName) {
        URL resourceUrl = TestUtils.class.getClassLoader()
                .getResource(resourceName);
        assertNotNull(String.format(
                "Expect the test resource to be present in test resource folder with name = '%s'",
                resourceName), resourceUrl);
        return resourceUrl;
    }

    /**
     * Lists all file (not directories) paths in directory specified. Fails if
     * directory specified does not exist or is not a directory.
     *
     * @param directory
     *            directory to list files in
     * @return list of paths, relative to the directory specified
     */
    public static List<String> listFilesRecursively(File directory) {
        assert directory != null && directory
                .isDirectory() : "This method expects valid directory as input, but got: "
                        + directory;

        try {
            return Files.walk(directory.toPath())
                    .filter(file -> Files.isRegularFile(file))
                    .map(Path::toString)
                    .map(path -> path.replace(directory.getAbsolutePath(), ""))
                    .map(path -> path.startsWith(File.separator)
                            ? path.substring(1)
                            : path)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AssertionError(String.format(
                    "Unexpected: could not list files in directory '%s'",
                    directory), e);
        }
    }
}
