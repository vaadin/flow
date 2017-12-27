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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DependencyFilter.FilterContext;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;

public class BundleDependencyFilterTest {

    BundleDependencyFilter dependencyFilter;
    FilterContext filterContext;

    @Before
    public void init() {
        dependencyFilter = new BundleDependencyFilter(createTestMapping());
        filterContext = Mockito.mock(FilterContext.class);
    }

    @Test(expected = NullPointerException.class)
    public void initialization_no_mapping_given() {
        new BundleDependencyFilter(null);
    }

    @Test
    public void import_in_fragment_filtered() {
        List<Dependency> dependencyWhichIsInFragment = Collections
                .singletonList(createDependency("in fragment 1"));
        List<Dependency> filteredFragmentDependency = dependencyFilter
                .filter(dependencyWhichIsInFragment, filterContext);

        Assert.assertEquals(
                "Should contain the main bundle as the first dependency and fragment as second.",
                Arrays.asList(
                        createMainBundleDependency(),
                        createDependency("fragment 1")),
                filteredFragmentDependency);
    }

    @Test
    public void two_dependencies_same_bundle() {
        List<Dependency> twoImportsThatAreInTheMainBundle = Arrays.asList(
                createDependency("in main bundle"),
                createDependency("also in main bundle"));
        List<Dependency> filteredTwoMainBundleImports = dependencyFilter
                .filter(twoImportsThatAreInTheMainBundle, filterContext);

        Assert.assertEquals(
                Collections.singletonList(createMainBundleDependency()),
                filteredTwoMainBundleImports);
    }

    @Test
    public void main_bundle_should_always_be_returned_first() {
        List<Dependency> toFilter = Arrays.asList(
                createDependency("in fragment 1"),
                createDependency("in fragment 2"),
                createDependency("in main bundle"));
        List<Dependency> filtered = dependencyFilter.filter(toFilter,
                filterContext);

        Assert.assertEquals(3, filtered.size());
        Assert.assertEquals(createMainBundleDependency(), filtered.get(0));
    }

    @Test
    public void filtering_returns_main_bundle_and_fragment() {
        List<Dependency> toFilter = Arrays.asList(
                createDependency("in main bundle"),
                createDependency("in fragment 1"));
        List<Dependency> filtered = dependencyFilter.filter(toFilter,
                filterContext);

        Assert.assertEquals(Arrays.asList(createMainBundleDependency(),
                createDependency("fragment 1")), filtered);
    }

    @Test
    public void non_bundled_dependency_passed_through() {
        List<Dependency> dependencies = Collections
                .singletonList(createDependency("not in bundle map"));
        List<Dependency> filteredDependencies = dependencyFilter
                .filter(dependencies, filterContext);

        Assert.assertEquals(
                "Dependencies not mapped in any import to bundle mapping should be passed through as is.",
                dependencies, filteredDependencies);
    }

    private Dependency createDependency(String url) {
        return new Dependency(Type.HTML_IMPORT, url, LoadMode.EAGER);
    }

    private Dependency createMainBundleDependency() {
        return createDependency(BundleDependencyFilter.MAIN_BUNDLE_URL);
    }

    private Map<String, Set<String>> createTestMapping() {
        Map<String, Set<String>> importContainedInBundles = new HashMap<>();
        importContainedInBundles.put("in main bundle", Collections
                .singleton(BundleDependencyFilter.MAIN_BUNDLE_URL));
        importContainedInBundles.put("also in main bundle", Collections
                .singleton(BundleDependencyFilter.MAIN_BUNDLE_URL));
        importContainedInBundles.put("in fragment 1",
                Collections.singleton("fragment 1"));
        importContainedInBundles.put("in fragment 2",
                Collections.singleton("fragment 2"));
        return importContainedInBundles;
    }
}
