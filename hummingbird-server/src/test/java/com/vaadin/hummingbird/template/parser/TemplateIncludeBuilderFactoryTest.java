/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.parser;

import java.util.Collection;

import org.jsoup.nodes.TextNode;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.template.TemplateParseException;

public class TemplateIncludeBuilderFactoryTest {

    @Test
    public void parseSimpleInclude() {
        assertIncludePath("foo.html", "@include foo.html@");
        assertIncludePath("foo.html", "@include foo.html @");
        assertIncludePath("foo.html", "@include foo.html @ ");
        assertIncludePath("foo.html", " @include foo.html @ ");

        assertIncludePath("foo/bar.html", "@include foo/bar.html@");
        assertIncludePath("foo bar.html", "@include foo bar.html@");
        assertIncludePath("fooäbar.html", "@include fooäbar.html@");
    }

    @Test
    public void parseMutlipleInclude() {
        Collection<String> parsed = TemplateIncludeBuilderFactory
                .getIncludePaths(new TextNode(
                        " @include foo.html@ @include bar.html @", ""));
        Assert.assertEquals(2, parsed.size());
        Assert.assertTrue(parsed.contains("foo.html"));
        Assert.assertTrue(parsed.contains("bar.html"));
    }

    @Test(expected = TemplateParseException.class)
    public void parseInclude_middleIsBroken() {
        TemplateIncludeBuilderFactory.getIncludePaths(new TextNode(
                "@include foo.html@ wrong @include bar.html@", ""));
    }

    @Test(expected = TemplateParseException.class)
    public void parseInclude_endIsBroken() {
        TemplateIncludeBuilderFactory.getIncludePaths(new TextNode(
                "@include foo.html@  @include bar.html@ wrong@", ""));
    }

    @Test
    public void parseInclude_isNotInclude() {
        assertIsNotIclude("include foo.html@  @include bar.html@");
        assertIsNotIclude("@include foo.html@  @include bar.html ");
    }

    private void assertIsNotIclude(String directive) {
        Collection<String> parsed = TemplateIncludeBuilderFactory
                .getIncludePaths(new TextNode(directive, ""));
        Assert.assertEquals(0, parsed.size());

        Assert.assertFalse(new TemplateIncludeBuilderFactory()
                .canHandle(new TextNode(directive, "")));
    }

    private void assertIncludePath(String expected, String includeText) {
        Collection<String> parsed = TemplateIncludeBuilderFactory
                .getIncludePaths(new TextNode(includeText, ""));
        Assert.assertEquals(1, parsed.size());
        Assert.assertEquals(expected, parsed.iterator().next());
    }
}
