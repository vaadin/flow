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
package com.vaadin.flow.template.angular.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.template.angular.ChildSlotNode;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TextTemplateNode;
import com.vaadin.flow.template.angular.parser.TemplateParser;
import com.vaadin.flow.template.angular.parser.TemplateResolver;

public class DefaultTextModelBuiderFactoryTest {

    private static class TestTemplateResolver implements TemplateResolver {

        private List<String> resolvedPaths = new ArrayList<>();

        @Override
        public InputStream resolve(String filename) throws IOException {
            resolvedPaths.add(filename.trim());
            return new ByteArrayInputStream(
                    "<li>".getBytes(StandardCharsets.UTF_8));
        }

        void verify(String... paths) {
            Assert.assertEquals(paths.length, resolvedPaths.size());
            Assert.assertEquals(Arrays.asList(paths), resolvedPaths);
        }
    }

    @Test
    public void parseSimpleInclude() {
        assertIncludePath("@include foo.html@", "foo.html");
        assertIncludePath("@include foo.html @", "foo.html");
        assertIncludePath("@include foo.html @ ", "foo.html");
        assertIncludePath(" @include foo.html @ ", "foo.html");

        assertIncludePath("@include foo/bar.html@", "foo/bar.html");
        assertIncludePath("@include foo bar.html@", "foo bar.html");
        assertIncludePath("@include fooäbar.html@", "fooäbar.html");
    }

    @Test
    public void parseMultipleInclude() {
        assertIncludePath(" @include foo.html@  @include bar.html @ ",
                "foo.html", "bar.html");
    }

    @Test
    public void parseStaticBinding() {
        List<TemplateNode> nodes = getNodes("sdfdsf sdf fgh@");
        Assert.assertEquals(1, nodes.size());
        verifyStaticText(nodes, 0, "sdfdsf sdf fgh@");
    }

    @Test
    public void parseChildNode() {
        List<TemplateNode> nodes = getNodes("@child@");
        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals(ChildSlotNode.class, nodes.get(0).getClass());
    }

    @Test
    public void parseTextBinding() {
        List<TemplateNode> nodes = getNodes("{{text}}");
        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals(TextTemplateNode.class, nodes.get(0).getClass());
    }

    @Test
    public void parseInclude_allDirectives() {
        List<TemplateNode> nodes = assertIncludePath(
                "sdf @include foo.html@ {{text}} dfg @include bar.html@ @child@",
                "foo.html", "bar.html");
        Assert.assertEquals(8, nodes.size());

        verifyStaticText(nodes, 0, "sdf ");

        Assert.assertEquals(ElementTemplateNode.class, nodes.get(1).getClass());

        verifyStaticText(nodes, 2, " ");

        Assert.assertEquals(TextTemplateNode.class, nodes.get(3).getClass());

        verifyStaticText(nodes, 4, " dfg ");

        Assert.assertEquals(ElementTemplateNode.class, nodes.get(5).getClass());

        verifyStaticText(nodes, 6, " ");

        Assert.assertEquals(ChildSlotNode.class, nodes.get(7).getClass());
    }

    @Test
    public void parseInclude_directivesMixture() {
        List<TemplateNode> nodes = assertIncludePath(
                "@include {{text}}dfg@ {{xzc @include}} include @child@",
                "{{text}}dfg");
        Assert.assertEquals(5, nodes.size());

        Assert.assertEquals(ElementTemplateNode.class, nodes.get(0).getClass());

        verifyStaticText(nodes, 1, " ");

        Assert.assertEquals(TextTemplateNode.class, nodes.get(2).getClass());

        verifyStaticText(nodes, 3, " include ");

        Assert.assertEquals(ChildSlotNode.class, nodes.get(4).getClass());
    }

    @Test
    public void parseNotInclude() {
        TemplateNode node = TemplateParser
                .parse("<div>foo@include.com, baz@foo.com</div>", null);
        Assert.assertEquals(1, node.getChildCount());
        TemplateNode child = node.getChild(0);
        Assert.assertTrue(child instanceof TextTemplateNode);
        TextTemplateNode text = (TextTemplateNode) child;
        Assert.assertEquals("foo@include.com, baz@foo.com",
                text.getTextBinding().getValue(null));
    }

    private void verifyStaticText(List<TemplateNode> nodes, int index,
            String expected) {
        Assert.assertEquals(TextTemplateNode.class,
                nodes.get(index).getClass());
        TextTemplateNode textNode = (TextTemplateNode) nodes.get(index);
        Assert.assertEquals(expected, textNode.getTextBinding().getValue(null));
    }

    private List<TemplateNode> getNodes(String text) {
        return getNodesAndCheckIncludePath(text, false);
    }

    private List<TemplateNode> assertIncludePath(String includeText,
            String... expected) {
        return getNodesAndCheckIncludePath(includeText, true, expected);
    }

    private List<TemplateNode> getNodesAndCheckIncludePath(String includeText,
            boolean verify, String... expected) {
        TestTemplateResolver resolver = new TestTemplateResolver();
        TemplateNode rootNode = TemplateParser
                .parse("<div>" + includeText + "</div>", resolver);

        List<TemplateNode> nodes = new ArrayList<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            nodes.add(rootNode.getChild(i));
        }

        if (verify) {
            resolver.verify(expected);
        }
        return nodes;
    }
}
