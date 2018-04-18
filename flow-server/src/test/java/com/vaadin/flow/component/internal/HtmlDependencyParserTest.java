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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServlet;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class HtmlDependencyParserTest {

    private VaadinSession session;
    private TestVaadinServlet servlet;
    private TestVaadinServletService service;
    private MockServletServiceSessionSetup mocks;

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        servlet = mocks.getServlet();
        session = mocks.getSession();
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void mainImportDoesntHaveContentToParse_mainInportIsReturnedAndNoExceptions() {
        String root = "foo.html";

        servlet.addServletContextResource("/frontend/foo.html", "");

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

        String importContent = "<link rel='import' href='relative1.html'>"
                + "<link rel='import' href='foo/../relative1.html'>"
                + "<link rel='import' href='../relative2.html'>"
                + "<link rel='import' href='sub/relative3.html'>"
                + "<link rel='import' href='/absolute.html'>";
        servlet.addServletContextResource("/frontend/" + root, importContent);

        servlet.addServletContextResource("/frontend/baz/relative1.html", "");
        servlet.addServletContextResource("/frontend/relative2.html", "");
        servlet.addServletContextResource("/frontend/baz/sub/relative3.html",
                "");
        servlet.addServletContextResource("/absolute.html", "");

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(5, dependencies.size());
        servlet.verifyServletContextResourceLoadedOnce("/frontend/" + root);
        servlet.verifyServletContextResourceLoadedOnce(
                "/frontend/baz/relative1.html");
        servlet.verifyServletContextResourceLoadedOnce(
                "/frontend/relative2.html");
        servlet.verifyServletContextResourceLoadedOnce(
                "/frontend/baz/sub/relative3.html");
        servlet.verifyServletContextResourceLoadedOnce("/absolute.html");

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains("frontend://" + root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("frontend://baz/relative1.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located in the parent folder",
                dependencies.contains("frontend://relative2.html"));
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

        String importContent = "<link rel='import' href='relative.html'>";
        servlet.addServletContextResource("/frontend/foo.html", importContent);
        servlet.addServletContextResource("/frontend/relative.html", "");

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

        String importContent = "<link rel='import' href='relative.html'>";
        servlet.addServletContextResource("/frontend/foo.html", importContent);
        servlet.addServletContextResource("/frontend/relative.html",
                "<link rel='import' href='relative1.html'>");
        servlet.addServletContextResource("/frontend/relative1.html", "");

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

    @Test
    public void dependenciesWithDifferentPathsIncludedOnlyOnce() {
        String root = "foo.html";
        HtmlDependencyParser parser = new HtmlDependencyParser(root);

        String importContent = "<link rel='import' href='relative.html'>"
                + "<link rel='import' href='foo/../relative.html'>"
                + "<link rel='import' href='./relative.html'>";

        servlet.addServletContextResource("/frontend/foo.html", importContent);
        servlet.addServletContextResource("/frontend/relative.html", "");

        Collection<String> dependencies = parser.parseDependencies();

        Assert.assertEquals(2, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains("frontend://" + root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI",
                dependencies.contains("frontend://relative.html"));
    }

    @Test
    public void normalizeURI() throws Exception {
        assertEquals("http://foo/bar", normalize("http://foo/bar"));
        assertEquals("http://foo/../bar", normalize("http://foo/../bar"));
        assertEquals("http://foo/bar", normalize("http://foo/baz/../bar"));

        for (String protocol : new String[] { "frontend", "context", "base" }) {
            assertEquals(protocol + "://foo/bar",
                    normalize(protocol + "://foo/bar"));
            assertEquals(protocol + "://bar",
                    normalize(protocol + "://foo/../bar"));
            assertEquals(protocol + "://foo/bar",
                    normalize(protocol + "://foo/baz/../bar"));
        }

    }

    private String normalize(String string) throws Exception {
        return HtmlDependencyParser.toNormalizedURI(new URI(string));
    }
}
