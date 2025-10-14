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
package com.vaadin.flow.spring.i18n;

import jakarta.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Properties;

import net.jcip.annotations.NotThreadSafe;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.spring.VaadinApplicationConfiguration;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@Import(VaadinApplicationConfiguration.class)
@NotThreadSafe
public class DefaultI18NProviderFactoryTest {

    @Autowired
    private ApplicationContext context;

    static private ClassLoader originalClassLoader;

    static private ClassLoader testClassLoader;

    static private TemporaryFolder temporaryFolder = new TemporaryFolder();

    static volatile private MockedConstruction<PathMatchingResourcePatternResolver> pathMatchingResourcePatternResolverMockedConstruction;

    @BeforeClass
    static public void setup() throws IOException {
        originalClassLoader = Thread.currentThread().getContextClassLoader();

        temporaryFolder.create();
        File resources = temporaryFolder.newFolder();

        File translations = new File(resources,
                DefaultI18NProvider.BUNDLE_FOLDER);
        translations.mkdirs();

        File defaultTranslation = new File(translations,
                DefaultI18NProvider.BUNDLE_FILENAME + ".properties");
        Files.writeString(defaultTranslation.toPath(), "title=Default lang",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        testClassLoader = new URLClassLoader(
                new URL[] { resources.toURI().toURL() },
                DefaultI18NProviderFactory.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(testClassLoader);

        Resource translationResource = new DefaultResourceLoader()
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER + "/"
                        + DefaultI18NProvider.BUNDLE_FILENAME + ".properties");

        pathMatchingResourcePatternResolverMockedConstruction = Mockito
                .mockConstruction(PathMatchingResourcePatternResolver.class,
                        (mock, context) -> {
                            Mockito.when(mock.getPathMatcher())
                                    .thenCallRealMethod();
                            Mockito.when(mock.getResources(Mockito.anyString()))
                                    .thenAnswer(invocationOnMock -> {
                                        String pattern = invocationOnMock
                                                .getArgument(0);
                                        Assert.assertEquals(
                                                "classpath*:/vaadin-i18n/*.properties",
                                                pattern);
                                        return new Resource[] {
                                                translationResource };
                                    });
                        });
    }

    @AfterClass
    static public void teardown() throws Exception {
        pathMatchingResourcePatternResolverMockedConstruction.close();
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    @Test
    public void create_usesThreadContextClassLoader() throws ServletException {
        Instantiator instantiator = getInstantiator(context);
        I18NProvider i18NProvider = instantiator.getI18NProvider();

        Assert.assertNotNull(i18NProvider);
        Assert.assertTrue(i18NProvider instanceof DefaultI18NProvider);
        Assert.assertEquals("Default lang",
                i18NProvider.getTranslation("title", Locale.getDefault()));
    }

    private static Instantiator getInstantiator(ApplicationContext context)
            throws ServletException {
        return SpringInstantiatorTest.getService(context, new Properties())
                .getInstantiator();
    }
}
