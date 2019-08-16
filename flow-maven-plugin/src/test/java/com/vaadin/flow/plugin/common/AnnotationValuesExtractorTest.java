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

package com.vaadin.flow.plugin.common;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

import org.apache.maven.plugins.annotations.Mojo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.plugin.TestUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class AnnotationValuesExtractorTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    ClassFinder finder = new ReflectionsClassFinder(
            TestUtils.getTestResource(TestUtils.SERVER_JAR),
            TestUtils.getTestResource(
                    "annotation-extractor-test/vaadin-grid-flow.jar"),
            TestUtils.getTestResource(TestUtils.DATA_JAR));

    private final AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(
            finder);

    @Test
    public void extractAnnotationValues_incorrectMethod() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("");

        extractor.extractAnnotationValues(
                Collections.singletonMap(HtmlImport.class, "doomed to fail"));
    }

    @Test
    public void extractAnnotationValues_annotationNotInClassLoader() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("");

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

}
