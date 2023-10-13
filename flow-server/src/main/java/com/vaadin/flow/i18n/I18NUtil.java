/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.i18n;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FILENAME;

/**
 * Utility class for use with determining default i18n property files and
 * locales.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class I18NUtil {

    public static final String PROPERTIES_SUFFIX = ".properties";

    /**
     * Check if we have a default translation properties file
     * {@link DefaultI18NProvider#BUNDLE_FILENAME} in the folder
     * {@link DefaultI18NProvider#BUNDLE_FOLDER}
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return {@code true} if default property file found
     */
    public static boolean containsDefaultTranslation() {
        URL resource = getClassLoader()
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER + "/"
                        + DefaultI18NProvider.BUNDLE_FILENAME
                        + PROPERTIES_SUFFIX);
        if (resource == null) {
            return false;
        }
        return true;
    }

    /**
     * Check that we have the translation folder
     * {@link DefaultI18NProvider#BUNDLE_FOLDER} and collect all translation
     * properties files. Parse names to get locales.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return List of locales parsed from property files.
     */
    public static List<Locale> getDefaultTranslationLocales() {
        List<Locale> locales = new ArrayList<>();

        URL resource = getClassLoader()
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER);
        if (resource == null) {
            return locales;
        }

        List<File> listedFiles = getTranslationFiles(resource).stream()
                .filter(file -> file.getName()
                        .startsWith(DefaultI18NProvider.BUNDLE_FILENAME)
                        && file.getName().endsWith(PROPERTIES_SUFFIX))
                .collect(Collectors.toList());
        for (File file : listedFiles) {
            String name = file.getName();

            if (!name.contains("_")) {
                // This is the default bundle and that doesn't have a
                // locale
                continue;
            }
            String langCode = name.substring(BUNDLE_FILENAME.length() + 1,
                    name.lastIndexOf('.'));
            String[] langParts = langCode.split("_");
            if (langParts.length == 1) {
                locales.add(new Locale(langParts[0]));
            } else if (langParts.length == 2) {
                locales.add(new Locale(langParts[0], langParts[1]));
            } else if (langParts.length == 3) {
                locales.add(
                        new Locale(langParts[0], langParts[1], langParts[2]));
            }
        }
        return locales;
    }

    protected static List<File> getTranslationFiles(URL resource) {
        List<File> files = new ArrayList<>();

        File bundleFolder = new File(resource.getFile());

        if ("jar".equals(resource.getProtocol())) {
            // Get the file path in jar
            final String pathInJar = resource.getPath()
                    .substring(resource.getPath().indexOf('!') + 1);
            FileSystemPair fileSystemPair = null;
            try {
                fileSystemPair = getNewOrExistingFileSystem(resource.toURI());
                // Get the file path inside the jar.
                final Path dirPath = fileSystemPair.fileSystem
                        .getPath(pathInJar);

                if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                    try (Stream<Path> jarFiles = Files.list(dirPath)) {
                        jarFiles.filter(path -> !Files.isDirectory(path))
                                .map(Path::toString).map(File::new)
                                .forEach(files::add);
                    }
                }
            } catch (IOException | URISyntaxException e) {
                getLogger().debug("failed to read jar file contents", e);
            } finally {
                if (fileSystemPair != null && fileSystemPair.external) {
                    try {
                        fileSystemPair.fileSystem.close();
                    } catch (IOException e) {
                        getLogger().debug("Failed to close FileSystem", e);
                    }
                }
            }
        } else if (bundleFolder.exists() && bundleFolder.isDirectory()) {
            Arrays.stream(bundleFolder.listFiles()).filter(File::isFile)
                    .forEach(files::add);
        }

        return files;
    }

    protected static FileSystemPair getNewOrExistingFileSystem(URI resourceURI)
            throws IOException {
        try {
            return new FileSystemPair(FileSystems.newFileSystem(resourceURI,
                    Collections.emptyMap()), false);
        } catch (FileSystemAlreadyExistsException fsaee) {
            getLogger().trace(
                    "Tried to get new filesystem, but it already existed for target uri.",
                    fsaee);
            FileSystem fileSystem = FileSystems.getFileSystem(resourceURI);

            return new FileSystemPair(fileSystem, true);
        }
    }

    private record FileSystemPair(FileSystem fileSystem, boolean external) {
    }

    protected static ClassLoader getClassLoader() {
        return I18NUtil.class.getClassLoader();
    }

    protected static Logger getLogger() {
        return LoggerFactory.getLogger(I18NUtil.class);
    }
}
