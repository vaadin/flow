/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import static com.vaadin.flow.server.startup.BundleFilterFactory.FLOW_BUNDLE_MANIFEST;
import static com.vaadin.flow.server.startup.BundleFilterFactory.MAIN_BUNDLE_NAME_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter.FilterContext;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonException;

public class BundleFilterFactoryTest {
    private static final String NON_HASHED_BUNDLE_NAME = MAIN_BUNDLE_NAME_PREFIX
            + ".html";
    private static final String HASHED_BUNDLE_NAME = MAIN_BUNDLE_NAME_PREFIX
            + "-SOME_HASH.cache.html";
    private static final String FRONTEND_ES6_BUNDLE_MANIFEST = FLOW_BUNDLE_MANIFEST
            .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX,
                    "/frontend-es6/");

    private static final String FRONTEND_ES5_BUNDLE_MANIFEST = FLOW_BUNDLE_MANIFEST
            .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX,
                    "/frontend-es5/");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MockServletServiceSessionSetup mocks;

    @Before
    public void init() throws Exception {
        mocks = new MockServletServiceSessionSetup(false);
        Assert.assertNull(
                "There is no user session when initializing the filter and test should check it",
                mocks.getSession());
        mocks.setProductionMode(true);
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);
        mocks.getDeploymentConfiguration().setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES6,
                Constants.FRONTEND_URL_ES6_DEFAULT_VALUE);
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void fail_to_load_bundle_manifest() {
        mocks.getServlet()
                .addServletContextResource(FRONTEND_ES6_BUNDLE_MANIFEST);
        Mockito.when(mocks.getServlet().getServletContext()
                .getResourceAsStream(FRONTEND_ES6_BUNDLE_MANIFEST))
                .thenAnswer(i -> new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Intentionally failed");
                    }
                });

        expectedException.expect(UncheckedIOException.class);
        expectedException.expectMessage(FLOW_BUNDLE_MANIFEST);

        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void when_json_file_not_found_fails() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Failed to find");
        expectedException.expectMessage(FLOW_BUNDLE_MANIFEST);

        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void when_bundle_disabled_doesnt_fail() {
        mocks.getDeploymentConfiguration().setApplicationOrSystemProperty(
                Constants.USE_ORIGINAL_FRONTEND_RESOURCES, "true");
        new BundleFilterFactory().createFilters(mocks.getService()).count();
    }

    @Test
    public void fail_when_loading_invalid_json() {
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_BUNDLE_MANIFEST, "{ wait this is not json");

        expectedException.expect(JsonException.class);
        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void does_not_contain_main_bundle_fails() {
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_BUNDLE_MANIFEST,
                "{'fragment-1':['dependency-1', 'dependency-2']}");
        mocks.getServlet()
                .addServletContextResource("/frontend-es6/fragment-1");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("contains no main bundle");
        expectedException.expectMessage(MAIN_BUNDLE_NAME_PREFIX);
        expectedException.expectMessage(FLOW_BUNDLE_MANIFEST);

        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void multiple_bundles_with_main_bundle_prefix_throws() {
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_BUNDLE_MANIFEST,
                String.format("{'%s':['dependency-1'], '%s': ['dependency-2']}",
                        HASHED_BUNDLE_NAME, NON_HASHED_BUNDLE_NAME));
        mocks.getServlet().addServletContextResource(
                "/frontend-es6/" + HASHED_BUNDLE_NAME);
        mocks.getServlet().addServletContextResource(
                "/frontend-es6/" + NON_HASHED_BUNDLE_NAME);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                "contains multiple bundle files with name that starts with");
        expectedException.expectMessage(MAIN_BUNDLE_NAME_PREFIX);
        expectedException.expectMessage(FLOW_BUNDLE_MANIFEST);

        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void bundle_file_not_found_throws() {
        String missingFragment = "fragment-1";
        mocks.getServlet().addServletContextResource(
                FRONTEND_ES6_BUNDLE_MANIFEST,
                String.format("{'%s':['dependency-1']}", missingFragment));

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(FLOW_BUNDLE_MANIFEST);
        expectedException.expectMessage(missingFragment);

        new BundleFilterFactory().createFilters(mocks.getService());
    }

    @Test
    public void empty_bundle_manifest_stream_no_dependency_filter_added() {
        mocks.getServlet()
                .addServletContextResource(FRONTEND_ES6_BUNDLE_MANIFEST, "{}");
        mocks.getServlet()
                .addServletContextResource(FRONTEND_ES5_BUNDLE_MANIFEST, "{}");

        long filterCount = new BundleFilterFactory()
                .createFilters(mocks.getService()).count();
        Assert.assertEquals(0, filterCount);
    }

    @Test
    public void happy_path_no_hash() {
        serviceInit(NON_HASHED_BUNDLE_NAME);
    }

    @Test
    public void happy_path_with_hash() {
        serviceInit(HASHED_BUNDLE_NAME);
    }

    private void serviceInit(String bundleName) {

        String json = String
                .format("{'fragment-1':['dependency-1', 'dependency-2'],"
                        + "'fragment-2':['dependency-3', 'dependency-4'],"
                        + "'%s':['dependency-0', 'dependency-5']}", bundleName);

        mocks.getServlet()
                .addServletContextResource(FRONTEND_ES6_BUNDLE_MANIFEST, json);
        mocks.getServlet()
                .addServletContextResource(FRONTEND_ES5_BUNDLE_MANIFEST, json);

        for (String frontend : Arrays.asList("/frontend-es6/",
                "/frontend-es5/")) {
            for (int i = 0; i < 6; i++) {
                mocks.getServlet().addServletContextResource("dependency-" + i);
            }
            mocks.getServlet()
                    .addServletContextResource(frontend + "fragment-1");
            mocks.getServlet()
                    .addServletContextResource(frontend + "fragment-2");
            mocks.getServlet().addServletContextResource(frontend + bundleName);
        }

        List<BundleDependencyFilter> filters = new BundleFilterFactory()
                .createFilters(mocks.getService()).collect(Collectors.toList());

        // BundleDependencyFilter for ES6 and ES5 should be added
        Assert.assertEquals(filters.size(), 2);

        List<Dependency> dependencies = IntStream.range(0, 6)
                .mapToObj(number -> new Dependency(Dependency.Type.HTML_IMPORT,
                        "dependency-" + number, LoadMode.EAGER))
                .collect(Collectors.toList());

        List<Dependency> filtered = Stream
                .of(bundleName, "fragment-1", "fragment-2")
                .map(url -> new Dependency(Dependency.Type.HTML_IMPORT, url,
                        LoadMode.EAGER))
                .collect(Collectors.toList());

        // First filter in list is ES6, it should filter dependencies only when
        // browser is ES6
        List<Dependency> filteredResult = filters.get(0).filter(dependencies,
                new FilterContext(null, FakeBrowser.getEs6()));
        List<Dependency> unfilteredResult = filters.get(0).filter(dependencies,
                new FilterContext(null, FakeBrowser.getEs5()));

        Assert.assertTrue(filteredResult.containsAll(filtered));
        Assert.assertEquals(filteredResult.size(), filtered.size());
        Assert.assertEquals(unfilteredResult, dependencies);

        // Second filter in list is ES5, it should filter dependencies only when
        // browser is ES5
        filteredResult = filters.get(1).filter(dependencies,
                new FilterContext(null, FakeBrowser.getEs5()));
        unfilteredResult = filters.get(1).filter(dependencies,
                new FilterContext(null, FakeBrowser.getEs6()));

        Assert.assertTrue(filteredResult.containsAll(filtered));
        Assert.assertEquals(filteredResult.size(), filtered.size());
        Assert.assertEquals(unfilteredResult, dependencies);
    }
}
