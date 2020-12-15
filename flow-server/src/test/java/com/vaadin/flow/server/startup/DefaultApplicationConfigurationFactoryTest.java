/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.Collections;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.VaadinConfig;
import com.vaadin.flow.server.VaadinConfigurationException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

public class DefaultApplicationConfigurationFactoryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static class TestDefaultApplicationConfigurationFactory
            extends DefaultApplicationConfigurationFactory {

        @Override
        protected String getTokenFileContent(
                Function<String, String> locationProvider) {
            return super.getTokenFileContent(locationProvider);
        }
    }

    @Test
    public void createInitParameters_checkWebpackGeneratedFromContext()
            throws VaadinConfigurationException, IOException {
        VaadinContext context = Mockito.mock(VaadinContext.class);
        VaadinConfig config = Mockito.mock(VaadinConfig.class);

        ResourceProvider resourceProvider = mockResourceProvider(config,
                context);

        String path = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        File tmpFile = temporaryFolder.newFile();
        Files.write(tmpFile.toPath(), Collections.singletonList("{}"));

        URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return tmpFile.toURI().toURL().openConnection();
            }
        };
        URL url = new URL("file", "", -1, "foo.jar!/" + path, handler);

        Mockito.when(resourceProvider.getApplicationResources(path))
                .thenReturn(Collections.singletonList(url));

        Mockito.when(resourceProvider
                .getApplicationResource(FrontendUtils.WEBPACK_GENERATED))
                .thenReturn(tmpFile.toURI().toURL());

        TestDefaultApplicationConfigurationFactory factory = new TestDefaultApplicationConfigurationFactory();

        factory.getTokenFileFromClassloader(context);

        Mockito.verify(resourceProvider)
                .getApplicationResource(FrontendUtils.WEBPACK_GENERATED);

    }

    private ResourceProvider mockResourceProvider(VaadinConfig config,
            VaadinContext context) throws VaadinConfigurationException {
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
}
