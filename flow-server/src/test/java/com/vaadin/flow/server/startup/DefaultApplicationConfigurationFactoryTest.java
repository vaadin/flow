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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinConfig;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

public class DefaultApplicationConfigurationFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void create_tokenFileIsReadFromClassloader_externalStatsFileIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String content = "{ \"externalStatsFile\":true }";
        mockClassPathTokenFile(resourceProvider, content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertTrue(
                propertyNames.contains(Constants.EXTERNAL_STATS_FILE));
        Assert.assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        Assert.assertFalse(configuration.isProductionMode());
    }

    @Test
    public void create_tokenFileIsSetViaContext_externalStatsFileIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        String content = "{ \"externalStatsFile\":true }";
        VaadinContext context = mockTokenFileViaContextParam(content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertTrue(
                propertyNames.contains(Constants.EXTERNAL_STATS_FILE));
        Assert.assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        Assert.assertFalse(configuration.isProductionMode());
    }

    @Test
    public void create_tokenFileIsSetViaContext_externalStatsUrlIsReadFromTokenFile_predefinedContext()
            throws MalformedURLException, IOException {
        String content = "{ \"externalStatsUrl\": \"http://my.server/static/stats.json\"}";
        VaadinContext context = mockTokenFileViaContextParam(content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_URL));
        Assert.assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        Assert.assertEquals("http://my.server/static/stats.json", configuration
                .getStringProperty(Constants.EXTERNAL_STATS_URL, null));
        Assert.assertFalse(configuration.isProductionMode());
    }

    @Test
    public void create_tokenFileIsReadFromClassloader_externalStatsUrlIsReadFromTokenFile_predefinedContext()
            throws IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        mockClassPathTokenFile(resourceProvider,
                "{ \"externalStatsUrl\": \"http://my.server/static/stats.json\"}");

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertTrue(propertyNames.contains(Constants.EXTERNAL_STATS_URL));
        Assert.assertTrue(configuration
                .getBooleanProperty(Constants.EXTERNAL_STATS_FILE, false));
        Assert.assertEquals("http://my.server/static/stats.json", configuration
                .getStringProperty(Constants.EXTERNAL_STATS_URL, null));
        Assert.assertFalse(configuration.isProductionMode());
    }

    @Test
    public void create_propertiesAreReadFromContext() throws IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);
        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        Mockito.when(context.getContextParameterNames()).thenReturn(
                Collections.enumeration(Collections.singleton("foo")));
        Mockito.when(context.getContextParameter("foo")).thenReturn("bar");

        mockClassPathTokenFile(resourceProvider, "{}");

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertEquals(1, propertyNames.size());
        Assert.assertEquals("foo", propertyNames.get(0));
        Assert.assertEquals("bar",
                configuration.getStringProperty("foo", null));
    }

    @Test
    public void create_tokenFileWithPremiumFlag_premiumFlagIsPropagatedToDeploymentConfiguration()
            throws IOException {
        assertTokenAttributeIsPropagatedToDeploymentConfiguration(
                Constants.PREMIUM_FEATURES, true);
    }

    @Test
    public void create_tokenFileWithCommercialBannerFlag_commercialBannerFlagIsPropagatedToDeploymentConfiguration()
            throws IOException {
        assertTokenAttributeIsPropagatedToDeploymentConfiguration(
                Constants.COMMERCIAL_BANNER_TOKEN, true);
    }

    private void assertTokenAttributeIsPropagatedToDeploymentConfiguration(
            String attributeName, Object value) throws IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String content = JsonUtils.mapToJson(Map.of(attributeName, value))
                .toString();
        mockClassPathTokenFile(resourceProvider, content);

        DefaultApplicationConfigurationFactory factory = new DefaultApplicationConfigurationFactory();
        ApplicationConfiguration configuration = factory.create(context);

        List<String> propertyNames = Collections
                .list(configuration.getPropertyNames());
        Assert.assertTrue(propertyNames.contains(attributeName));
        if (value instanceof Boolean) {
            Assert.assertTrue(
                    configuration.getBooleanProperty(attributeName, false));
        } else {
            Assert.assertEquals(
                    configuration.getStringProperty(attributeName, null),
                    value.toString());
        }
    }

    private void mockClassPathTokenFile(ResourceProvider resourceProvider,
            String content) throws IOException, MalformedURLException {
        String path = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        File tmpFile = temporaryFolder.newFile();
        Files.write(tmpFile.toPath(), Collections.singletonList(content));

        URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return tmpFile.toURI().toURL().openConnection();
            }
        };
        URL url = new URL("file", "", -1, "foo.jar!/" + path, handler);

        Mockito.when(resourceProvider.getApplicationResources(path))
                .thenReturn(Collections.singletonList(url));
    }

    private ResourceProvider mockResourceProvider(VaadinConfig config,
            VaadinContext context) {
        Mockito.when(config.getVaadinContext()).thenReturn(context);

        Mockito.when(context.getContextParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(config.getConfigParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(context.getAttribute(ApplicationConfiguration.class))
                .thenReturn(appConfig);
        Mockito.when(context.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(appConfig);

        Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        return resourceProvider;
    }

    private VaadinContext mockTokenFileViaContextParam(String content)
            throws IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(context.getContextParameterNames())
                .thenReturn(Collections.enumeration(
                        Collections.singleton(FrontendUtils.PARAM_TOKEN_FILE)));

        File tmpFile = temporaryFolder.newFile();
        Files.write(tmpFile.toPath(), Collections.singletonList(content));

        Mockito.when(
                context.getContextParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(tmpFile.getPath());
        return context;
    }
}
