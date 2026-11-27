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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinConfig;
import com.vaadin.flow.server.VaadinContext;

import static com.vaadin.flow.internal.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DefaultApplicationConfigurationFactoryTest {

    @TempDir
    Path temporaryFolder;

    @Test
    void create_tokenFileIsReadFromClassloader_externalStatsFileIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String content = "{ \"externalStatsFile\":true }";
        mockClassPathTokenFile(resourceProvider, content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_FILE));
        assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        assertFalse(configuration.isProductionMode());
    }

    @Test
    void create_tokenFileIsSetViaContext_externalStatsFileIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        String content = "{ \"externalStatsFile\":true }";
        VaadinContext context = mockTokenFileViaContextParam(content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_FILE));
        assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        assertFalse(configuration.isProductionMode());
    }

    @Test
    void create_tokenFileIsSetViaContext_externalStatsUrlIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        String content = "{ \"externalStatsUrl\": \"http://my.server/static/stats.json\"}";
        VaadinContext context = mockTokenFileViaContextParam(content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_URL));
        assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        assertEquals("http://my.server/static/stats.json", configuration
                .getStringProperty(Constants.EXTERNAL_STATS_URL, null));
        assertFalse(configuration.isProductionMode());
    }

    @Test
    void create_tokenFileIsReadFromClassloader_externalStatsUrlIsReadFromTokenFile_predefinedContext()
            throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        mockClassPathTokenFile(resourceProvider,
                "{ \"externalStatsUrl\": \"http://my.server/static/stats.json\"}");

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_URL));
        assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        assertEquals("http://my.server/static/stats.json", configuration
                .getStringProperty(Constants.EXTERNAL_STATS_URL, null));
        assertFalse(configuration.isProductionMode());
    }

    @Test
    void create_propertiesAreReadFromContext() throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);
        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        when(context.getContextParameterNames()).thenReturn(
                Collections.enumeration(Collections.singleton("foo")));
        when(context.getContextParameter("foo")).thenReturn("bar");

        mockClassPathTokenFile(resourceProvider, "{}");

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertEquals(1, propertyNames.size());
        assertEquals("foo", propertyNames.get(0));
        assertEquals("bar", configuration.getStringProperty("foo", null));
    }

    @Test
    void create_tokenFileWithPremiumFlag_premiumFlagIsPropagatedToDeploymentConfiguration()
            throws IOException {
        assertTokenAttributeIsPropagatedToDeploymentConfiguration(
                Constants.PREMIUM_FEATURES, true);
    }

    @Test
    void create_tokenFileWithNpmAllowExoticVersions_flagIsPropagatedToDeploymentConfiguration()
            throws IOException {
        assertTokenAttributeIsPropagatedToDeploymentConfiguration(
                InitParameters.NPM_ALLOW_EXOTIC_VERSIONS, true);
    }

    @Test
    void create_tokenFileWithCommercialBannerFlag_commercialBannerFlagIsPropagatedToDeploymentConfiguration()
            throws IOException {
        assertTokenAttributeIsPropagatedToDeploymentConfiguration(
                Constants.COMMERCIAL_BANNER_TOKEN, true);
    }

    @Test
    void create_nestedJarsOfSpringBoot_tokenFileOfTheApplicationIsUsed()
            throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);
        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        // Spring Boot 3.2 and newer separate the jar of the application from
        // the archive nested in it with '/!' instead of '!/'
        when(resourceProvider
                .getApplicationResource(FrontendUtils.VITE_GENERATED_CONFIG))
                .thenReturn(new URL("file", "", -1,
                        "nested:/opt/app.jar/!BOOT-INF/lib/flow-server.jar!/"
                                + FrontendUtils.VITE_GENERATED_CONFIG));

        mockClassPathTokenFiles(resourceProvider, mockTokenFileUrl(
                "nested:/opt/app.jar/!BOOT-INF/lib/addon.jar!/",
                "{ \"externalStatsUrl\": \"http://addon/stats.json\" }"),
                mockTokenFileUrl("file:/opt/app.jar!/",
                        "{ \"externalStatsUrl\": \"http://application/stats.json\" }"));

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        assertEquals("http://application/stats.json",
                configuration.getStringProperty(Constants.EXTERNAL_STATS_URL,
                        null),
                "The token file of the application should be used instead of the one of a dependency");
    }

    @Test
    void getMode_returnsLivereload_tailwindCssIsEnabled() throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);
        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);
        mockClassPathTokenFile(resourceProvider, "{}");
        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        FeatureFlags featureFlags = mock(FeatureFlags.class);
        try (MockedStatic<FeatureFlags> flags = mockStatic(
                FeatureFlags.class)) {
            flags.when(() -> FeatureFlags.get(context))
                    .thenReturn(featureFlags);

            assertEquals(Mode.DEVELOPMENT_BUNDLE, configuration.getMode(),
                    "Should have bundle mode by default");

            when(featureFlags.isEnabled(CoreFeatureFlagProvider.TAILWIND_CSS))
                    .thenReturn(true);

            assertEquals(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD,
                    configuration.getMode(),
                    "Should have livereload mode when TailwindCSS is enabled");
        }
    }

    @Test
    void create_tokenFileWithOutdatedNodeVersion_versionIsIgnored()
            throws IOException {
        assertEquals("v26.0.0", nodeVersionFromTokenFile("v26.0.0"),
                "A usable Node.js version in the token file should be used");
        assertEquals("lts", nodeVersionFromTokenFile("lts"),
                "A version that cannot be parsed should be left for the frontend tooling to report");
        assertNull(nodeVersionFromTokenFile("v18.14.1"),
                "A Node.js version older than the frontend tooling supports comes from a stale token file and should be ignored");
    }

    private String nodeVersionFromTokenFile(String nodeVersion)
            throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);
        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);
        mockClassPathTokenFile(resourceProvider,
                JacksonUtils.mapToJson(
                        Map.of(InitParameters.NODE_VERSION, nodeVersion))
                        .toString());

        return new DefaultApplicationConfigurationFactory().create(context)
                .getStringProperty(InitParameters.NODE_VERSION, null);
    }

    private void assertTokenAttributeIsPropagatedToDeploymentConfiguration(
            String attributeName, Object value) throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        VaadinConfig config = mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String content = JacksonUtils.mapToJson(Map.of(attributeName, value))
                .toString();
        mockClassPathTokenFile(resourceProvider, content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        assertTrue(propertyNames.contains(attributeName));
        if (value instanceof Boolean) {
            assertTrue(configuration.getBooleanProperty(attributeName, false));
        } else {
            assertEquals(configuration.getStringProperty(attributeName, null),
                    value.toString());
        }
    }

    private void mockClassPathTokenFile(ResourceProvider resourceProvider,
            String content) throws IOException, MalformedURLException {
        mockClassPathTokenFiles(resourceProvider,
                mockTokenFileUrl("foo.jar!/", content));
    }

    private void mockClassPathTokenFiles(ResourceProvider resourceProvider,
            URL... urls) throws IOException {
        when(resourceProvider
                .getApplicationResources(VAADIN_SERVLET_RESOURCES + TOKEN_FILE))
                .thenReturn(List.of(urls));
    }

    private URL mockTokenFileUrl(String pathPrefix, String content)
            throws IOException, MalformedURLException {
        String path = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        File tmpFile = java.nio.file.Files
                .createTempFile(temporaryFolder, "tmp", null).toFile();
        Files.write(tmpFile.toPath(), Collections.singletonList(content));

        URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return tmpFile.toURI().toURL().openConnection();
            }
        };
        return new URL("file", "", -1, pathPrefix + path, handler);
    }

    private ResourceProvider mockResourceProvider(VaadinConfig config,
            VaadinContext context) {
        when(config.getVaadinContext()).thenReturn(context);

        when(context.getContextParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        when(config.getConfigParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        ApplicationConfiguration appConfig = mock(
                ApplicationConfiguration.class);

        when(context.getAttribute(ApplicationConfiguration.class))
                .thenReturn(appConfig);
        when(context.getAttribute(eq(ApplicationConfiguration.class), any()))
                .thenReturn(appConfig);

        Lookup lookup = mock(Lookup.class);
        ResourceProvider resourceProvider = mock(ResourceProvider.class);
        when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        return resourceProvider;
    }

    private VaadinContext mockTokenFileViaContextParam(String content)
            throws IOException {
        VaadinContext context = mock(VaadinContext.class);
        when(context.getContextParameterNames())
                .thenReturn(Collections.enumeration(
                        Collections.singleton(FrontendUtils.PARAM_TOKEN_FILE)));

        File tmpFile = java.nio.file.Files
                .createTempFile(temporaryFolder, "tmp", null).toFile();
        Files.write(tmpFile.toPath(), Collections.singletonList(content));

        when(context.getContextParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(tmpFile.getPath());
        return context;
    }
}
