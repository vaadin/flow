/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.vaadin.spring.annotation.SpringView;

/**
 */
public class ConventionsTest {

    // static test classes

    @SpringView
    public static class SimpleView {
    }

    @SpringView
    public static class SimpleViewWithoutViewName {
    }

    @SpringView(name = "simpleview")
    public static class SimpleViewWithViewName {
    }

    // test methods

    @Test
    public void extractViewNameUsingPath() {
        String expected = "simpleview";
        String actual = deriveMappingForView(SimpleViewWithViewName.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractViewNameUsingConvention() {
        String expected = "simple-view-without-view-name";
        String actual = deriveMappingForView(SimpleViewWithoutViewName.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractViewNameUsingConventionStrippingView() {
        String expected = "simple";
        String actual = deriveMappingForView(SimpleView.class);
        assertThat(actual, is(expected));
    }

    public String deriveMappingForView(Class<?> beanClass) {
        SpringView annotation = beanClass.getAnnotation(SpringView.class);
        return Conventions.deriveMappingForView(beanClass, annotation);
    }

    @Test
    public void upperCamelCaseToLowerHyphenatedTest() {
        String original = "AlphaBetaGamma";
        String expected = "alpha-beta-gamma";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "alphaBetaGamma";
        expected = "alpha-beta-gamma";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "";
        expected = "";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "a";
        expected = "a";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "A";
        expected = "a";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "ABC";
        expected = "abc";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "alllowercase";
        expected = "alllowercase";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "main/sub";
        expected = "main/sub";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyCAPSTest";
        expected = "my-caps-test";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyATest";
        expected = "my-a-test";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "aB";
        expected = "a-b";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "Ab";
        expected = "ab";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyTEST";
        expected = "my-test";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));
    }

}