/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.ui;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import elemental.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class DependencyTest {

    @Test
    public void checkJsonSerialization_3ArgsCTor() {
        Dependency dependency = new Dependency(Dependency.Type.JAVASCRIPT,
                "url", LoadMode.INLINE);

        assertDependency(dependency);
    }

    @Test
    public void dynamicDependency_hasLazyMode() {
        Dependency dependency = new Dependency(Dependency.Type.DYNAMIC_IMPORT,
                "foo");

        // It's important that the load mode of the dependency is Lazy because
        // any other mode is not sent to the client at all when it's added at
        // the initial request: it's processed by the bootstrap handler via
        // adding an element into the document head right away (no client side
        // processing is involved).
        assertThat(dependency.getLoadMode(),
                CoreMatchers.equalTo(LoadMode.LAZY));
    }

    @Test
    public void checkJsonSerialization_2ArgsCTor() {
        Dependency dependency = new Dependency(Dependency.Type.DYNAMIC_IMPORT,
                "foo");

        assertDependency(dependency);

    }

    private void assertDependency(Dependency dependency) {
        JsonObject dependencyJson = dependency.toJson();

        assertThat("No contents should be present in json now",
                dependencyJson.hasKey(Dependency.KEY_CONTENTS), is(false));
        assertThat(
                "Dependency type should match corresponding enum name in pojo",
                dependencyJson.getString(Dependency.KEY_TYPE),
                is(dependency.getType().name()));
        assertThat("Dependency url should match corresponding url in pojo",
                dependencyJson.getString(Dependency.KEY_URL),
                is(dependency.getUrl()));
        assertThat(
                "Dependency load mode should match corresponding enum name in pojo",
                dependencyJson.getString(Dependency.KEY_LOAD_MODE),
                is(dependency.getLoadMode().name()));
    }

}
