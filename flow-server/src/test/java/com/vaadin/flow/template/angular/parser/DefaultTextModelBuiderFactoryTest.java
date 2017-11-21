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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.vaadin.flow.template.angular.ChildSlotNode;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TextTemplateNode;

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
            assertEquals(paths.length, resolvedPaths.size());
            assertEquals(Arrays.asList(paths), resolvedPaths);
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
        assertEquals(1, nodes.size());
        verifyStaticText(nodes, 0, "sdfdsf sdf fgh@");
    }

    @Test
    public void parseChildNode() {
        List<TemplateNode> nodes = getNodes("@child@");
        assertEquals(1, nodes.size());
        assertEquals(ChildSlotNode.class, nodes.get(0).getClass());
    }

    @Test
    public void parseTextBinding() {
        List<TemplateNode> nodes = getNodes("{{text}}");
        assertEquals(1, nodes.size());
        assertEquals(TextTemplateNode.class, nodes.get(0).getClass());
    }

    @Test
    public void parseInclude_allDirectives() {
        List<TemplateNode> nodes = assertIncludePath(
                "sdf @include foo.html@ {{text}} dfg @include bar.html@ @child@",
                "foo.html", "bar.html");
        assertEquals(8, nodes.size());

        verifyStaticText(nodes, 0, "sdf ");

        assertEquals(ElementTemplateNode.class, nodes.get(1).getClass());

        verifyStaticText(nodes, 2, " ");

        assertEquals(TextTemplateNode.class, nodes.get(3).getClass());

        verifyStaticText(nodes, 4, " dfg ");

        assertEquals(ElementTemplateNode.class, nodes.get(5).getClass());

        verifyStaticText(nodes, 6, " ");

        assertEquals(ChildSlotNode.class, nodes.get(7).getClass());
    }

    @Test
    public void parseInclude_directivesMixture() {
        List<TemplateNode> nodes = assertIncludePath(
                "@include {{text}}dfg@ {{xzc @include}} include @child@",
                "{{text}}dfg");
        assertEquals(5, nodes.size());

        assertEquals(ElementTemplateNode.class, nodes.get(0).getClass());

        verifyStaticText(nodes, 1, " ");

        assertEquals(TextTemplateNode.class, nodes.get(2).getClass());

        verifyStaticText(nodes, 3, " include ");

        assertEquals(ChildSlotNode.class, nodes.get(4).getClass());
    }

    @Test
    public void parseNotInclude() {
        TemplateNode node = TemplateParser
                .parse("<div>foo@include.com, baz@foo.com</div>", null);
        assertEquals(1, node.getChildCount());
        TemplateNode child = node.getChild(0);
        assertTrue(child instanceof TextTemplateNode);
        TextTemplateNode text = (TextTemplateNode) child;
        assertEquals("foo@include.com, baz@foo.com",
                text.getTextBinding().getValue(null));
    }

    private void verifyStaticText(List<TemplateNode> nodes, int index,
            String expected) {
        assertEquals(TextTemplateNode.class,
                nodes.get(index).getClass());
        TextTemplateNode textNode = (TextTemplateNode) nodes.get(index);
        assertEquals(expected, textNode.getTextBinding().getValue(null));
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
