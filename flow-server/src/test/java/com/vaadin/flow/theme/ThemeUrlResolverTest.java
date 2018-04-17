/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.theme;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServlet;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webjar.WebJarServer;

import net.jcip.annotations.NotThreadSafe;

/**
 * Test resolving the theme url through
 * {@link com.vaadin.flow.server.VaadinServlet#getUrlTranslation}.
 */
@NotThreadSafe
public class ThemeUrlResolverTest {

    private MockServletServiceSessionSetup mocks;
    private TestVaadinServlet servlet;
    private DeploymentConfiguration deploymentConfiguration;

    @Before
    public void init() throws Exception {
        assert VaadinSession
                .getCurrent() == null : "Required no current vaadin session.";
        assert VaadinRequest
                .getCurrent() == null : "Required no current vaadin request.";

        mocks = new MockServletServiceSessionSetup();
        servlet = mocks.getServlet();
        deploymentConfiguration = mocks.getDeploymentConfiguration();
        Properties initParameters = new Properties();
        ServletContext servletContext = mocks.getServletContext();
        Mockito.when(servletContext.getInitParameterNames()).thenReturn(
                (Enumeration<String>) initParameters.propertyNames());
        Mockito.when(servletContext.getInitParameterNames()).thenReturn(
                (Enumeration<String>) initParameters.propertyNames());

        Mockito.when(servletContext.getResource(Mockito.anyString()))
                .thenAnswer(
                        i -> new URL("http://localhost" + i.getArguments()[0]));
    }

    @After
    public void teardown() {
        mocks.cleanup();
    }

    AbstractTheme theme = new AbstractTheme() {
        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/custom/";
        }
    };

    @Test
    public void servlet_context_file_resolves_correct_url() throws Exception {
        String path = "theme/custom/button.html";
        Mockito.when(servlet.getResource("/" + path))
                .thenReturn(new URL("http://theme/custom/button.html"));

        String urlTranslation = servlet.getUrlTranslation(theme,
                "src/button.html");

        assertEquals("Translation url was not the theme path one.", path,
                urlTranslation);
    }

    @Test
    public void no_file_resolves_original_url() throws Exception {
        servlet.setResourceFoundOverride(resource -> false);

        String urlTranslation = servlet.getUrlTranslation(theme,
                "src/button.html");

        assertEquals("Translation did not return original path.",
                "src/button.html", urlTranslation);
    }

    @Test
    public void theme_translation_accepts_web_jar() throws Exception {
        String path = "theme/custom/button.html";
        Mockito.when(servlet.getResource("/" + path)).thenReturn(null);
        WebJarServer webJarServer = Mockito.mock(WebJarServer.class);
        Field webJarServerField = VaadinServlet.class
                .getDeclaredField("webJarServer");
        webJarServerField.setAccessible(true);
        webJarServerField.set(servlet, webJarServer);
        webJarServerField.setAccessible(false);

        Mockito.when(webJarServer.getWebJarResourcePath("/" + path))
                .thenReturn(Optional.of("/webjar/theme/custom/button.html"));

        String urlTranslation = servlet.getUrlTranslation(theme,
                "src/button.html");

        assertEquals("Translation url was not the theme path one.", path,
                urlTranslation);
    }

    @Test
    public void themeTranslationCache_somethingIsCachedInProductionMode()
            throws Exception {
        mocks.setProductionMode(true);
        String path = "theme/custom/button.html";
        Mockito.when(servlet.getResource("/" + path))
                .thenReturn(new URL("http://theme/custom/button.html"));

        // Prime cache
        servlet.getUrlTranslation(theme, "src/button.html");

        Mockito.when(servlet.getResource("/" + path))
                .thenThrow(AssertionError.class);

        // Ask again, should not trigger servletContext
        servlet.getUrlTranslation(theme, "src/button.html");
    }

    @Test
    public void themeTranslationCache_somethingIsNotCachedWithoutProductionMode()
            throws Exception {
        mocks.setProductionMode(false);

        String path = "/theme/custom/button.html";

        servlet.setResourceFoundOverride(resource -> resource.equals(path));

        // Prime the cache
        String urlTranslation = servlet.getUrlTranslation(theme,
                "/src/button.html");
        // Sanity check
        assertEquals("Translation should be used", "/theme/custom/button.html",
                urlTranslation);

        servlet.setResourceFoundOverride(resource -> false);

        urlTranslation = servlet.getUrlTranslation(theme, "/src/button.html");
        assertEquals("Result should be computed again", "/src/button.html",
                urlTranslation);
    }

}
