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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

@NotThreadSafe
public class TranslationFileRequestHandlerTest {

    private final VaadinSession session = Mockito.mock(VaadinSession.class);

    private final VaadinRequest request = Mockito.mock(VaadinRequest.class);

    private VaadinResponse response;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassLoader urlClassLoader;

    private TranslationFileRequestHandler handler;

    private StringWriter out;

    private PrintWriter writer;

    private File translationsFolder;

    private ArgumentCaptor<String> retrievedLocaleCapture;

    private final List<Locale> providedLocales = new ArrayList<>();

    private I18NProvider i18NProvider;

    @Before
    public void configure()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        initTranslationsFolder();
    }

    @After
    public void cleanup() {
        ResourceBundle.clearCache(urlClassLoader);
    }

    @Test
    public void pathDoesNotMatch_requestNotHandled() throws IOException {
        configure(true);
        setRequestParams(null, "other", null);
        Assert.assertFalse(handler.handleRequest(session, request, response));
    }

    @Test
    public void withRootBundle_languageTagIsNull_responseIsRootBundle()
            throws IOException {
        configure(true);
        testResponseContent(null, "{\"title\":\"Root bundle lang\"}", "und");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withoutRootBundle_languageTagIsNull_responseIsEmpty()
            throws IOException {
        configure(false);
        testResponseContent(null, "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void withRootBundle_languageTagIsEmpty_responseIsRootBundle()
            throws IOException {
        configure(true);
        testResponseContent("", "{\"title\":\"Root bundle lang\"}", "und");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withoutRootBundle_languageTagIsEmpty_responseIsEmpty()
            throws IOException {
        configure(false);
        testResponseContent("", "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void languageTagWithoutCountryAvailable_responseIsCorrect()
            throws IOException {
        configure(true);
        testResponseContent("fi", "{\"title\":\"Suomi\"}", "fi");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void tagContainsOnlyLanguage_languageOnlyAvailableWithCountry_responseHasTheCorrectLanguage()
            throws IOException {
        configure(false);
        testResponseContent("es", "{\"title\":\"Espanol (Spain)\"}", "es-ES");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void languageTagWithCountryAvailable_responseIsCorrect()
            throws IOException {
        configure(false);
        testResponseContent("es-ES", "{\"title\":\"Espanol (Spain)\"}",
                "es-ES");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withRootBundle_requestedLocaleBundleNotAvailable_responseIsRootBundle()
            throws IOException {
        configure(true);
        testResponseContentWithMockedDefaultLocale("es-ES", "en-US",
                "{\"title\":\"Root bundle lang\"}", "und");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withoutRootBundle_requestedLocaleBundleNotAvailable_responseIsEmpty()
            throws IOException {
        configure(false);
        testResponseContentWithMockedDefaultLocale("es-ES", "en-US", "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void languageTagWithUnderscoresAvailable_responseIsCorrect()
            throws IOException {
        configure(false);
        testResponseContent("es_ES", "{\"title\":\"Espanol (Spain)\"}",
                "es-ES");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withRootBundle_languageTagWithUnderscoresNotAvailable_responseIsRootBundle()
            throws IOException {
        configure(true);
        testResponseContent("it_IT", "{\"title\":\"Root bundle lang\"}", "und");
        Mockito.verify(response).setStatus(HttpStatusCode.OK.getCode());
    }

    @Test
    public void withoutRootBundle_languageTagWithUnderscoresNotAvailable_responseIsEmpty()
            throws IOException {
        configure(false);
        testResponseContent("it_IT", "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void withCustomI18nProvider_requestedLocaleBundleAvailable_responseIsEmpty()
            throws IOException {
        configure(true, I18NProvider.class, false);
        testResponseContent("fi", "", null);
        Mockito.verify(response).sendError(
                Mockito.eq(HttpStatusCode.NOT_IMPLEMENTED.getCode()),
                Mockito.anyString());
    }

    @Test
    public void productionMode_withCustomI18nProvider_requestedLocaleBundleAvailable_responseIsEmpty()
            throws IOException {
        configure(true, I18NProvider.class, true);
        testResponseContent("fi", "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void withCustomizedDefaultI18nProvider_requestedLocaleBundleAvailable_responseIsEmpty()
            throws IOException {
        configure(true, CustomizedDefaultI18NProvider.class, false);
        testResponseContent("fi", "", null);
        Mockito.verify(response).sendError(
                Mockito.eq(HttpStatusCode.NOT_IMPLEMENTED.getCode()),
                Mockito.anyString());
    }

    @Test
    public void productionMode_withCustomizedDefaultI18nProvider_requestedLocaleBundleAvailable_responseIsEmpty()
            throws IOException {
        configure(true, CustomizedDefaultI18NProvider.class, true);
        testResponseContent("fi", "", null);
        Mockito.verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    @Test
    public void productionMode_withChunks_filtersKeys() throws IOException {
        // configure(true, DefaultI18NProvider.class, true);
        createTranslationFile("""
                title=Root bundle lang
                key1=Value1
                key2=Value2
                """.stripIndent(), "");
        createTranslationFile("""
                title=Suomi
                key1=SuomiValue1
                key2=SuomiValue2
                """.stripIndent(), "_fi");
        createTranslationFile("""
                title=Espanol (Spain)
                key1=EspanolValue1
                key2=EspanolValue2
                """.stripIndent(), "_es_ES");
        File file = new File(translationsFolder, "i18n.json");
        var json = """
                {
                    "chunks": {
                        "indexhtml": {
                            "keys": [
                                "title"
                            ]
                        },
                        "other": {
                            "keys": [
                                "key1",
                                "key2"
                            ]
                        }
                    }
                }
                """.stripIndent();
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);
        mockI18nProvider(DefaultI18NProvider.class);
        mockService(true);
        handler = new TranslationFileRequestHandler(i18NProvider);
        mockResponse();
        testResponseContent("", new String[] { "indexhtml" },
                "{\"title\":\"Root bundle lang\"}", "und");
    }

    private void testResponseContentWithMockedDefaultLocale(
            String defaultLocaleLanguageTag, String requestedLanguageTag,
            String expectedResponseContent, String expectedResponseLanguageTag)
            throws IOException {
        Locale originalDefaultLocale = Locale.getDefault();
        try {
            Locale defaultLocale = Locale
                    .forLanguageTag(defaultLocaleLanguageTag);
            Locale.setDefault(defaultLocale);
            testResponseContent(requestedLanguageTag, expectedResponseContent,
                    expectedResponseLanguageTag);
        } finally {
            Locale.setDefault(originalDefaultLocale);
        }
    }

    private void testResponseContent(String requestedLanguageTag,
            String expectedResponseContent, String expectedResponseLanguageTag)
            throws IOException {
        testResponseContent(requestedLanguageTag, null, expectedResponseContent,
                expectedResponseLanguageTag);
    }

    private void testResponseContent(String requestedLanguageTag,
            String[] requestedChunks, String expectedResponseContent,
            String expectedResponseLanguageTag) throws IOException {
        setRequestParams(requestedLanguageTag,
                HandlerHelper.RequestType.TRANSLATION_FILE.getIdentifier(),
                requestedChunks);
        Assert.assertTrue("The request was not handled by the handler.",
                handler.handleRequest(session, request, response));
        Assert.assertEquals(
                "The expected response content does not match the actual response content.",
                expectedResponseContent, getResponseContent());
        if (expectedResponseLanguageTag == null) {
            Assert.assertEquals("The response language tag was not found.", 0,
                    retrievedLocaleCapture.getAllValues().size());
        } else {
            Assert.assertEquals(
                    "The expected response language tag does not match the actual response language tag.",
                    expectedResponseLanguageTag,
                    retrievedLocaleCapture.getValue());
        }
    }

    private String getResponseContent() {
        writer.flush();
        return out.toString();
    }

    private void setRequestParams(String langtag, String requestTypeId,
            String[] chunks) {
        Mockito.when(request.getParameter(
                TranslationFileRequestHandler.LANGUAGE_TAG_PARAMETER_NAME))
                .thenReturn(langtag);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(requestTypeId);
        if (chunks != null) {
            // the parameter map is incomplete, but at the moment only the chunk
            // functionality uses it
            Mockito.when(request.getParameterMap())
                    .thenReturn(Map.of(
                            TranslationFileRequestHandler.CHUNK_PARAMETER_NAME,
                            chunks));
        }
    }

    private void createTranslationFiles(boolean withRootBundleFile)
            throws IOException {
        if (withRootBundleFile) {
            createTranslationFile("title=Root bundle lang", "");
        }
        providedLocales.add(Locale.forLanguageTag("fi"));
        createTranslationFile("title=Suomi", "_fi");
        providedLocales.add(Locale.forLanguageTag("es-ES"));
        createTranslationFile("title=Espanol (Spain)", "_es_ES");
    }

    private void createTranslationFile(String content, String fileNameSuffix)
            throws IOException {
        File file = new File(translationsFolder,
                DefaultI18NProvider.BUNDLE_FILENAME + fileNameSuffix
                        + ".properties");
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);
    }

    private void configure(boolean withRootBundle) throws IOException {
        configure(withRootBundle, DefaultI18NProvider.class, false);
    }

    private void configure(boolean withRootBundle,
            Class<? extends I18NProvider> i18NProviderClass,
            boolean isProductionMode) throws IOException {
        createTranslationFiles(withRootBundle);
        mockI18nProvider(i18NProviderClass);
        mockService(isProductionMode);
        handler = new TranslationFileRequestHandler(i18NProvider);
        mockResponse();
    }

    private void initTranslationsFolder() throws IOException {
        File resources = temporaryFolder.newFolder();
        translationsFolder = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translationsFolder.mkdirs();
        urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });
    }

    private void mockI18nProvider(
            Class<? extends I18NProvider> i18NProviderClass) {
        if (i18NProviderClass.equals(DefaultI18NProvider.class)) {
            i18NProvider = Mockito.spy(
                    new DefaultI18NProvider(providedLocales, urlClassLoader));
        } else {
            i18NProvider = Mockito.mock(i18NProviderClass,
                    Mockito.CALLS_REAL_METHODS);
            Mockito.when(i18NProvider.getProvidedLocales())
                    .thenReturn(providedLocales);
        }
    }

    private void mockResponse() throws IOException {
        response = Mockito.mock(VaadinResponse.class);
        out = new StringWriter();
        writer = new PrintWriter(out);
        Mockito.when(response.getWriter()).thenReturn(writer);
        retrievedLocaleCapture = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(response).setHeader(Mockito
                .eq(TranslationFileRequestHandler.RETRIEVED_LOCALE_HEADER_NAME),
                retrievedLocaleCapture.capture());
    }

    private void mockService(boolean isProductionMode) {
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getI18NProvider()).thenReturn(i18NProvider);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode())
                .thenReturn(isProductionMode);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(session.getService()).thenReturn(service);
    }

    private static class CustomizedDefaultI18NProvider
            extends DefaultI18NProvider {
        public CustomizedDefaultI18NProvider(List<Locale> providedLocales) {
            super(providedLocales,
                    TranslationFileRequestHandlerTest.class.getClassLoader());
        }
    }
}
