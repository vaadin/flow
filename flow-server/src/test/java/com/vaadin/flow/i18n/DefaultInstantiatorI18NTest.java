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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.ResourceBundle;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.I18NProviderTest;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class DefaultInstantiatorI18NTest {
    @TempDir
    Path temporaryFolder;
    private File translations;
    private ClassLoader urlClassLoader;

    @BeforeEach
    public void init()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        File resources = Files.createTempDirectory(temporaryFolder, "temp")
                .toFile();

        translations = new File(resources, DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });
        I18NProviderTest.clearI18NProviderField();
    }

    @AfterEach
    public void cleanup() throws NoSuchFieldException, IllegalAccessException {
        ResourceBundle.clearCache(urlClassLoader);
        I18NProviderTest.clearI18NProviderField();
        VaadinService.setCurrent(null);
    }

    @Test
    public void translationFileOnClasspath_instantiateDefaultI18N()
            throws IOException {

        createTranslationFiles(translations);

        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected ClassLoader getClassLoader() {
                return urlClassLoader;
            }
        };
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);
        I18NProvider i18NProvider = defaultInstantiator.getI18NProvider();
        assertNotNull(i18NProvider);
        assertTrue(i18NProvider instanceof DefaultI18NProvider);

        assertEquals("Suomi",
                i18NProvider.getTranslation("title", new Locale("fi", "FI")));
        assertEquals("Suomi",
                I18NProvider.translate(new Locale("fi", "FI"), "title"));

        assertEquals("deutsch",
                i18NProvider.getTranslation("title", new Locale("de")));
        assertEquals("deutsch",
                I18NProvider.translate(new Locale("de"), "title"));

        assertEquals("deutsch",
                i18NProvider.getTranslation("title", new Locale("de", "AT")),
                "non existing country should select language bundle");
        assertEquals("deutsch",
                I18NProvider.translate(new Locale("de", "AT"), "title"),
                "non existing country should select language bundle");

        assertEquals("Korean",
                i18NProvider.getTranslation("title", new Locale("ko", "KR")));
        assertEquals("Korean",
                I18NProvider.translate(new Locale("ko", "KR"), "title"));

        // Note!
        // default translations.properties will be used if
        // the locale AND system default locale is not found
        assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        assertEquals("Default lang",
                I18NProvider.translate(new Locale("en", "GB"), "title"));
    }

    @Test
    public void onlyDefaultTranslation_instantiateDefaultI18N()
            throws IOException {
        File file = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(file.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected ClassLoader getClassLoader() {
                return urlClassLoader;
            }
        };
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);
        I18NProvider i18NProvider = defaultInstantiator.getI18NProvider();
        assertNotNull(i18NProvider);
        assertTrue(i18NProvider instanceof DefaultI18NProvider);

        assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("fi", "FI")));
        assertEquals("Default lang",
                I18NProvider.translate(new Locale("fi", "FI"), "title"));

        assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("de")));
        assertEquals("Default lang",
                I18NProvider.translate(new Locale("de"), "title"));

        assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("ko", "KR")));
        assertEquals("Default lang",
                I18NProvider.translate(new Locale("ko", "KR"), "title"));

        // Note!
        // default translations.properties will be used if
        // the locale AND system default locale is not found
        assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        assertEquals("Default lang",
                I18NProvider.translate(new Locale("en", "GB"), "title"));
    }

    @Test
    public void onlyLangTransalation_nonExistingLangReturnsKey()
            throws IOException {
        File file = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + "_ja.properties");
        Files.writeString(file.toPath(), "title=No Default",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected ClassLoader getClassLoader() {
                return urlClassLoader;
            }
        };
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);
        I18NProvider i18NProvider = defaultInstantiator.getI18NProvider();
        assertNotNull(i18NProvider);
        assertTrue(i18NProvider instanceof DefaultI18NProvider);

        assertEquals("No Default",
                i18NProvider.getTranslation("title", new Locale("ja")));
        assertEquals("No Default",
                I18NProvider.translate(new Locale("ja"), "title"));

        assertEquals("title",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        assertEquals("title",
                I18NProvider.translate(new Locale("en", "GB"), "title"));
    }

    @Test
    public void translate_withoutProvider_returnsKey() {
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service);
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);

        assertEquals("!{foo.bar}!",
                I18NProvider.translate("foo.bar"),
                "Should return the key with !{}! to show no translation available");
    }

    @Test
    public void translationFilesOnClassPath_getI18NProvider_usesThreadContextClassLoader()
            throws IOException {
        createTranslationFiles(translations);

        VaadinService service = Mockito.mock(VaadinService.class);
        mockLookup(service);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service);
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);

        ClassLoader threadContextClassLoader = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            try (MockedConstruction<DefaultI18NProvider> mockedConstruction = Mockito
                    .mockConstruction(DefaultI18NProvider.class,
                            (mock, context) -> {
                                ClassLoader classLoaderArgument = (ClassLoader) context
                                        .arguments().get(1);
                                assertEquals(urlClassLoader,
                                        classLoaderArgument);
                            })) {
                I18NProvider i18NProvider = defaultInstantiator
                        .getI18NProvider();

                assertNotNull(i18NProvider);
                assertEquals(i18NProvider,
                        mockedConstruction.constructed().get(0));
            }
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(threadContextClassLoader);
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
