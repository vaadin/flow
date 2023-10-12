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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class I18NUtilTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File resources;

    @Mock
    private ClassLoader mockLoader;

    @Before
    public void init() throws IOException {
        resources = temporaryFolder.newFolder();
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

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class)) {
            util.when(() -> I18NUtil.getClassLoader()).thenReturn(mockLoader);
            util.when(() -> I18NUtil.getDefaultTranslationLocales())
                    .thenCallRealMethod();

            List<Locale> defaultTranslationLocales = I18NUtil
                    .getDefaultTranslationLocales();
            Assert.assertEquals(3, defaultTranslationLocales.size());

            Assert.assertTrue("Missing German bundle",
                    defaultTranslationLocales.contains(new Locale("de")));
            Assert.assertTrue("Missing English bundle",
                    defaultTranslationLocales.contains(new Locale("en", "GB")));
            Assert.assertTrue("Missing Finnish bundle",
                    defaultTranslationLocales.contains(new Locale("fi", "FI")));
        }
    }

    @Test
    public void noTranslationFiles_returnsEmptyList() throws IOException {
        Mockito.when(mockLoader.getResource(DefaultI18NProvider.BUNDLE_FOLDER))
                .thenReturn(resources.toURI().toURL());

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class)) {
            util.when(() -> I18NUtil.getClassLoader()).thenReturn(mockLoader);
            util.when(() -> I18NUtil.getDefaultTranslationLocales())
                    .thenCallRealMethod();

            List<Locale> defaultTranslationLocales = I18NUtil
                    .getDefaultTranslationLocales();
            Assert.assertTrue("Nothing should be returned for empty folder",
                    defaultTranslationLocales.isEmpty());
        }
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

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class)) {
            util.when(() -> I18NUtil.getClassLoader()).thenReturn(mockLoader);
            util.when(() -> I18NUtil.getDefaultTranslationLocales())
                    .thenCallRealMethod();

            List<Locale> defaultTranslationLocales = I18NUtil
                    .getDefaultTranslationLocales();
            Assert.assertTrue("Nothing should be returned for empty folder",
                    defaultTranslationLocales.isEmpty());
        }
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

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class)) {
            util.when(() -> I18NUtil.getClassLoader())
                    .thenReturn(urlClassLoader);
            util.when(() -> I18NUtil.getDefaultTranslationLocales())
                    .thenCallRealMethod();
            util.when(() -> I18NUtil.containsDefaultTranslation())
                    .thenCallRealMethod();

            Assert.assertTrue("Default file should return true",
                    I18NUtil.containsDefaultTranslation());
        }
    }

    @Test
    public void noTranslationFilesInExistingFolder_returnsFalseForDefault()
            throws IOException {
        File translations = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        ClassLoader urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class)) {
            util.when(() -> I18NUtil.getClassLoader())
                    .thenReturn(urlClassLoader);
            util.when(() -> I18NUtil.getDefaultTranslationLocales())
                    .thenCallRealMethod();
            util.when(() -> I18NUtil.containsDefaultTranslation())
                    .thenCallRealMethod();

            Assert.assertFalse("Nothing should be returned for empty folder",
                    I18NUtil.containsDefaultTranslation());
        }
    }
}