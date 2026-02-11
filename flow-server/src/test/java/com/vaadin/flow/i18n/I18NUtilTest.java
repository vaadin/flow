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
package com.vaadin.flow.i18n;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18NUtilTest {
    @TempDir
    Path temporaryFolder;
    private File resources;

    @Mock
    private ClassLoader mockLoader;

    @BeforeEach
    public void init() throws IOException {
        MockitoAnnotations.openMocks(this);
        resources = Files.createTempDirectory(temporaryFolder, "temp").toFile();
    }

    @Test
    public void foundResourceFolder_returnsExpectedLocales()
            throws IOException {
        Mockito.when(mockLoader.getResource(DefaultI18NProvider.BUNDLE_FOLDER))
                .thenReturn(resources.toURI().toURL());

        File file = new File(resources,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        file = new File(resources,
                DefaultI18NProvider.BUNDLE_FILENAME + "_en_GB.properties");
        Files.writeString(file.toPath(), "title=English",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        file = new File(resources,
                DefaultI18NProvider.BUNDLE_FILENAME + "_fi_FI.properties");
        Files.writeString(file.toPath(), "title=Suomi", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        file = new File(resources,
                DefaultI18NProvider.BUNDLE_FILENAME + "_de.properties");
        Files.writeString(file.toPath(), "title=deutsch",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(mockLoader);
        assertEquals(3, defaultTranslationLocales.size());

        assertTrue(
                defaultTranslationLocales.contains(new Locale("de")),
                "Missing German bundle");
        assertTrue(
                defaultTranslationLocales.contains(new Locale("en", "GB")),
                "Missing English bundle");
        assertTrue(
                defaultTranslationLocales.contains(new Locale("fi", "FI")),
                "Missing Finnish bundle");
    }

    @Test
    public void noTranslationFiles_returnsEmptyList() throws IOException {
        Mockito.when(mockLoader.getResource(DefaultI18NProvider.BUNDLE_FOLDER))
                .thenReturn(resources.toURI().toURL());

        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(mockLoader);
        assertTrue(defaultTranslationLocales.isEmpty(),
                "Nothing should be returned for empty folder");
    }

    @Test
    public void onlyDefaultTranslationFile_returnsEmptyList()
            throws IOException {
        Mockito.when(mockLoader.getResource(DefaultI18NProvider.BUNDLE_FOLDER))
                .thenReturn(resources.toURI().toURL());

        File file = new File(resources,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(mockLoader);
        assertTrue(defaultTranslationLocales.isEmpty(),
                "Nothing should be returned for empty folder");
    }

    @Test
    public void onlyDefaultTranslationFile_returnsTrueForDefault()
            throws IOException {
        File translations = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });

        File file = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        assertTrue(
                I18NUtil.containsDefaultTranslation(urlClassLoader),
                "Default file should return true");
    }

    @Test
    public void noTranslationFilesInExistingFolder_returnsFalseForDefault()
            throws IOException {
        File translations = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });

        assertFalse(
                I18NUtil.containsDefaultTranslation(urlClassLoader),
                "Nothing should be returned for empty folder");
    }

    @Test
    public void translationFilesInJar_returnsTrueForDefault_findsLanguages()
            throws IOException {
        Path path = generateZipArchive(temporaryFolder);

        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { path.toUri().toURL() });

        assertTrue(
                I18NUtil.containsDefaultTranslation(urlClassLoader),
                "Default file should return true");
        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(urlClassLoader);
        assertEquals(2, defaultTranslationLocales.size(),
                "Translation files with locale inside JAR should be resolved");

        assertTrue(
                defaultTranslationLocales.contains(new Locale("fi", "FI")),
                "Finnish locale translation should have been found");
        assertTrue(
                defaultTranslationLocales.contains(new Locale("ja", "JP")),
                "Japan locale translation should have been found");
    }

    // Open Liberty may use 'wsjar' as protocol of JAR resources
    // https://openliberty.io/docs/latest/reference/config/classloading.html
    @Test
    public void openliberty_translationFilesInJar_returnsTrueForDefault_findsLanguages()
            throws IOException {
        Path path = generateZipArchive(temporaryFolder);

        URLStreamHandler wsjarMockHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                url = new URL("jar", url.getPath(), url.getFile());
                return url.openConnection();
            }
        };
        URLStreamHandlerFactory wsjarMockHandlerFactory = protocol -> {
            if ("wsjar".equals(protocol)) {
                return wsjarMockHandler;
            }
            return null;
        };
        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { path.toUri().toURL() },
                ClassLoader.getSystemClassLoader(), wsjarMockHandlerFactory) {
            @Override
            public URL getResource(String name) {
                URL url = super.getResource(name);
                if (url != null && url.getProtocol().equals("jar")) {
                    try {
                        return new URL("wsjar", url.getHost(), url.getPort(),
                                url.getFile(), wsjarMockHandler);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                return url;
            }
        };

        assertTrue(
                I18NUtil.containsDefaultTranslation(urlClassLoader),
                "Default file should return true");
        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(urlClassLoader);
        assertEquals(2, defaultTranslationLocales.size(),
                "Translation files with locale inside JAR should be resolved");

        assertTrue(
                defaultTranslationLocales.contains(new Locale("fi", "FI")),
                "Finnish locale translation should have been found");
        assertTrue(
                defaultTranslationLocales.contains(new Locale("ja", "JP")),
                "Japan locale translation should have been found");
    }

    public static class MockVirtualFile {

        private final JarEntry entry;
        private final JarFile jarFile;

        private MockVirtualFile(JarFile jarFile, JarEntry entry) {
            this.jarFile = jarFile;
            this.entry = entry;
        }

        public List<MockVirtualFile> getChildren() {
            return jarFile.stream().filter(e -> !e.getName()
                    .equals(entry.getName())
                    && e.getName().startsWith(entry.getName())
                    && (e.getName().endsWith("/") || !e.getName()
                            .substring(entry.getName().length()).contains("/")))
                    .map(e -> new MockVirtualFile(jarFile, e)).toList();
        }

        public File getPhysicalFile() {
            return new File(entry.getName());
        }
    }

    // vfs:/content/my.war/WEB-INF/classes/vaadin-i18n/
    @Test
    public void jbossVfs_translationFilesInJar_returnsTrueForDefault_findsLanguages()
            throws IOException {
        Path path = generateZipArchive(temporaryFolder);
        JarFile jarFile = new JarFile(path.toFile());
        URLConnection urlConnection = Mockito.mock(URLConnection.class);
        Mockito.when(urlConnection.getContent()).thenReturn(new MockVirtualFile(
                jarFile, jarFile.getJarEntry("vaadin-i18n/")));

        URLStreamHandler vfsMockHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                if (url.getFile().endsWith("/vaadin-i18n")) {
                    return urlConnection;
                }
                return null;
            }
        };
        URLStreamHandlerFactory vfsMockHandlerFactory = protocol -> {
            if ("vfs".equals(protocol)) {
                return vfsMockHandler;
            }
            return null;
        };
        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { path.toUri().toURL() },
                ClassLoader.getSystemClassLoader(), vfsMockHandlerFactory) {
            @Override
            public URL getResource(String name) {
                URL url = super.getResource(name);
                if (url != null && url.getProtocol().equals("jar")
                        && url.getFile().contains("fake.jar!")) {
                    try {
                        return new URL("vfs", null, 0,
                                "/content/my.war/WEB-INF/lib/fake.jar"
                                        + url.getFile().replaceFirst(
                                                ".*fake.jar!", ""),
                                vfsMockHandler);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                return url;
            }
        };

        assertTrue(
                I18NUtil.containsDefaultTranslation(urlClassLoader),
                "Default file should return true");
        List<Locale> defaultTranslationLocales = I18NUtil
                .getDefaultTranslationLocales(urlClassLoader);
        assertEquals(2, defaultTranslationLocales.size(),
                "Translation files with locale inside JAR should be resolved");

        assertTrue(
                defaultTranslationLocales.contains(new Locale("fi", "FI")),
                "Finnish locale translation should have been found");
        assertTrue(
                defaultTranslationLocales.contains(new Locale("ja", "JP")),
                "Japan locale translation should have been found");
    }

    private Path generateZipArchive(Path folder) throws IOException {
        File archiveFile = folder.resolve("fake.jar").toFile();
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempArchive))) {
            // Create a directory to the zip
            zipOutputStream.putNextEntry(
                    new ZipEntry(DefaultI18NProvider.BUNDLE_FOLDER + "/"));
            zipOutputStream.closeEntry();
            zipOutputStream
                    .putNextEntry(new ZipEntry(DefaultI18NProvider.BUNDLE_FOLDER
                            + "/translations.properties"));
            zipOutputStream.closeEntry();
            zipOutputStream
                    .putNextEntry(new ZipEntry(DefaultI18NProvider.BUNDLE_FOLDER
                            + "/translations_fi_FI.properties"));
            zipOutputStream.closeEntry();
            zipOutputStream
                    .putNextEntry(new ZipEntry(DefaultI18NProvider.BUNDLE_FOLDER
                            + "/translations_ja_JP.properties"));
            zipOutputStream.closeEntry();
        }
        return tempArchive;
    }
}
