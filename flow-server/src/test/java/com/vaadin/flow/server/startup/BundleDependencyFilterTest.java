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

import static com.vaadin.flow.server.startup.BundleFilterFactory.MAIN_BUNDLE_NAME_PREFIX;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.server.DependencyFilter.FilterContext;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;

public class BundleDependencyFilterTest {
    private static final String NON_HASHED_BUNDLE_NAME = MAIN_BUNDLE_NAME_PREFIX
            + ".html";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private BundleDependencyFilter dependencyFilter;
    private FilterContext filterContext;

    @Before
    public void init() {
        dependencyFilter = new BundleDependencyFilter(
                FakeBrowser.getEs6(), NON_HASHED_BUNDLE_NAME, createTestMapping());
        filterContext = new FilterContext(null, FakeBrowser.getEs6());
    }

    @Test
    public void initialization_no_mapping_given() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("bundle mapping");

        new BundleDependencyFilter(FakeBrowser.getEs6(), NON_HASHED_BUNDLE_NAME, null);
    }

    @Test
    public void initialization_no_main_bundle_name_given() {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Main bundle name");

        new BundleDependencyFilter(FakeBrowser.getEs6(), null, Collections.emptyMap());
    }

    @Test
    public void import_in_fragment_filtered() {
        List<Dependency> dependencyWhichIsInFragment = Collections
                .singletonList(createDependency("in fragment 1"));

        List<Dependency> filteredFragmentDependency = dependencyFilter
                .filter(dependencyWhichIsInFragment, filterContext);

        Assert.assertEquals(
                "Should contain the main bundle as the first dependency and fragment as second.",
                Arrays.asList(createMainBundleDependency(),
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
        List<Dependency> toFilter = Collections
                .singletonList(createDependency("not in bundle map"));

        List<Dependency> filtered = dependencyFilter.filter(toFilter,
                filterContext);

        Assert.assertEquals(
                "Dependencies not mapped in any import to bundle mapping should be passed through as is.",
                toFilter, filtered);
    }

    @Test
    public void main_bundle_added_if_fragments_present_and_non_fragment_imports_go_last() {
        Dependency dependencyNotInFragment = createDependency(
                "not in bundle map");
        List<Dependency> toFilter = Arrays.asList(
                createDependency("in fragment 1"), dependencyNotInFragment,
                createDependency("in fragment 2"));

        List<Dependency> filtered = dependencyFilter.filter(toFilter,
                filterContext);

        Assert.assertEquals(
                "When dependency from any fragment is returned after filtering, main bundle should be also returned even if not explicitly mentioned",
                toFilter.size() + 1, filtered.size());
        Assert.assertEquals("Main bundle should always go first",
                createMainBundleDependency(), filtered.get(0));
        Assert.assertEquals(
                "Dependencies not in fragments should go after fragments",
                dependencyNotInFragment, filtered.get(filtered.size() - 1));
        Assert.assertTrue("Fragments should also be in filtered results",
                filtered.containsAll(
                        Arrays.asList(createDependency("fragment 1"),
                                createDependency("fragment 2"))));
    }

    private Dependency createDependency(String url) {
        return new Dependency(Type.HTML_IMPORT, url, LoadMode.EAGER);
    }

    private Dependency createMainBundleDependency() {
        return createDependency(NON_HASHED_BUNDLE_NAME);
    }

    private Map<String, Set<String>> createTestMapping() {
        Map<String, Set<String>> importContainedInBundles = new HashMap<>();
        importContainedInBundles.put("in main bundle",
                Collections.singleton(NON_HASHED_BUNDLE_NAME));
        importContainedInBundles.put("also in main bundle",
                Collections.singleton(NON_HASHED_BUNDLE_NAME));
        importContainedInBundles.put("in fragment 1",
                Collections.singleton("fragment 1"));
        importContainedInBundles.put("in fragment 2",
                Collections.singleton("fragment 2"));
        return importContainedInBundles;
    }
}
