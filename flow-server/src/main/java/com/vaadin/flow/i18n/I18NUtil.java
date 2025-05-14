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

package com.vaadin.flow.i18n;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FILENAME;
import static com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FOLDER;

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
    public static boolean containsDefaultTranslation(ClassLoader classLoader) {
        URL resource = classLoader.getResource(DefaultI18NProvider.BUNDLE_FOLDER
                + "/" + DefaultI18NProvider.BUNDLE_FILENAME
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
    public static List<Locale> getDefaultTranslationLocales(
            ClassLoader classLoader) {
        List<Locale> locales = new ArrayList<>();

        URL resource = classLoader
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER);
        if (resource == null) {
            return locales;
        }

        List<File> listedFiles = getTranslationFiles(resource).stream()
                .filter(file -> file.getName()
                        .startsWith(DefaultI18NProvider.BUNDLE_FILENAME)
                        && file.getName().endsWith(PROPERTIES_SUFFIX))
                .collect(Collectors.toList());
        return collectLocalesFromFiles(listedFiles);
    }

    /**
     * Get list of locales collected from the given list of translation file
     * names.
     *
     * @param fileNames
     *            List of file names
     * @return List of locales
     */
    public static List<Locale> collectLocalesFromFileNames(
            List<String> fileNames) {
        List<Locale> locales = new ArrayList<>();
        for (String name : fileNames) {

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

    private static List<Locale> collectLocalesFromFiles(List<File> files) {
        return collectLocalesFromFileNames(
                files.stream().map(File::getName).collect(Collectors.toList()));
    }

    protected static List<File> getTranslationFiles(URL resource) {
        List<File> files = new ArrayList<>();

        File bundleFolder = new File(resource.getFile());

        if ("jar".equals(resource.getProtocol()) ||
        // wsjar check is for OpenLiberty
                "wsjar".equals(resource.getProtocol())) {
            String file = resource.getFile().substring("file:".length(),
                    resource.getFile().indexOf('!'));
            try {
                Enumeration<JarEntry> entries = new JarFile(file).entries();
                entries.asIterator().forEachRemaining(entry -> {
                    String fileName = entry.getName();
                    if (fileName.contains(BUNDLE_FOLDER)
                            && fileName.endsWith(PROPERTIES_SUFFIX)) {
                        files.add(new File(fileName));
                    }
                });
            } catch (IOException ioe) {
                getLogger().debug(
                        "failed to read jar file '" + file + "' contents", ioe);
            }
        } else if ("vfs".equals(resource.getProtocol())) {
            files.addAll(listJBossVfsDirectory(resource));
        } else if (bundleFolder.exists() && bundleFolder.isDirectory()) {
            Arrays.stream(bundleFolder.listFiles()).filter(File::isFile)
                    .forEach(files::add);
        }
        return files;
    }

    // Borrowed from DevModeInitializer
    private static List<File> listJBossVfsDirectory(URL url) {
        List<File> files = new ArrayList<>();
        try {
            Object virtualFile = url.openConnection().getContent();
            Class virtualFileClass = virtualFile.getClass();

            // Reflection as we cannot afford a dependency to
            // WildFly or JBoss
            Method getChildren = virtualFileClass.getMethod("getChildren");
            Method getPhysicalFileMethod = virtualFileClass
                    .getMethod("getPhysicalFile");

            List virtualFiles = (List) getChildren.invoke(virtualFile);
            for (Object child : virtualFiles) {
                // side effect: create real-world files
                files.add((File) getPhysicalFileMethod.invoke(child));
            }
        } catch (Exception exc) {
            getLogger().debug(
                    "Failed to list entries in JBoss VFS directory {}", url,
                    exc);
        }
        return files;
    }

    protected static Logger getLogger() {
        return LoggerFactory.getLogger(I18NUtil.class);
    }
}
