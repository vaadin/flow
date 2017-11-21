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
package com.vaadin.ui.polymertemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.DependencyFilter;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinUriResolverFactory;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.Dependency.Type;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.PolymerTemplateTest.ModelClass;
import com.vaadin.util.CurrentInstance;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class DefaultTemplateParserTest {

    @Tag("foo")
    @HtmlImport("/bar.html")
    @HtmlImport("/bar1.html")
    private static class ImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Tag("foo")
    @HtmlImport("/bar.html")
    private static class RootImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    @Tag("bar")
    @HtmlImport("/bar.html")
    private static class OtherImportsInspectTemplate
            extends PolymerTemplate<ModelClass> {

    }

    private ServletContext context;

    private VaadinUriResolver resolver;

    private VaadinService service;

    @Before
    public void setUp() {
        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        service = Mockito.mock(VaadinService.class);

        Mockito.when(service.getDependencyFilters())
                .thenReturn(Collections.emptyList());

        WrappedHttpSession wrappedSession = Mockito
                .mock(WrappedHttpSession.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(wrappedSession.getHttpSession()).thenReturn(httpSession);

        resolver = Mockito.mock(VaadinUriResolver.class);

        Mockito.when(resolver.resolveVaadinUri("/bar.html"))
                .thenReturn("bar.html");
        Mockito.when(resolver.resolveVaadinUri("/bar1.html"))
                .thenReturn("bar1.html");
        Mockito.when(resolver.resolveVaadinUri("/bundle.html"))
                .thenReturn("bundle.html");

        VaadinUriResolverFactory factory = rqst -> resolver;
        Mockito.when(session.getAttribute(VaadinUriResolverFactory.class))
                .thenReturn(factory);

        context = Mockito.mock(ServletContext.class);
        Mockito.when(httpSession.getServletContext()).thenReturn(context);

        Mockito.when(request.getWrappedSession()).thenReturn(wrappedSession);
        Mockito.when(request.getServletPath()).thenReturn("");

        Mockito.when(context.getResourceAsStream("/bar.html")).thenReturn(
                new ByteArrayInputStream("<dom-module id='bar'></dom-module>"
                        .getBytes(StandardCharsets.UTF_8)));
        Mockito.when(context.getResourceAsStream("/bar1.html")).thenReturn(
                new ByteArrayInputStream("<dom-module id='foo'></dom-module>"
                        .getBytes(StandardCharsets.UTF_8)));

        Mockito.when(context.getResourceAsStream("/bundle.html"))
                .thenReturn(getBundle(), getBundle(), getBundle());

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinSession.class, session);
        CurrentInstance.set(VaadinService.class, service);
    }

    private ByteArrayInputStream getBundle() {
        return new ByteArrayInputStream(
                "<dom-module id='bar'></dom-module><dom-module id='foo'></dom-module>"
                        .getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        CurrentInstance.set(VaadinRequest.class, null);
        CurrentInstance.set(VaadinSession.class, null);
        CurrentInstance.set(VaadinService.class, null);
    }

    @Test
    public void defaultParser_hasTemplate_returnsContent() {
        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");

        assertTrue(element.getElementById("foo") != null);
    }

    @Test
    public void defaultParser_templateWithLeadingSlash_returnsContent() {
        Mockito.when(resolver.resolveVaadinUri("/bar.html"))
                .thenReturn("/foo.html");

        Mockito.when(context.getResourceAsStream("/foo.html")).thenReturn(
                new ByteArrayInputStream("<dom-module id='foo'></dom-module>"
                        .getBytes(StandardCharsets.UTF_8)));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(RootImportsInspectTemplate.class, "foo");

        assertTrue(element.getElementById("foo") != null);
    }

    @Test
    public void defaultParser_servletPathIsEmpty_returnsContent() {
        VaadinServletRequest request = (VaadinServletRequest) CurrentInstance
                .get(VaadinRequest.class);

        Mockito.when(request.getServletPath()).thenReturn("");

        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");

        assertTrue(element.getElementById("foo") != null);
    }

    @Test
    public void defaultParser_servletPathIsNotEmpty_returnsContent() {
        VaadinServletRequest request = (VaadinServletRequest) CurrentInstance
                .get(VaadinRequest.class);
        Mockito.when(resolver.resolveVaadinUri("bar.html"))
                .thenReturn("./../bar.html");
        Mockito.when(resolver.resolveVaadinUri("bar1.html"))
                .thenReturn("./../bar1.html");

        Mockito.when(request.getServletPath()).thenReturn("/run/");

        Mockito.when(context.getResourceAsStream("/run/./../bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='bar'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));
        Mockito.when(context.getResourceAsStream("/run/./../bar1.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='foo'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");

        assertTrue(element.getElementById("foo") != null);
    }

    @Test
    public void defaultParser_servletPathIsNotEmpty_doubleSlashIsRemovedFromRequest() {
        VaadinServletRequest request = (VaadinServletRequest) CurrentInstance
                .get(VaadinRequest.class);
        Mockito.when(resolver.resolveVaadinUri("/bar.html"))
                .thenReturn("/./../bar.html");
        Mockito.when(resolver.resolveVaadinUri("/bar1.html"))
                .thenReturn("/./../bar1.html");

        Mockito.when(request.getServletPath()).thenReturn("/run/");

        Mockito.when(context.getResourceAsStream("/run/./../bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='bar'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));
        Mockito.when(context.getResourceAsStream("/run/./../bar1.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<dom-module id='foo'></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");

        assertTrue(element.getElementById("foo") != null);

        // The double slash should be ignored e.g. `/run//./..` should become
        // `/run/./..`
        Mockito.verify(context).getResourceAsStream("/run/./../bar.html");
        Mockito.verify(context).getResourceAsStream("/run/./../bar1.html");
    }

    @Test
    public void defaultParser_removesComments() {
        Mockito.when(context.getResourceAsStream("/bar.html"))
                .thenReturn(new ByteArrayInputStream(
                        "<!-- comment1 --><dom-module id='foo'><!-- comment2 --></dom-module>"
                                .getBytes(StandardCharsets.UTF_8)));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");
        assertTrue(element.getElementById("foo") != null);
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

    @Test(expected = IllegalStateException.class)
    public void defaultParser_noTemplate_throws() {
        DefaultTemplateParser parser = new DefaultTemplateParser();
        parser.getTemplateContent(ImportsInspectTemplate.class, "bar1");
    }

    @Test(expected = IllegalStateException.class)
    public void defaultParser_templateResourceIsNotFound_throws() {
        Mockito.when(context.getResourceAsStream("/bar1.html"))
                .thenReturn(null);

        DefaultTemplateParser parser = new DefaultTemplateParser();
        parser.getTemplateContent(ImportsInspectTemplate.class, "foo");
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

        Mockito.when(service.getDependencyFilters())
                .thenReturn(Collections.singletonList(filter));

        DefaultTemplateParser parser = new DefaultTemplateParser();

        Element element = parser
                .getTemplateContent(ImportsInspectTemplate.class, "foo");
        assertTrue(element.getElementById("foo") != null);

        element = parser.getTemplateContent(RootImportsInspectTemplate.class,
                "foo");
        assertTrue(element.getElementById("foo") != null);

        element = parser.getTemplateContent(OtherImportsInspectTemplate.class,
                "bar");
        assertTrue(element.getElementById("bar") != null);

        assertEquals(
                "The DependencyFilter should be called exactly 3 times", 3,
                calls.get());

    }

    @Test(expected = IllegalStateException.class)
    public void defaultParser_useDependencyFilters_noDependencies_throws() {
        DependencyFilter filter = (list, context) -> {
            list.clear();
            return list;
        };

        Mockito.when(service.getDependencyFilters())
                .thenReturn(Collections.singletonList(filter));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        parser.getTemplateContent(ImportsInspectTemplate.class, "foo");
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

        Mockito.when(service.getDependencyFilters())
                .thenReturn(Collections.singletonList(filter));

        DefaultTemplateParser parser = new DefaultTemplateParser();
        parser.getTemplateContent(ImportsInspectTemplate.class, "foo");
    }

}
