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
import org.mockito.Mock;
import org.mockito.MockedConstruction;
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
        Assert.assertNotNull(i18NProvider);
        Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

        Assert.assertEquals("Suomi",
                i18NProvider.getTranslation("title", new Locale("fi", "FI")));
        Assert.assertEquals("Suomi",
                I18NProvider.translate(new Locale("fi", "FI"), "title"));

        Assert.assertEquals("deutsch",
                i18NProvider.getTranslation("title", new Locale("de")));
        Assert.assertEquals("deutsch",
                I18NProvider.translate(new Locale("de"), "title"));

        Assert.assertEquals(
                "non existing country should select language bundle", "deutsch",
                i18NProvider.getTranslation("title", new Locale("de", "AT")));
        Assert.assertEquals(
                "non existing country should select language bundle", "deutsch",
                I18NProvider.translate(new Locale("de", "AT"), "title"));

        Assert.assertEquals("Korean",
                i18NProvider.getTranslation("title", new Locale("ko", "KR")));
        Assert.assertEquals("Korean",
                I18NProvider.translate(new Locale("ko", "KR"), "title"));

        // Note!
        // default translations.properties will be used if
        // the locale AND system default locale is not found
        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        Assert.assertEquals("Default lang",
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
        Assert.assertNotNull(i18NProvider);
        Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("fi", "FI")));
        Assert.assertEquals("Default lang",
                I18NProvider.translate(new Locale("fi", "FI"), "title"));

        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("de")));
        Assert.assertEquals("Default lang",
                I18NProvider.translate(new Locale("de"), "title"));

        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("ko", "KR")));
        Assert.assertEquals("Default lang",
                I18NProvider.translate(new Locale("ko", "KR"), "title"));

        // Note!
        // default translations.properties will be used if
        // the locale AND system default locale is not found
        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        Assert.assertEquals("Default lang",
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
        Assert.assertNotNull(i18NProvider);
        Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);

        Assert.assertEquals("No Default",
                i18NProvider.getTranslation("title", new Locale("ja")));
        Assert.assertEquals("No Default",
                I18NProvider.translate(new Locale("ja"), "title"));

        Assert.assertEquals("title",
                i18NProvider.getTranslation("title", new Locale("en", "GB")));
        Assert.assertEquals("title",
                I18NProvider.translate(new Locale("en", "GB"), "title"));
    }

    @Test
    public void translate_withoutProvider_returnsKey() {
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);

        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service);
        Mockito.when(service.getInstantiator()).thenReturn(defaultInstantiator);

        Assert.assertEquals(
                "Should return the key with !{}! to show no translation available",
                "!{foo.bar}!", I18NProvider.translate("foo.bar"));
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
                                Assert.assertEquals(urlClassLoader,
                                        classLoaderArgument);
                            })) {
                I18NProvider i18NProvider = defaultInstantiator
                        .getI18NProvider();

                Assert.assertNotNull(i18NProvider);
                Assert.assertEquals(i18NProvider,
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
