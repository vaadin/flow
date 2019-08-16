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
package com.vaadin.flow.component.internal;

import java.util.Collection;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServlet;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;

@NotThreadSafe
@Deprecated
public class HtmlDependencyParserTest {

    private TestVaadinServlet servlet;
    private MockServletServiceSessionSetup mocks;
    private TestVaadinServletService service;

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        servlet = mocks.getServlet();
        service = mocks.getService();
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);
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
        Collection<String> dependencies = parser.parseDependencies(service);
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

        Collection<String> dependencies = parser.parseDependencies(service);

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

        Collection<String> dependencies = parser.parseDependencies(service);

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

        Collection<String> dependencies = parser.parseDependencies(service);

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

        Collection<String> dependencies = parser.parseDependencies(service);

        Assert.assertEquals(2, dependencies.size());

        Assert.assertTrue("Dependencies parser doesn't return the root URI",
                dependencies.contains("frontend://" + root));
        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI",
                dependencies.contains("frontend://relative.html"));
    }
}
