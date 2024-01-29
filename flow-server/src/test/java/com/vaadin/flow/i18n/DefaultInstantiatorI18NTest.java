/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.util.Locale;
import java.util.ResourceBundle;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.I18NProviderTest;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

@NotThreadSafe
public class DefaultInstantiatorI18NTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File translations;
    private ClassLoader urlClassLoader;

    @Before
    public void init()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        File resources = temporaryFolder.newFolder();

        translations = new File(resources, DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });
        I18NProviderTest.clearI18NProviderField();
    }

    @After
    public void cleanup() throws NoSuchFieldException, IllegalAccessException {
        ResourceBundle.clearCache(urlClassLoader);
        I18NProviderTest.clearI18NProviderField();
    }

    @Test
    public void translationFileOnClasspath_instantiateDefaultI18N()
            throws IOException {

        createTranslationFiles(translations);

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> I18NUtil.getClassLoader())
                    .thenReturn(urlClassLoader);

            VaadinService service = Mockito.mock(VaadinService.class);
            mockLookup(service);

            I18NProvider i18NProvider = new DefaultInstantiator(service)
                    .getI18NProvider();
            Assert.assertNotNull(i18NProvider);
            Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

            Assert.assertEquals("Suomi", i18NProvider.getTranslation("title",
                    new Locale("fi", "FI")));

            Assert.assertEquals("deutsch",
                    i18NProvider.getTranslation("title", new Locale("de")));

            Assert.assertEquals(
                    "non existing country should select language bundle",
                    "deutsch", i18NProvider.getTranslation("title",
                            new Locale("de", "AT")));

            Assert.assertEquals("Korean", i18NProvider.getTranslation("title",
                    new Locale("ko", "KR")));

            // Note!
            // default translations.properties will be used if
            // the locale AND system default locale is not found
            Assert.assertEquals("Default lang", i18NProvider
                    .getTranslation("title", new Locale("en", "GB")));
        }
    }

    @Test
    public void onlyDefaultTranslation_instantiateDefaultI18N()
            throws IOException {
        File file = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> I18NUtil.getClassLoader())
                    .thenReturn(urlClassLoader);

            VaadinService service = Mockito.mock(VaadinService.class);
            mockLookup(service);

            I18NProvider i18NProvider = new DefaultInstantiator(service)
                    .getI18NProvider();
            Assert.assertNotNull(i18NProvider);
            Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

            Assert.assertEquals("Default lang", i18NProvider
                    .getTranslation("title", new Locale("fi", "FI")));

            Assert.assertEquals("Default lang",
                    i18NProvider.getTranslation("title", new Locale("de")));

            Assert.assertEquals("Default lang", i18NProvider
                    .getTranslation("title", new Locale("ko", "KR")));

            // Note!
            // default translations.properties will be used if
            // the locale AND system default locale is not found
            Assert.assertEquals("Default lang", i18NProvider
                    .getTranslation("title", new Locale("en", "GB")));
        }
    }

    @Test
    public void onlyLangTransalation_nonExistingLangReturnsKey()
            throws IOException {
        File file = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + "_ja.properties");
        Files.writeString(file.toPath(), "title=No Default",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> I18NUtil.getClassLoader())
                    .thenReturn(urlClassLoader);

            VaadinService service = Mockito.mock(VaadinService.class);
            mockLookup(service);

            I18NProvider i18NProvider = new DefaultInstantiator(service)
                    .getI18NProvider();
            Assert.assertNotNull(i18NProvider);
            Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

            Assert.assertEquals("No Default",
                    i18NProvider.getTranslation("title", new Locale("ja")));

            Assert.assertEquals("title", i18NProvider.getTranslation("title",
                    new Locale("en", "GB")));
        }
    }

    private static void createTranslationFiles(File translationsFolder)
            throws IOException {
        File file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + "_ko_KR.properties");
        Files.writeString(file.toPath(), "title=Korean", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + "_fi_FI.properties");
        Files.writeString(file.toPath(), "title=Suomi", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + "_de.properties");
        Files.writeString(file.toPath(), "title=deutsch",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    private Lookup mockLookup(VaadinService service) {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        return lookup;
    }
}
