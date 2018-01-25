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

package com.vaadin.flow.plugin.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugins.annotations.Mojo;
import org.junit.Test;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.plugin.TestUtils;

/**
 * @author Vaadin Ltd.
 */
public class AnnotationValuesExtractorTest {
    private final AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
            TestUtils.getTestResource(
                    "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
            TestUtils.getTestResource(
                    "annotation-extractor-test/vaadin-grid-flow.jar"),
            TestUtils.getTestResource(
                    "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar"));

    @Test(expected = IllegalArgumentException.class)
    public void extractAnnotationValues_incorrectMethod() {
        extractor.extractAnnotationValues(
                Collections.singletonMap(HtmlImport.class, "doomed to fail"));
    }

    @Test(expected = IllegalStateException.class)
    public void extractAnnotationValues_annotationNotInClassLoader() {
        extractor.extractAnnotationValues(
                Collections.singletonMap(Mojo.class, "whatever"));
    }

    @Test
    public void extractAnnotationValues_missingAnnotation() {
        Map<Class<? extends Annotation>, String> input = Collections
                .singletonMap(StyleSheet.class, "whatever");

        Map<Class<? extends Annotation>, Set<String>> result = extractor
                .extractAnnotationValues(input);

        assertEquals("Keys in input and result should always match",
                input.keySet(), result.keySet());
        assertTrue(
                "Test jar should not contain annotations from input hence nothing should be extracted",
                result.get(input.keySet().iterator().next()).isEmpty());
    }

    @Test
    public void extractAnnotationValues_singleAnnotation() {
        Map<Class<? extends Annotation>, String> input = Collections
                .singletonMap(JavaScript.class, "value");

        Map<Class<? extends Annotation>, Set<String>> result = extractor
                .extractAnnotationValues(input);

        assertEquals("Keys in input and result should always match",
                input.keySet(), result.keySet());
        assertThat(
                "Test jar should contain single JavaScript annotations from input and it should be extracted",
                result.get(input.keySet().iterator().next()).size(), is(1));
    }

    @Test
    public void extractAnnotationValues_repeatedAnnotation() {
        Map<Class<? extends Annotation>, String> input = Collections
                .singletonMap(HtmlImport.class, "value");

        Map<Class<? extends Annotation>, Set<String>> result = extractor
                .extractAnnotationValues(input);

        assertEquals("Keys in input and result should always match",
                input.keySet(), result.keySet());
        assertTrue(
                "Test jar should contain repeated annotations from input and they should be extracted",
                result.get(input.keySet().iterator().next()).size() > 1);
    }

    @Test
    public void collectThemedHtmlImports_twoThemesDirectlyOnComponents_throw() {
        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-collision-two-componnets.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar"));
        extractor.collectThemedHtmlImports((theme, set) -> {
        });
    }

    @Test
    public void collectThemedHtmlImports_twoThemes_themeIsDiscoveredViaParentLayout_throw() {
        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-collision-via-parent-layout.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar"));
        extractor.collectThemedHtmlImports((theme, set) -> {
        });
    }

    @Test
    public void collectThemedHtmlImports_twoThemesDiscoveredViaRoute_throw() {
        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-collision-via-route.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar"));
        extractor.collectThemedHtmlImports((theme, set) -> {
        });
    }

    @Test
    public void collectThemedHtmlImports_twoThemesDiscoveredViaRouteAlias_throw() {
        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-collision-via-alias.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar"));
        extractor.collectThemedHtmlImports((theme, set) -> {
        });
    }
}
