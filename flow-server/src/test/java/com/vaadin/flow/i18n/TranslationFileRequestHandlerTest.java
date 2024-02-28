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

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import net.jcip.annotations.NotThreadSafe;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.ResourceBundle;

@NotThreadSafe
public class TranslationFileRequestHandlerTest {

    private final VaadinSession session = Mockito.mock(VaadinSession.class);

    private final VaadinRequest request = Mockito.mock(VaadinRequest.class);

    private final VaadinResponse response = Mockito.mock(VaadinResponse.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassLoader urlClassLoader;

    private TranslationFileRequestHandler handler;

    private StringWriter out;

    private PrintWriter writer;

    private File translationsFolder;

    private ArgumentCaptor<String> retrievedLocaleCapture;

    @Before
    public void init()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        init(false);
    }

    private void init(boolean useCustomI18nProvider) throws IOException {
        Class<? extends I18NProvider> i18NProviderClass = useCustomI18nProvider
                ? I18NProvider.class
                : DefaultI18NProvider.class;
        I18NProvider i18NProvider = Mockito.mock(i18NProviderClass,
                Mockito.CALLS_REAL_METHODS);
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getI18NProvider()).thenReturn(i18NProvider);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);
        Mockito.when(session.getService()).thenReturn(service);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        File resources = temporaryFolder.newFolder();
        translationsFolder = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translationsFolder.mkdirs();
        urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });
        handler = new TranslationFileRequestHandler(i18NProvider);
        out = new StringWriter();
        writer = new PrintWriter(out);
        Mockito.when(response.getWriter()).thenReturn(writer);
        retrievedLocaleCapture = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(response).setHeader(Mockito
                .eq(TranslationFileRequestHandler.RETRIEVED_LOCALE_HEADER_NAME),
                retrievedLocaleCapture.capture());
    }

    @After
    public void cleanup() {
        ResourceBundle.clearCache(urlClassLoader);
    }

    @Test
    public void pathDoesNotMatch_requestNotHandled() throws IOException {
        createTranslationFiles(true);
        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(I18NUtil::getClassLoader).thenReturn(urlClassLoader);
            setRequestParams(null, "other");
            Assert.assertFalse(
                    handler.handleRequest(session, request, response));
        }
    }

    @Test
    public void withRootBundle_languageTagIsNull_responseIsRootBundle()
            throws IOException {
        testResponseContent(true, null, "{\"title\":\"Root bundle lang\"}",
                "und");
    }

    @Test
    public void withoutRootBundle_languageTagIsNull_responseIsEmpty()
            throws IOException {
        testResponseContent(false, null, "", null);
    }

    @Test
    public void withRootBundle_languageTagWithoutCountryAvailable_responseIsCorrect()
            throws IOException {
        testResponseContent(true, "fi", "{\"title\":\"Suomi\"}", "fi");
    }

    @Test
    public void withRootBundle_languageTagWithoutCountryNotAvailable_responseIsRootBundle()
            throws IOException {
        testResponseContent(true, "es", "{\"title\":\"Root bundle lang\"}",
                "und");
    }

    @Test
    public void withoutRootBundle_languageTagWithoutCountryNotAvailable_responseIsEmpty()
            throws IOException {
        testResponseContent(false, "es", "", null);
    }

    @Test
    public void withoutRootBundle_languageTagWithCountryAvailable_responseIsCorrect()
            throws IOException {
        testResponseContent(false, "es-ES", "{\"title\":\"Espanol (Spain)\"}",
                "es-ES");
    }

    @Test
    public void withRootBundle_requestedLocaleBundleNotAvailable_responseIsRootBundle()
            throws IOException {
        testResponseContentWithMockedDefaultLocale("es-ES", true, "en-US",
                "{\"title\":\"Root bundle lang\"}", "und");
    }

    @Test
    public void withoutRootBundle_requestedLocaleBundleNotAvailable_responseIsEmpty()
            throws IOException {
        testResponseContentWithMockedDefaultLocale("es-ES", false, "en-US", "",
                null);
    }

    @Test
    public void withCustomI18nProvider_withRootBundle_requestedLocaleBundleAvailable_responseIsEmpty()
            throws IOException {
        init(true);
        createTranslationFiles(true);
        testResponseContent(true, "fi", "", null);
    }

    private void testResponseContentWithMockedDefaultLocale(
            String defaultLocaleLanguageTag, boolean withRootBundleFile,
            String requestedLanguageTag, String expectedResponseContent,
            String expectedResponseLanguageTag) throws IOException {
        try (MockedStatic<Locale> locale = Mockito.mockStatic(Locale.class,
                Mockito.CALLS_REAL_METHODS)) {
            Locale defaultLocale = Locale
                    .forLanguageTag(defaultLocaleLanguageTag);
            locale.when(Locale::getDefault).thenReturn(defaultLocale);
            testResponseContent(withRootBundleFile, requestedLanguageTag,
                    expectedResponseContent, expectedResponseLanguageTag);
        }
    }

    private void testResponseContent(boolean withRootBundleFile,
            String requestedLanguageTag, String expectedResponseContent,
            String expectedResponseLanguageTag) throws IOException {
        createTranslationFiles(withRootBundleFile);
        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(I18NUtil::getClassLoader).thenReturn(urlClassLoader);
            setRequestParams(requestedLanguageTag,
                    HandlerHelper.RequestType.TRANSLATION_FILE.getIdentifier());
            Assert.assertTrue(
                    handler.handleRequest(session, request, response));
            Assert.assertEquals(expectedResponseContent, getResponseContent());
            if (expectedResponseLanguageTag == null) {
                Assert.assertEquals(0,
                        retrievedLocaleCapture.getAllValues().size());
            } else {
                Assert.assertEquals(expectedResponseLanguageTag,
                        retrievedLocaleCapture.getValue());
            }
        }
    }

    private String getResponseContent() {
        writer.flush();
        return out.toString();
    }

    private void setRequestParams(String langtag, String requestTypeId) {
        Mockito.when(request.getParameter(
                TranslationFileRequestHandler.LANGUAGE_TAG_PARAMETER_NAME))
                .thenReturn(langtag);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(requestTypeId);
    }

    private void createTranslationFiles(boolean withRootBundleFile)
            throws IOException {
        if (withRootBundleFile) {
            createTranslationFile("title=Root bundle lang", "");
        }
        createTranslationFile("title=Suomi", "_fi");
        createTranslationFile("title=Espanol (Spain)", "_es_ES");
        createTranslationFile("title=Espanol (Argentina)", "_es_AR");
    }

    private void createTranslationFile(String content, String fileNameSuffix)
            throws IOException {
        File file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + fileNameSuffix
                        + ".properties");
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);
    }
}
