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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
    private VaadinServletService service;

    @Before
    public void setUp() {
        service = Mockito.mock(VaadinServletService.class);
        CurrentInstance.set(VaadinService.class, service);

        servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(service.getServlet()).thenReturn(servlet);

        session = Mockito.mock(VaadinSession.class);
        CurrentInstance.set(VaadinSession.class, session);

        Mockito.when(session.hasLock()).thenReturn(true);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void mainImportDoesntHaveContentToParse_mainInportIsReturnedAndNoExceptions() {
        String root = "foo.html";

        Mockito.when(servlet.resolveResource("frontend://foo.html"))
                .thenReturn("foo.html");
        Mockito.when(service.getResourceAsStream("foo.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        HtmlDependencyParser parser = new HtmlDependencyParser(root);
        Collection<String> dependencies = parser.parseDependencies();
        Assert.assertTrue(
                "Dependencies parser doesn't return the root URI but '"
                        + dependencies + "'",
                dependencies.contains("frontend://" + root));
    }

    @Test
    public void oneLevelDependency_variousURIs_URIsAreCollectedCorrectly() {
        String root = "baz/foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        String resolvedRoot = "baz/bar/" + root;
        Mockito.when(servlet.resolveResource("frontend://" + root))
                .thenReturn(resolvedRoot);

        String importContent = "<link rel='import' href='relative1.html'>"
                + "<link rel='import' href='../relative2.html'>"
                + "<link rel='import' href='sub/relative3.html'>"
                + "<link rel='import' href='/absolute.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(servlet.getResourceAsStream(resolvedRoot))
                .thenReturn(stream);

        Mockito.when(servlet.resolveResource("frontend://baz/relative1.html"))
                .thenReturn("baz/relative1.html");
        Mockito.when(servlet.getResourceAsStream("baz/relative1.html"))
                .thenReturn(new ByteArrayInputStream(
                        "".getBytes(StandardCharsets.UTF_8)));

        Mockito.when(
                servlet.resolveResource("frontend://baz/../relative2.html"))
                .thenReturn("relative2.html");
        Mockito.when(servlet.getResourceAsStream("relative2.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        Mockito.when(
                servlet.resolveResource("frontend://baz/sub/relative3.html"))
                .thenReturn("relative3.html");
        Mockito.when(servlet.getResourceAsStream("relative3.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        Mockito.when(servlet.resolveResource("/absolute.html"))
                .thenReturn("absolute.html");
        Mockito.when(servlet.getResourceAsStream("absolute.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(5, dependencies.size());

        Mockito.verify(servlet).getResourceAsStream(resolvedRoot);
        Mockito.verify(servlet).getResourceAsStream("baz/relative1.html");
        Mockito.verify(servlet).getResourceAsStream("relative2.html");
        Mockito.verify(servlet).getResourceAsStream("relative3.html");
        Mockito.verify(servlet).getResourceAsStream("absolute.html");

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains("frontend://" + root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("frontend://baz/relative1.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located in the parent folder",
                dependencies.contains("frontend://baz/../relative2.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located sub folder",
                dependencies.contains("frontend://baz/sub/relative3.html"));
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
        Mockito.when(servlet.getResourceAsStream(root)).thenReturn(stream);

        Mockito.when(servlet.resolveResource("frontend://relative.html"))
                .thenReturn("relative.html");
        Mockito.when(service.getResourceAsStream("relative.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

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
        Mockito.when(servlet.resolveResource("frontend://" + root))
                .thenReturn(resolvedRoot);

        String importContent = "<link rel='import' href='relative.html'>";
        InputStream stream = new ByteArrayInputStream(
                importContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(servlet.getResourceAsStream(resolvedRoot))
                .thenReturn(stream);

        String resolvedRelative = "baz/bar/relative.html";
        Mockito.when(servlet.resolveResource("frontend://relative.html"))
                .thenReturn(resolvedRelative);
        Mockito.when(servlet.resolveResource("frontend://relative1.html"))
                .thenReturn("relative1.html");

        InputStream relativeContent = new ByteArrayInputStream(
                "<link rel='import' href='relative1.html'>"
                        .getBytes(StandardCharsets.UTF_8));
        Mockito.when(servlet.getResourceAsStream(resolvedRelative))
                .thenReturn(relativeContent);
        Mockito.when(service.getResourceAsStream("relative1.html")).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(3, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains("frontend://" + root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("frontend://relative.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located in the parent folder",
                dependencies.contains("frontend://relative1.html"));
    }

}
