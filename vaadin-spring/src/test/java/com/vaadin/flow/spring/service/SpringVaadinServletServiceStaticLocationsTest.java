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

package com.vaadin.flow.spring.service;

import jakarta.servlet.ServletException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringVaadinServletServiceStaticLocationsTest.TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SpringVaadinServletServiceStaticLocationsTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    WebProperties webProperties;

    @TempDir
    Path staticResources;

    @Test
    void getStaticResource_defaultClasspathStaticLocation_notExistingResource_getsNull()
            throws ServletException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        assertNull(service.getStaticResource("not-existing"));
    }

    @Test
    void getStaticResource_defaultClasspathStaticLocation_existingResource_getsURL()
            throws ServletException, URISyntaxException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        URL url = service.getStaticResource("dummy.txt");
        assertNotNull(url);
        assertFalse(url.toURI().isOpaque(), "Expected a hierarchical URI");
    }

    @Test
    void getStaticResource_classpathStaticLocation_notExistingResource_getsNull()
            throws ServletException {
        webProperties.getResources()
                .setStaticLocations(new String[] { "classpath:VAADIN/config" });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        assertNull(service.getStaticResource("not-existing"));
    }

    @Test
    void getStaticResource_classpathStaticLocation_existingResource_getsURL()
            throws ServletException, URISyntaxException {
        webProperties.getResources().setStaticLocations(
                new String[] { "classpath:/META-INF/VAADIN/config" });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        URL url = service.getStaticResource("flow-build-info.json");
        assertNotNull(url);
        assertFalse(url.toURI().isOpaque(), "Expected a hierarchical URI");
    }

    @Test
    void getStaticResource_relativeFilesystemStaticLocation_notExistingResource_getsNull()
            throws ServletException {
        webProperties.getResources().setStaticLocations(
                new String[] { "file:src/test/resources/public" });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        assertNull(service.getStaticResource("not-existing"));
    }

    @Test
    void getStaticResource_relativeFilesystemStaticLocation_existingResource_getsURL()
            throws ServletException, IOException, URISyntaxException {
        webProperties.getResources().setStaticLocations(
                new String[] { "file:src/test/resources/public" });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        URL url = service.getStaticResource("dummy.txt");
        assertNotNull(url);
        assertFalse(url.toURI().isOpaque(), "Expected a hierarchical URI");
    }

    @Test
    void getStaticResource_notExistingRelativeFilesystemStaticLocation_getsNull()
            throws ServletException {
        webProperties.getResources()
                .setStaticLocations(new String[] { "file:a/b/c" });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        assertNull(service.getStaticResource("not-existing"));
    }

    @Test
    void getStaticResource_absoluteFilesystemStaticLocation_notExistingResource_getsNull()
            throws ServletException {
        webProperties.getResources().setStaticLocations(
                new String[] { "file:" + staticResources.toAbsolutePath() });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        assertNull(service.getStaticResource("not-existing"));
    }

    @Test
    void getStaticResource_absoluteFilesystemStaticLocation_existingResource_getsURL()
            throws ServletException, IOException, URISyntaxException {
        Files.createFile(staticResources.resolve("dummy.txt"));
        webProperties.getResources().setStaticLocations(
                new String[] { "file:" + staticResources.toAbsolutePath() });
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());
        URL url = service.getStaticResource("dummy.txt");
        assertNotNull(url);
        assertFalse(url.toURI().isOpaque(), "Expected a hierarchical URI");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        WebProperties webProperties() {
            return new WebProperties();
        }
    }

}
