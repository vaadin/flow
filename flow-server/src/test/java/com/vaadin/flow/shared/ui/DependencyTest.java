/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.shared.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
class DependencyTest {

    @Test
    void checkJsonSerialization_3ArgsCTor() {
        Dependency dependency = new Dependency(Dependency.Type.JAVASCRIPT,
                "url", LoadMode.INLINE);

        assertDependency(dependency);
    }

    @Test
    void dynamicDependency_hasLazyMode() {
        Dependency dependency = new Dependency(Dependency.Type.DYNAMIC_IMPORT,
                "foo");

        // It's important that the load mode of the dependency is Lazy because
        // any other mode is not sent to the client at all when it's added at
        // the initial request: it's processed by the bootstrap handler via
        // adding an element into the document head right away (no client side
        // processing is involved).
        assertThat(dependency.getLoadMode(), equalTo(LoadMode.LAZY));
    }

    @Test
    void functionDependency_lazyDynamicImportKeptByCopy() {
        Dependency dependency = new Dependency(
                JsFunction.of("return Promise.resolve($0)", "a"));

        assertEquals(Dependency.Type.DYNAMIC_IMPORT, dependency.getType());
        // Lazy for the same reason as dynamicDependency_hasLazyMode
        assertEquals(LoadMode.LAZY, dependency.getLoadMode());
        assertDependency(dependency);
        // The function is sent once, as the url
        assertFalse(JacksonUtils.createNode(dependency)
                .has("encodedDynamicImport"));
        // As a dependency filter or DependencyList copies a dependency
        Dependency copy = new Dependency(dependency.getType(),
                dependency.getUrl(), dependency.getLoadMode());
        assertEquals("a", copy.getEncodedDynamicImport().get("@v-fn")
                .get("captures").get(0).asString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "return import('a.js')",
            // A block is a valid function body that starts like JSON
            "{ return import('a.js'); }",
            // JSON, but not an encoded function
            "{\"other\":1}" })
    void expression_hasNoEncodedFunction(String expression) {
        assertNull(new Dependency(Dependency.Type.DYNAMIC_IMPORT, expression)
                .getEncodedDynamicImport());
    }

    @Test
    void encodedFunctionWithWhitespace_isRecognized() {
        // Depends only on the key, not on how the JSON is written
        Dependency dependency = new Dependency(Dependency.Type.DYNAMIC_IMPORT,
                "{ \"" + JacksonCodec.FUNCTION_KEY
                        + "\" : { \"id\": \"f\", \"captures\": [] } }");

        assertEquals("f", dependency.getEncodedDynamicImport()
                .get(JacksonCodec.FUNCTION_KEY).get("id").asString());
    }

    @Test
    void otherTypeWithEncodedFunctionUrl_hasNoEncodedFunction() {
        assertNull(new Dependency(Dependency.Type.JAVASCRIPT,
                "{\"" + JacksonCodec.FUNCTION_KEY + "\":{}}")
                .getEncodedDynamicImport());
    }

    @Test
    void checkJsonSerialization_2ArgsCTor() {
        Dependency dependency = new Dependency(Dependency.Type.DYNAMIC_IMPORT,
                "foo");

        assertDependency(dependency);

    }

    @Test
    void layer_partOfEquality_serializedOnlyWhenSet() {
        Dependency plain = new Dependency(Dependency.Type.STYLESHEET, "a.css",
                LoadMode.EAGER, "id");
        Dependency layered = new Dependency(Dependency.Type.STYLESHEET, "a.css",
                LoadMode.EAGER, "id", "theme");

        assertNotEquals(plain, layered);
        assertEquals(layered, new Dependency(Dependency.Type.STYLESHEET,
                "a.css", LoadMode.EAGER, "id", "theme"));
        assertTrue(layered.toString().contains("layer=theme"));
        assertFalse(JacksonUtils.createNode(plain).has(Dependency.KEY_LAYER));
        assertEquals("theme", JacksonUtils.createNode(layered)
                .get(Dependency.KEY_LAYER).asString());
    }

    @ParameterizedTest
    @EnumSource(value = Dependency.Type.class, names = "STYLESHEET", mode = EnumSource.Mode.EXCLUDE)
    void layer_notStyleSheet_throws(Dependency.Type type) {
        assertThrows(IllegalArgumentException.class,
                () -> new Dependency(type, "a", LoadMode.EAGER, null, "theme"));
    }

    private void assertDependency(Dependency dependency) {
        JsonNode dependencyJson = JacksonUtils.createNode(dependency);

        assertThat("No contents should be present in json now",
                dependencyJson.has(Dependency.KEY_CONTENTS), is(false));
        assertThat(
                "Dependency type should match corresponding enum name in pojo",
                dependencyJson.get(Dependency.KEY_TYPE).asText(),
                is(dependency.getType().name()));
        assertThat("Dependency url should match corresponding url in pojo",
                dependencyJson.get(Dependency.KEY_URL).asText(),
                is(dependency.getUrl()));
        assertThat(
                "Dependency load mode should match corresponding enum name in pojo",
                dependencyJson.get(Dependency.KEY_LOAD_MODE).asText(),
                is(dependency.getLoadMode().name()));
    }

}
