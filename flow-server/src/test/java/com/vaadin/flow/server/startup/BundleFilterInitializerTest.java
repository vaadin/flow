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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonException;

public class BundleFilterInitializerTest {

    private static final String FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON = "/frontend-es6/vaadin-flow-bundle-manifest.json";

    private ServiceInitEvent event;

    private Consumer<DependencyFilter> dependencyFilterAddHandler = dependency -> {
    };

    private MockServletServiceSessionSetup mocks;

    @Before
    public void init() throws Exception {
        mocks = new MockServletServiceSessionSetup(false);
        // There is no user session when initializing the filter
        Assert.assertNull(mocks.getSession());
        event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(mocks.getService());
        mocks.setProductionMode(true);
        mocks.getDeploymentConfiguration().setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES6,
                Constants.FRONTEND_URL_ES6_DEFAULT_VALUE);

        Mockito.doAnswer(invocation -> {
            dependencyFilterAddHandler.accept(
                    invocation.getArgumentAt(0, DependencyFilter.class));
            return null;
        }).when(event).addDependencyFilter(Mockito.any(DependencyFilter.class));
    }

    @Test(expected = UncheckedIOException.class)
    public void fail_to_load_bundle_manifest() {
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON);
        Mockito.when(mocks.getServlet().getServletContext().getResourceAsStream(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON))
                .thenAnswer(i -> {
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            throw new IOException("Intentionally failed");
                        }
                    };
                });

        new BundleFilterInitializer().serviceInit(event);
    }

    @Test()
    public void when_json_file_not_found_should_disable_bundle() {
        Assert.assertFalse(Boolean.getBoolean("vaadin." + Constants.DISABLE_BUNDLE));
        new BundleFilterInitializer().serviceInit(event);
        Assert.assertTrue(Boolean.getBoolean("vaadin." + Constants.DISABLE_BUNDLE));
    }

    @Test(expected = JsonException.class)
    public void fail_when_loading_invalid_json() {
        String manifestString = "{ wait this is not json";
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON, manifestString);
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void does_not_contain_main_bundle_fails() {
        String manifestString = "{'fragment-1':['dependency-1', 'dependency-2']}";
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON, manifestString);
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void bundle_file_not_found_throws() {
        String manifestString = getBasicTestBundleString();

        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON, manifestString);
        new BundleFilterInitializer().serviceInit(event);
    }

    @Test
    public void null_bundle_manifest_stream_no_dependency_filter_added() {
        new BundleFilterInitializer().serviceInit(event);
        Mockito.verify(event, Mockito.never())
                .addDependencyFilter(Mockito.any(DependencyFilter.class));
    }

    @Test
    public void happy_path_no_hash() {
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

        service_init(getBasicTestBundleString(), BundleDependencyFilter.MAIN_BUNDLE_URL);
    }

    @Test
    public void happy_path_with_hash() {
        String bundleName = BundleDependencyFilter.MAIN_BUNDLE_URL.replace(".html","-XXX.cache.html");
        service_init(getBasicTestBundleHashedString(bundleName), bundleName);
    }

    private void service_init(String manifestString, String bundleName) {
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_VAADIN_FLOW_BUNDLE_MANIFEST_JSON, manifestString);
        for (int i = 0; i < 5; i++) {
            mocks.getServlet()
                    .addServletContextResource("/frontend-es6/dependency-" + i);
        }
        mocks.getServlet()
                .addServletContextResource("/frontend-es6/fragment-1");
        mocks.getServlet()
                .addServletContextResource("/frontend-es6/fragment-2");

        mocks.getServlet().addServletContextResource(
                "/frontend-es6/" + bundleName);

        new BundleFilterInitializer().serviceInit(event);
    }

    private String getBasicTestBundleString() {
        return "{'fragment-1':['dependency-1', 'dependency-2'],"
                + "'fragment-2':['dependency-3', 'dependency-4']," + "'"
                + BundleDependencyFilter.MAIN_BUNDLE_URL + "':['dependency-0']"
                + "}";
    }

    private String getBasicTestBundleHashedString(String bundleName) {
        return "{'fragment-1':['dependency-1', 'dependency-2'],"
                + "'fragment-2':['dependency-3', 'dependency-4'],"
                + "'" + bundleName + "':['dependency-0','" + BundleDependencyFilter.MAIN_BUNDLE_URL + "']"
                + "}";
    }
}
