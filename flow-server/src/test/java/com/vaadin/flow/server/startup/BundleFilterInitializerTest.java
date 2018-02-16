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
package com.vaadin.flow.server.startup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonException;

public class BundleFilterInitializerTest {

    @FunctionalInterface
    private interface CheckedFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    private ServiceInitEvent event;
    private ServletContext context;

    private Consumer<DependencyFilter> dependencyFilterAddHandler = dependency -> {
    };
    private CheckedFunction<String, InputStream, IOException> inputStreamProducer;
    private CheckedFunction<String, URL, MalformedURLException> resourceProducer = str -> new URL(
            "http://some/test/url");

    @Before
    public void init() throws MalformedURLException {
        event = Mockito.mock(ServiceInitEvent.class);
        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        context = Mockito.mock(ServletContext.class);

        Mockito.when(event.getSource()).thenReturn(service);
        Mockito.when(service.getServlet()).thenReturn(servlet);
        Mockito.when(servlet.getServletContext()).thenReturn(context);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(new DefaultDeploymentConfiguration(
                        BundleFilterInitializerTest.class, new Properties(),
                        (base, consumer) -> {
                        }) {
                    @Override
                    public boolean isProductionMode() {
                        return true;
                    }
                });

        Mockito.doAnswer(invocation -> {
            dependencyFilterAddHandler.accept(
                    invocation.getArgumentAt(0, DependencyFilter.class));
            return null;
        }).when(event).addDependencyFilter(Mockito.any(DependencyFilter.class));
        Mockito.doAnswer(invocation -> {
            return inputStreamProducer
                    .apply(invocation.getArgumentAt(0, String.class));
        }).when(context).getResourceAsStream("/frontend-es6/vaadin-flow-bundle-manifest.json");
        Mockito.doAnswer(invocation -> {
            return resourceProducer
                    .apply(invocation.getArgumentAt(0, String.class));
        }).when(context).getResource(Mockito.anyString());
    }

    @Test(expected = UncheckedIOException.class)
    public void fail_to_load_bundle_manifest() {
        inputStreamProducer = str -> {
            throw new IOException();
        };
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test(expected = JsonException.class)
    public void fail_when_loading_invalid_json() {
        String manifestString = "{ wait this is not json";
        inputStreamProducer = str -> getTestBundleManifestStream(
                manifestString);
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void does_not_contain_main_bundle_fails() {
        String manifestString = "{'fragment-1':['dependency-1', 'dependency-2']}";
        inputStreamProducer = str -> getTestBundleManifestStream(
                manifestString);
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bundle_file_not_found_throws() {
        String manifestString = getBasicTestBundleString();
        inputStreamProducer = str -> getTestBundleManifestStream(
                manifestString);
        resourceProducer = str -> null;
        new BundleFilterInitializer().serviceInit(event);
    }
    
    @Test
    public void null_bundle_manifest_stream_no_dependency_filter_added() {
        inputStreamProducer = str -> null;
        new BundleFilterInitializer().serviceInit(event);
        Mockito.verify(event, Mockito.never())
                .addDependencyFilter(Mockito.any(DependencyFilter.class));
    }

    @Test
    public void happy_path() {
        String manifestString = getBasicTestBundleString();
        inputStreamProducer = str -> getTestBundleManifestStream(
                manifestString);
        dependencyFilterAddHandler = dependencyFilter -> {
            List<Dependency> dependencies = IntStream.range(0, 5)
                    .mapToObj(number -> new Dependency(
                            Dependency.Type.HTML_IMPORT, "dependency-" + number,
                            LoadMode.EAGER))
                    .collect(Collectors.toList());
            List<Dependency> filtered = dependencyFilter.filter(dependencies,
                    null);
            List<Dependency> expected = Stream
                    .of(BundleDependencyFilter.MAIN_BUNDLE_URL, "fragment-1",
                            "fragment-2")
                    .map(url -> new Dependency(Dependency.Type.HTML_IMPORT, url,
                            LoadMode.EAGER))
                    .collect(Collectors.toList());
            Assert.assertTrue(expected.containsAll(filtered));
            Assert.assertEquals(expected.size(), filtered.size());
        };
        new BundleFilterInitializer().serviceInit(event);
    }

    private String getBasicTestBundleString() {
        return "{'fragment-1':['dependency-1', 'dependency-2'],"
                + "'fragment-2':['dependency-3', 'dependency-4']," + "'"
                + BundleDependencyFilter.MAIN_BUNDLE_URL + "':['dependency-0']"
                + "}";
    }

    private InputStream getTestBundleManifestStream(String manifestString) {
        try {
            return new ByteArrayInputStream(
                    manifestString.getBytes(StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("manifestString does not have proper encoding.");
        }
        return null;
    }
}
