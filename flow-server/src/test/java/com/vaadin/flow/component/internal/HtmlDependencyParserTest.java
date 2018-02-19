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
package com.vaadin.flow.component.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.internal.HtmlDependencyParser.HtmlDependenciesCache;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class HtmlDependencyParserTest {

    private VaadinSession session;
    private VaadinServlet servlet;
    private ServletContext context;
    private VaadinServletService service;

    @Before
    public void setUp() {
        service = Mockito.mock(VaadinServletService.class);
        CurrentInstance.set(VaadinService.class, service);

        servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(service.getServlet()).thenReturn(servlet);

        session = Mockito.mock(VaadinSession.class);
        CurrentInstance.set(VaadinSession.class, session);

        context = Mockito.mock(ServletContext.class);

        Mockito.when(servlet.getServletContext()).thenReturn(context);
        Mockito.when(session.hasLock()).thenReturn(true);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void mainImportDoesntHaveContentToParse_mainInportIsReturnedAndNoExceptions() {
        String root = "foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);
        Collection<String> dependencies = parser.parseDependencies();
        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains(root));
    }

    @Test
    public void oneLevelDependency_variousURIs_URIsAreCollectedCorrectly() {
        String root = "baz/foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        String resolvedRoot = "baz/bar/" + root;
        Mockito.when(servlet.resolveResource(root)).thenReturn(resolvedRoot);

        String importContent = "<link rel='import' href='relative1.html'>"
                + "<link rel='import' href='../relative2.html'>"
                + "<link rel='import' href='sub/relative3.html'>"
                + "<link rel='import' href='/absolute.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(context.getResourceAsStream(resolvedRoot))
                .thenReturn(stream);

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(5, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains(root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("baz/relative1.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the realtive URI which is located in the parent folder",
                dependencies.contains("relative2.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the realtive URI which is located sub folder",
                dependencies.contains("baz/sub/relative3.html"));
        Assert.assertTrue("Dependencies parser doesn't return the absolute URI",
                dependencies.contains("/absolute.html"));
    }

    @Test
    public void oneLevelDependency_frontendUri_URIsAreCollectedCorrectly() {
        String root = "frontend://foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        Mockito.when(servlet.resolveResource(root)).thenReturn(root);

        String importContent = "<link rel='import' href='relative.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(context.getResourceAsStream(root)).thenReturn(stream);

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(2, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains(root));
        Assert.assertTrue("Dependencies parser doesn't return the relative URI",
                dependencies.contains("frontend://relative.html"));
    }

    @Test
    public void nestedDependencyLevels_variousURIs_URIsAreCollectedCorrectly() {
        String root = "foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        String resolvedRoot = "baz/bar/" + root;
        Mockito.when(servlet.resolveResource(root)).thenReturn(resolvedRoot);

        String importContent = "<link rel='import' href='relative.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(context.getResourceAsStream(resolvedRoot))
                .thenReturn(stream);

        String resolvedRelative = "baz/bar/relative.html";
        Mockito.when(servlet.resolveResource("relative.html"))
                .thenReturn(resolvedRelative);

        InputStream relativeContent = new ByteArrayInputStream(
                "<link rel='import' href='relative1.html'>"
                        .getBytes(StandardCharsets.UTF_8));
        Mockito.when(context.getResourceAsStream(resolvedRelative))
                .thenReturn(relativeContent);

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(3, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains(root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("relative.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the realtive URI which is located in the parent folder",
                dependencies.contains("relative1.html"));
    }

    @Test
    public void dependenciesAreCached() {
        String root = "foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        String resolvedRoot = "baz/bar/" + root;
        Mockito.when(servlet.resolveResource(root)).thenReturn(resolvedRoot);

        String importContent = "<link rel='import' href='relative.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(context.getResourceAsStream(resolvedRoot))
                .thenReturn(stream);

        HtmlDependenciesCache cache = new HtmlDependenciesCache();
        Mockito.when(session.getAttribute(HtmlDependenciesCache.class))
                .thenReturn(cache);

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(2, dependencies.size());
        Mockito.verify(context).getResourceAsStream(resolvedRoot);

        // call one more time
        dependencies = parser.parseDependencies();
        // this time only root resource should be returned
        Assert.assertEquals(1, dependencies.size());
        // and there shouldn't be one more call for reading the content of the
        // import
        Mockito.verify(context).getResourceAsStream(resolvedRoot);
    }

}
