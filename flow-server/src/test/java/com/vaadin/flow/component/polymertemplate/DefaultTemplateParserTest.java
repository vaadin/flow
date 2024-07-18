/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplateTest.ModelClass;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class DefaultTemplateParserTest {

    private MockServletServiceSessionSetup mocks;
    private VaadinService service;

    @Tag("foo")
    private static class ImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Tag("foo")
    private static class RootImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Tag("bar")
    private static class OtherImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        ServletContext servletContext = mocks.getServletContext();
        Mockito.when(servletContext.getResource("/bar.html"))
                .thenReturn(new URL("file://bar.html"));
        Mockito.when(servletContext.getResourceAsStream("/bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='bar'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));
        Mockito.when(servletContext.getResource("/bar1.html"))
                .thenReturn(new URL("file://bar1.html"));
        Mockito.when(servletContext.getResourceAsStream("/bar1.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='foo'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        Mockito.when(servletContext.getResource("/bundle.html"))
                .thenReturn(new URL("file://bundle.html"));
        Mockito.when(servletContext.getResourceAsStream("/bundle.html"))
                .thenReturn(getBundle(), getBundle(), getBundle());
    }

    private ByteArrayInputStream getBundle() {
        return new ByteArrayInputStream(
                "<dom-module id='bar'></dom-module><dom-module id='foo'></dom-module>"
                        .getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Ignore
    @Test
    public void defaultParser_hasTemplate_returnsContent() {
        TemplateData data = DefaultTemplateParser.getInstance()
                .getTemplateContent(ImportsInspectTemplate.class, "foo",
                        service);
        Element element = data.getTemplateElement();

        Assert.assertTrue(element.getElementById("foo") != null);
        Assert.assertEquals("/bar1.html", data.getHtmlImportUri());
    }

    @Ignore
    @Test
    public void defaultParser_templateWithLeadingSlash_returnsContent() {
        Mockito.when(mocks.getServletContext().getResourceAsStream("/bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='foo'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        Element element = DefaultTemplateParser.getInstance()
                .getTemplateContent(RootImportsInspectTemplate.class, "foo",
                        service)
                .getTemplateElement();

        Assert.assertTrue(element.getElementById("foo") != null);
    }

    @Ignore
    @Test
    public void defaultParser_servletPathIsEmpty_returnsContent() {
        Element element = DefaultTemplateParser.getInstance()
                .getTemplateContent(ImportsInspectTemplate.class, "foo",
                        service)
                .getTemplateElement();

        Assert.assertTrue(element.getElementById("foo") != null);
    }

    @Ignore
    @Test
    public void defaultParser_servletPathIsNotEmpty_returnsContent() {
        Mockito.when(mocks.getServletContext().getResourceAsStream("/bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='bar'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));
        Mockito.when(
                mocks.getServletContext().getResourceAsStream("/bar1.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='foo'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        Element element = DefaultTemplateParser.getInstance()
                .getTemplateContent(ImportsInspectTemplate.class, "foo",
                        service)
                .getTemplateElement();

        Assert.assertTrue(element.getElementById("foo") != null);
    }

    @Ignore
    @Test
    public void defaultParser_removesComments() {
        Mockito.when(mocks.getServletContext().getResourceAsStream("/bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<!-- comment1 --><dom-module id='foo'><!-- comment2 --></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        Element element = DefaultTemplateParser.getInstance()
                .getTemplateContent(ImportsInspectTemplate.class, "foo",
                        service)
                .getTemplateElement();
        Assert.assertTrue(element.getElementById("foo") != null);
        assertThat("No comments should be present in the parsing result",
                extractCommentNodes(element), is(empty()));
    }

    private static List<Node> extractCommentNodes(Node parent) {
        return parent.childNodes().stream()
                .flatMap(child -> Stream.concat(Stream.of(child),
                        extractCommentNodes(child).stream()))
                .filter(node -> node instanceof Comment)
                .collect(Collectors.toList());
    }

    @Ignore
    @Test(expected = IllegalStateException.class)
    public void defaultParser_noTemplate_throws() {
        DefaultTemplateParser.getInstance().getTemplateContent(
                ImportsInspectTemplate.class, "bar1", service);
    }

    @Test(expected = IllegalStateException.class)
    public void defaultParser_templateResourceIsNotFound_throws() {
        Mockito.when(
                mocks.getServletContext().getResourceAsStream("/bar1.html"))
                .thenReturn(null);

        DefaultTemplateParser.getInstance().getTemplateContent(
                ImportsInspectTemplate.class, "foo", service);
    }

    @Test
    public void defaultParser_useDependencyFilters_returnBundle() {
        AtomicInteger calls = new AtomicInteger();
        DependencyFilter filter = (list, context) -> {
            list.clear();
            list.add(new Dependency(Type.HTML_IMPORT, "/bundle.html",
                    LoadMode.EAGER));
            calls.incrementAndGet();
            return list;
        };

        TestVaadinServletService service = mocks.getService();
        service.setDependencyFilters(Collections.singletonList(filter));

        TemplateParser parser = DefaultTemplateParser.getInstance();

        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo",
                        service)
                .getTemplateElement();
        Assert.assertTrue(element.getElementById("foo") != null);

        element = parser.getTemplateContent(RootImportsInspectTemplate.class,
                "foo", service).getTemplateElement();
        Assert.assertTrue(element.getElementById("foo") != null);

        element = parser.getTemplateContent(OtherImportsInspectTemplate.class,
                "bar", service).getTemplateElement();
        Assert.assertTrue(element.getElementById("bar") != null);

        Assert.assertEquals(
                "The DependencyFilter should be called exactly 3 times", 3,
                calls.get());

    }

    @Test(expected = IllegalStateException.class)
    public void defaultParser_useDependencyFilters_noDependencies_throws() {
        DependencyFilter filter = (list, context) -> {
            list.clear();
            return list;
        };

        mocks.getService()
                .setDependencyFilters(Collections.singletonList(filter));

        DefaultTemplateParser.getInstance().getTemplateContent(
                ImportsInspectTemplate.class, "foo", service);
    }

    @Test(expected = IllegalStateException.class)
    public void defaultParser_useDependencyFilters_noHtmlDependencies_throws() {
        DependencyFilter filter = (list, context) -> {
            list.clear();
            list.add(new Dependency(Type.STYLESHEET, "something.css",
                    LoadMode.EAGER));
            list.add(new Dependency(Type.JAVASCRIPT, "something.js",
                    LoadMode.EAGER));
            return list;
        };

        mocks.getService()
                .setDependencyFilters(Collections.singletonList(filter));

        DefaultTemplateParser.getInstance().getTemplateContent(
                ImportsInspectTemplate.class, "foo", service);
    }

}
