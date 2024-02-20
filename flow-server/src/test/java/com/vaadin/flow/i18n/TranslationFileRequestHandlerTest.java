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

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import net.jcip.annotations.NotThreadSafe;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
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

    @Before
    public void init()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        File resources = temporaryFolder.newFolder();
        translationsFolder = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translationsFolder.mkdirs();
        urlClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() });
        handler = new TranslationFileRequestHandler();
        out = new StringWriter();
        writer = new PrintWriter(out);
        Mockito.when(response.getWriter()).thenReturn(writer);
    }

    @After
    public void cleanup() throws NoSuchFieldException, IllegalAccessException {
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
    public void withDefaultFile_languageTagIsNull_responseIsDefault()
            throws IOException {
        testResponseContent(true, null, "{\"title\":\"Default lang\"}");
    }

    @Test
    public void withoutDefaultFile_languageTagIsNull_responseIsEmpty()
            throws IOException {
        testResponseContent(false, null, "");
    }

    @Test
    public void languageTagWithoutCountryAvailable_responseIsCorrect()
            throws IOException {
        testResponseContent(true, "fi", "{\"title\":\"Suomi\"}");
    }

    @Test
    public void withDefaultFile_languageTagWithoutCountryNotAvailable_responseIsDefault()
            throws IOException {
        testResponseContent(true, "es", "{\"title\":\"Default lang\"}");
    }

    @Test
    public void withoutDefaultFile_languageTagWithoutCountryNotAvailable_responseIsEmpty()
            throws IOException {
        testResponseContent(false, "es", "");
    }

    @Test
    public void withoutDefaultFile_languageTagWithCountryAvailable_responseIsCorrect()
            throws IOException {
        testResponseContent(false, "es-ES", "{\"title\":\"Espanol (Spain)\"}");
    }

    private void testResponseContent(boolean withDefault, String langtag,
            String expectedResponseContent) throws IOException {
        createTranslationFiles(withDefault);
        try (MockedStatic<I18NUtil> util = Mockito.mockStatic(I18NUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            util.when(I18NUtil::getClassLoader).thenReturn(urlClassLoader);
            setRequestParams(langtag,
                    HandlerHelper.RequestType.TRANSLATION_FILE.getIdentifier());
            Assert.assertTrue(
                    handler.handleRequest(session, request, response));
            Assert.assertEquals(expectedResponseContent, getResponseContent());
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

    private void createTranslationFiles(boolean withDefault)
            throws IOException {
        if (withDefault) {
            createTranslationFile("title=Default lang", "");
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
